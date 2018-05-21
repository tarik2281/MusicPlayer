package com.example.musicplayer.library;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.library.database.SongStatsTable;

/**
 * Created by Tarik on 21.06.2016.
 */
public class MostPlayedPlaylist extends Playlist implements MusicLibrary.Observer {

    //private static final String KEY_ITEMS_COUNT = "most_played_items_count";

    static final String NAME = "most_played";
    static final int RESOURCE_ID = R.string.playlist_most_played;

    //private int mMaxItems;

    MostPlayedPlaylist(int id, String name) {
        super(id, name, null, false);

        //mMaxItems = -1;

        MusicLibrary.getInstance().addObserver(this);
    }

    @Override
    public void load() {
        mSongs.clear();

        //if (mMaxItems == -1)
        //    mMaxItems = PreferenceManager.getInstance().getInt(KEY_ITEMS_COUNT, 25);

        MusicLibrary.getInstance().getMostPlayedSongs(mSongs);
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
            PreferenceManager.getInstance().setInt(KEY_ITEMS_COUNT, value, true);
            invalidate();
        }
    }*/

    /*@Override
    public int getEditingTitleRes() {
        return R.string.dialog_title_set_items_count;
    }*/
}
