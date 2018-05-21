package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.Observable;
import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowPlaylistRequest;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.adapters.PlaylistListAdapter;
import com.example.musicplayer.ui.dialogs.PlaylistNameDialog;
import com.example.musicplayer.ui.fragments.options.PlaylistOptionsHandler;

import java.util.Collection;

public class PlaylistListFragment extends LibraryAdapterFragment<Playlist> implements OnItemClickListener,
		View.OnClickListener {

	public class Task extends ItemsTask {
		@Override
		protected Collection<Playlist> doInBackground(Void... params) {
			if (!Util.stringIsEmpty(filter))
				return MusicLibrary.getInstance().getEditablePlaylists(filter);

			return MusicLibrary.getInstance().getAllPlaylists();
		}
	}

	public static final int TYPE = Util.HashFNV1a32("PlaylistList");

	private static final String TAG_HANDLER = "HANDLER";
	private static final String TAG_NEW_PLAYLIST = "PLAYLIST_LIST_NEW_PLAYLIST";

	private PlaylistListAdapter mAdapter;
	private PlaylistOptionsHandler mOptionsHandler;
	private FloatingActionButton mAddButton;

	public PlaylistListFragment() {

	}

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_playlists;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		mAddButton = (FloatingActionButton)v.findViewById(R.id.button_add);
		mAddButton.setOnClickListener(this);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		getRecyclerView().setLayoutManager(layoutManager);

		mOptionsHandler = (PlaylistOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

		if (mOptionsHandler == null) {
			mOptionsHandler = new PlaylistOptionsHandler();
			mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
		}

		if (mAdapter != null)
			mAdapter.setOnOptionsClickListener(mOptionsHandler);

		addButtonSpace(mAddButton);

		return v;
	}

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.title_playlists);
    }

	@Override
	protected int getNoItemsTextRes() {
		return R.string.no_items_playlists;
	}

	@Override
    protected int getNoItemsFoundTextRes() {
        return R.string.search_no_playlists;
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        mAdapter = new PlaylistListAdapter();
        mAdapter.setOnOptionsClickListener(mOptionsHandler);
        return mAdapter;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

	@Override
	public void onClick(View v) {
		PlaylistNameDialog.newInstance().show(getChildFragmentManager(), TAG_NEW_PLAYLIST);
	}

    @Override
    public void onItemClick(View v, int position, int id) {
		ShowPlaylistRequest request = new ShowPlaylistRequest(id);
		request.setSenderFragment(getRootPaneFragment());
		RequestManager.getInstance().pushRequest(request);

		setHighlightedPosition(position);
    }

    @Override
    protected void onResize(int width) {
        super.onResize(width);

        // request layout for proper FAB positioning when resizing
        if (mAddButton != null)
            mAddButton.requestLayout();
    }

	@Override
	public void update(Observable sender, MusicLibrary.ObserverData data) {
		switch (data.type) {
			case PlaylistsUpdated: {
				//long id = 0;
				//int position = getHighlightedPosition();
				//if (position > -1)
				//	id = mAdapter.getId(position);

				updateItems();

				//if ((id = updateItems(id)) != 0) {
				//	ShowPlaylistRequest request = new ShowPlaylistRequest(id);
				//	request.setSenderFragment(getRootPaneFragment());
				//	RequestManager.getInstance().pushRequest(request);
				//}
				break;
			}
		}
	}
}
