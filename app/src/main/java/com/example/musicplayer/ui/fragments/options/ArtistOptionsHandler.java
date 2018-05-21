package com.example.musicplayer.ui.fragments.options;

import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.library.Artist;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;

import java.util.TreeSet;

public class ArtistOptionsHandler extends MenuOptionsHandler<Artist> {

	private int mNumAlbums;

	public ArtistOptionsHandler() {
		super(Artist.class);

		mNumAlbums = -1;
	}

	@Override
	protected void onCreateMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.options_artist, menu);
	}

	@Override
	protected boolean getInformationMessage(StringBuilder builder, boolean loaded) {
		String albums = loaded ? String.valueOf(mNumAlbums) : getLoadingString();
		addTextInfo(builder, R.string.info_num_albums, albums);

		mNumAlbums = -1;

		super.getInformationMessage(builder, loaded);

		return true;
	}

	@Override
	protected void loadInformation() {
		int artistId;

		synchronized (getItem()) {
			artistId = getItem().getId();
		}

		TreeSet<Album> albums = MusicLibrary.getInstance().getAlbumsForArtist(artistId, null, Sorting.ID, false);

		mNumAlbums = albums.size();

		super.loadInformation();
	}

	@Override
	protected Sorting getSorting() {
		return Sorting.Name;
	}
}
