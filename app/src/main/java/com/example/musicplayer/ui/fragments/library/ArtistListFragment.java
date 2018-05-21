package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowArtistRequest;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Artist;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.ArtistListAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.fragments.options.ArtistOptionsHandler;

import java.util.Collection;

public class ArtistListFragment extends LibraryAdapterFragment<Artist> {

	public class Task extends ItemsTask {

		@Override
		protected Collection<Artist> doInBackground(Void... params) {
			if (isCancelled())
				return null;

			MusicLibrary lib = MusicLibrary.getInstance();

			switch (objectType) {
				case LibraryObject.GENRE:
					return lib.getArtistsForGenre(objectId, filter, sorting, reversed);
				default:
					return lib.getAllArtists(filter, sorting, reversed);
			}
		}
	}

	public static final int TYPE = Util.HashFNV1a32("ArtistList");

	private static final String TAG_HANDLER = "HANDLER";

	private static final Sorting[] SORTINGS = new Sorting[] {
			Sorting.Name
	};

	private ArtistListAdapter mAdapter;
	private ArtistOptionsHandler mOptionsHandler;

	public ArtistListFragment() {

	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getSorting() == Sorting.ID)
			setSorting(Sorting.Name, false);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		getRecyclerView().setLayoutManager(layoutManager);

		mOptionsHandler = (ArtistOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

		if (mOptionsHandler == null) {
			mOptionsHandler = new ArtistOptionsHandler();
			mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
		}

		if (mAdapter != null)
			mAdapter.setOnOptionsClickListener(mOptionsHandler);

		return v;
	}

	@Override
	public void onItemClick(View v, int position, int id) {
		ShowArtistRequest request = new ShowArtistRequest(id);
		request.setSenderFragment(getRootPaneFragment());
		RequestManager.getInstance().pushRequest(request);

		setHighlightedPosition(position);
	}

	/*@Override
	public void update(Observable observable, Object o) {
		MusicLibrary.ObserverData data = (MusicLibrary.ObserverData)o;

		switch (data.type) {
			case LibraryUpdated: {
				//long id = 0;
				//int position = getHighlightedPosition();
				//if (position > -1)
				//	id = mAdapter.getId(position);

				updateItems();
				//setItems(getObjectType(), getObjectId(), getSorting(), isSortingReversed());

				//if ((id = updateItems(id)) != 0) {
				//	ShowArtistRequest request = new ShowArtistRequest(id);
				//	request.setSenderFragment(getRootPaneFragment());
				//	RequestManager.getInstance().pushRequest(request);
				//}
				break;
			}
		}
	}*/

	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.title_artists);
	}

	@Override
	public int getType() {
		return TYPE;
	}

	public void setItems(Collection<Artist> items) {
		if (mAdapter == null) {
			mAdapter = new ArtistListAdapter(items);

			mAdapter.setOnOptionsClickListener(mOptionsHandler);

			setAdapter(mAdapter);
		}
		else {
			mAdapter.setItems(items);
			notifyDataSetChanged();
		}
	}

	@Override
	protected OptionsAdapter initializeAdapter() {
		mAdapter = new ArtistListAdapter();
		mAdapter.setOnOptionsClickListener(mOptionsHandler);
		return mAdapter;
	}

	@Override
	protected ItemsTask getItemsTask() {
		return new Task();
	}

	@Override
	public Sorting[] getSortings() {
		return SORTINGS;
	}

	@Override
	public int getSortingTitleRes() {
		return R.string.dialog_title_sorting_artists;
	}

	@Override
	protected int getNoItemsTextRes() {
		return R.string.no_items_artists;
	}

	@Override
	protected int getNoItemsFoundTextRes() {
		return R.string.search_no_artists;
	}
}
