package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.playback.SleepTimer;

import java.text.MessageFormat;

/**
 * Created by Tarik on 17.07.2016.
 */
public class SleepTimerDialog extends DialogFragment implements DialogInterface.OnClickListener,
        SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    private static final int DURATION = 0;
    private static final int TIME = 1;

    private static final int DURATION_SCALE = 15; // value * DURATION_SCALE
    private static final int MAX_DURATION = 180; // in minutes

    private Spinner mSpinner;
    private TimePicker mTimePicker;
    private CheckBox mRepeatCheck;
    private SeekBar mDurationSeekBar;
    private TextView mDurationText;

    private int mCurrentType;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_timer, null);

        mSpinner = (Spinner)v.findViewById(R.id.spinner_sleep_timer);
        mTimePicker = (TimePicker)v.findViewById(R.id.time_picker);
        mRepeatCheck = (CheckBox)v.findViewById(R.id.check_repeat);
        mDurationSeekBar = (SeekBar)v.findViewById(R.id.seekbar_duration);
        mDurationText = (TextView)v.findViewById(R.id.text_duration);

        boolean is24HourView = DateFormat.is24HourFormat(getContext());
        mTimePicker.setIs24HourView(is24HourView);

        mDurationSeekBar.setMax(getDurationValue(MAX_DURATION));

        builder.setNegativeButton(R.string.dialog_button_cancel, this);
        builder.setPositiveButton(R.string.dialog_button_start, this);

        mSpinner.setOnItemSelectedListener(this);
        mDurationSeekBar.setOnSeekBarChangeListener(this);

        SleepTimer timer = PlaybackState.getInstance().getSleepTimer();

        mDurationSeekBar.setProgress(getDurationValue(timer.getDurationMinutes()));
        setDurationText(timer.getDurationMinutes());

        setHour(mTimePicker, timer.getAlarmHour());
        setMinute(mTimePicker, timer.getAlarmMinute());
        mRepeatCheck.setChecked(timer.getAlarmRepeat());

        mSpinner.setSelection(timer.getType(), false);
        setCurrentType(timer.getType());

        builder.setTitle(R.string.dialog_title_sleep_timer);
        builder.setView(v);

        return builder.create();
    }

    private void setCurrentType(int type) {
        mCurrentType = type;

        switch (type) {
            case DURATION:
                mTimePicker.setVisibility(View.GONE);
                mRepeatCheck.setVisibility(View.GONE);
                mDurationSeekBar.setVisibility(View.VISIBLE);
                mDurationText.setVisibility(View.VISIBLE);
                break;
            case TIME:
                mTimePicker.setVisibility(View.VISIBLE);
                mRepeatCheck.setVisibility(View.VISIBLE);
                mDurationText.setVisibility(View.GONE);
                mDurationSeekBar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            case DialogInterface.BUTTON_POSITIVE: {
                SleepTimer sleepTimer = PlaybackState.getInstance().getSleepTimer();
                switch (mCurrentType) {
                    case DURATION:
                        sleepTimer.startDurationTimer(getDurationMinutes(mDurationSeekBar.getProgress()));
                        break;
                    case TIME:
                        int hour = getHour(mTimePicker);
                        int minute = getMinute(mTimePicker);
                        boolean repeat = mRepeatCheck.isChecked();
                        sleepTimer.startAlarmTimer(hour, minute, repeat);
                        break;
                }

                break;
            }
        }
    }

    public static SleepTimerDialog newInstance() {
        return new SleepTimerDialog();
    }

    private void setDurationText(int minutes) {
        mDurationText.setText(MessageFormat.format(getString(R.string.dialog_sleep_timer_text_duration), minutes));
    }

    private int getDurationMinutes(int value) {
        return (value + 1) * DURATION_SCALE;
    }

    private int getDurationValue(int minutes) {
        return (minutes / DURATION_SCALE) - 1;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int position, boolean userChange) {
        setDurationText(getDurationMinutes(position));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setCurrentType(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private static int getHour(TimePicker picker) {
        if (Build.VERSION.SDK_INT >= 23)
            return picker.getHour();
        else
            return picker.getCurrentHour();
    }

    private static int getMinute(TimePicker picker) {
        if (Build.VERSION.SDK_INT >= 23)
            return picker.getMinute();
        else
            return picker.getCurrentMinute();
    }

    private static void setHour(TimePicker picker, int hour) {
        if (Build.VERSION.SDK_INT >= 23)
            picker.setHour(hour);
        else
            picker.setCurrentHour(hour);
    }

    private static void setMinute(TimePicker picker, int minute) {
        if (Build.VERSION.SDK_INT >= 23)
            picker.setMinute(minute);
        else
            picker.setCurrentMinute(minute);
    }
}
