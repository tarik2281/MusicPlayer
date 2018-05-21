package com.example.musicplayer.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.LibraryBarCreateCallback;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.ui.SideBar;
import com.example.musicplayer.ui.fragments.library.LibraryPagerFragment;

/**
 * Created by 19tarik97 on 10.09.16.
 */
public class MainFragment extends LibraryPagerFragment implements LibraryBarCreateCallback, LibraryBar.Callback {
    public static final int ARTIST_FRAGMENT = 0;
    public static final int ALBUM_FRAGMENT = 1;
    public static final int SONG_FRAGMENT = 2;
    public static final int GENRE_FRAGMENT = 3;
    public static final int FOLDER_FRAGMENT = 4;
    public static final int PLAYLIST_FRAGMENT = 5;
    public static final int PAGES_COUNT = 6;

    private SideBar mSideBar;
    private Class[] mFragmentClasses;
    private boolean mActive = false;

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (data.key.equals(PreferenceManager.KEY_START_PAGE_LAYOUT)) {
                        mFragmentClasses = null;
                        mSideBar.loadItems();

                        if (mActive)
                            invalidateFragments();
                    }
                    break;
            }
        }
    };

    public MainFragment() {
        mSideBar = new SideBar();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSideBar.loadItems();

        PreferenceManager.getInstance().addObserver(mPreferenceObserver);

        if (savedInstanceState == null)
            setCurrentItem(mSideBar.getStartingPosition());
    }

    @Override
    public void onStart() {
        super.onStart();

        mActive = true;

        if (mFragmentClasses == null) {
            getDelegate().invalidateBarMenu();
            invalidateFragments();
            setCurrentItem(mSideBar.getStartingPosition());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mActive = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    @Override
    public boolean hasBarMenu() {
        return true;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        mSideBar.toMenu(menu);
    }

    @Override
    protected Class[] getFragments() {
        if (mFragmentClasses == null) {
            int[] items = mSideBar.getItems();
            mFragmentClasses = new Class[items.length];

            for (int i = 0; i < items.length; i++)
                mFragmentClasses[i] = SideBar.getClassForItem(items[i]);
        }

        return mFragmentClasses;
    }
}
