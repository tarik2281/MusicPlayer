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
import com.example.musicplayer.request.ShowGenreRequest;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Genre;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.GenreListAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.fragments.options.GenreOptionsHandler;

import java.util.Collection;

public class GenreListFragment extends LibraryAdapterFragment<Genre> {

	public class Task extends ItemsTask {
		@Override
		protected Collection<Genre> doInBackground(Void... params) {
			return MusicLibrary.getInstance().getAllGenres(filter, sorting, reversed);
		}
	}

	public static final int TYPE = Util.HashFNV1a32("GenreList");

	private static final String TAG_HANDLER = "HANDLER";

	private static final Sorting[] SORTINGS = new Sorting[] {
			Sorting.Name
	};

	private GenreListAdapter mAdapter;
	private GenreOptionsHandler mOptionsHandler;
	
	public GenreListFragment() {

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

		mOptionsHandler = (GenreOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

		if (mOptionsHandler == null) {
			mOptionsHandler = new GenreOptionsHandler();
			mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
		}

		if (mAdapter != null)
			mAdapter.setOnOptionsClickListener(mOptionsHandler);

		return v;
	}

	@Override
	public void onItemClick(View v, int position, int id) {
		ShowGenreRequest request = new ShowGenreRequest(id);
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

				setItems(getSorting(), isSortingReversed());

				//if ((id = updateItems(id)) != 0) {
				//	ShowGenreRequest request = new ShowGenreRequest(id);
				//	request.setSenderFragment(getRootPaneFragment());
				//	RequestManager.getInstance().pushRequest(request);
				//}

				break;
			}
		}
	}*/

	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.title_genres);
	}

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public Sorting[] getSortings() {
		return SORTINGS;
	}

	@Override
	public int getSortingTitleRes() {
		return R.string.dialog_title_sorting_genres;
	}

	@Override
	protected int getNoItemsTextRes() {
		return R.string.no_items_genres;
	}

	@Override
	protected int getNoItemsFoundTextRes() {
		return R.string.search_no_genres;
	}

	@Override
	protected OptionsAdapter initializeAdapter() {
		mAdapter = new GenreListAdapter();
		mAdapter.setOnOptionsClickListener(mOptionsHandler);
		return mAdapter;
	}

	@Override
	protected ItemsTask getItemsTask() {
		return new Task();
	}
}
