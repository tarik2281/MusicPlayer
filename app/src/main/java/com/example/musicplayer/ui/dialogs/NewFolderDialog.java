package com.example.musicplayer.ui.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;

import java.io.File;
import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 16.11.16.
 */

public class NewFolderDialog extends TextInputDialog {

    private static final String KEY_FOLDER = "folder";

    private String mFolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mFolder = args.getString(KEY_FOLDER);
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.dialog_title_new_folder);
    }

    @Override
    protected boolean onConfirm(String text) {
        File file = new File(mFolder);

        File directory = new File(file, text);

        boolean created = false;

        if (directory.exists()) {
            setError(MessageFormat.format(getString(R.string.error_directory_exists), text));
            return false;
        }
        else if (!file.canWrite()) {
            DocumentFile documentFile = StorageManager.getInstance().getSDCardFile(null, mFolder, false);

            if (documentFile != null)
                created = documentFile.createDirectory(text) != null;
        }
        else {
            created = directory.mkdir();
        }

        int message;

        if (created)
            message = R.string.toast_new_folder_success;
        else
            message = R.string.toast_new_folder_error;

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        return true;
    }

    public static NewFolderDialog newInstance(String folder) {
        NewFolderDialog dialog = new NewFolderDialog();

        Bundle args = new Bundle();
        args.putString(KEY_FOLDER, folder);

        dialog.setArguments(args);
        return dialog;
    }
}
