package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * Created by 19tar on 13.09.2017.
 */

public class CacheManager {

    private static final String PREFERENCES_NAME = "cache_prefs";

    private static CacheManager sSingleton;

    private boolean mInited;
    private SharedPreferences mCachePreferences;


    private CacheManager() {

    }

    public void initialize(Context context) {
        if (!mInited) {
            mCachePreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

            mInited = true;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mCachePreferences.getBoolean(key, defaultValue);
    }

    public int getInteger(String key, int defaultValue) {
        return mCachePreferences.getInt(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return mCachePreferences.getString(key, defaultValue);
    }

    public Set<String> getStringSet(String key) {
        return mCachePreferences.getStringSet(key, null);
    }

    public void setBoolean(String key, boolean value) {
        mCachePreferences.edit().putBoolean(key, value).apply();
    }

    public void setInteger(String key, int value) {
        mCachePreferences.edit().putInt(key, value).apply();
    }

    public void setString(String key, String value) {
        mCachePreferences.edit().putString(key, value).apply();
    }

    public void setStringSet(String key, Set<String> value) {
        mCachePreferences.edit().putStringSet(key, value).apply();
    }

    public static CacheManager getInstance() {
        if (sSingleton == null)
            sSingleton = new CacheManager();

        return sSingleton;
    }
}
