package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;

/**
 * Created by 19tarik97 on 05.10.16.
 */
public class PlaylistNameDialog extends TextInputDialog {

    private static final String KEY_PLAYLIST = "playlist";

    private int mPlaylistId;
    private Playlist mPlaylist;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mPlaylistId = args.getInt(KEY_PLAYLIST);
            mPlaylist = MusicLibrary.getInstance().getPlaylistById(mPlaylistId);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (mPlaylist != null)
            setValue(mPlaylist.getName());
        return dialog;
    }

    @Override
    protected String getTitle() {
        return mPlaylist == null ? getString(R.string.dialog_title_new_playlist) : getString(R.string.dialog_title_rename_playlist);
    }

    @Override
    protected boolean onConfirm(String text) {
        MusicLibrary lib = MusicLibrary.getInstance();

        if (Util.stringIsEmpty(text)) {
            setError(getString(R.string.error_name_empty));
            return false;
        }

        if (mPlaylist == null)
            lib.createPlaylist(text, null);
        else
            lib.renamePlaylist(mPlaylist.getId(), text);

        return true;
    }

    public static PlaylistNameDialog newInstance() {
        return newInstance(0);
    }

    public static PlaylistNameDialog newInstance(int playlistId) {
        PlaylistNameDialog dialog = new PlaylistNameDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_PLAYLIST, playlistId);

        dialog.setArguments(args);
        return dialog;
    }
}
