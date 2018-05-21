package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.example.musicplayer.R;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 28.11.16.
 */

public class RemoveFromPlaylistDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_SONG = "song";
    private static final String KEY_PLAYLIST = "playlist";

    private Song mSong;
    private Playlist mPlaylist;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        MusicLibrary lib = MusicLibrary.getInstance();

        mSong = lib.getSongById(getArguments().getInt(KEY_SONG));
        mPlaylist = lib.getPlaylistById(getArguments().getInt(KEY_PLAYLIST));

        builder.setTitle(R.string.dialog_title_remove_from_playlist);
        builder.setMessage(MessageFormat.format(getString(R.string.dialog_message_remove_from_playlist), Song.getTitle(mSong)));

        builder.setPositiveButton(R.string.dialog_button_remove, this);
        builder.setNegativeButton(R.string.dialog_button_cancel, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                int index = mPlaylist.getSongPosition(mSong);
                if (index > -1) {
                    mPlaylist.removeSong(index);
                }
                break;
        }
    }

    public static RemoveFromPlaylistDialog newInstance(int songId, int playlistId) {
        RemoveFromPlaylistDialog dialog = new RemoveFromPlaylistDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_SONG, songId);
        args.putInt(KEY_PLAYLIST, playlistId);

        dialog.setArguments(args);

        return dialog;
    }
}
