package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Playlist;

import java.util.Collection;

/**
 * Created by Tarik on 26.10.2015.
 */

public class PlaylistListAdapter extends OptionsAdapter<Playlist, PlaylistListAdapter.ViewHolder> {
    public class ViewHolder extends OptionsAdapter.OptionsHolder {
        protected View mView;
        protected TextView mNameView;
        protected ImageView mOptionsView;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mNameView = (TextView)itemView.findViewById(R.id.text_playlist);
            mOptionsView = (ImageView)itemView.findViewById(R.id.options_button);

            initialize();
        }

        @Override
        public View getItemView() {
            return mView;
        }

        @Override
        public View getOptionsView() {
            return mOptionsView;
        }
    }

    public PlaylistListAdapter() {
        super();
    }

    public PlaylistListAdapter(Collection<Playlist> items) {
        super(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_playlist, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Playlist playlist = getItem(position);

        holder.mNameView.setText(playlist.getName());

        holder.mNameView.setTextColor(getItemColor(holder));
        holder.mOptionsView.setColorFilter(getColorFilter(holder));

        super.onBindViewHolder(holder, position);
    }
}
