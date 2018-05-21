package com.example.musicplayer.library;

import android.content.Context;

import com.example.musicplayer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by 19tar on 10.09.2017.
 */

class PresetsParser {
    private BufferedReader mReader;
    private String mLine;
    private String[] mValues;

    private static final int NAME = 0;
    private static final int BAND_GAINS = 2;

    public void open(Context context) {
        mReader = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.eq_presets)));
    }

    public boolean next() {
        mValues = null;

        try {
            do {
                mLine = mReader.readLine();
            }
            while (mLine != null && mLine.startsWith("#"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mLine != null) {
            mValues = mLine.split("\\|");
        }

        return mValues != null;
    }

    public String getName() {
        if (mValues != null)
            return mValues[NAME];
        return null;
    }

    public String getBandGains() {
        if (mValues != null)
            return mValues[BAND_GAINS];
        return null;
    }

    public void close() {
        try {
            if (mReader != null)
                mReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
