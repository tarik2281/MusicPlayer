package com.example.musicplayer.request;

import com.example.musicplayer.Util;

/**
 * Created by Tarik on 07.08.2016.
 */
public class ShowFolderRequest extends RequestManager.Request {

    public static final int TYPE = Util.HashFNV1a32("ShowFolder");

    private final int mFolderId;

    public ShowFolderRequest(int id) {
        mFolderId = id;
    }

    public int getFolderId() {
        return mFolderId;
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
