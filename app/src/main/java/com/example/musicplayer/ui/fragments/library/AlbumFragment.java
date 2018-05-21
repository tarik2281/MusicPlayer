package com.example.musicplayer.ui.fragments.library;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.KeepQueueDelegate;
import com.example.musicplayer.MemoryManager;
import com.example.musicplayer.Observable;
import com.example.musicplayer.library.Thumbnails;
import com.example.musicplayer.request.PlaySongRequest;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.AlbumAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.fragments.options.AlbumOptionsHandler;
import com.example.musicplayer.ui.fragments.options.SongOptionsHandler;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Created by Tarik on 01.06.2016.
 */
public class AlbumFragment extends LibraryAdapterFragment<Song> implements ViewTreeObserver.OnGlobalLayoutListener {

    private class LoadTask extends AsyncTask<Object, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Object... params) {
            int albumId = (int)params[0];
            int size = (int)params[1];

            Decoder decoder = getDecoder(size);

            System.gc();

            Bitmap bitmap = Thumbnails.getInstance().decodeBitmap(albumId, decoder, null);
            if (bitmap != null)
                bitmap = bitmap.copy(Bitmap.Config.RGB_565, false);

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                if (!isCancelled())
                    setAlbumArt(bitmap);
                else
                    bitmap.recycle();
            }
        }
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
           // if (!isLandscape()) {
                int yOff = getRecyclerView().computeVerticalScrollOffset();
                mCoverView.offsetTopAndBottom(-mCoverView.getTop() - yOff / 2);
            //}
        }
    }

    public static final int TYPE = Util.HashFNV1a32("Album");

    private static final String KEY_SONG_ID = "play_song_id";
    private static final String TAG_SONG_HANDLER = "SONG_HANDLER";

    private Album mAlbum;
    private AlbumAdapter mAdapter;
    private SongOptionsHandler mSongOptionsHandler;

    private ImageView mCoverView;
    private View mHeaderView;
    private TextView mTitleView;
    private TextView mArtistView;
    private TextView mNumTitlesView;

    private Bitmap mAlbumArtBitmap;
    private LoadTask mLoadTask;
    private boolean mInited;

    private int mPlaySongId;
    private KeepQueueDelegate mQueueDelegate;

    private int mHeaderHeight;

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

    private static Decoder sDecoder;
    private static MemoryManager.Trimmable sDecoderTrimmer = new MemoryManager.Trimmable() {
        @Override
        public void onTrimMemory() {
            if (sDecoder != null)
                sDecoder.release();

            sDecoder = null;
            System.gc();
        }
    };

    static {
        MemoryManager.getInstance().registerForTrim(sDecoderTrimmer);
    }

    public AlbumFragment() {

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_album;
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        return null;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mPlaySongId = savedInstanceState.getInt(KEY_SONG_ID);

        setSearchAvailable(false);

        setAlbum(MusicLibrary.getInstance().getAlbumById(getObjectId()));

        setOptionsHandlerClass(AlbumOptionsHandler.class);
        setHasOptionsMenu(true);

        PlaybackState.getInstance().addObserver(mPlaybackStateObserver);

        mQueueDelegate = new KeepQueueDelegate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        v.getViewTreeObserver().addOnGlobalLayoutListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        getRecyclerView().setLayoutManager(layoutManager);
        getRecyclerView().addOnScrollListener(new ScrollListener());

        mCoverView = (ImageView)v.findViewById(R.id.cover_view);

        mHeaderView = inflater.inflate(R.layout.header_album_view, getRecyclerView(), false);
        mTitleView = (TextView) mHeaderView.findViewById(R.id.text_title);
        mArtistView = (TextView) mHeaderView.findViewById(R.id.text_artist);
        mNumTitlesView = (TextView) mHeaderView.findViewById(R.id.text_num_titles);

        mAdapter.addHeader(mHeaderView);

        mSongOptionsHandler = (SongOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_SONG_HANDLER);
        if (mSongOptionsHandler == null) {
            mSongOptionsHandler = new SongOptionsHandler();
            mSongOptionsHandler.attach(getChildFragmentManager(), TAG_SONG_HANDLER);
        }

        mSongOptionsHandler.setShowAlbum(false);

        if (mAdapter != null)
            mAdapter.setOnOptionsClickListener(mSongOptionsHandler);

        updateViews();

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SONG_ID, mPlaySongId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Util.removeLayoutListener(getView(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLoadTask != null)
            mLoadTask.cancel(false);

        releaseAlbumArt();

        PlaybackState.getInstance().removeObserver(mPlaybackStateObserver);
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        super.onLibraryObjectChanged(type, id);
        setAlbum(MusicLibrary.getInstance().getAlbumById(id));
    }

    @Override
    protected void onResize(int width) {
        if (mInited && mHeaderView != null) {
            if (!isLandscape())
                mHeaderView.setPadding(0, width - mHeaderHeight, 0, 0);
            mHeaderView.requestLayout();
        }
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return Album.getTitle(mAlbum) + " - " + Album.getArtist(mAlbum);
    }

    @Override
    public int getTitleIcon() {
        return R.drawable.ic_album_black_36dp;
    }

    @Override
    public void onGlobalLayout() {
        if (!mInited) {
            int coverWidth = mCoverView.getMeasuredWidth();
            mHeaderHeight = mHeaderView.getMeasuredHeight();

            if (coverWidth > 0 && mHeaderHeight > 0) {
                if (isLandscape()) {
                    mCoverView.getLayoutParams().height = coverWidth / 4;
                    mHeaderView.setPadding(0, (coverWidth / 4) - mHeaderHeight, 0, 0);
                }
                else
                    mHeaderView.setPadding(0, coverWidth - mHeaderHeight, 0, 0);

                mCoverView.requestLayout();
                mHeaderView.requestLayout();
                mInited = true;

                postLoadAlbumArt(coverWidth);
            }
        }
        else {
            int yOff = getRecyclerView().computeVerticalScrollOffset();
            mCoverView.offsetTopAndBottom(-mCoverView.getTop() - yOff / 2);
        }
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                mAlbum = MusicLibrary.getInstance().getAlbumById(getObjectId());

                if (mAlbum == null)
                    requestBack();

                break;
        }
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        mPlaySongId = id;

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
        PlaySongRequest request = new PlaySongRequest(LibraryObject.ALBUM, mAlbum.getId(),
                mPlaySongId, Sorting.Number, false, PlaySongRequest.Mode.Retain, mQueueDelegate.shouldKeepQueue());
        RequestManager.getInstance().pushRequest(request);
    }

    private void setAlbum(Album album) {
        MusicLibrary lib = MusicLibrary.getInstance();

        mAlbum = album;

        Collection<Song> items = lib.getSongsForObject(album, null, Sorting.Number, false);

        if (mAdapter == null) {
            mAdapter = new AlbumAdapter(items);

            mAdapter.setOnOptionsClickListener(mSongOptionsHandler);
            mAdapter.setShowArtist(album.hasVariousArtists());

            PlaybackState state = PlaybackState.getInstance();
            if (state.getCurrentSong() != null)
                mAdapter.setPlayedSongId(state.getCurrentSong().getId());

            setAdapter(mAdapter);
        }
        else {
            mAdapter.setItems(items);
            mAdapter.setShowArtist(album.hasVariousArtists());
            notifyDataSetChanged();
        }

        updateViews();
    }

    private void updateViews() {
        if (mTitleView != null)
            mTitleView.setText(Album.getTitle(mAlbum));
        if (mArtistView != null)
            mArtistView.setText(Album.getArtist(mAlbum));
        if (mNumTitlesView != null) {
            MessageFormat format = new MessageFormat("{0}");
            double[] limits = { 0, 1, 2 };
            ChoiceFormat choice = new ChoiceFormat(limits, getResources().getStringArray(R.array.toast_added_songs_choice));
            format.setFormatByArgumentIndex(0, choice);
            Object[] formArgs = new Object[] { mAlbum.getSongCount() };
            mNumTitlesView.setText(format.format(formArgs));
        }

        setAlbumArt(Thumbnails.getInstance().loadBitmap(mAlbum.getId()));

        if (mInited)
            postLoadAlbumArt(getWidth());
    }

    private void postLoadAlbumArt(int size) {
        if (mLoadTask != null)
            mLoadTask.cancel(false);

        mLoadTask = new LoadTask();
        mLoadTask.execute(mAlbum.getId(), size);
    }

    private void setAlbumArt(Bitmap bitmap) {
        if (mCoverView != null) {
            if (bitmap != null)
                mCoverView.setImageBitmap(bitmap);
            else
                mCoverView.setImageResource(R.drawable.standard_cover_medium);
        }

        releaseAlbumArt();

        mAlbumArtBitmap = bitmap;
    }

    private void releaseAlbumArt() {
        if (mAlbumArtBitmap != null) {
            mAlbumArtBitmap.recycle();
            mAlbumArtBitmap = null;
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static Decoder getDecoder(int size) {
        if (sDecoder == null)
            sDecoder = new Decoder();

        sDecoder.setVideoSize(size, size);
        return sDecoder;
    }
}
