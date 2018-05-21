package com.example.musicplayer.ui.fragments;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.Observable;
import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.SideBar;
import com.example.musicplayer.ui.adapters.MenuAdapter;
import com.example.musicplayer.ui.fragments.library.LibraryAdapterFragment;

/**
 * Created by Tarik on 27.05.2016.
 */
public class MenuFragment extends PaneFragment implements
        OnItemClickListener {

    private static final String KEY_HIGHLIGHTED = "HIGHLIGHTED";

    private SideBar mSideBar;
    private MenuAdapter mAdapter;
    private boolean mActive;
    private boolean mUpdated = false;

    private LibraryAdapterFragment mLibraryFragment;

    private PreferenceManager.Observer mPrefsObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (PreferenceManager.KEY_START_PAGE_LAYOUT.equals(data.key)) {
                        mSideBar.loadItems();
                        updateMenu();
                        mAdapter.notifyDataSetChanged();

                        if (mActive) {
                            setFragment(mSideBar.getStartingPosition());
                        }
                        else
                            mUpdated = true;
                    }
                    break;
            }
        }
    };

    public MenuFragment() {
        mSideBar = new SideBar();
        mActive = false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSideBar.loadItems();

        PreferenceManager.getInstance().addObserver(mPrefsObserver);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_menu, container, false);

        RecyclerView recyclerView = (RecyclerView)v.findViewById(R.id.list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new MenuAdapter(null);
        mAdapter.setOnItemClickListener(this);

        updateMenu();

        recyclerView.setAdapter(mAdapter);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TwoPaneLayout parent = getParent();

        if (parent.getRightFragment() == null) {
            setFragment(mSideBar.getStartingPosition());
        }
        else {
            PaneFragment fragment = parent.findNextFragment(this);
            if (fragment != null)
                mLibraryFragment = (LibraryAdapterFragment) fragment;
        }

        if (savedInstanceState != null) {
            mAdapter.setHighlightedItem(savedInstanceState.getInt(KEY_HIGHLIGHTED));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mActive = true;

        if (mUpdated) {
            setFragment(mSideBar.getStartingPosition());
            mUpdated = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mActive = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_HIGHLIGHTED, mAdapter.getHighlightedItem());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPrefsObserver);
    }

    @Override
    public String getTitle(Resources resources) {
        return "Menu";
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        if (mAdapter.getHighlightedItem() == position)
            return;

        setFragment(position);
    }

    private void setFragment(int position) {
        Class fragClass = SideBar.getClassForItem(mSideBar.getItems()[position]);

        try {
            mLibraryFragment = (LibraryAdapterFragment) fragClass.newInstance();
        }
        catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        TwoPaneLayout.Entry entry = getParent().findNextEntry(this);
        if (entry != null)
            entry.setFragment(mLibraryFragment);
        else
            requestNextFragment(mLibraryFragment);

        mAdapter.setHighlightedItem(position);
    }

    private void updateMenu() {
        @SuppressLint("RestrictedApi") Menu menu = new MenuBuilder(getContext());
        mSideBar.toMenu(menu);
        mAdapter.setMenu(menu);
    }
}
