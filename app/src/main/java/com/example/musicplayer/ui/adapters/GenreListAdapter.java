package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Genre;

import java.util.Collection;

/**
 * Created by Tarik on 25.10.2015.
 */

public class GenreListAdapter extends OptionsAdapter<Genre, GenreListAdapter.ViewHolder> {

    public class ViewHolder extends OptionsAdapter.OptionsHolder {
        protected View mView;
        protected TextView mNameView;
        protected ImageView mOptionsView;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mNameView = (TextView)mView.findViewById(R.id.text_genre);
            mOptionsView = (ImageView)mView.findViewById(R.id.options_button);

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

    public GenreListAdapter() {
        super();
    }

    public GenreListAdapter(Collection<Genre> items) {
        super(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_genre, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Genre genre = getItem(position);

        holder.mNameView.setText(Genre.getName(genre));

        int itemColor = getItemColor(holder);
        holder.mNameView.setTextColor(itemColor);
        holder.mOptionsView.setColorFilter(getColorFilter(holder));

        super.onBindViewHolder(holder, position);
    }
}
