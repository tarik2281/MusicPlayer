package com.example.musicplayer.ui.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 04.12.16.
 */

@TargetApi(21)
public class SDCardAccessDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {

    private static final String KEY_ROOT = "root";

    private static final int REQUEST_CODE = 1;

    private boolean mDismiss = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.dialog_title_sd_card_access);
        builder.setMessage(MessageFormat.format(getString(R.string.dialog_text_sd_card_access), getArguments().getString(KEY_ROOT)));
        builder.setPositiveButton(R.string.dialog_button_choose, null);
        builder.setNegativeButton(R.string.dialog_button_cancel, this);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mDismiss)
            dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            StorageManager.getInstance().setSDCardRoot(data.getData());
            mDismiss = true;
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog)dialog;
        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    public static SDCardAccessDialog newInstance(String root) {
        SDCardAccessDialog dialog = new SDCardAccessDialog();

        Bundle args = new Bundle();
        args.putString(KEY_ROOT, root);
        dialog.setArguments(args);

        return dialog;
    }
}
