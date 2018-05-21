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

public class IntroCoverFragment extends Fragment {

    private IntroChooserDelegate mDelegate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_intro_theme, null);

        mDelegate = new IntroChooserDelegate(v, getContext(), savedInstanceState);

        mDelegate.setWelcomeVisible(false);
        mDelegate.setInstructionText(R.string.intro_instruction_cover);
        mDelegate.setLeftDrawable(R.drawable.screen_full_sized);
        mDelegate.setRightDrawable(R.drawable.screen_small);
        mDelegate.setLeftText(R.string.pref_album_cover_size_full);
        mDelegate.setRightText(R.string.pref_album_cover_size_small);

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
                value = PreferenceManager.COVER_FULL_SIZED;
                break;
            case IntroChooserDelegate.RIGHT:
                value = PreferenceManager.COVER_SMALL;
                break;
        }

        PreferenceManager.getInstance().setAlbumCoverType(value);
    }
}
