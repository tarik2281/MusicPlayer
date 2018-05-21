package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.MusicLibrary;

/**
 * Created by 19tar on 10.09.2017.
 */

public class PresetNameDialog extends TextInputDialog {

    private static final String KEY_PRESET_ID = "preset_id";

    private int mPresetId;
    private EqualizerPreset mPreset;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mPresetId = args.getInt(KEY_PRESET_ID);
            mPreset = MusicLibrary.getInstance().getPresetById(mPresetId);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (mPreset != null)
            setValue(mPreset.getName());
        return dialog;
    }

    @Override
    protected String getTitle() {
        return mPreset == null ? getString(R.string.dialog_title_new_preset) : getString(R.string.dialog_title_rename_preset);
    }

    @Override
    protected boolean onConfirm(String text) {
        if (Util.stringIsEmpty(text)) {
            setError(getString(R.string.error_name_empty));
            return false;
        }

        if (mPreset == null) {
            float[] gains = PlaybackState.getInstance().getFilterState().getEqualizerGains();
            MusicLibrary.getInstance().createPreset(text, EqualizerPreset.getGainsStr(gains));
        }
        else {
            MusicLibrary.getInstance().updatePresetName(mPresetId, text);
        }

        return true;
    }

    public static PresetNameDialog newInstance() {
        return newInstance(0);
    }

    public static PresetNameDialog newInstance(int presetId) {
        PresetNameDialog dialog = new PresetNameDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_PRESET_ID, presetId);

        dialog.setArguments(args);
        return dialog;
    }
}
