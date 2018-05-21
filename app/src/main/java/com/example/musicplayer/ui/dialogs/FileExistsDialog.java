package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.example.musicplayer.R;

/**
 * Created by 19tarik97 on 31.12.16.
 */

public class FileExistsDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private boolean mResult = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_file_exists_title);
        builder.setMessage(R.string.dialog_file_exists_message);
        builder.setPositiveButton(R.string.dialog_button_yes, this);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                mResult = true;
                break;
        }
    }

    public boolean confirmed() {
        return mResult;
    }

    public static FileExistsDialog newInstance() {
        return new FileExistsDialog();
    }
}