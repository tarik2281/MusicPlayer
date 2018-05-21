package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import com.example.musicplayer.R;

/**
 * Created by 19tar on 21.09.2017.
 */

public class KeepQueueDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private boolean mResult;
    private CheckBox mCheckBox;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.dialog_keep_queue_title);
        builder.setMessage(R.string.dialog_keep_queue_message);

        mCheckBox = (CheckBox) LayoutInflater.from(getContext()).inflate(R.layout.dialog_check_box, null);
        mCheckBox.setText(R.string.dialog_checkbox_remember_decision);

        int spacing = getResources().getDimensionPixelSize(R.dimen.dialog_view_spacing);
        mCheckBox.setPadding(spacing, spacing, spacing, spacing);
        builder.setView(mCheckBox);
        builder.setPositiveButton(R.string.dialog_button_yes, this);
        builder.setNegativeButton(R.string.dialog_button_no, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                mResult = true;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mResult = false;
                break;
        }
    }

    public boolean shouldRemember() {
        return mCheckBox.isChecked();
    }

    public boolean getResult() {
        return mResult;
    }

    public static KeepQueueDialog newInstance() {
        return new KeepQueueDialog();
    }
}
