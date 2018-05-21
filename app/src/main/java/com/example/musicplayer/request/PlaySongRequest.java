package com.example.musicplayer.request;

import com.example.musicplayer.Util;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.Sorting;

/**
 * Created by Tarik on 12.06.2016.
 */
public class PlaySongRequest extends RequestManager.Request {

    public enum Mode {
        Shuffle, ByOrder, Retain
    }

    public static final int TYPE = Util.HashFNV1a32("PlaySong");

    private final int mObjectType;
    private final int mObjectId;
    private final int mSongId;
    private final Sorting mSorting;
    private final boolean mSortingReversed;
    private final Mode mMode;
    private final boolean mKeepQueue;

    public PlaySongRequest(LibraryObject object, int songId, Sorting sorting, boolean reversed, Mode mode, boolean keepQueue) {
        this(object.getType(), object.getId(), songId, sorting, reversed, mode, keepQueue);
    }

    public PlaySongRequest(int objectType, int objectId, int songId, Sorting sorting, boolean reversed, Mode mode, boolean keepQueue) {
        mObjectType = objectType;
        mObjectId = objectId;
        mSongId = songId;
        mSorting = sorting;
        mSortingReversed = reversed;
        mMode = mode;
        mKeepQueue = keepQueue;
    }

    public int getObjectType() {
        return mObjectType;
    }

    public int getObjectId() {
        return mObjectId;
    }

    public int getSongId() {
        return mSongId;
    }

    public Sorting getSorting() {
        return mSorting;
    }

    public boolean isSortingReversed() {
        return mSortingReversed;
    }

    public Mode getMode() {
        return mMode;
    }

    public boolean getKeepQueue() {
        return mKeepQueue;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
