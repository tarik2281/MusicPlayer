package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;

/**
 Copyright 2017 Tarik

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

public final class AddDuplicatesDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    public static final int ADD_DUPLICATE_ALWAYS = 0;
    public static final int ADD_DUPLICATE_NEVER = 1;
    public static final int ADD_DUPLICATE_ASK = 2;

    private static final String KEY_MULTIPLE_SONGS = "mulitple_songs";

    private CheckBox mCheckBox;
    private boolean mMultipleSongs;
    private boolean mAddDuplicates;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mMultipleSongs = args.getBoolean(KEY_MULTIPLE_SONGS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.dialog_title_add_duplicate);
        builder.setMessage(mMultipleSongs ? R.string.dialog_message_add_duplicate_multiple :
                R.string.dialog_message_add_duplicate);

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
                if (mCheckBox.isChecked())
                    PreferenceManager.getInstance().setString(PreferenceManager.KEY_PLAYLIST_ADD_DUPLICATE, String.valueOf(ADD_DUPLICATE_ALWAYS), true);

                mAddDuplicates = true;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                if (mCheckBox.isChecked())
                    PreferenceManager.getInstance().setString(PreferenceManager.KEY_PLAYLIST_ADD_DUPLICATE, String.valueOf(ADD_DUPLICATE_NEVER), true);

                mAddDuplicates = false;
                break;
        }
    }

    public boolean getAddDuplicates() {
        return mAddDuplicates;
    }

    public static AddDuplicatesDialog newInstance(boolean multipleSongs) {
        AddDuplicatesDialog dialog = new AddDuplicatesDialog();

        Bundle args = new Bundle();
        args.putBoolean(KEY_MULTIPLE_SONGS, multipleSongs);
        dialog.setArguments(args);

        return dialog;
    }
}
