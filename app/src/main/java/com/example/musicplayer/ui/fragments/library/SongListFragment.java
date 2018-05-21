package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.KeepQueueDelegate;
import com.example.musicplayer.Observable;
import com.example.musicplayer.request.PlaySongRequest;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.adapters.SongListAdapter;
import com.example.musicplayer.ui.fragments.options.SongOptionsHandler;

import java.util.Collection;

public class SongListFragment extends LibraryAdapterFragment<Song> {

	private class Task extends ItemsTask {
		@Override
		protected Collection<Song> doInBackground(Void... params) {
            if (isCancelled())
                return null;

            MusicLibrary lib = MusicLibrary.getInstance();

            return lib.getSongsForObject(objectType, objectId, filter, sorting, reversed);
		}
	}

	public static final int TYPE = Util.HashFNV1a32("SongList");

    private static final String KEY_SONG_ID = "play_song_id";

	private static final String TAG_HANDLER = "HANDLER";

	protected static final Sorting[] SORTINGS = new Sorting[] {
			Sorting.Title, Sorting.Artist,
			Sorting.Album, Sorting.FileName,
            Sorting.Duration
	};

	protected SongListAdapter mAdapter;
	protected SongOptionsHandler mOptionsHandler;

    private int mPlaySongId;
    private KeepQueueDelegate mQueueDelegate;

    protected boolean mShowHandles;
    protected boolean mUseFileNames;

	private PlaybackState.Observer mPlaybackStateObserver = new PlaybackState.Observer() {

		@Override
		public void update(Observable sender, PlaybackState.ObserverData data) {
            switch (data.type) {
                case SongChanged:
                    if (mAdapter != null)
                        mAdapter.setPlayedSongId(data.song.getId());
                    break;
            }
		}
	};

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (PreferenceManager.KEY_USE_SONG_FILE_NAMES.equals(data.key)) {
                        mUseFileNames = (boolean) data.value;

                        if (mUseFileNames && getSorting() == Sorting.Title)
                            setSorting(Sorting.FileName, isSortingReversed());
                        else if (!mUseFileNames && getSorting() == Sorting.FileName)
                            setSorting(Sorting.Title, isSortingReversed());
                        else if (mAdapter != null)
                            mAdapter.notifyDataSetChanged();
                    }

                    break;
            }
        }
    };

    public SongListFragment() {
		mShowHandles = false;
	}

    protected Sorting getDefaultSorting() {
        return mUseFileNames ? Sorting.FileName : Sorting.Title;
    }

    @Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mPlaySongId = savedInstanceState.getInt(KEY_SONG_ID);

        PreferenceManager prefs = PreferenceManager.getInstance();
        mUseFileNames = prefs.getBoolean(PreferenceManager.KEY_USE_SONG_FILE_NAMES);

		if (getSorting() == Sorting.ID)
			setSorting(getDefaultSorting(), false);

        PlaybackState.getInstance().addObserver(mPlaybackStateObserver);
        prefs.addObserver(mPreferenceObserver);

        mQueueDelegate = new KeepQueueDelegate();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		getRecyclerView().setLayoutManager(layoutManager);

		mOptionsHandler = (SongOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

		if (mOptionsHandler == null) {
			mOptionsHandler = new SongOptionsHandler();
			mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
		}

		mOptionsHandler.setShowArtist(showArtist());
		mOptionsHandler.setShowAlbum(showAlbum());
		mOptionsHandler.setShowFolder(showFolder());

		if (mAdapter != null)
			mAdapter.setOnOptionsClickListener(mOptionsHandler);

		return v;
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SONG_ID, mPlaySongId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PlaybackState.getInstance().removeObserver(mPlaybackStateObserver);
        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    @Override
	public void onItemClick(View v, final int position, final int id) {
		mPlaySongId = getObjectType() == LibraryObject.PLAYLIST ? position : id;

        if (mQueueDelegate.checkKeepQueue(this))
            pushSongRequest();
	}

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        super.onDialogDismiss(dialog, tag);

        if (mQueueDelegate.onDialogDismiss(dialog, tag))
            pushSongRequest();
    }

    private void pushSongRequest() {
        PlaySongRequest request = new PlaySongRequest(getObjectType(), getObjectId(), mPlaySongId,
                getSorting(), isSortingReversed(), PlaySongRequest.Mode.Retain, mQueueDelegate.shouldKeepQueue());
        RequestManager.getInstance().pushRequest(request);
    }

    @Override
	public int getType() {
		return TYPE;
	}

	@Override
	public Sorting[] getSortings() {
		return getObjectType() != LibraryObject.PLAYLIST ? SORTINGS : null;
	}

	@Override
	public int getSortingTitleRes() {
		return R.string.dialog_title_sorting_songs;
	}

    @Override
    protected int getNoItemsTextRes() {
        return R.string.no_items_songs;
    }

    @Override
	protected int getNoItemsFoundTextRes() {
		return R.string.search_no_songs;
	}

    @Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.title_songs);
	}

    @Override
    protected OptionsAdapter initializeAdapter() {
        mAdapter = new SongListAdapter();
        mAdapter.setOnOptionsClickListener(mOptionsHandler);
        mAdapter.setShowArtists(showArtist());
        mAdapter.setDragHandleVisible(mShowHandles);

        PlaybackState state = PlaybackState.getInstance();
        if (state.getCurrentSong() != null)
            mAdapter.setPlayedSongId(state.getCurrentSong().getId());

        return mAdapter;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

    public void setDragHandlesVisible(boolean visible) {
        mShowHandles = visible;

        if (mAdapter != null && getRecyclerView() != null) {
            mAdapter.setDragHandleVisible(mShowHandles);
            requestLayout();
        }
    }

    private boolean showArtist() {
        return getObjectType() != LibraryObject.ARTIST;
    }

    private boolean showAlbum() {
        return getObjectType() != LibraryObject.ALBUM;
    }

    private boolean showFolder() {
        return getObjectType() != LibraryObject.FOLDER;
    }
}
