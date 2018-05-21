package com.example.musicplayer.ui.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.AlbumArtView;

public class SongListAdapter extends OptionsAdapter<Song, SongListAdapter.ViewHolder> {

    public interface OnDragHandleTouchListener {
        void onDragHandleTouch(RecyclerView.ViewHolder holder);
    }

	public class ViewHolder extends OptionsAdapter.OptionsHolder implements View.OnTouchListener {

        protected int mViewType;
		protected View mView;
        protected ImageView mDragHandle;
        protected int mTextColor;
		protected TextView mTitleView;
		protected TextView mArtistView;
		protected AlbumArtView mCoverView;
        protected ImageButton mOptionsButton;

		public ViewHolder(View itemView, int viewType) {
            super(itemView);

            mViewType = viewType;
            mView = itemView;
            mTitleView = (TextView) mView.findViewById(R.id.text_title);

            switch (mViewType) {
                case VIEW_TYPE_ITEM:
                    mDragHandle = (ImageView) mView.findViewById(R.id.drag_handle);
                    mArtistView = (TextView) mView.findViewById(R.id.text_artist);
                    mCoverView = (AlbumArtView) mView.findViewById(R.id.album_art_view);
                    mOptionsButton = (ImageButton)mView.findViewById(R.id.options_button);

                    mCoverView.setForegroundDrawable(R.drawable.foreground_options_button);
                    mCoverView.setEmptyDrawable(R.drawable.standard_cover);

                    mTextColor = mTitleView.getCurrentTextColor();

                    if (mAlbumArtVisible) {
                        mCoverView.setVisibility(View.VISIBLE);
                        mOptionsButton.setVisibility(View.GONE);
                    }
                    else {
                        mCoverView.setVisibility(View.GONE);
                        mOptionsButton.setVisibility(View.VISIBLE);
                    }

                    if (mShowHandles)
                        mDragHandle.setOnTouchListener(this);
                    else if (mShowAdd)
                        mDragHandle.setImageResource(R.drawable.ic_add_black_24dp);
                    else
                        mDragHandle.setVisibility(View.GONE);
                    break;
                case VIEW_TYPE_HEADER:
                    break;
            }

            initialize();
        }

        public View getItemView() {
            switch (mViewType) {
                case VIEW_TYPE_ITEM:
                    return mView;
                default:
                    return null;
            }
        }

        public View getOptionsView() {
            return mAlbumArtVisible ? mCoverView : mOptionsButton;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (mTouchListener != null)
                        mTouchListener.onDragHandleTouch(this);

                    return true;
            }

            return false;
        }
    }

    private class Header {
        private int position;
        private String name;
    }

    private static final int[] LAYOUT_IDS = { R.layout.entry_song, R.layout.disc_header };

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_HEADER = 1;

    private boolean mAlbumArtVisible = true;
    private boolean mShowArtists;
    private boolean mShowHandles;
    private boolean mShowAdd;
    private long mPlayedSongId = 0;
    private ArrayList<Integer> mPlayedSongIndices;
    private OnDragHandleTouchListener mTouchListener;
    private Playlist mPlaylist;
    private ArrayList<Header> mHeaders;

    public SongListAdapter() {
        super();
        mPlayedSongIndices = new ArrayList<>(1);
    }

    public SongListAdapter(Song[] items, int itemsCount) {
        super(items, itemsCount);
        mPlayedSongIndices = new ArrayList<>(1);
    }

    public SongListAdapter(Collection<Song> items) {
        super(items);
        mPlayedSongIndices = new ArrayList<>(1);
    }

    public SongListAdapter(List<Song> items) {
        super(items);
        mPlayedSongIndices = new ArrayList<>(1);
    }

    @Override
    public void setItems(List<Song> items) {
        super.setItems(items);

        updatePlayedSong(false);
    }

    @Override
    public void setItems(Collection<Song> items) {
        super.setItems(items);

        updatePlayedSong(false);
    }

    @Override
    public void setItems(Song[] items, int itemsCount) {
        super.setItems(items, itemsCount);

        updatePlayedSong(false);
    }

    @Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = LAYOUT_IDS[viewType];
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);

		return new ViewHolder(v, viewType);
	}

    @Override
	public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.mViewType) {
            case VIEW_TYPE_ITEM: {
                Song song = getItem(position);

                holder.mTitleView.setText(Song.getTitle(song));

                boolean isCurrent = mPlayedSongIndices.contains(position);
                Drawable drawable = null;

                if (isCurrent) {
                    Context context = holder.getItemView().getContext();
                    drawable = ResourcesCompat.getDrawable(holder.mView.getResources(), R.drawable.play_icon, null);
                    drawable.setColorFilter(Util.getAttrColor(context, R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);
                }

                holder.mTitleView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                if (isCurrent) {
                    holder.mTitleView.setTypeface(holder.mTitleView.getTypeface(), Typeface.BOLD);
                }
                else if (holder.mTitleView.getTypeface() != null && holder.mTitleView.getTypeface().isBold()) {
                    holder.mTitleView.setTypeface(Typeface.create(holder.mTitleView.getTypeface(), Typeface.NORMAL));
                }

                if (mShowArtists) {
                    holder.mArtistView.setText(Song.getArtistText(song));
                }
                else {
                    holder.mArtistView.setText(Song.getAlbum(song));
                }

                if (mPlaylist != null) {
                    float alpha = mPlaylist.getSongList().contains(song) ? 0.5f : 1.0f;
                    holder.mTitleView.setAlpha(alpha);
                    holder.mArtistView.setAlpha(alpha);
                    holder.mCoverView.setAlpha(alpha);
                    holder.mDragHandle.setAlpha(alpha);
                }

                if (mAlbumArtVisible)
                    holder.mCoverView.setAlbumArt(song.getInfo().getAlbumId());
                break;
            }
            case VIEW_TYPE_HEADER: {
                Header header = getHeaderByPosition(position);
                holder.mTitleView.setText(header.name);
                break;
            }
        }

        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        int itemCount = super.getItemCount();

        if (mHeaders != null)
            itemCount += mHeaders.size();

        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaders != null)
            for (Header header : mHeaders)
               if (header.position == position)
                   return VIEW_TYPE_HEADER;

        return VIEW_TYPE_ITEM;
    }

    public void setShowArtists(boolean showArtists) {
        mShowArtists = showArtists;
    }

    public void setAlbumArtVisible(boolean visible) {
        mAlbumArtVisible = visible;
    }

    public void setDragHandleVisible(boolean visible) {
        mShowHandles = visible;
    }

    public void setAddHandleVisible(boolean visible) {
        mShowAdd = visible;
    }

    public void setOnDragHandleTouchListener(OnDragHandleTouchListener l) {
        mTouchListener = l;
    }

    public void setPlaylist(Playlist playlist) {
        mPlaylist = playlist;
    }

    public void setPlayedSongId(long id) {
        mPlayedSongId = id;

        updatePlayedSong(true);
    }

    private int getSongsCount() {
        int count = getItemCount();

        if (mHeaders != null)
            count -= mHeaders.size();

        return count;
    }

    protected void updatePlayedSong(boolean notify) {
        if (getItemCount() > 0 && mPlayedSongId != 0) {
            if (notify)
                for (Integer index : mPlayedSongIndices)
                    notifyItemChanged(index);

            mPlayedSongIndices.clear();

            int songsCount = getSongsCount();
            for (int i = 0; i < songsCount; i++) {
                if (getItem(i).getId() == mPlayedSongId) {
                    mPlayedSongIndices.add(i);

                    if (notify)
                        notifyItemChanged(i);
                }
            }
        }
    }

    // returns index of header
    public int addHeader(int position, String name) {
        if (mHeaders == null)
            mHeaders = new ArrayList<>(2);

        Header header = new Header();
        header.position = position;
        header.name = name;
        mHeaders.add(header);

        updatePlayedSong(false);

        return mHeaders.size() - 1;
    }

    /**
     *
     * @param index index of header
     * @param position move to position
     */
    public void moveHeader(int index, int position) {
        mHeaders.get(index).position = position;

        updatePlayedSong(false);
    }

    public void removeHeader(int index) {
        mHeaders.remove(index);

        // TODO: list change event
        updatePlayedSong(false);
    }

    /**
     * returns item position without headers
     * @param position
     * @return
     */
    public int getItemPosition(int position) {
        int numHeaders = 0;

        if (mHeaders != null) {
            for (int i = 0; i < mHeaders.size(); i++) {
                if (position > mHeaders.get(i).position)
                    numHeaders++;
            }
        }

        return position - numHeaders;
    }

    @Override
    public Song getItem(int position) {
        return super.getItem(getItemPosition(position));
    }

    private Header getHeaderByPosition(int position) {
        for (Header header : mHeaders)
            if (position == header.position)
                return header;

        return null;
    }
}
