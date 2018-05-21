package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Genre;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.fragments.options.GenreOptionsHandler;

/**
 * Created by Tarik on 01.06.2016.
 */
public class GenreFragment extends LibraryPagerFragment implements LibraryBar.Callback {

    public static final int TYPE = Util.HashFNV1a32("Genre");

    private static final int STARTING_FRAGMENT = 2;
    private static final Class[] FRAGMENTS = {
            ArtistListFragment.class, AlbumGridFragment.class, SongListFragment.class
    };

    private Genre mGenre;

    public GenreFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGenre = MusicLibrary.getInstance().getGenreById(getObjectId());

        setOptionsHandlerClass(GenreOptionsHandler.class);

        if (savedInstanceState == null)
            setCurrentItem(STARTING_FRAGMENT);
    }

    @Override
    public boolean hasBarMenu() {
        return true;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.bar_genre, menu);
    }

    @Override
    protected Class[] getFragments() {
        return FRAGMENTS;
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return Genre.getName(mGenre);
    }

    @Override
    public int getTitleIcon() {
        return R.drawable.ic_straighten_black_36dp;
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                mGenre = MusicLibrary.getInstance().getGenreById(getObjectId());

                if (mGenre == null) {
                    requestBack();
                }

                break;
        }
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        super.onLibraryObjectChanged(type, id);

        mGenre = MusicLibrary.getInstance().getGenreById(id);
    }
}
