package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Artist;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.fragments.options.ArtistOptionsHandler;

/**
 * Created by Tarik on 01.06.2016.
 */
public class ArtistFragment extends LibraryPagerFragment {

    public static final int TYPE = Util.HashFNV1a32("Artist");

    private static final int STARTING_FRAGMENT = 1;
    private static final Class[] FRAGMENTS = { AlbumGridFragment.class, SongListFragment.class };

    private Artist mArtist;

    public ArtistFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mArtist = MusicLibrary.getInstance().getArtistById(getObjectId());

        setOptionsHandlerClass(ArtistOptionsHandler.class);

        if (savedInstanceState == null)
            setCurrentItem(STARTING_FRAGMENT);
    }

    @Override
    public int getTitleIcon() {
        return R.drawable.ic_mic_black_36dp;
    }

    @Override
    public boolean hasBarMenu() {
        return true;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.bar_artist, menu);
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return Artist.getName(mArtist);
    }

    @Override
    protected Class[] getFragments() {
        return FRAGMENTS;
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        super.onLibraryObjectChanged(type, id);

        mArtist = MusicLibrary.getInstance().getArtistById(id);
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                mArtist = MusicLibrary.getInstance().getArtistById(getObjectId());

                if (mArtist == null) {
                    requestBack();
                }

                break;
        }
    }
}
