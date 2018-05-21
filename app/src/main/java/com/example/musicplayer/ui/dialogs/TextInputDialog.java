package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.example.musicplayer.R;

/**
 * Created by 19tarik97 on 19.03.16.
 */
public abstract class TextInputDialog extends BaseDialogFragment implements DialogInterface.OnClickListener,
        DialogInterface.OnShowListener, View.OnClickListener {

    private EditText mEditText;
    private TextView mErrorText;

    private boolean mResult = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = inflater.inflate(R.layout.dialog_text, null);

        mEditText = (EditText)v.findViewById(R.id.edit_text);
        mErrorText = (TextView)v.findViewById(R.id.text_error);

        builder.setNegativeButton(R.string.dialog_button_cancel, this);
        builder.setPositiveButton(R.string.dialog_button_confirm, null);

        builder.setTitle(getTitle());
        builder.setView(v);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);

        mEditText.selectAll();
        mEditText.requestFocus();

        InputMethodManager inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel button clicked
    }

    @Override
    public void onClick(View v) {
        // confirm button clicked
        if (onConfirm(mEditText.getText().toString())) {
            mResult = true;
            dismiss();
        }
    }

    public boolean confirmed() {
        return mResult;
    }

    protected void setValue(String value) {
        mEditText.setText(value);
    }

    protected void setError(String error) {
        mErrorText.setText(error);
        mErrorText.setVisibility(View.VISIBLE);
    }

    protected abstract String getTitle();

    // return true to dismiss, false to keep open
    protected abstract boolean onConfirm(String text);
}
