package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.EqualizerPreset;

import java.util.Collection;

/**
 * Created by 19tarik97 on 11.12.16.
 */

public class EqualizerPresetAdapter extends OptionsAdapter<EqualizerPreset, EqualizerPresetAdapter.ViewHolder> {

    public class ViewHolder extends OptionsAdapter.OptionsHolder {

        private View mView;
        private TextView mNameView;
        private ImageButton mOptionsButton;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mNameView = (TextView)itemView.findViewById(R.id.text_name);
            mOptionsButton = (ImageButton)itemView.findViewById(R.id.options_button);

            initialize();
        }

        @Override
        public View getItemView() {
            return mView;
        }

        @Override
        public View getOptionsView() {
            return mOptionsButton;
        }
    }

    public EqualizerPresetAdapter() {
        super();
    }

    public EqualizerPresetAdapter(Collection<EqualizerPreset> items) {
        super(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.entry_preset, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        EqualizerPreset preset = getItem(position);

        holder.mNameView.setText(preset.getName());
    }
}
