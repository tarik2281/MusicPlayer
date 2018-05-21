package com.example.musicplayer.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.example.musicplayer.CacheManager;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.adapters.PagerAdapter;
import com.example.musicplayer.ui.fragments.GettingStartedFragment;
import com.example.musicplayer.ui.fragments.IntroCoverFragment;
import com.example.musicplayer.ui.fragments.IntroThemeFragment;

/**
 * Created by 19tar on 04.09.2017.
 */

public class IntroActivity extends AppCompatActivity {

    public static final String CACHE_INTRO_DONE = "intro_done";

    private static final Class[] FRAGMENTS = { IntroThemeFragment.class, IntroCoverFragment.class, GettingStartedFragment.class };

    private ViewPager mPager;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);

        mPager = (ViewPager)findViewById(R.id.pager);
        mTabLayout = (TabLayout)findViewById(R.id.tab_layout);

        mPager.setOffscreenPageLimit(2);

        Class[] fragments = getFragments();

        PagerAdapter adapter = new PagerAdapter(fragments, getSupportFragmentManager());
        mPager.setAdapter(adapter);

        mTabLayout.setupWithViewPager(mPager, true);
    }

    protected Class[] getFragments() {
        return FRAGMENTS;
    }

    public void finishSuccess() {
        setResult(RESULT_OK);
        CacheManager.getInstance().setBoolean(CACHE_INTRO_DONE, true);
        finish();
    }

    public static void startResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, IntroActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }
}
