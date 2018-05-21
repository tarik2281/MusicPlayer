package com.example.musicplayer.ui.fragments.options;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.musicplayer.ui.activities.FilePickerActivity;
import com.example.musicplayer.request.PlaySongRequest;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.dialogs.ExportPlaylistDialog;
import com.example.musicplayer.ui.dialogs.PlaylistNameDialog;

/**
 * Created by Tarik on 26.10.2015.
 */
public class PlaylistOptionsHandler extends MenuOptionsHandler<Playlist> {

    private static final String TAG_RENAME = "RENAME";
    private static final String TAG_EXPORT_PLAYLIST = "EXPORT_PLAYLIST";

    private static final int FILE_REQUEST_CODE = 10;

    private String mExportFilePath;

    public PlaylistOptionsHandler() {
        super(Playlist.class);
    }

    @Override
    protected void onCreateMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.options_playlist, menu);

        if (!getItem().isMutable())
            menu.setGroupVisible(R.id.option_group_editing, false);
    }

    @Override
    protected boolean onMenuItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_shuffle: {
                PlaySongRequest request = new PlaySongRequest(getItem(), -1, Sorting.ID, false, PlaySongRequest.Mode.Shuffle, false);
                RequestManager.getInstance().pushRequest(request);
                return true;
            }
            case R.id.option_export: {
                Intent intent = FilePickerActivity.exportIntent(getContext());
                startActivityForResult(intent, FILE_REQUEST_CODE);
                return true;
            }
            case R.id.option_rename: {
                PlaylistNameDialog.newInstance(getItem().getId()).show(getFragmentManager(), TAG_RENAME);
                return true;
            }
        }

        return super.onMenuItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            mExportFilePath = data.getStringExtra(FilePickerActivity.KEY_FILEPATH);
    }

    @Override
    public void onStart() {
        if (mExportFilePath != null) {
            ExportPlaylistDialog.newInstance(mExportFilePath, getItem().getId()).show(getChildFragmentManager(), TAG_EXPORT_PLAYLIST);
            mExportFilePath = null;
        }

        super.onStart();
    }

    @Override
    protected boolean getInformationMessage(StringBuilder builder, boolean loaded) {
        boolean load = super.getInformationMessage(builder, loaded);

        if (getItem().getImportPath() != null)
            addTextInfo(builder, R.string.info_imported_from, getItem().getImportPath());

        return load;
    }
}
