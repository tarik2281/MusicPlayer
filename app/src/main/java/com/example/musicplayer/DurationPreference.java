package com.example.musicplayer;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.MessageFormat;

/**
 * Created by 19tarik97 on 05.09.16.
 */
public class DurationPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    // values in seconds
    private static final int RANGE = 120;
    //private static final int[] VALUES = { 0, 5, 10, 15, 20, 25, 30, 40, 50, 60 };

    private int mValue;

    private SeekBar mSeekBar;
    private TextView mText;

    public DurationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setDialogLayoutResource(R.layout.preference_duration);
    }

    public DurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_duration);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar)view.findViewById(R.id.seekbar);
        mText = (TextView)view.findViewById(R.id.text);

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(RANGE);
        //mSeekBar.setMax(VALUES.length - 1);

        //int index = 0;
//
        //for (int i = 0; i < VALUES.length; i++) {
        //    if (VALUES[i] == mValue) {
        //        index = i;
        //        break;
        //    }
        //}

        mSeekBar.setProgress(mValue);
        mText.setText(getSummaryText(mValue));
    }

    private String getSummaryText(int value) {
        Resources resources = getContext().getResources();

        if (value == 0)
            return resources.getString(R.string.pref_ignore_songs_none);

        return MessageFormat.format(resources.getString(R.string.pref_ignore_songs_duration), value);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        SavedState state = new SavedState(superState);
        state.value = mValue;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setValue(savedState.value);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            //int value = VALUES[progress];

            mText.setText(getSummaryText(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private static class SavedState extends BaseSavedState {

        int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        mValue = restorePersistedValue ? getPersistedInt(0) : (int)defaultValue;

        setSummary(getSummaryText(mValue));
    }

    @Override
    public boolean shouldDisableDependents() {
        return mValue == 0 || super.shouldDisableDependents();
    }

    private void setValue(int value) {
        final boolean wasBlocking = shouldDisableDependents();

        mValue = value;

        persistInt(value);

        final boolean isBlocking = shouldDisableDependents();

        if (wasBlocking != isBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            //int index = mSeekBar.getProgress();
            //int value = VALUES[index];
            int value = mSeekBar.getProgress();

            if (callChangeListener(value)) {
                setValue(value);
            }

            setSummary(getSummaryText(value));
        }
    }
}
