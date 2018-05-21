package com.example.musicplayer.library;

import android.support.annotation.NonNull;

import com.example.musicplayer.Util;

import java.util.Locale;

/**
 * Created by 19tarik97 on 10.12.16.
 */

public class EqualizerPreset implements LibraryObject, Comparable<EqualizerPreset> {

    public static final int NUM_GAINS = 10;

    private int mId;
    private String mName;
    private String mBandGainsStr;
    private float[] mBandGains;
    private boolean mPrebuilt;
    String mResourceName;

    public EqualizerPreset(int id, String name, String bandGains, boolean prebuilt) {
        mId = id;
        mName = name;
        mBandGainsStr = bandGains;
        mPrebuilt = prebuilt;
    }

    @Override
    public int compareTo(@NonNull EqualizerPreset another) {
        int res = Util.boolCompare(mPrebuilt, another.mPrebuilt);

        if (res == 0)
            res = Util.stringCompare(mName, another.mName);

        if (res == 0)
            res = Util.intCompare(mId, another.mId);

        return res;
    }

    @Override
    public int getType() {
        return LibraryObject.PRESET;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public float[] getBandGains() {
        if (mBandGains == null) {
            mBandGains = new float[NUM_GAINS];
            parseBandGains(mBandGainsStr);
        }

        return mBandGains;
    }

    public String getGainsStr() {
        if (mBandGainsStr == null)
            mBandGainsStr = getGainsStr(mBandGains);

        return mBandGainsStr;
    }

    public boolean isPrebuilt() {
        return mPrebuilt;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setBandGains(String gains) {
        mBandGainsStr = gains;

        if (mBandGains != null)
            parseBandGains(mBandGainsStr);
    }

    private void parseBandGains(String gainsStr) {
        if (Util.stringIsEmpty(gainsStr))
            return;

        String[] parts = gainsStr.split(";");
        for (int i = 0; i < NUM_GAINS; i++) {
            mBandGains[i] = Float.parseFloat(parts[i]);
        }
    }


    public static String getGainsStr(float[] bandGains) {
        return String.format(Locale.US, "%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
                bandGains[0], bandGains[1], bandGains[2], bandGains[3], bandGains[4],
                bandGains[5], bandGains[6], bandGains[7], bandGains[8], bandGains[9]);
    }

    @Override
    public String toString() {
        return mName;
    }
}
