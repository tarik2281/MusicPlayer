package com.example.musicplayer.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import com.example.musicplayer.MemoryManager;
import com.example.musicplayer.R;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Thumbnails;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Tarik on 31.03.2016.
 */
public class AlbumArtCache implements MemoryManager.Trimmable {

    public static abstract class Callback {
        private WeakReference<AlbumArt> mAlbumArtRef;

        public void cancel() {
            AlbumArt albumArt = getAlbumArt();
            if (albumArt != null)
                albumArt.removeCallback(this);
        }

        @Nullable
        public AlbumArt getAlbumArt() {
            if (mAlbumArtRef == null)
                return null;

            return mAlbumArtRef.get();
        }

        public abstract void onArtLoad(Bitmap bitmap);

        public void onArtReleased() {

        }
    }

    public class AlbumArt {

        private class LoadTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                String filePath = Thumbnails.getInstance().getFile(mAlbumId).getAbsolutePath();

                Bitmap temp = getBitmap();
                if (temp == null)
                    return null;

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inBitmap = temp;
                opts.inMutable = true;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                BitmapFactory.decodeFile(filePath, opts);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mIsCached = true;

                callOnArtLoad();

                onTaskFinished();
            }
        }

        private int mAlbumId;
        private ArrayList<Callback> mCallbacks;

        private boolean mIsCancelled;
        private boolean mIsCached;
        private LoadTask mTask;
        private BitmapPool.Entry mBitmap;

        private AlbumArt(int albumId) {
            mAlbumId = albumId;
            mCallbacks = new ArrayList<>();

            mIsCancelled = false;
            mIsCached = false;
            mTask = null;
            mBitmap = null;
        }

        public int getAlbumId() {
            return mAlbumId;
        }

        public boolean isCached() {
            return mIsCached;
        }

        public Bitmap getBitmap() {
            if (mBitmap == null)
                return null;

            return mBitmap.get();
        }

        public void addCallback(Callback callback) {
            callback.mAlbumArtRef = new WeakReference<AlbumArt>(this);

            mCallbacks.add(callback);

            mIsCancelled = false;
        }

        public void removeCallback(Callback callback) {
            mCallbacks.remove(callback);

            callback.mAlbumArtRef = null;

            if (mCallbacks.size() == 0)
                mIsCancelled = true;
        }

        private void load() {
            mBitmap = mBitmapPool.getBitmap();

            mTask = new LoadTask();
            mTask.execute();
            //mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        private boolean isCancelled() {
            return mIsCancelled;
        }

        private void release() {
            if (mBitmap != null)
                mBitmap.release();

            mBitmap = null;
        }

        private void callOnArtLoad() {
            Bitmap bitmap = getBitmap();

            if (bitmap != null)
                for (Callback cb : mCallbacks)
                    cb.onArtLoad(bitmap);
        }

        private void callOnArtReleased() {
            for (Callback cb : mCallbacks)
                cb.onArtReleased();
        }
    }

    private static AlbumArtCache sSingleton;

    public static AlbumArtCache getInstance() {
        if (sSingleton == null)
            sSingleton = new AlbumArtCache();

        return sSingleton;
    }

    private static final int MESSAGE_INVALIDATE = 1;

    private boolean mInited;
    private int mNumActiveTasks;
    private int mMaxConcurrentTasks;

    private WeakHandler mHandler;
    private BitmapPool mBitmapPool;
    private LruCache<Integer, AlbumArt> mCache;
    private LinkedHashMap<Integer, AlbumArt> mQueue;

    private AlbumArtCache() {
        mNumActiveTasks = 0;
        mMaxConcurrentTasks = 10;
    }

    public void initialize(Context context) {
        if (!mInited) {
            final int size = context.getResources().getDimensionPixelSize(R.dimen.album_cover_size);

            mHandler = new WeakHandler(this);

            mBitmapPool = new BitmapPool(size, size, Bitmap.Config.RGB_565);

            mCache = new LruCache<Integer, AlbumArt>(10 * 1024 * 1024) {

                @Override
                protected int sizeOf(Integer key, AlbumArt value) {
                    return size * size * 2;
                }

                @Override
                protected void entryRemoved(boolean evicted, Integer key, AlbumArt oldValue, AlbumArt newValue) {
                    oldValue.callOnArtReleased();
                    oldValue.release();
                }
            };

            mQueue = new LinkedHashMap<>();

            MemoryManager.getInstance().registerForTrim(this);

            mInited = true;
        }
    }

    public void clearCache() {
        if (mCache != null)
            mCache.evictAll();
    }

    @Override
    public void onTrimMemory() {
        clearCache();

        if (mBitmapPool != null)
            mBitmapPool.release();

        System.gc();
    }

    public void invalidateAlbumArt(int albumId) {
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_INVALIDATE, albumId));
    }

    public AlbumArt getAlbumArt(int albumId) {
        // check if album art is already cached
        AlbumArt albumArt = mCache.get(albumId);

        if (albumArt == null) {
            // check if it is already enqueued
            albumArt = mQueue.get(albumId);

            if (albumArt == null) {
                if (!(Thumbnails.getInstance().getFile(albumId).exists()))
                    return null;

                albumArt = new AlbumArt(albumId);
                push(albumArt);
            }
        }

        return albumArt;
    }


    private void push(AlbumArt albumArt) {
        mQueue.put(albumArt.mAlbumId, albumArt);

        runTasks();
    }

    private void runTasks() {
        Iterator<Map.Entry<Integer, AlbumArt>> iterator = mQueue.entrySet().iterator();

        while (mNumActiveTasks < mMaxConcurrentTasks && iterator.hasNext()) {
            AlbumArt albumArt = iterator.next().getValue();

            if (!albumArt.isCancelled()) {
                mCache.put(albumArt.mAlbumId, albumArt);
                albumArt.load();
                mNumActiveTasks++;
            }

            iterator.remove();
        }
    }

    private void onTaskFinished() {
        mNumActiveTasks--;

        runTasks();
    }

    private static class WeakHandler extends Handler {

        private WeakReference<AlbumArtCache> mReference;

        public WeakHandler(AlbumArtCache cache) {
            mReference = new WeakReference<AlbumArtCache>(cache);
        }

        @Override
        public void handleMessage(Message msg) {
            AlbumArtCache cache = mReference.get();
            if (cache == null)
                return;

            switch (msg.what) {
                case MESSAGE_INVALIDATE:
                    int albumId = (int)msg.obj;
                    cache.mCache.remove(albumId);
                    break;
            }
        }
    }
}
