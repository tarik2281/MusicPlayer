package com.example.musicplayer.ui.fragments.library;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.request.PlaySongRequest;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.ui.adapters.OptionsAdapter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by 19tarik97 on 03.10.16.
 */
public class HiddenSongsFragment extends SongListFragment {

    public class Task extends ItemsTask {

        @Override
        protected void onPostExecute(Collection<Song> items) {
            super.onPostExecute(items);

            if (!isCancelled()) {
                mSongs = (ArrayList<Song>)items;
            }
        }

        @Override
        protected Collection<Song> doInBackground(Void... params) {
            if (!isCancelled()) {
                return new ArrayList<>(MusicLibrary.getInstance().getHiddenSongs(filter, sorting, reversed));
            }

            return null;
        }
    }

    private static final String KEY_HAS_CHANGED = "HAS_CHANGED";

    private ArrayList<Song> mSongs;
    private boolean mHasChanged;

    public HiddenSongsFragment() {
        super();

        setLibraryObject(LibraryObject.NONE, -1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mHasChanged = savedInstanceState.getBoolean(KEY_HAS_CHANGED);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mOptionsHandler.setShowAlbum(false);
        mOptionsHandler.setShowArtist(false);
        mOptionsHandler.setShowFolder(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_HAS_CHANGED, mHasChanged);
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        PlaySongRequest request = new PlaySongRequest(LibraryObject.NONE, 0, id, Sorting.ID, false, PlaySongRequest.Mode.Retain, true);
        RequestManager.getInstance().pushRequest(request);
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        super.initializeAdapter();

        mAdapter.setAlbumArtVisible(false);

        return mAdapter;
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        super.update(sender, data);

        switch (data.type) {
            case SongUnhidden:
                int position = -1;

                for (int i = 0; i < mSongs.size() && position == -1; i++) {
                    if (mSongs.get(i).getId() == data.songId)
                        position = i;
                }

                mSongs.remove(position);
                mAdapter.notifyItemRemoved(position);
                mHasChanged = true;

                break;
        }
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

    @Override
    protected int getNoItemsTextRes() {
        return R.string.empty_no_hidden_songs;
    }

    public boolean hasChanged() {
        return mHasChanged;
    }
}
