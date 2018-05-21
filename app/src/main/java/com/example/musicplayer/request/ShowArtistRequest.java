package com.example.musicplayer.request;

import com.example.musicplayer.Util;

/**
 * Created by Tarik on 12.06.2016.
 */
public class ShowArtistRequest extends RequestManager.Request {

    public static final int TYPE = Util.HashFNV1a32("ShowArtist");

    private final int mArtistId;

    public ShowArtistRequest(int artistId) {
        mArtistId = artistId;
    }

    public int getArtistId() {
        return mArtistId;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
