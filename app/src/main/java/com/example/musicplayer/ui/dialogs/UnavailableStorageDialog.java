package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.example.musicplayer.R;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 16.12.16.
 */

public class UnavailableStorageDialog extends BaseDialogFragment implements DialogInterface.OnShowListener,
        View.OnClickListener {

    private static final String KEY_UNAVAILABLES = "unavailables";
    private static final String KEY_RESULT = "result";
    private static final String KEY_INDEX = "index";

    private String[] mUnavailables;
    private boolean[] mResult;
    private int mIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mUnavailables = args.getStringArray(KEY_UNAVAILABLES);
        }

        if (savedInstanceState != null) {
            mResult = savedInstanceState.getBooleanArray(KEY_RESULT);
            mIndex = savedInstanceState.getInt(KEY_INDEX);
        }
        else {
            mResult = new boolean[mUnavailables.length];
            mIndex = 0;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.dialog_title_unavailable_storage);
        builder.setMessage(getMessage());
        builder.setPositiveButton(R.string.dialog_button_yes, null);
        builder.setNegativeButton(R.string.dialog_button_no, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBooleanArray(KEY_RESULT, mResult);
        outState.putInt(KEY_INDEX, mIndex);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog)dialog;

        Button positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        positive.setTag(DialogInterface.BUTTON_POSITIVE);
        negative.setTag(DialogInterface.BUTTON_NEGATIVE);
        positive.setOnClickListener(this);
        negative.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int tag = (int)v.getTag();

        mResult[mIndex] = tag == DialogInterface.BUTTON_POSITIVE;

        if (++mIndex >= mResult.length)
            dismiss();
        else {
            ((AlertDialog)getDialog()).setMessage(getMessage());
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        for (int i = 0; i < mResult.length; i++)
            mResult[i] = false;
    }

    public String[] getUnavailables() {
        return mUnavailables;
    }

    public boolean[] getResult() {
        return mResult;
    }

    private String getMessage() {
        return MessageFormat.format(getString(R.string.dialog_message_unavailable_storage), mUnavailables[mIndex]);
    }


    public static UnavailableStorageDialog newInstance(String[] unavailables) {
        UnavailableStorageDialog dialog = new UnavailableStorageDialog();

        Bundle args = new Bundle();
        args.putStringArray(KEY_UNAVAILABLES, unavailables);
        dialog.setArguments(args);

        return dialog;
    }
}
