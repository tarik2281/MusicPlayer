package com.example.musicplayer.ui.adapters;

import com.example.musicplayer.library.Song;

import java.util.List;

/**
 * Created by Tarik on 01.12.2016.
 */

public class QueueListAdapter extends SongListAdapter {

    private List<Song> mQueue;

    @Override
    public Song getItem(int position) {
        if (mQueue == null)
            return super.getItem(position);

        int itemPosition = getItemPosition(position);
        if (itemPosition >= mQueue.size())
            return getActualItem(itemPosition - mQueue.size());
        else
            return mQueue.get(itemPosition);
    }

    @Override
    public int getItemCount() {
        if (mQueue == null)
            return super.getItemCount();

        return super.getItemCount() + mQueue.size();
    }

    public void setQueue(List<Song> queue) {
        mQueue = queue;
    }
}
