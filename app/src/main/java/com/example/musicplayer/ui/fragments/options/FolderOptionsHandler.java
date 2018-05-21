package com.example.musicplayer.ui.fragments.options;

import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Folder;
import com.example.musicplayer.library.Sorting;

/**
 * Created by Tarik on 06.08.2016.
 */
public class FolderOptionsHandler extends MenuOptionsHandler<Folder> {
    public FolderOptionsHandler() {
        super(Folder.class);
    }

    @Override
    protected void onCreateMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.options_folder, menu);
    }

    @Override
    protected boolean getInformationMessage(StringBuilder builder, boolean loaded) {
        addTextInfo(builder, R.string.info_parent_folder, getItem().getPath());

        return super.getInformationMessage(builder, loaded);
    }

    @Override
    protected Sorting getSorting() {
        return Sorting.Name;
    }
}
