package com.example.musicplayer.ui.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicplayer.CacheManager;
import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.playback.FilterState;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.ui.TwoPageDragLayout;
import com.example.musicplayer.ui.fragments.FragmentCreateListener;
import com.example.musicplayer.ui.fragments.library.PresetListFragment;

import java.util.Locale;

/**
 * Created by Tarik on 22.12.2015.
 */
public class EqualizerActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, FragmentCreateListener {

    private static final String KEY_OPEN_PAGE = "equalizer_open_page";
    private static final String KEY_ENABLE_EQ = "enable_eq";

    private static final int HALF_RANGE = 120;

    private SeekBar mBandSeekBars[];
    private TextView mBandGainTexts[];

    private SwitchCompat mEqualizerSwitch;

    private SeekBar mBassSeekBar;
    private SeekBar mTrebleSeekBar;
    private TextView mBassGainText;
    private TextView mTrebleGainText;

    private boolean mInternalChange;
    private boolean mInternalUpdate;

    private TwoPageDragLayout mDragLayout;

    private boolean mShouldEnableEq;
    private PresetListFragment mPresetsFragment;

    private static final String[] mBandFreqs = {"31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K", "Bass", "Treble" };

    private PlaybackState.Observer mPlaybackStateObserver = new PlaybackState.Observer() {
        @Override
        public void update(Observable sender, PlaybackState.ObserverData data) {
            switch (data.type) {
                case FilterStateChanged:
                    updateValues(data.filterState);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            mShouldEnableEq = savedInstanceState.getBoolean(KEY_ENABLE_EQ);
        else
            mShouldEnableEq = true;

        super.onCreate(savedInstanceState);

        PlaybackState state = PlaybackState.getInstance();

        setTitle(R.string.title_equalizer);

        showBackButton(true);

        setContentView(R.layout.activity_equalizer);

        LayoutInflater inflater = getLayoutInflater();

        mDragLayout = (TwoPageDragLayout)findViewById(R.id.drag_layout);
        ViewGroup eqLayout = null;

        if (mDragLayout != null) {
            View eqExtraLayout = inflater.inflate(R.layout.equalizer_extra_layout, mDragLayout, false);
            View eqScrollLayout = inflater.inflate(R.layout.equalizer_layout, mDragLayout, false);
            eqLayout = (ViewGroup) eqScrollLayout.findViewById(R.id.eq_layout);

            int page = CacheManager.getInstance().getInteger(KEY_OPEN_PAGE, TwoPageDragLayout.LEFT);

            mDragLayout.setLeftView(eqScrollLayout);
            mDragLayout.setRightView(eqExtraLayout);
            mDragLayout.setCurrentPage(page, false);
            mDragLayout.initialize();

            mBassSeekBar = (SeekBar) eqExtraLayout.findViewById(R.id.seekbar_bass);
            mTrebleSeekBar = (SeekBar) eqExtraLayout.findViewById(R.id.seekbar_treble);
            mBassGainText = (TextView) eqExtraLayout.findViewById(R.id.text_bass_gain);
            mTrebleGainText = (TextView) eqExtraLayout.findViewById(R.id.text_treble_gain);
        }
        else {
            eqLayout = (ViewGroup)findViewById(R.id.eq_layout);

            mBassSeekBar = (SeekBar)findViewById(R.id.seekbar_bass);
            mTrebleSeekBar = (SeekBar)findViewById(R.id.seekbar_treble);
            mBassGainText = (TextView)findViewById(R.id.text_bass_gain);
            mTrebleGainText = (TextView)findViewById(R.id.text_treble_gain);
        }

        setupSeekbar(mBassSeekBar);
        setupSeekbar(mTrebleSeekBar);

        mBandSeekBars = new SeekBar[10];
        mBandGainTexts = new TextView[10];

        for (int i = 0; i < 10; i++) {
            View v = getLayoutInflater().inflate(R.layout.view_eq_band, null);
            TextView text = (TextView)v.findViewById(R.id.eq_band_text);
            text.setText(mBandFreqs[i]);

            mBandGainTexts[i] = (TextView)v.findViewById(R.id.eq_band_gain);

            mBandSeekBars[i] = (SeekBar)v.findViewById(R.id.eq_band_bar);
            setupSeekbar(mBandSeekBars[i]);
            mBandSeekBars[i].setTag(i);

            eqLayout.addView(v);
        }

        state.addObserver(mPlaybackStateObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_equalizer, menu);

        MenuItem item = menu.findItem(R.id.switch_equalizer);
        mEqualizerSwitch = (SwitchCompat)item.getActionView();
        mEqualizerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyEqState();
                if (!mInternalChange && !mInternalUpdate) {
                    mShouldEnableEq = false;
                    mPresetsFragment.setShouldEnableEq(false);
                }
            }
        });

        updateValues(PlaybackState.getInstance().getFilterState());

        return true;
    }

    @Override
    public void onCreateFragment(Class<?> fragmentClass, Fragment fragment) {
        if (fragmentClass == PresetListFragment.class) {
            mPresetsFragment = (PresetListFragment)fragment;
            mPresetsFragment.setShouldEnableEq(mShouldEnableEq);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_reset:
                new ResetValuesDialog().show(getSupportFragmentManager(), "reset_dialog");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateValues(PlaybackState.getInstance().getFilterState());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_ENABLE_EQ, mShouldEnableEq);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PlaybackState.getInstance().removeObserver(mPlaybackStateObserver);

        if (mDragLayout != null)
            CacheManager.getInstance().setInteger(KEY_OPEN_PAGE, mDragLayout.getCurrentPage());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            switch (seekBar.getId()) {
                case R.id.seekbar_bass:
                    setGainText(mBassGainText, progress);
                    break;
                case R.id.seekbar_treble:
                    setGainText(mTrebleGainText, progress);
                    break;
                default:
                    int index = (Integer)seekBar.getTag();
                    setGainText(mBandGainTexts[index], progress);
                    break;
            }
        }
    }

    private void setupSeekbar(SeekBar seekBar) {
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(HALF_RANGE * 2);
        seekBar.setProgress(HALF_RANGE);
    }

    private void updateValues(FilterState state) {
        if (state == null) return;

        // to prevent calling this again because setChecked will call the listener which would call this
        if (!mInternalUpdate && !mInternalChange) {
            mInternalUpdate = true;
            if (mEqualizerSwitch != null)
                mEqualizerSwitch.setChecked(state.isEqualizerEnabled());
            float[] eqGains = state.getEqualizerGains();

            for (int i = 0; i < 10; i++) {
                mBandSeekBars[i].setProgress(getProgressValue(eqGains[i]));
                setGainText(mBandGainTexts[i], eqGains[i]);
            }

            mBassSeekBar.setProgress(getProgressValue(state.getBassGain()));
            setGainText(mBassGainText, state.getBassGain());

            mTrebleSeekBar.setProgress(getProgressValue(state.getTrebleGain()));
            setGainText(mTrebleGainText, state.getTrebleGain());
            mInternalUpdate = false;
        }
    }

    private void setGainText(TextView view, float gain) {
        view.setText(String.format(Locale.getDefault(), "%.1f", gain));
    }

    private void setGainText(TextView view, int progress) {
        float value = getGainValue(progress);
        setGainText(view, value);
    }

    private float getGainValue(int progress) {
        return (progress - HALF_RANGE) / 10.0f;
    }

    private int getProgressValue(float gain) {
        return (int)(gain * 10.0f + HALF_RANGE);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        applyEqState();
    }

    private void applyEqState() {
        // mInternalChange must be used because setFilterState will call the observer which would call this again
        if (!mInternalChange && !mInternalUpdate) {
            PlaybackState state = PlaybackState.getInstance();
            FilterState fState = FilterState.Factory.getFilterState();

            float[] eqGains = fState.getEqualizerGains();

            for (int i = 0; i < 10; i++)
                eqGains[i] = getGainValue(mBandSeekBars[i].getProgress());

            if (mShouldEnableEq)
                mEqualizerSwitch.setChecked(true);

            fState.setEqualizer(mShouldEnableEq || mEqualizerSwitch.isChecked(), eqGains,
                    getGainValue(mBassSeekBar.getProgress()), getGainValue(mTrebleSeekBar.getProgress()));

            mInternalChange = true;
            state.setFilterState(fState);
            mInternalChange = false;
        }
    }

    @Override
    public void onClick(View v) {

    }

    public static void start(Context context) {
        Intent intent = new Intent(context, EqualizerActivity.class);
        context.startActivity(intent);
    }

    public static class ResetValuesDialog extends DialogFragment implements DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setTitle(R.string.dialog_title_reset_values);
            builder.setMessage(R.string.dialog_message_reset_values);

            builder.setPositiveButton(R.string.dialog_button_yes, this);
            builder.setNegativeButton(R.string.dialog_button_no, null);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            PlaybackState state = PlaybackState.getInstance();

            FilterState fState = FilterState.Factory.getFilterState();
            float[] gains = new float[10];
            fState.setEqualizer(state.getFilterState().isEqualizerEnabled(), gains, 0.0f, 0.0f);
            state.setFilterState(fState);
        }
    }
}
