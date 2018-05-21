package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.ui.AlbumArtView;

import java.util.Collection;

public class AlbumGridAdapter extends OptionsAdapter<Album, AlbumGridAdapter.ViewHolder> {

	public class ViewHolder extends OptionsAdapter.OptionsHolder {

		private View mView;
		private TextView mTitleView;
		private TextView mArtistView;
		private AlbumArtView mAlbumArtView;
		private ImageButton mOptionsView;

		public ViewHolder(View itemView) {
			super(itemView);

			mView = itemView;
			mTitleView = (TextView)mView.findViewById(R.id.text_title);
			mArtistView = (TextView)mView.findViewById(R.id.text_artist);
			mAlbumArtView = (AlbumArtView) mView.findViewById(R.id.album_art_view);
			mOptionsView = (ImageButton)mView.findViewById(R.id.options_button);

			mAlbumArtView.setEmptyDrawable(R.drawable.standard_cover);

			if (!mShowArtist)
				mArtistView.setVisibility(View.GONE);

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

	private boolean mShowArtist;

	public AlbumGridAdapter() {
		super();
	}

	public AlbumGridAdapter(Collection<Album> items) {
		super(items);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_grid_album, parent, false);

		return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Album album = getItem(position);

		holder.mTitleView.setText(Album.getTitle(album));
		holder.mArtistView.setText(Album.getArtist(album));

		holder.mAlbumArtView.setAlbumArt(album.getId());

		int itemColor = getItemColor(holder);
		holder.mTitleView.setTextColor(itemColor);
		holder.mArtistView.setTextColor(itemColor);
		holder.mOptionsView.setColorFilter(getColorFilter(holder));

		super.onBindViewHolder(holder, position);
	}

	public void setShowArtist(boolean show) {
		mShowArtist = show;
	}
}
