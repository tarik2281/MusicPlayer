package com.example.musicplayer.library;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.library.database.SongsTable;

/**
 * Created by 19tarik97 on 22.03.16.
 */
public class RecentlyAddedPlaylist extends Playlist implements MusicLibrary.Observer {

    //private static final String KEY_DAY_RANGE = "recently_added_day_range";
    static final String NAME = "recently_added";
    static final int RESOURCE_ID = R.string.playlist_recently_added;

    //private int mDayRange;

    RecentlyAddedPlaylist(int id, String name) {
        super(id, name, null, false);

        //mDayRange = -1;

        MusicLibrary.getInstance().addObserver(this);
    }

    @Override
    public void load() {
        mSongs.clear();

        //if (mDayRange == -1)
        //    readDayRange();

        MusicLibrary.getInstance().getRecentlyAddedSongs(mSongs);
    }

    @Override
    public void save() {

    }

    @Override
    public void delete() {

    }

    /*public int getDayRange() {
        if (mDayRange == -1)
            readDayRange();

        return mDayRange;
    }

    public void setDayRange(int dayRange) {
        PreferenceManager.getInstance().setInt(KEY_DAY_RANGE, dayRange, true);

        mDayRange = dayRange;
    }

    private void readDayRange() {
        mDayRange = PreferenceManager.getInstance().getInt(KEY_DAY_RANGE, 14);
    }*/

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                invalidate();
                break;
        }
    }

    /*@Override
    public int getValue() {
        return mDayRange;
    }

    @Override
    public void setValue(int value) {
        if (value > 0) {
            setDayRange(value);
            invalidate();
        }
    }*/

    /*@Override
    public int getEditingTitleRes() {
        return R.string.dialog_title_set_day_range;
    }*/
}
