package com.example.musicplayer.io;

import android.graphics.Bitmap;

import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.Util;

import java.nio.ByteBuffer;

/**
 * Created by Tarik on 15.11.2015.
 */
public class Decoder {

    private static native void nInit();
    private static native int nNewInstance();
    private static native void nReleaseInstance(int handle);
    private static native void nSetVideoSize(int handle, int width, int height);
    private static native boolean nExtractAlbumArt(int decHandle, int tagHandle, byte[] buffer);

    private int mHandle;

    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private byte[] mBuffer;

    public Decoder() {
        if (MusicApplication.isNativeLoaded())
            mHandle = nNewInstance();
        else
            Util.loge("Decoder", "Decoder: Could not create instance: library not loaded");
    }

    public void release() {
        if (mHandle == 0)
            return;

        nReleaseInstance(mHandle);

        if (mBitmap != null)
            mBitmap.recycle();

        mBuffer = null;
    }

    public Bitmap getCachedBitmap() {
        return mBitmap;
    }

    public void setVideoSize(int width, int height) {
        if (mHandle == 0 || (width == mWidth && height == mHeight))
            return;

        mWidth = width;
        mHeight = height;

        nSetVideoSize(mHandle, width, height);

        if (mBitmap != null)
            mBitmap.recycle();

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mBuffer = new byte[mBitmap.getByteCount()];
    }

    public Bitmap readAlbumArt(MediaTag tag) {
        if (mHandle == 0)
            return null;

        if (mBuffer == null)
            throw new IllegalStateException("Video size is not set.");

        if (tag == null)
            throw new IllegalArgumentException("MediaTag may not be null.");

        if (nExtractAlbumArt(mHandle, tag.getHandle(), mBuffer)) {
            mBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mBuffer));
            return mBitmap;
        }

        return null;
    }

    public static void initialize() {
        nInit();
    }
}
