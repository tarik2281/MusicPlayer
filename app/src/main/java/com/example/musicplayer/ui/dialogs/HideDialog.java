package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.example.musicplayer.R;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.util.Collection;

/**
 * Created by Tarik on 15.10.2017.
 */

public class HideDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {
    private static final String KEY_TYPE = "type";
    private static final String KEY_ID = "id";

    private int mObjectType;
    private int mObjectId;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mObjectType = getArguments().getInt(KEY_TYPE);
        mObjectId = getArguments().getInt(KEY_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_hide_title);
        builder.setMessage(R.string.dialog_hide_songs);

        builder.setPositiveButton(R.string.dialog_button_hide, this);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        MusicLibrary lib = MusicLibrary.getInstance();
        Collection<Song> songs = lib.getSongsForObject(mObjectType, mObjectId, null, Sorting.ID, false);
        lib.postRemoveSongs(songs, false);
    }

    public static HideDialog newInstance(int objectType, int objectId) {
        HideDialog dialog = new HideDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, objectType);
        args.putInt(KEY_ID, objectId);
        dialog.setArguments(args);

        return dialog;
    }

    public static HideDialog newInstance(LibraryObject object) {
        return newInstance(object.getType(), object.getId());
    }
}
