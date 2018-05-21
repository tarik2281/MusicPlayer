package com.example.musicplayer.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;

/**
 * Created by Tarik on 20.12.2015.
 */
public class BaseActivity extends AppCompatActivity {
    public static final String ACTION_ACTIVITY_CHANGED = "com.example.musicplayer.ACTIVITY_CHANGED";

    private static Class<?> sTopActivity;

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferencesRestored:
                    recreate();
                    break;
                case PreferenceChange:
                    if (PreferenceManager.KEY_APP_THEME.equals(data.key))
                        recreate();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager prefManager = PreferenceManager.getInstance();

        prefManager.applyCurrentTheme(this);

        super.onCreate(savedInstanceState);

        prefManager.addObserver(mPreferenceObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setActivityOnTop(getClass());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (sTopActivity == getClass())
            setActivityOnTop(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setSubtitle(@StringRes int title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setSubtitle(title);
    }

    public void setSubtitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setSubtitle(title);
    }

    protected void showBackButton(boolean show) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(show);
    }

    private void setActivityOnTop(Class<?> clazz) {
        sTopActivity = clazz;

        Intent intent = new Intent(ACTION_ACTIVITY_CHANGED);
        sendBroadcast(intent);
    }

    public static Class<?> getActivityOnTop() {
        return sTopActivity;
    }
}
