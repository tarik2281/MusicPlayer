package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Folder;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.fragments.options.FolderOptionsHandler;

/**
 * Created by Tarik on 06.08.2016.
 */
public class FolderFragment extends LibraryPagerFragment {

    public static final int TYPE = Util.HashFNV1a32("Folder");

    private static final int STARTING_FRAGMENT = 1;
    private static final Class[] FRAGMENTS = { FolderListFragment.class, SongListFragment.class };

    private Folder mFolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOptionsHandlerClass(FolderOptionsHandler.class);

        mFolder = MusicLibrary.getInstance().getFolderById(getObjectId());

        if (savedInstanceState == null)
            setCurrentItem(STARTING_FRAGMENT);
    }

    @Override
    public boolean hasBarMenu() {
        return true;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.bar_folder, menu);
    }

    @Override
    public String getTitle(Resources resources) {
        return mFolder != null ? mFolder.getName() : null;
    }

    @Override
    public int getTitleIcon() {
        return R.drawable.ic_folder_black_36dp;
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        super.onLibraryObjectChanged(type, id);

        mFolder = MusicLibrary.getInstance().getFolderById(id);
    }

    @Override
    protected Class[] getFragments() {
        return FRAGMENTS;
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                mFolder = MusicLibrary.getInstance().getFolderById(getObjectId());

                if (mFolder == null)
                    requestBack();

                break;
        }
    }
}
