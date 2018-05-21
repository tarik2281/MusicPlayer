package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Artist;

import java.util.Collection;

public class ArtistListAdapter extends OptionsAdapter<Artist, ArtistListAdapter.ViewHolder> {

	public class ViewHolder extends OptionsAdapter.OptionsHolder {
		protected View mView;
		protected TextView mNameView;
		protected ImageView mOptionsView;

		public ViewHolder(View itemView) {
			super(itemView);

			mView = itemView;
			mNameView = (TextView)mView.findViewById(R.id.text_artist);
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

	public ArtistListAdapter() {
		super();
	}

	public ArtistListAdapter(Collection<Artist> items) {
		super(items);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_artist, parent, false);

		return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Artist artist = getItem(position);

		holder.mNameView.setText(Artist.getName(artist));

		holder.mNameView.setTextColor(getItemColor(holder));
		holder.mOptionsView.setColorFilter(getColorFilter(holder));

		super.onBindViewHolder(holder, position);
	}
}
