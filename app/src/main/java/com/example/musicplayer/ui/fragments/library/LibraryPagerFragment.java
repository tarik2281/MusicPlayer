package com.example.musicplayer.ui.fragments.library;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.view.menu.MenuBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.adapters.PagerAdapter;
import com.example.musicplayer.ui.fragments.FragmentCreateListener;

/**
 * Created by 19tarik97 on 10.09.16.
 */
public abstract class LibraryPagerFragment extends LibraryFragment implements
        FragmentCreateListener, ViewPager.OnPageChangeListener {

    private static final String KEY_CURRENT_ITEM = "CURRENT_ITEM";

    private LibraryFragment[] mFragments;
    private PagerAdapter mAdapter;
    private ViewPager mViewPager;
    private LibraryBar mLibraryBar;

    private int mCurrentItem = 0;
    private int mAlbumFragIndex;
    private int mSongFragIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mFragments == null)
            mFragments = new LibraryFragment[getFragments().length];

        if (savedInstanceState != null)
            mCurrentItem = savedInstanceState.getInt(KEY_CURRENT_ITEM);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (usePager()) {
            mViewPager = new ViewPager(getContext());
            mViewPager.setId(R.id.pager);
            mViewPager.addOnPageChangeListener(this);

            if (mAdapter == null)
                invalidateFragments();

            if (savedInstanceState == null)
                mViewPager.setCurrentItem(mCurrentItem);

            return mViewPager;
        }
        else {
            View v = inflater.inflate(R.layout.fragment_pager, null);

            Menu menu = new MenuBuilder(getContext());
            onCreateBarMenu(getActivity().getMenuInflater(), menu);

            mLibraryBar = (LibraryBar)v.findViewById(R.id.library_bar);
            mLibraryBar.setCallback(this);
            mLibraryBar.setMenu(menu);
            mLibraryBar.setHighlightedItem(mCurrentItem);

            if (savedInstanceState == null)
                setCurrentFragment(mCurrentItem);

            return v;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mViewPager != null)
            outState.putInt(KEY_CURRENT_ITEM, mViewPager.getCurrentItem());
    }

    @Override
    public void onCreateFragment(Class<?> fragmentClass, Fragment fragment) {
        Class[] fragments = getFragments();

        if (mFragments == null)
            mFragments = new LibraryFragment[getFragments().length];

        for (int i = 0; i < fragments.length; i++) {
            if (fragmentClass == fragments[i]) {
                mFragments[i] = (LibraryFragment)fragment;
                mFragments[i].setLibraryObject(getObjectType(), getObjectId());
                mFragments[i].setWidth(getWidth());
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setCurrentItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            if (getCurrentItem() == mAlbumFragIndex) {
                if (mFragments[mAlbumFragIndex] != null)
                    ((AlbumGridFragment)mFragments[mAlbumFragIndex]).notifyDataSetChanged();
            }
            else if (getCurrentItem() == mSongFragIndex) {
                if (mFragments[mSongFragIndex] != null)
                    ((SongListFragment)mFragments[mSongFragIndex]).notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onStateChanged(int state) {
        if (mFragments != null) {
            for (LibraryFragment fragment : mFragments)
                if (fragment != null)
                    fragment.setState(state);
        }
    }

    @Override
    public void onBarItemClick(LibraryBar.Item item) {
        int position = item.getPosition();

        // minus one because of play/pause button
        if (usePager())
            position--;

        setCurrentItem(position);
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        if (mFragments != null) {
            for (LibraryFragment fragment : mFragments)
                if (fragment != null)
                    fragment.setLibraryObject(type, id);
        }
    }

    @Override
    public void onResize(int width) {
        if (mFragments != null) {
            for (LibraryFragment fragment : mFragments)
                if (fragment != null)
                    fragment.setWidth(width);
        }
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    public void setCurrentItem(int position) {
        if (mCurrentItem == position)
            return;

        mCurrentItem = position;

        if (usePager()) {
            setHighlightedBarItem(position);

            if (mViewPager != null)
                mViewPager.setCurrentItem(position);
        }
        else {
            if (mLibraryBar != null)
                mLibraryBar.setHighlightedItem(position);

            setCurrentFragment(position);
        }
    }

    protected abstract Class[] getFragments();

    protected void invalidateFragments() {
        mAlbumFragIndex = -1;
        mSongFragIndex = -1;

        Class[] fragments = getFragments();

        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] == AlbumGridFragment.class) {
                mAlbumFragIndex = i;
            } else if (fragments[i] == SongListFragment.class) {
                mSongFragIndex = i;
            }
        }

        mAdapter = new PagerAdapter(fragments, getChildFragmentManager());
        mFragments = new LibraryFragment[fragments.length];

        mViewPager.setAdapter(mAdapter);
    }

    // only when no pager (two pane)
    private void setCurrentFragment(int position) {
        LibraryFragment fragment = null;

        if (mFragments != null && mFragments[position] != null)
            fragment = mFragments[position];
        else {
            try {
                Class[] fragments = getFragments();

                fragment = (LibraryFragment)fragments[position].newInstance();
            }
            catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (fragment != null) {
            fragment.setWidth(getWidth());
            fragment.setLibraryObject(getObjectType(), getObjectId());
            getChildFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    private boolean usePager() {
        return getParent().isSinglePane();
    }
}
