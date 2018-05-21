package com.example.musicplayer.ui.fragments.library;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.musicplayer.ui.FastScroller;
import com.example.musicplayer.Observable;
import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.playback.PlaybackList;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.adapters.QueueListAdapter;
import com.example.musicplayer.ui.adapters.SongListAdapter;
import com.example.musicplayer.ui.fragments.options.SongOptionsHandler;

import java.util.List;

/**
 * Created by Tarik on 04.06.2016.
 */
public class QueueFragment extends DialogFragment implements OnItemClickListener {

    private class Callback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            boolean movable = true;

            if (mQueueIndex > -1) {
                int position = viewHolder.getAdapterPosition();
                movable = position != 0 && position != mHeaderPosition;
            }

            int dragFlags = movable ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
            int swipeFlags = movable ? ItemTouchHelper.START | ItemTouchHelper.END : 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            List<Song> queue = mList.getQueue();
            List<Song> next = mList.getNextSongsList();

            int fromAdapter = viewHolder.getAdapterPosition();
            int toAdapter = target.getAdapterPosition();
            int from = mAdapter.getItemPosition(fromAdapter);
            int to = mAdapter.getItemPosition(toAdapter);

            if (mQueueIndex == -1) {
                Song song = next.remove(from);
                next.add(to, song);
            }
            else {
                if (toAdapter == 0)
                    return false;

                Song song;

                if (toAdapter < mHeaderPosition) {
                    song = queue.remove(from);
                    queue.add(to, song);
                }
                else if (toAdapter == mHeaderPosition) {
                    if (fromAdapter > mHeaderPosition) {
                        song = next.remove(0);
                        queue.add(queue.size(), song);
                    }
                    else {
                        song = queue.remove(from);
                        next.add(0, song);
                    }

                    mHeaderPosition = fromAdapter;
                    mAdapter.moveHeader(mNextIndex, mHeaderPosition);
                }
                else {
                    song = next.remove(from - queue.size());
                    next.add(to - queue.size(), song);
                }
            }

            mAdapter.notifyItemMoved(fromAdapter, toAdapter);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            List<Song> queue = mList.getQueue();
            List<Song> next = mList.getNextSongsList();

            int fromAdapter = viewHolder.getAdapterPosition();
            int from = mAdapter.getItemPosition(fromAdapter);

            if (mQueueIndex == -1) {
                next.remove(from);
            }
            else {
                if (fromAdapter < mHeaderPosition) {
                    queue.remove(from);
                    mAdapter.moveHeader(mNextIndex, --mHeaderPosition);
                }
                else {
                    next.remove(from - queue.size());
                }
            }

            mAdapter.notifyItemRemoved(fromAdapter);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if (mQueueIndex > -1 && mList.isQueueEmpty())
                removeHeaders();
        }
    }

    private static final String KEY_DIALOG = "dialog";
    private static final String TAG_HANDLER = "HANDLER";

    private MusicLibrary.Observer mLibraryObserver = new MusicLibrary.Observer() {
        @Override
        public void update(Observable sender, MusicLibrary.ObserverData data) {
            switch (data.type) {
                case LibraryUpdated:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private PlaybackList.Observer mPlaybackListObserver = new PlaybackList.Observer() {
        @Override
        public void update(Observable sender, PlaybackList.ObserverData data) {
            List<Song> queue = data.playbackList.getQueue();

            switch (data.type) {
                case SongsAdded:
                    mAdapter.setQueue(queue);
                    mAdapter.notifyItemRangeInserted((mQueueIndex > -1 ? data.position + 1 : data.position), data.size);

                    if (mQueueIndex == -1 && !data.playbackList.isQueueEmpty())
                        addHeaders(true);

                    break;
                case SongsRemoved: {
                    int start = data.position;

                    if (mQueueIndex > -1) {
                        if (data.playbackList.isQueueEmpty()) {
                            removeHeaders();
                        } else {
                            start++;
                        }
                    }

                    mAdapter.notifyItemRangeRemoved(start, data.size);

                    break;
                }
                case ListChanged:
                    mAdapter.notifyDataSetChanged();
                    break;
            }

            if (mQueueIndex > -1)
                mAdapter.moveHeader(mNextIndex, (mHeaderPosition = queue.size() + 1));
        }
    };

    private QueueListAdapter mAdapter;
    private ItemTouchHelper mTouchHelper;
    private SongOptionsHandler mOptionsHandler;

    private RecyclerView mRecyclerView;

    private PlaybackList mList;

    private int mQueueIndex = -1;
    private int mNextIndex = -1;
    private int mHeaderPosition;

    public QueueFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlaybackState state = PlaybackState.getInstance();
        mList = state.getPlaybackList();

        Bundle args = getArguments();
        if (args != null) {
            setShowsDialog(args.getBoolean(KEY_DIALOG));
        }

        mAdapter = new QueueListAdapter();
        mAdapter.setShowArtists(true);
        mAdapter.setDragHandleVisible(true);
        mAdapter.setOnItemClickListener(this);

        if (getShowsDialog())
            mAdapter.setBackgroundResource(R.drawable.background_dialog_item);

        if (mList != null) {
            mList.addObserver(mPlaybackListObserver);
            mAdapter.setItems(mList.getNextSongsList());

            if (!mList.isQueueEmpty()) {
                mAdapter.setQueue(mList.getQueue());
                addHeaders(false);
            }
        }

        mAdapter.setOnDragHandleTouchListener(new SongListAdapter.OnDragHandleTouchListener() {
            @Override
            public void onDragHandleTouch(RecyclerView.ViewHolder holder) {
                mTouchHelper.startDrag(holder);
            }
        });

        MusicLibrary.getInstance().addObserver(mLibraryObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library, null);

        if (getShowsDialog()) {
            float density = getResources().getDisplayMetrics().density;
            int padding = (int) (density * 5.0f);
            v.setPadding(padding, padding * 3, padding, padding);
        }

        mRecyclerView = (RecyclerView)v.findViewById(R.id.list);
        FastScroller scroller = (FastScroller)v.findViewById(R.id.scroller);

        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mTouchHelper = new ItemTouchHelper(new Callback());
        mTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setAdapter(mAdapter);
        scroller.attachRecyclerView(mRecyclerView);

        mOptionsHandler = (SongOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);
        if (mOptionsHandler == null) {
            mOptionsHandler = new SongOptionsHandler();
            mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
        }

        if (mAdapter != null)
            mAdapter.setOnOptionsClickListener(mOptionsHandler);

        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.title_queue);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getShowsDialog()) {
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.dimAmount = 0.0f;
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mList != null)
            mList.removeObserver(mPlaybackListObserver);

        MusicLibrary.getInstance().removeObserver(mLibraryObserver);
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        PlaybackState.getInstance().skipSongs(mAdapter.getItemPosition(position));
    }


    private void addHeaders(boolean notify) {
        mQueueIndex = mAdapter.addHeader(0, getString(R.string.list_header_queue));
        mHeaderPosition = mList.getQueue().size() + 1;
        mNextIndex = mAdapter.addHeader(mHeaderPosition, getString(R.string.list_header_next));

        if (notify) {
            mAdapter.notifyItemInserted(0);
            mAdapter.notifyItemInserted(mHeaderPosition);
        }
    }

    private void removeHeaders() {
        mAdapter.notifyItemRemoved(0);
        mAdapter.notifyItemRemoved(mHeaderPosition - 1);
        mAdapter.removeHeader(mNextIndex);
        mAdapter.removeHeader(mQueueIndex);
        mHeaderPosition = mQueueIndex = mNextIndex = -1;
    }


    public static QueueFragment newInstance(boolean showDialog) {
        QueueFragment fragment = new QueueFragment();
        Bundle bundle = new Bundle(1);
        bundle.putBoolean(KEY_DIALOG, showDialog);
        fragment.setArguments(bundle);
        return fragment;
    }
}
