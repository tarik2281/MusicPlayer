package com.example.musicplayer.playback;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.Util;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by Tarik on 15.11.2015.
 */
public class MediaPlayer {

    private static native int nNewInstance(int sampleRate);
    private static native void nReleaseInstance(int handle);
    private static native void nSetNextData(int handle, String path, int stateHandle, boolean forceClear);
    private static native void nSetFinishCallback(int handle, Runnable run);
    private static native void nSetErrorCallback(int handle, ErrorCallback callback);
    private static native void nStartDecodeThread(int handle);
    private static native void nStopDecodeThread(int handle);
    private static native void nSeek(int handle, long offset);
    private static native long nGetCurrentPosition(int handle);
    private static native boolean nFillStreamArray(int handle, byte[] array);
    private static native boolean nFillStreamBuffer(int handle, ByteBuffer buffer);

    public interface CompletionListener {
        void onComplete();
    }

    public interface ErrorListener {
        void onError(int errorCode, Object[] args);
    }

    public interface FadeListener {
        void onFadeEnd();
    }

    private class ErrorCallback {
        public void onError(int errorCode, Object[] args) {
            Message msg = mHandler.obtainMessage(MESSAGE_ERROR, errorCode, 0, args);
            mHandler.sendMessage(msg);
        }
    }

    public static final int ERROR_PLAYER_INIT = -10;

    /**
     * extra args: String filePath
     */
    public static final int ERROR_INVALID_FILE = -1;

    private static final int MESSAGE_COMPLETE = 1;
    private static final int MESSAGE_ERROR = 2;

    private static final int DEFAULT_FRAMES_PER_BUFFER = 256;
    private static final int DEFAULT_SAMPLE_RATE = 44100;

    private static final int BYTES_PER_SAMPLE = 2; // 16 bit signed pcm
    private static final int NUM_OUT_CHANNELS = 2; // stereo

    private static final float FADE_DURATION = 0.25f; // in seconds

    private int mHandle;
    private WeakHandler mHandler;

    private AudioTrack mAudioTrack;
    private byte[] mStreamArray;
    private ByteBuffer mStreamBuffer;

    private CompletionListener mCompletionListener;

    private ErrorCallback mErrorCallback;
    private ErrorListener mErrorListener;

    private boolean mIsPlaying;
    private boolean mIsRunning;
    private boolean mFinished = true;

    private long mFadeStart;
    private float mCurrentVolume;
    private float mFadeVolumeFrom;
    private float mFadeVolumeTo;
    private float mFadeScale;

    private boolean mPauseAfterFade;

    private Runnable mFinishCallback = new Runnable() {
        @Override
        public void run() {
            Message msg = mHandler.obtainMessage(MESSAGE_COMPLETE);
            mHandler.sendMessage(msg);
        }
    };

    private Runnable mStreamThreadRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (MediaPlayer.class) {
                mFinished = false;
            }

            boolean isRunning;
            boolean isPlaying;
            boolean pauseAfterFade;
            long fadeStart;
            float currentVolume, fadeVolumeFrom, fadeVolumeTo, fadeScale;

            do {

                synchronized (MediaPlayer.this) {
                    isRunning = mIsRunning;
                    isPlaying = mIsPlaying;
                    pauseAfterFade = mPauseAfterFade;
                    fadeStart = mFadeStart;
                    fadeVolumeFrom = mFadeVolumeFrom;
                    fadeVolumeTo = mFadeVolumeTo;
                    fadeScale = mFadeScale;
                }

                if (fadeStart >= 0) {
                    // fade calculation depending on time
                    currentVolume = fadeVolumeFrom + (SystemClock.elapsedRealtime() - fadeStart) * fadeScale;

                    boolean fadeIn = fadeVolumeTo > fadeVolumeFrom;
                    if ((fadeIn && currentVolume >= fadeVolumeTo) || (!fadeIn && currentVolume <= fadeVolumeTo)) {
                        // fade ended
                        currentVolume = fadeVolumeTo;
                        fadeStart = -1;

                        if (pauseAfterFade) {
                            mAudioTrack.stop();
                            isPlaying = false;
                        }

                        pauseAfterFade = false;
                    }

                    setVolume(mAudioTrack, currentVolume);

                    synchronized (MediaPlayer.this) {
                        mIsPlaying = isPlaying;
                        mCurrentVolume = currentVolume;
                        mFadeStart = fadeStart;
                        mPauseAfterFade = pauseAfterFade;
                    }
                }

                if (isPlaying) {
                    if (audioTrackNewApi()) {
                        if (nFillStreamBuffer(mHandle, mStreamBuffer)) {
                            mAudioTrack.write(mStreamBuffer, mStreamBuffer.capacity(), AudioTrack.WRITE_BLOCKING);
                            mStreamBuffer.rewind();
                        }
                    }
                    else {
                        if (nFillStreamArray(mHandle, mStreamArray))
                            mAudioTrack.write(mStreamArray, 0, mStreamArray.length);
                    }
                }

            } while (isRunning);

            synchronized (MediaPlayer.class) {
                mFinished = true;
                MediaPlayer.class.notifyAll();
            }
        }
    };

    public MediaPlayer() {
        mHandle = 0;
        mCompletionListener = null;
        mErrorListener = null;
        mCurrentVolume = 1.0f;
        mFadeStart = -1;
        mFinished = true;

        mErrorCallback = new ErrorCallback();
        mHandler = new WeakHandler(this);
    }

    public int initialize(Context context) {
        if (!MusicApplication.isNativeLoaded()) {
            Util.loge("MediaPlayer", "initialize: Could not create MediaPlayer instance: Library not loaded.");
            return ERROR_PLAYER_INIT;
        }

        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int framesPerBuffer = getFramesPerBuffer(am);
        int sampleRate = getSampleRate(am);

        if ((mHandle = nNewInstance(sampleRate)) == 0) {
            Util.loge("MediaPlayer", "initialize: Could not initialize audio player");
            return ERROR_PLAYER_INIT;
        }

        int bufferSize = framesPerBuffer * NUM_OUT_CHANNELS * BYTES_PER_SAMPLE;

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        if (audioTrackNewApi())
            mStreamBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            mStreamArray = new byte[bufferSize];

        nSetFinishCallback(mHandle, mFinishCallback);
        nSetErrorCallback(mHandle, mErrorCallback);

        return 0;
    }

    public void release() {
        synchronized (MediaPlayer.class) {
            while (!mFinished) {
                try {
                    MediaPlayer.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mHandle != 0) {
            mAudioTrack.release();
            nReleaseInstance(mHandle);
        }

        mAudioTrack = null;
        mHandle = 0;
    }

    public void setCompletionListener(CompletionListener listener) {
        mCompletionListener = listener;
    }

    public void setErrorListener(ErrorListener listener) {
        mErrorListener = listener;
    }

    public void setNextData(String path, FilterState state, boolean forceClear) {
        if (mHandle != 0) {
            nSetNextData(mHandle, path, state.getHandle(), forceClear);

            if (forceClear && !mIsPlaying)
                mAudioTrack.flush();
        }
    }

    public void play(boolean fade) {
        if (mHandle != 0) {
            mAudioTrack.play();

            synchronized (MediaPlayer.this) {
                mIsPlaying = true;
                mPauseAfterFade = false;

                if (!mIsRunning) {
                    mIsRunning = true;
                    new Thread(mStreamThreadRunnable).start();
                }

                if (fade)
                    fadeVolumeUnsafe(1.0f, FADE_DURATION);
                else {
                    setVolume(mAudioTrack, 1.0f);
                    mCurrentVolume = 1.0f;
                }
            }

            nStartDecodeThread(mHandle);
        }
    }

    public void pause(boolean fade) {
        if (mHandle != 0) {
            synchronized (MediaPlayer.this) {
                if (fade) {
                    mPauseAfterFade = true;
                    fadeVolumeUnsafe(0.0f, FADE_DURATION);
                }
                else
                    mIsPlaying = false;
            }

            if (!fade)
                mAudioTrack.stop();
        }
    }

    public void stop() {
        synchronized (MediaPlayer.this) {
            if (mIsRunning)
                mIsRunning = false;
            mIsPlaying = false;
        }

        if (mHandle != 0)
            nStopDecodeThread(mHandle);
    }

    public void seek(long offset) {
        if (mHandle != 0)
            nSeek(mHandle, offset);
    }

    public long getCurrentPosition() {
        if (mHandle == 0)
            return 0;
        return nGetCurrentPosition(mHandle);
    }

    /**
     *
     * @param volume
     * @param duration in seconds
     */
    public void fadeVolume(float volume, float duration) {
        synchronized (MediaPlayer.this) {
            if (!mPauseAfterFade)
                fadeVolumeUnsafe(volume, duration);
        }
    }

    private void fadeVolumeUnsafe(float volume, float duration) {
        mFadeStart = SystemClock.elapsedRealtime();
        mFadeVolumeFrom = mCurrentVolume;
        mFadeVolumeTo = volume;
        mFadeScale = (mFadeVolumeTo - mFadeVolumeFrom) / 1000.0f / duration;
    }


    private static int getFramesPerBuffer(AudioManager manager) {
        if (isPropertySupported()) {
            int res = Integer.parseInt(manager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));

            if (res != 0)
                return res;
        }

        return DEFAULT_FRAMES_PER_BUFFER;
    }

    private static int getSampleRate(AudioManager manager) {
        if (isPropertySupported()) {
            int res = Integer.parseInt(manager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));

            if (res != 0)
                return res;
        }

        return DEFAULT_SAMPLE_RATE;
    }

    private static boolean isPropertySupported() {
        return Build.VERSION.SDK_INT >= 17;
    }

    private static boolean audioTrackNewApi() {
        return Build.VERSION.SDK_INT >= 21;
    }

    private static void setVolume(AudioTrack track, float volume) {
        if (audioTrackNewApi())
            track.setVolume(volume);
        else
            track.setStereoVolume(volume, volume);
    }

    private static class WeakHandler extends Handler {
        private WeakReference<MediaPlayer> mReference;

        public WeakHandler(MediaPlayer mediaPlayer) {
            mReference = new WeakReference<MediaPlayer>(mediaPlayer);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaPlayer player = mReference.get();
            if (player == null)
                return;

            switch (msg.what) {
                case MESSAGE_ERROR:
                    if (player.mErrorListener != null)
                        player.mErrorListener.onError(msg.arg1, (Object[])msg.obj);
                    break;
                case MESSAGE_COMPLETE:
                    if (player.mCompletionListener != null)
                        player.mCompletionListener.onComplete();
                    break;
            }
        }
    }
}
