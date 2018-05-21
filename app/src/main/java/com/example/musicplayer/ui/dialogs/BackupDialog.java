package com.example.musicplayer.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.musicplayer.ui.activities.FilePickerActivity;
import com.example.musicplayer.ui.adapters.ItemAdapter;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;

import java.io.File;
import java.util.Date;

/**
 * Created by 19tarik97 on 31.12.16.
 */

public class BackupDialog extends DialogFragment implements DialogInterface.OnShowListener, View.OnClickListener {

    private static final int REQUEST_EXPORT = 50;

    private static final int STORAGE_LOCAL = 0;
    private static final int STORAGE_SHARE = 1;

    private boolean mDismissRequest = false;

    private EditText mEditName;
    private Spinner mSpinnerStorage;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_backup_title);

        builder.setPositiveButton(R.string.dialog_button_save, null);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_backup, null);

        ItemAdapter<String> adapter = new ItemAdapter<String>(getActivity(),
                getResources().getStringArray(R.array.dialog_backup_save_options));
        mEditName = (EditText)v.findViewById(R.id.edit_name);
        mSpinnerStorage = (Spinner)v.findViewById(R.id.spinner_storage_type);

        mEditName.setText(DateFormat.format("dd_MM_yyyy-HH_mm", new Date()));
        mEditName.selectAll();
        mSpinnerStorage.setAdapter(adapter);

        builder.setView(v);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mDismissRequest)
            dismiss();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int storageType = mSpinnerStorage.getSelectedItemPosition();
        File file = PreferenceManager.getInstance().makeBackupFile(mEditName.getText().toString());

        switch (storageType) {
            case STORAGE_LOCAL: {
                Intent intent = FilePickerActivity.exportIntent(getActivity(),
                        file.getAbsolutePath(), FilePickerActivity.TYPE_BACKUP);
                startActivityForResult(intent, REQUEST_EXPORT);
                break;
            }
            case STORAGE_SHARE: {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getActivity(),
                        getActivity().getPackageName(), file));
                startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_title_save_backup)), REQUEST_EXPORT);
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EXPORT && resultCode == Activity.RESULT_OK) {
            mDismissRequest = true;
        }
    }

    public static BackupDialog newInstance() {
        return new BackupDialog();
    }
}
