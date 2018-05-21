package com.example.musicplayer.ui.activities;

import android.app.Activity;
import android.content.Intent;

import com.example.musicplayer.ui.fragments.GettingStartedFragment;
import com.example.musicplayer.ui.fragments.IntroThemeFragment;

/**
 * Created by TarikKaraca on 29.10.2017.
 */

public class IntroTabletActivity extends IntroActivity {

    private static final Class[] FRAGMENTS_TABLET = { IntroThemeFragment.class, GettingStartedFragment.class };

    @Override
    protected Class[] getFragments() {
        return FRAGMENTS_TABLET;
    }

    public static void startResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, IntroTabletActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }
}
