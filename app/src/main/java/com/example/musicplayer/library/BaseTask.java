package com.example.musicplayer.library;

import android.os.Handler;

import com.example.musicplayer.library.database.Helper;

/**
 * Created by 19tar on 04.10.2017.
 */

abstract class BaseTask {

    private Runnable mBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            onRunBackground();

            mHandler.post(mFinishRunnable);
        }
    };

    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            onUpdate();
        }
    };

    private Runnable mFinishRunnable = new Runnable() {
        @Override
        public void run() {
            onFinish();
        }
    };

    private boolean mCancelRequested;
    private Handler mHandler;

    private MusicLibrary.DataUpdater mUpdater;

    private Helper mDBHelper;
    private boolean mTransactionActive;

    BaseTask(Helper helper, MusicLibrary.DataUpdater updater) {
        mDBHelper = helper;
        mUpdater = updater;

        mHandler = new Handler();

        mCancelRequested = false;
        mTransactionActive = false;
    }

    public void start() {
        onStart();

        new Thread(mBackgroundRunnable).start();
    }

    public synchronized void cancel() {
        mCancelRequested = true;
    }

    public synchronized boolean isCancelled() {
        return mCancelRequested;
    }

    protected void publishProgress() {
        mHandler.post(mUpdateRunnable);
    }

    protected MusicLibrary.DataUpdater getDataUpdater() {
        return mUpdater;
    }

    protected Helper getDBHelper() {
        return mDBHelper;
    }

    protected void beginTransaction() {
        if (!mTransactionActive) {
            mTransactionActive = true;
            mDBHelper.beginTransaction();
        }
    }

    protected void endTransaction() {
        if (mTransactionActive) {
            mTransactionActive = false;
            mDBHelper.setTransactionSuccessful();
            mDBHelper.endTransaction();
        }
    }

    protected abstract void onStart();
    protected abstract void onRunBackground();
    protected abstract void onUpdate();
    protected abstract void onFinish();
}
