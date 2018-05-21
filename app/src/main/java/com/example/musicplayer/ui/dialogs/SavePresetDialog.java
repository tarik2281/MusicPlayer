package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.MusicLibrary;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 11.12.16.
 */

/*public class SavePresetDialog extends BaseDialogFragment implements AdapterView.OnItemClickListener,
        DialogInterface.OnShowListener, View.OnClickListener, OnDialogDismissListener {

    public static class ConfirmDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {
        private static final String KEY_TITLE = "title";
        private static final String KEY_MESSAGE = "message";

        private boolean mResult;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            Bundle args = getArguments();

            builder.setTitle(args.getString(KEY_TITLE));
            builder.setMessage(args.getString(KEY_MESSAGE));

            builder.setPositiveButton(R.string.dialog_button_yes, this);
            builder.setNegativeButton(R.string.dialog_button_cancel, null);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mResult = true;
        }

        public boolean confirmed() {
            return mResult;
        }

        public static ConfirmDialog newInstance(String title, String message) {
            ConfirmDialog dialog = new ConfirmDialog();

            Bundle args = new Bundle();
            args.putString(KEY_TITLE, title);
            args.putString(KEY_MESSAGE, message);
            dialog.setArguments(args);

            return dialog;
        }
    }

    private static final String KEY_GAINS_STR = "band_gains_str";

    private static final String KEY_ID = "id";

    private int mId;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EqualizerPreset preset = mAdapter.getItem(position);
        mId = mAdapter.getItem(position).getId();
        ConfirmDialog.newInstance(getString(R.string.dialog_title_overwrite_preset),
                MessageFormat.format(getString(R.string.dialog_message_overwrite_preset), preset.getName()))
                .show(getChildFragmentManager(), "confirm_overwrite");
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        NewPresetDialog.newInstance(getArguments().getString(KEY_GAINS_STR)).show(getChildFragmentManager(), "new_preset");
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof NewPresetDialog) {
            if (((NewPresetDialog)dialog).confirmed())
                dismiss();
        }
        else if (dialog instanceof ConfirmDialog) {
            if (((ConfirmDialog)dialog).confirmed()) {
                MusicLibrary.getInstance().updatePresetGains(mId, getArguments().getString(KEY_GAINS_STR));
                dismiss();
            }
        }
    }

    public static class NewPresetDialog extends TextInputDialog {

        @Override
        protected String getTitle() {
            return getString(R.string.dialog_title_new_preset);
        }

        @Override
        protected boolean onConfirm(String text) {
            MusicLibrary lib = MusicLibrary.getInstance();
            int id = EqualizerPreset.makeId(text);

            if (id == 0) {
                setError(getString(R.string.error_name_empty));
                return false;
            }

            if (lib.getPresetById(id) != null) {
                setError(MessageFormat.format(getString(R.string.error_playlist_exists), text));
                return false;
            }

            lib.createPreset(text, getArguments().getString(KEY_GAINS_STR));

            return true;
        }

        public static NewPresetDialog newInstance(String bandGainsStr) {
            NewPresetDialog dialog = new NewPresetDialog();

            Bundle args = new Bundle();
            args.putString(KEY_GAINS_STR, bandGainsStr);
            dialog.setArguments(args);

            return dialog;
        }
    }

    private ItemAdapter<EqualizerPreset> mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mId = savedInstanceState.getInt(KEY_ID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        mAdapter = new ItemAdapter<EqualizerPreset>(getContext(), MusicLibrary.getInstance().getPresets(null, true));

        builder.setTitle(R.string.dialog_title_save_preset);

        ListView lv = new ListView(getContext());
        lv.setAdapter(mAdapter);
        int padding = getResources().getDimensionPixelSize(R.dimen.list_view_padding);
        lv.setPadding(padding, padding, padding, padding);
        lv.setOnItemClickListener(this);
        builder.setView(lv);

        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        builder.setPositiveButton(R.string.dialog_button_new, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_ID, mId);
    }

    public static SavePresetDialog newInstance(String bandGainsStr) {
        SavePresetDialog dialog = new SavePresetDialog();

        Bundle args = new Bundle();
        args.putString(KEY_GAINS_STR, bandGainsStr);
        dialog.setArguments(args);

        return dialog;
    }
}*/

public class SavePresetDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_PRESET_ID = "preset_id";
    private static final String KEY_GAINS = "band_gains";

    private int mPresetId;
    private String mGains;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mPresetId = args.getInt(KEY_PRESET_ID);
            mGains = args.getString(KEY_GAINS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        EqualizerPreset preset = MusicLibrary.getInstance().getPresetById(mPresetId);

        builder.setTitle(R.string.dialog_title_save_preset);
        builder.setMessage(MessageFormat.format(getString(R.string.dialog_message_overwrite_preset), preset.getName()));
        // TODO: don't show again option and toast when saved
        builder.setPositiveButton(R.string.dialog_button_yes, this);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        MusicLibrary.getInstance().updatePresetGains(mPresetId, mGains);
    }

    public static SavePresetDialog newInstance(int presetId) {
        SavePresetDialog dialog = new SavePresetDialog();

        String gains = EqualizerPreset.getGainsStr(
                PlaybackState.getInstance().getFilterState().getEqualizerGains());

        Bundle args = new Bundle();
        args.putInt(KEY_PRESET_ID, presetId);
        args.putString(KEY_GAINS, gains);
        dialog.setArguments(args);

        return dialog;
    }
}
