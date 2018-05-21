package com.example.musicplayer.ui.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;

import java.util.ArrayList;
import java.util.Collection;

public class AlbumAdapter extends OptionsAdapter<Song, AlbumAdapter.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_DISC_HEADER = 1;
    private static final int VIEW_TYPE_ALBUM_HEADER = 2;

    private static final int[] LAYOUT_IDS = { R.layout.entry_song_album, R.layout.disc_header };

    public class ViewHolder extends OptionsAdapter.OptionsHolder {
        private int mViewType;

        private View mView;
        private TextView mTitleNumberView;
        private TextView mTitleView;
        private TextView mArtistView;
        private ImageView mOptionsView;

        private TextView mDiscView;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);

            mViewType = viewType;

            mView = itemView;
            switch (mViewType) {
                case VIEW_TYPE_ALBUM_HEADER: {
                    mTitleNumberView = null;
                    mTitleView = null;
                    mArtistView = null;
                    mOptionsView = null;
                    mDiscView = null;
                    break;
                }
                case VIEW_TYPE_DISC_HEADER: {
                    mTitleNumberView = null;
                    mTitleView = null;
                    mArtistView = null;
                    mOptionsView = null;

                    mDiscView = (TextView)mView.findViewById(R.id.text_title);
                    break;
                }
                case VIEW_TYPE_ITEM: {
                    mTitleNumberView = (TextView)mView.findViewById(R.id.text_title_number);
                    mTitleView = (TextView)mView.findViewById(R.id.text_title);
                    mArtistView = (TextView)mView.findViewById(R.id.text_artist);
                    mOptionsView = (ImageView)mView.findViewById(R.id.options_button);

                    mDiscView = null;
                    break;
                }
                default:
                    break;
            }

            initialize();
        }

        @Override
        public View getItemView() {
            switch (mViewType) {
                case VIEW_TYPE_ITEM:
                    return mView;
                case VIEW_TYPE_ALBUM_HEADER:
                case VIEW_TYPE_DISC_HEADER:
                    return null;
                default:
                    return null;
            }
        }

        @Override
        public View getOptionsView() {
            return mOptionsView;
        }
    }

    private boolean mShowArtist;

    private View mHeader;
    private boolean mShowHeaders;
    private ArrayList<Integer> mHeaderIndices;
    private long mPlayedSongId = 0;
    private int mPlayedSongPosition = -1;

    private boolean mInited;

    public AlbumAdapter(Collection<Song> items) {
        super(items);
    }

    @Override
    public void setItems(Collection<Song> items) {
        super.setItems(items);

        initializeDiscHeaders();
        updatePlayedSong(false);
    }

    @Override
    public void setItems(Song[] items, int itemsCount) {
        super.setItems(items, itemsCount);

        initializeDiscHeaders();
        updatePlayedSong(false);
    }

    @Override
    public Song getItem(int position) {
        if (!mInited)
            return super.getItem(position);

        if (mHeader != null)
            position--;

        if (!mShowHeaders)
            return super.getItem(position);
        else {
            return super.getItem(position - getDiscNumber(position));
        }
    }

    @Override
    public int getItemCount() {
        if (!mInited)
            return super.getItemCount();

        int itemCount = super.getItemCount();
        if (mShowHeaders)
            itemCount += mHeaderIndices.size();
        if (mHeader != null)
            itemCount++;
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeader != null) {
            if (position == 0)
                return VIEW_TYPE_ALBUM_HEADER;
            else
                position--;
        }

        if (!mShowHeaders)
            return VIEW_TYPE_ITEM;

        for (int i = 0; i < mHeaderIndices.size(); i++)
            if (position == mHeaderIndices.get(i))
                return VIEW_TYPE_DISC_HEADER;

        return VIEW_TYPE_ITEM;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;

        switch (viewType) {
            case VIEW_TYPE_DISC_HEADER:
            case VIEW_TYPE_ITEM: {
                int layoutId = LAYOUT_IDS[viewType];
                v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
                break;
            }
            case VIEW_TYPE_ALBUM_HEADER:
                v = mHeader;
                break;
        }

        ViewHolder holder = new ViewHolder(v, viewType);

        if (holder.mArtistView != null) {
            int visibility = mShowArtist ? View.VISIBLE : View.GONE;
            holder.mArtistView.setVisibility(visibility);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.mViewType) {
            case VIEW_TYPE_ITEM: {
                MusicLibrary lib = MusicLibrary.getInstance();

                Song song = getItem(position);

                boolean isCurrent = position == mPlayedSongPosition;
                Drawable drawable = null;

                if (isCurrent) {
                    Context context = holder.getItemView().getContext();
                    drawable = ResourcesCompat.getDrawable(holder.mView.getContext().getResources(),
                            R.drawable.play_icon, null);
                    drawable.setColorFilter(Util.getAttrColor(context, R.attr.colorAccent),
                            PorterDuff.Mode.SRC_ATOP);
                }

                holder.mTitleView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                if (isCurrent) {
                    holder.mTitleView.setTypeface(holder.mTitleView.getTypeface(), Typeface.BOLD);
                }
                else if (holder.mTitleView.getTypeface() != null && holder.mTitleView.getTypeface().isBold()) {
                    holder.mTitleView.setTypeface(Typeface.create(holder.mTitleView.getTypeface(), Typeface.NORMAL));
                }

                Song.Info info = song.getInfo();
                String titleNumber = (info.getTitleNumber() <= 0 ? "" : String.valueOf(info.getTitleNumber()));
                holder.mTitleNumberView.setText(titleNumber);
                holder.mTitleView.setText(Song.getTitle(song));

                if (mShowArtist) {
                    holder.mArtistView.setText(Song.getArtist(song));
                }
                break;
            }
            case VIEW_TYPE_DISC_HEADER: {
                if (mHeader != null)
                    position--;

                holder.mDiscView.setText("DISC " + String.valueOf(getDiscNumber(position) + 1));
                break;
            }
        }
    }

    public void setPlayedSongId(long id) {
        mPlayedSongId = id;

        updatePlayedSong(true);
    }

    public void addHeader(View v) {
        mHeader = v;

        updatePlayedSong(false);
    }

    public void setShowArtist(boolean showArtist) {
        mShowArtist = showArtist;
    }


    private int getSongsCount() {
        int count = getItemCount();

        if (mInited) {
            if (mShowHeaders)
                count -= mHeaderIndices.size();
            if (mHeader != null)
                count--;
        }

        return count;
    }

    private void initializeDiscHeaders() {
        if (mHeaderIndices != null)
            mHeaderIndices.clear();

        mInited = false;
        int discNumber = -1;
        for (int i = 0; i < getItemCount(); i++) {
            Song song = getItem(i);

            int number = song.getInfo().getDiscNumber();
            if (number > 0 && number > discNumber) {
                if (mHeaderIndices == null)
                    mHeaderIndices = new ArrayList<>();

                mHeaderIndices.add(i + mHeaderIndices.size());
                discNumber = number;
            }
        }

        mShowHeaders = (mHeaderIndices != null && mHeaderIndices.size() > 1);
        mInited = true;
    }

    private int getDiscNumber(int position) {
        if (!mShowHeaders)
            return 0;

        int offset = -1;
        for (int i = 0; i < mHeaderIndices.size(); i++) {
            if (position <= mHeaderIndices.get(i))
                break;

            offset = i;
        }

        return offset + 1;
    }

    private void updatePlayedSong(boolean notify) {
        if (getItemCount() > 0 && mPlayedSongId != 0) {
            int songPosition = -1;

            for (int i = 0; i < getItemCount() && songPosition < 0; i++) {
                if (getItemViewType(i) == VIEW_TYPE_ITEM) {
                    if (getItem(i).getId() == mPlayedSongId)
                        songPosition = i;
                }
            }



            //if (notify && mPlayedSongPosition > -1)
            //    notifyItemChanged(mPlayedSongPosition, null);

            mPlayedSongPosition = songPosition;

            //if (notify && mPlayedSongPosition > -1)
            //    notifyItemChanged(mPlayedSongPosition, null);
            notifyDataSetChanged();
        }
    }
}
