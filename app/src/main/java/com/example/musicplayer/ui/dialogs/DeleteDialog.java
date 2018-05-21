package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.view.View;

import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Created by 19tarik97 on 14.02.16.
 */
public class DeleteDialog extends DialogFragment implements View.OnClickListener, DialogInterface.OnShowListener {

    private static final String KEY_TYPE = "type";
    private static final String KEY_ID = "id";

    private int mObjectType;
    private int mObjectId;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mObjectType = getArguments().getInt(KEY_TYPE);
        mObjectId = getArguments().getInt(KEY_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setNegativeButton(R.string.dialog_button_cancel, null);

        builder.setPositiveButton(R.string.dialog_button_delete, null);

        String warningString = null;

        switch (mObjectType) {
            case LibraryObject.ARTIST:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_songs_artist), MusicLibrary.getInstance().getArtistById(mObjectId).getName());
                break;
            case LibraryObject.ALBUM:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_songs_album), MusicLibrary.getInstance().getAlbumById(mObjectId).getTitle());
                break;
            case LibraryObject.GENRE:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_songs_genre), MusicLibrary.getInstance().getGenreById(mObjectId).getName());
                break;
            case LibraryObject.FOLDER:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_songs_folder), MusicLibrary.getInstance().getFolderById(mObjectId).getName());
                break;
            case LibraryObject.SONG:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_song), Song.getTitle(MusicLibrary.getInstance().getSongById(mObjectId)));
                break;
            case LibraryObject.PLAYLIST:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_playlist), MusicLibrary.getInstance().getPlaylistById(mObjectId).getName());
                break;
            case LibraryObject.PRESET:
                warningString = MessageFormat.format(getString(R.string.dialog_delete_preset), MusicLibrary.getInstance().getPresetById((int)mObjectId).getName());
                break;
        }


        builder.setTitle(R.string.dialog_delete_title);
        builder.setMessage(warningString);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // delete button clicked
        MusicLibrary lib = MusicLibrary.getInstance();

        if (mObjectType == LibraryObject.PLAYLIST) {
            lib.deletePlaylist(mObjectId);
        }
        else if (mObjectType == LibraryObject.PRESET) {
            lib.removePreset((int)mObjectId);
        }
        else {
            Collection<Song> songs = lib.getSongsForObject(mObjectType, mObjectId, null, Sorting.ID, false);

            boolean currentSong = false;
            PlaybackState state = PlaybackState.getInstance();

            for (Song song : songs) {
                if (!currentSong)
                    currentSong = song.equals(state.getCurrentSong());

                if (StorageManager.getInstance().checkSDCardAccess(song.getInfo().getFilePath(),
                        getChildFragmentManager()) == StorageManager.RESULT_REQUEST_ACCESS)
                    return;
            }

            if (currentSong)
                state.forceStopPlayback();

            lib.postRemoveSongs(songs, true);
        }

        dismiss();
    }

    public static DeleteDialog newInstance(int objectType, int objectId) {
        DeleteDialog dialog = new DeleteDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, objectType);
        args.putInt(KEY_ID, objectId);
        dialog.setArguments(args);

        return dialog;
    }

    public static DeleteDialog newInstance(LibraryObject object) {
        return newInstance(object.getType(), object.getId());
    }
}
