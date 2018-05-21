package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Toast;

import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.dialogs.AddDuplicatesDialog;
import com.example.musicplayer.ui.dialogs.OnDialogDismissListener;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 04.10.16.
 */
public class AddSongsFragment extends SongListFragment implements OnDialogDismissListener {

    public static final int TYPE = Util.HashFNV1a32("AddSongs");

    private static final String PLAYLIST_KEY = "playlist";
    private static final String KEY_DUPLICATE_SONG_ID = "duplicate_song_id";

    private Playlist mPlaylist;
    private int mDuplicateId;

    private Toast mDuplicateToast;

    private Playlist.Observer mPlaylistObserver = new Playlist.Observer() {
        @Override
        public void update(Observable sender, Playlist.ObserverData data) {
            switch (data.type) {
                case Added:
                case Removed: {
                    int position = -1;

                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        if (mAdapter.getItem(i).getId() == data.items[0].getId()) {
                            position = i;
                            break;
                        }
                    }

                    mAdapter.notifyItemChanged(position);
                    break;
                }
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setEditingPlaylist(savedInstanceState.getInt(PLAYLIST_KEY));
            mDuplicateId = savedInstanceState.getInt(KEY_DUPLICATE_SONG_ID);
        }
        else
            setEditingPlaylist(mPlaylist);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPlaylist != null)
            outState.putInt(PLAYLIST_KEY, mPlaylist.getId());

        outState.putInt(KEY_DUPLICATE_SONG_ID, mDuplicateId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setEditingPlaylist(null);
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        if (mPlaylist != null) {
            if (mPlaylist.getSongList().contains(mAdapter.getItem(position))) {
                int state = PreferenceManager.getInstance().parseInt(PreferenceManager.KEY_PLAYLIST_ADD_DUPLICATE);

                switch (state) {
                    case AddDuplicatesDialog.ADD_DUPLICATE_ALWAYS:
                        addSong(position, true);
                        break;
                    case AddDuplicatesDialog.ADD_DUPLICATE_ASK: {
                        mDuplicateId = id;
                        AddDuplicatesDialog.newInstance(false).show(getChildFragmentManager(), "add_duplicate_dialog");
                        break;
                    }
                }
            }
            else {
                addSong(id, false);
            }
        }
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        super.onDialogDismiss(dialog, tag);

        if (dialog instanceof AddDuplicatesDialog) {
            AddDuplicatesDialog duplicatesDialog = (AddDuplicatesDialog)dialog;
            if (duplicatesDialog.getAddDuplicates())
                addSong(mDuplicateId, true);
        }
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        super.initializeAdapter();
        mAdapter.setAddHandleVisible(mPlaylist != null);
        mAdapter.setPlaylist(mPlaylist);

        return mAdapter;
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.title_add_songs);
    }

    public void setEditingPlaylist(Playlist playlist) {
        if (mPlaylist != null && created())
            mPlaylist.removeObserver(mPlaylistObserver);

        mPlaylist = playlist;

        if (mPlaylist != null && created())
            mPlaylist.addObserver(mPlaylistObserver);
    }

    public void setEditingPlaylist(int id) {
        setEditingPlaylist(MusicLibrary.getInstance().getPlaylistById(id));
    }

    private void addSong(int id, boolean duplicate) {
        Song song = MusicLibrary.getInstance().getSongById(id);
        mPlaylist.addSong(song);

        if (duplicate) {
            String text = MessageFormat.format(getString(R.string.toast_playlist_added_duplicate), Song.getTitle(song));

            if (mDuplicateToast == null)
                mDuplicateToast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
            else
                mDuplicateToast.setText(text);

            mDuplicateToast.show();
        }
    }
}
