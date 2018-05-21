package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.musicplayer.R;

/**
 * Created by 19tarik97 on 14.02.16.
 */
public class NumberPickerDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                mResult = true;
                if (mNumberEdit.getText().length() > 0)
                    mValue = Integer.parseInt(mNumberEdit.getText().toString());
                break;
        }
    }

    private static final String KEY_TITLE = "title";
    private static final String KEY_VALUE = "value";

    private int mValue = -1;
    private boolean mResult;

    private EditText mNumberEdit;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = inflater.inflate(R.layout.dialog_number, null);

        mNumberEdit = (EditText)v.findViewById(R.id.edit_number);

        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        builder.setPositiveButton(R.string.dialog_button_confirm, this);

        int value = getArguments().getInt(KEY_VALUE);

        if (value > 0)
            mNumberEdit.setText(String.valueOf(value));

        builder.setTitle(getArguments().getInt(KEY_TITLE));
        builder.setView(v);

        return builder.create();
    }

    public boolean confirmed() {
        return mResult;
    }

    public int getValue() {
        return mValue;
    }

    public static NumberPickerDialog newInstance(@StringRes int titleRes, int value) {
        NumberPickerDialog dialog = new NumberPickerDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, titleRes);
        args.putInt(KEY_VALUE, value);
        dialog.setArguments(args);

        return dialog;
    }
}
