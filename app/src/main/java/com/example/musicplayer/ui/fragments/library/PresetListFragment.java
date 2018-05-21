package com.example.musicplayer.ui.fragments.library;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.musicplayer.Observable;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.adapters.EqualizerPresetAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.dialogs.PresetNameDialog;
import com.example.musicplayer.ui.fragments.options.PresetOptionsHandler;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Created by 19tar on 11.09.2017.
 */

public class PresetListFragment extends LibraryAdapterFragment<EqualizerPreset> implements View.OnClickListener {

    private class Task extends ItemsTask {

        @Override
        protected Collection<EqualizerPreset> doInBackground(Void... voids) {
            return MusicLibrary.getInstance().getPresets(null);
        }
    }

    private static final String TAG_NEW_PRESET = "new_preset";
    private static final String TAG_HANDLER = "handler";

    private EqualizerPresetAdapter mAdapter;
    private PresetOptionsHandler mOptionsHandler;

    private boolean mShouldEnableEq;

    private Toast mToast;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_playlists;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSearchAvailable(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        FloatingActionButton addButton = (FloatingActionButton) v.findViewById(R.id.button_add);
        addButton.setOnClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        getRecyclerView().setLayoutManager(layoutManager);

        mOptionsHandler = (PresetOptionsHandler) getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

        if (mOptionsHandler == null) {
            mOptionsHandler = new PresetOptionsHandler();
            mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
        }

        if (mAdapter != null)
            mAdapter.setOnOptionsClickListener(mOptionsHandler);

        addButtonSpace(addButton);

        return v;
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        mAdapter = new EqualizerPresetAdapter();
        mAdapter.setOnOptionsClickListener(mOptionsHandler);
        return mAdapter;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

    @Override
    public void onClick(View view) {
        PresetNameDialog.newInstance().show(getChildFragmentManager(), TAG_NEW_PRESET);
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        EqualizerPreset preset = mAdapter.getItem(position);
        String text = MessageFormat.format(getString(R.string.toast_preset_applied), preset.getName());

        if (mToast == null) {
            mToast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
        }
        else {
            mToast.setText(text);
        }

        mToast.show();

        PlaybackState.getInstance().applyPreset(preset, mShouldEnableEq);
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case PresetsUpdated:
                updateItems();
                break;
        }
    }

    public void setShouldEnableEq(boolean enable) {
        mShouldEnableEq = enable;
    }
}
