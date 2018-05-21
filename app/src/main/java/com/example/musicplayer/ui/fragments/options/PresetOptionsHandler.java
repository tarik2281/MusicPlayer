package com.example.musicplayer.ui.fragments.options;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.musicplayer.R;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.dialogs.BaseDialogFragment;
import com.example.musicplayer.ui.dialogs.PresetNameDialog;
import com.example.musicplayer.ui.dialogs.SavePresetDialog;

import java.text.MessageFormat;

/**
 * Created by 19tar on 10.09.2017.
 */

public class PresetOptionsHandler extends MenuOptionsHandler<EqualizerPreset> {

    public static class RestoreDefaultDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

        private static final String KEY_PRESET_ID = "preset_id";

        private int mPresetId;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getArguments() != null) {
                mPresetId = getArguments().getInt(KEY_PRESET_ID);
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            EqualizerPreset preset = MusicLibrary.getInstance().getPresetById(mPresetId);

            builder.setTitle(R.string.dialog_title_restore_default);
            builder.setMessage(MessageFormat.format(getString(R.string.dialog_message_restore_default), preset.getName()));
            builder.setPositiveButton(R.string.dialog_button_yes, this);
            builder.setNegativeButton(R.string.dialog_button_cancel, null);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            MusicLibrary.getInstance().restorePresetDefault(mPresetId);
        }

        public static RestoreDefaultDialog newInstance(int presetId) {
            RestoreDefaultDialog dialog = new RestoreDefaultDialog();

            Bundle args = new Bundle();
            args.putInt(KEY_PRESET_ID, presetId);
            dialog.setArguments(args);

            return dialog;
        }
    }

    private static final String TAG_SAVE = "save_dialog";
    private static final String TAG_RESTORE = "restore_dialog";
    private static final String TAG_RENAME = "rename_dialog";

    public PresetOptionsHandler() {
        super(EqualizerPreset.class);
    }

    @Override
    protected void onCreateMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.options_preset, menu);

        if (getItem().isPrebuilt()) {
            menu.setGroupVisible(R.id.preset_group_custom, false);
        }
        else {
            menu.setGroupVisible(R.id.preset_group_prebuilt, false);
        }
    }

    @Override
    protected boolean onMenuItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_save:
                SavePresetDialog.newInstance(getItem().getId()).show(getFragmentManager(), TAG_SAVE);
                return true;
            case R.id.option_restore_default:
                RestoreDefaultDialog.newInstance(getItem().getId()).show(getFragmentManager(), TAG_RESTORE);
                return true;
            case R.id.option_rename:
                PresetNameDialog.newInstance(getItem().getId()).show(getFragmentManager(), TAG_RENAME);
                return true;
        }
        return super.onMenuItemSelected(item);
    }
}
