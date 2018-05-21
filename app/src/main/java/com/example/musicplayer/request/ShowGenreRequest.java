package com.example.musicplayer.request;

import com.example.musicplayer.Util;

/**
 * Created by Tarik on 12.06.2016.
 */
public class ShowGenreRequest extends RequestManager.Request {

    public static final int TYPE = Util.HashFNV1a32("ShowGenre");

    private final int mGenreId;

    public ShowGenreRequest(int genreId) {
        mGenreId = genreId;
    }

    public int getGenreId() {
        return mGenreId;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
