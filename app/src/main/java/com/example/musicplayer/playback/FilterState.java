package com.example.musicplayer.playback;

import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.Util;
import com.example.musicplayer.io.Decoder;

import java.util.LinkedList;

/**
 * Created by Tarik on 18.07.2016.
 */
public class FilterState {

    public static class Factory {

        private static Factory sSingleton;

        private LinkedList<FilterState> mStack;

        private Factory() {
            mStack = new LinkedList<>();
        }

        public static FilterState getFilterState() {
            if (sSingleton == null)
                sSingleton = new Factory();

            if (sSingleton.mStack.isEmpty()) {
                FilterState state = new FilterState();
                state.mEqualizerEnabled = false;
                state.mEqualizerGains = new float[NUM_EQ_ARGS];
                state.mCompressorArgs = new double[NUM_COMP_ARGS];
                return state;
            }
            else {
                return sSingleton.mStack.pop();
            }
        }

        public static void releaseFilterState(FilterState state) {
            if (sSingleton == null)
                sSingleton = new Factory();

            sSingleton.mStack.push(state);
        }

        public static void release() {
            if (sSingleton != null) {
                while (!sSingleton.mStack.isEmpty()) {
                    FilterState state = sSingleton.mStack.pop();
                    state.releaseResources();
                }
            }
        }
    }

    private static native int nNewInstance();
    private static native void nReleaseInstance(int handle);
    private static native void nSetEqualizer(int handle, boolean enabled, float[] gains);
    private static native void nSetCompressor(int handle, boolean enabled, double[] args);

    public static final int NUM_EQ_ARGS = 12;
    public static final int NUM_COMP_ARGS = 10;

    private int mHandle;

    private boolean mEqualizerEnabled;
    private float[] mEqualizerGains;

    private double[] mCompressorArgs;

    private FilterState() {
        if (MusicApplication.isNativeLoaded())
            mHandle = nNewInstance();
        else
            Util.loge("FilterState", "FilterState: Could not create FilterState instance: Library not loaded");
    }

    public int getHandle() {
        return mHandle;
    }

    public void release() {
        Factory.releaseFilterState(this);
    }

    private void releaseResources() {
        if (mHandle != 0)
            nReleaseInstance(mHandle);
        mHandle = 0;
    }

    public float getBassGain() {
        if (mEqualizerGains != null)
            return mEqualizerGains[10];

        return 0.0f;
    }

    public float getTrebleGain() {
        if (mEqualizerGains != null)
            return mEqualizerGains[11];

        return 0.0f;
    }

    public void setEqualizer(boolean enabled, float[] gains, float bass, float treble) {
        double thresholdDB = 0.0;
        double extraThreshold = Math.max(Math.max(0.0, bass), treble);

        for (int i = 0; i < 10; i++)
            thresholdDB = Math.max(thresholdDB, gains[i]);

        mCompressorArgs[0] = 1.0;
        mCompressorArgs[1] = Math.pow(10.0, -(thresholdDB + extraThreshold + 2.0) / 20.0);
        mCompressorArgs[2] = 0.01;
        mCompressorArgs[3] = 250.0;
        mCompressorArgs[4] = 2.82843;
        mCompressorArgs[5] = 10.0;
        mCompressorArgs[6] = 1.0;
        mCompressorArgs[7] = 0.0;
        mCompressorArgs[8] = 1.0;
        mCompressorArgs[9] = 1.0;

        if (gains != mEqualizerGains)
            System.arraycopy(gains, 0, mEqualizerGains, 0, 10);
        mEqualizerGains[10] = bass;
        mEqualizerGains[11] = treble;

        if (mHandle != 0) {
            nSetEqualizer(mHandle, enabled, mEqualizerGains);
            nSetCompressor(mHandle, enabled, mCompressorArgs);
        }

        mEqualizerEnabled = enabled;
    }

    public boolean isEqualizerEnabled() {
        return mEqualizerEnabled;
    }

    public float[] getEqualizerGains() {
        return mEqualizerGains;
    }
}
