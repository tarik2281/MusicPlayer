package com.example.musicplayer.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;

/**
 * Created by Tarik on 16.10.2017.
 */

public class IntroThemeFragment extends Fragment {

    private IntroChooserDelegate mDelegate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_intro_theme, null);

        mDelegate = new IntroChooserDelegate(v, getContext(), savedInstanceState);

        mDelegate.setWelcomeVisible(true);
        mDelegate.setInstructionText(R.string.intro_instruction_theme);

        if (getResources().getBoolean(R.bool.isTablet)) {
            mDelegate.setLeftDrawable(R.drawable.screen_light_tablet);
            mDelegate.setRightDrawable(R.drawable.screen_dark_tablet);
        }
        else {
            mDelegate.setLeftDrawable(R.drawable.screen_light);
            mDelegate.setRightDrawable(R.drawable.screen_dark);
        }

        mDelegate.setLeftText(R.string.pref_theme_light);
        mDelegate.setRightText(R.string.pref_theme_dark);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mDelegate.saveState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        String value = null;
        switch (mDelegate.getSelection()) {
            case IntroChooserDelegate.LEFT:
                value = PreferenceManager.THEME_LIGHT;
                break;
            case IntroChooserDelegate.RIGHT:
                value = PreferenceManager.THEME_DARK;
                break;
        }

        PreferenceManager.getInstance().setAppTheme(value);
    }
}
