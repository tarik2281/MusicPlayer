package com.example.musicplayer.library;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.library.database.SongStatsTable;

/**
 * Created by Tarik on 21.06.2016.
 */
public class LastPlayedPlaylist extends Playlist implements MusicLibrary.Observer {

    //private static final String KEY_MAX_ITEMS = "last_played_items_count";
    static final String NAME = "last_played";
    static final int RESOURCE_ID = R.string.playlist_last_played;

    //private int mMaxItems;

    LastPlayedPlaylist(int id, String name) {
        super(id, name, null, false);

        //mMaxItems = -1;

        MusicLibrary.getInstance().addObserver(this);
    }

    @Override
    public void load() {
        mSongs.clear();

        //if (mMaxItems == -1)
        //    mMaxItems = PreferenceManager.getInstance().getInt(KEY_MAX_ITEMS, 25);

        MusicLibrary.getInstance().getLastPlayedSongs(mSongs);
    }

    @Override
    public void save() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case SongPlayedUpdated:
                invalidate();
                break;
        }
    }

    /*@Override
    public int getValue() {
        return mMaxItems;
    }

    @Override
    public void setValue(int value) {
        if (value > 0) {
            mMaxItems = value;
            PreferenceManager.getInstance().setInt(KEY_MAX_ITEMS, mMaxItems, true);
            invalidate();
        }
    }*/

    /*@Override
    public int getEditingTitleRes() {
        return R.string.dialog_title_set_items_count;
    */
}
