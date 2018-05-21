package com.example.musicplayer.request;

import com.example.musicplayer.Util;

/**
 * Created by Tarik on 12.06.2016.
 */
public class ShowAlbumRequest extends RequestManager.Request {

    public static final int TYPE = Util.HashFNV1a32("ShowAlbum");

    private final int mAlbumId;

    public ShowAlbumRequest(int albumId) {
        mAlbumId = albumId;
    }

    public int getAlbumId() {
        return mAlbumId;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
