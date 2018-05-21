package com.example.musicplayer;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import java.lang.ref.WeakReference;

/**
 * Created by Tarik on 14.07.2016.
 */
public abstract class CountDownTimer {

    private static final int MSG = 1;

    private final long mMillisInFuture;
    private final long mCountdownInterval;
    private long mStopTimeInFuture;
    private long mPauseTime;

    private boolean mCancelled = false;

    private WeakHandler mHandler = new WeakHandler(this);

    public CountDownTimer(long millisInFuture, long countdownInterval) {
        mMillisInFuture = millisInFuture;
        mCountdownInterval = countdownInterval;
        mCancelled = true;
    }

    public void start() {
        long delta = mPauseTime == 0 ? mMillisInFuture : mPauseTime;

        mCancelled = false;
        onStateChanged(true);
        mStopTimeInFuture = SystemClock.elapsedRealtime() + delta;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
    }

    public void cancel() {
        mCancelled = true;
        mHandler.removeMessages(MSG);
    }

    public void pause() {
        mPauseTime = timeLeft();
        cancel();
        onStateChanged(false);
    }

    public void reset() {
        if (!mCancelled)
            throw new IllegalStateException("Cannot reset timer while it is running.");

        mPauseTime = 0;
    }

    public boolean isRunning() {
        return !mCancelled;
    }

    public boolean finished() {
        return timeLeft() <= 0;
    }

    public long timeLeft() {
        return isRunning() ? mStopTimeInFuture - SystemClock.elapsedRealtime() : mPauseTime;
    }

    public abstract void onTick(long millisUntilFinished);

    public abstract void onStateChanged(boolean isRunning);

    public abstract void onFinish();


    private static class WeakHandler extends Handler {

        private WeakReference<CountDownTimer> mReference;

        public WeakHandler(CountDownTimer reference) {
            mReference = new WeakReference<CountDownTimer>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            CountDownTimer timer = mReference.get();
            if (timer == null || timer.mCancelled)
                return;

            final long millisLeft = timer.timeLeft();

            if (millisLeft <= 0) {
                timer.onFinish();
            } else if (millisLeft < timer.mCountdownInterval) {
                sendMessageDelayed(obtainMessage(MSG), millisLeft);
            } else {
                long lastTickStart = SystemClock.elapsedRealtime();
                timer.onTick(millisLeft);

                long delay = lastTickStart + timer.mCountdownInterval - SystemClock.elapsedRealtime();

                while (delay < 0) delay += timer.mCountdownInterval;

                sendMessageDelayed(obtainMessage(MSG), delay);
            }
        }
    }
}
