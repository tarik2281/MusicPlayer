package com.example.musicplayer.request;

import com.example.musicplayer.Util;

/**
 * Created by Tarik on 12.06.2016.
 */
public class ShowPlaylistRequest extends RequestManager.Request {

    public static final int TYPE = Util.HashFNV1a32("ShowPlaylist");

    private final int mPlaylistId;

    public ShowPlaylistRequest(int playlistId) {
        mPlaylistId = playlistId;
    }

    public int getPlaylistId() {
        return mPlaylistId;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
