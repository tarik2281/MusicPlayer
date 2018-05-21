package com.example.musicplayer.ui;

import android.graphics.Bitmap;

import java.util.Stack;

/**
 * Created by Tarik on 28.11.2015.
 */
public class BitmapPool {

    public class Entry {
        private Bitmap mBitmap;

        private Entry(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Bitmap get() {
            return mBitmap;
        }

        public void release() {
            mBitmaps.push(mBitmap);
        }
    }

    private Stack<Bitmap> mBitmaps;
    private int mWidth;
    private int mHeight;
    private Bitmap.Config mConfig;

    public BitmapPool(int width, int height, Bitmap.Config config) {
        mWidth = width;
        mHeight = height;
        mConfig = config;

        mBitmaps = new Stack<>();
    }

    public Entry getBitmap() {
        Entry entry;

        if (mBitmaps.isEmpty()) {
            entry = new Entry(Bitmap.createBitmap(mWidth, mHeight, mConfig));
        }
        else
            entry = new Entry(mBitmaps.pop());

        return entry;
    }

    public void release() {
        while (!mBitmaps.empty()) {
            Bitmap bitmap = mBitmaps.pop();
            bitmap.recycle();
        }
    }
}
