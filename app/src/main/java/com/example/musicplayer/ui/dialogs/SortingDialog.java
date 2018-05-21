package com.example.musicplayer.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Sorting;

/**
 * Created by 19tarik97 on 30.08.16.
 */
public class SortingDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_TITLE = "TITLE";
    private static final String KEY_VALUES = "VALUES";
    private static final String KEY_CURRENT = "current";
    private static final String KEY_REVERSED = "reversed";

    private int mTitle;
    private int[] mValues;
    private int mCurrent;
    private boolean mReversed;

    private RadioGroup mSortingGroup;
    private CheckBox mReversedCheck;

    private Sorting mSorting = null;
    private boolean mSortingReversed;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getInt(KEY_TITLE);
            mValues = args.getIntArray(KEY_VALUES);
            mCurrent = args.getInt(KEY_CURRENT);
            mReversed = args.getBoolean(KEY_REVERSED);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View v = inflater.inflate(R.layout.dialog_sorting, null);

        builder.setNegativeButton(R.string.dialog_button_cancel, this);
        builder.setPositiveButton(R.string.dialog_button_sort, this);

        builder.setTitle(mTitle);
        builder.setView(v);

        mSortingGroup = (RadioGroup)v.findViewById(R.id.radio_group_sorting);
        mReversedCheck = (CheckBox)v.findViewById(R.id.check_reversed);

        String[] sortingTitles = getResources().getStringArray(R.array.sorting_titles);

        for (int value : mValues) {
            RadioButton button = (RadioButton) inflater.inflate(R.layout.dialog_sorting_entry, mSortingGroup, false);
            button.setText(sortingTitles[value]);
            button.setId(value + 1);
            mSortingGroup.addView(button);
        }

        mSortingGroup.check(mCurrent + 1);
        mReversedCheck.setChecked(mReversed);

        return builder.create();
    }

    @SuppressLint("ResourceType")
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                mSorting = Sorting.values()[mSortingGroup.getCheckedRadioButtonId() - 1];
                mSortingReversed = mReversedCheck.isChecked();
                break;
        }
    }

    public Sorting getSorting() {
        return mSorting;
    }

    public boolean isSortingReversed() {
        return mSortingReversed;
    }

    @NonNull
    public static SortingDialog newInstance(int titleRes, Sorting[] values,
                                            Sorting current, boolean reversed) {
        int[] intValues = new int[values.length];

        for (int i = 0; i < values.length; i++)
            intValues[i] = values[i].ordinal();

        Bundle bundle = new Bundle();

        bundle.putInt(KEY_TITLE, titleRes);
        bundle.putIntArray(KEY_VALUES, intValues);
        bundle.putInt(KEY_CURRENT, current.ordinal());
        bundle.putBoolean(KEY_REVERSED, reversed);

        SortingDialog dialog = new SortingDialog();

        dialog.setArguments(bundle);

        return dialog;
    }
}
