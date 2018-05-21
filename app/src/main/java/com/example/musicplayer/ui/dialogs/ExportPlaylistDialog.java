package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.ui.adapters.ItemAdapter;
import com.example.musicplayer.io.PlaylistFile;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Tarik on 15.08.2016.
 */
public class ExportPlaylistDialog extends DialogFragment implements DialogInterface.OnClickListener,
        View.OnClickListener, DialogInterface.OnShowListener, OnDialogDismissListener {

    private static final String KEY_PLAYLIST = "playlist_id";
    private static final String KEY_PATH = "PATH";
    private static final String KEY_FILE_PATH = "file_path";

    private Playlist mPlaylist;
    private String mPath;

    private EditText mNameEdit;
    private Spinner mTypeSpinner;
    private CheckBox mRelativePathsCheck;

    private String mFilePath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            mPath = args.getString(KEY_PATH);
            mPlaylist = MusicLibrary.getInstance().getPlaylistById(args.getInt(KEY_PLAYLIST));
        }

        if (savedInstanceState != null)
            mFilePath = savedInstanceState.getString(KEY_FILE_PATH);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View v = inflater.inflate(R.layout.dialog_export_playlist, null);
        mNameEdit = (EditText)v.findViewById(R.id.edit_name);
        mTypeSpinner = (Spinner)v.findViewById(R.id.spinner_type);
        mRelativePathsCheck = (CheckBox)v.findViewById(R.id.check_relative_paths);

        mNameEdit.setText(mPlaylist.getName());
        ((TextView)v.findViewById(R.id.text_path)).setText(mPath);

        ItemAdapter<String> adapter = new ItemAdapter<>(getContext(), getResources().getStringArray(R.array.playlist_types));
        mTypeSpinner.setAdapter(adapter);

        builder.setTitle(R.string.dialog_title_export_playlist);
        builder.setView(v);

        builder.setNegativeButton(R.string.dialog_button_cancel, this);
        builder.setPositiveButton(R.string.dialog_button_export, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog)dialog;

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_FILE_PATH, mFilePath);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel button clicked
    }

    @Override
    public void onClick(View v) {
        // export button clicked
        String name = mNameEdit.getText().toString();

        PlaylistFile.Type type = PlaylistFile.Type.values()[mTypeSpinner.getSelectedItemPosition()];
        mFilePath = PlaylistFile.makePath(mPath, name, type);

        final File file = new File(mFilePath);
        if (file.exists()) {
            new FileExistsDialog().show(getChildFragmentManager(), "file_exists_dialog");
        }
        else {
            savePlaylist(file);
        }
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof FileExistsDialog) {
            if (((FileExistsDialog)dialog).confirmed())
                savePlaylist(new File(mFilePath));
        }
    }

    private void savePlaylist(File file) {
        PlaylistFile playlist = new PlaylistFile();
        playlist.setType(PlaylistFile.Type.values()[mTypeSpinner.getSelectedItemPosition()]);
        playlist.setName(mNameEdit.getText().toString());
        if (mRelativePathsCheck.isChecked())
            playlist.entriesFromSongs(mPlaylist.getSongList(), mPath);
        else
            playlist.entriesFromSongs(mPlaylist.getSongList());

        OutputStream stream = null;

        try {
            if (file.canWrite())
                stream = new FileOutputStream(file);
            else {
                DocumentFile documentFile = StorageManager.getInstance().getSDCardFile(null, file.getAbsolutePath(), true);
                if (documentFile != null)
                    stream = getContext().getContentResolver().openOutputStream(documentFile.getUri(), "w");
            }

            if (stream != null) {
                playlist.saveToStream(stream);
                handleExportSuccess();
            }
            else {
                handleError();
            }
        }
        catch (FileNotFoundException e) {
            handleError();
            e.printStackTrace();
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (IOException e) {
                handleError();
                e.printStackTrace();
            }
        }
    }

    private void handleExportSuccess() {
        Toast.makeText(getContext(), R.string.toast_playlist_export_success, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void handleError() {
        Toast.makeText(getContext(), R.string.toast_playlist_export_error, Toast.LENGTH_SHORT).show();
        dismiss();
    }


    public static ExportPlaylistDialog newInstance(String path, int playlistId) {
        ExportPlaylistDialog dialog = new ExportPlaylistDialog();

        Bundle args = new Bundle();
        args.putString(KEY_PATH, path);
        args.putInt(KEY_PLAYLIST, playlistId);
        dialog.setArguments(args);

        return dialog;
    }
}
