package com.example.musicplayer.ui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by 19tarik97 on 30.01.16.
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    private Class<?>[] mFragmentClasses;

    public PagerAdapter(Class<?>[] fragments, FragmentManager fm) {
        super(fm);
        mFragmentClasses = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        try {
            return (Fragment) mFragmentClasses[position].newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getCount() {
        return mFragmentClasses.length;
    }
}
