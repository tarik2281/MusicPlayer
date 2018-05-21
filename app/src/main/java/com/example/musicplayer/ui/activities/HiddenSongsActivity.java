package com.example.musicplayer.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.fragments.library.HiddenSongsFragment;

public class HiddenSongsActivity extends BaseActivity {

    private static final String TAG_FRAGMENT = "FRAGMENT";

    private HiddenSongsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showBackButton(true);

        if (savedInstanceState == null) {
            mFragment = new HiddenSongsFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFragment, TAG_FRAGMENT).commit();
        }
        else {
            mFragment = (HiddenSongsFragment)getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mFragment.hasChanged()) {
            MusicLibrary.getInstance().unhideSongsAsync();
        }
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, HiddenSongsActivity.class);
        context.startActivity(starter);
    }
}
