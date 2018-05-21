package com.example.musicplayer.backup;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.library.MusicLibrary;

import java.io.FileNotFoundException;

/**
 * Created by 19tar on 05.10.2017.
 */

public class RestoreTask extends AsyncTask<Void, Integer, Boolean> {

    public interface Callback {
        void onProgress(int progress);
        void onFinish(boolean result);
    }

    private MusicLibrary.Observer mLibraryObserver = new MusicLibrary.Observer() {
        @Override
        public void update(Observable sender, MusicLibrary.ObserverData data) {
            switch (data.type) {
                case ScanStateChanged:

                    synchronized (RestoreTask.this) {
                        mScanned = !data.scanState;

                        RestoreTask.this.notifyAll();
                    }

                    break;
            }
        }
    };

    public static final int PROGRESS_PREFS = 0;
    public static final int PROGRESS_SCAN = 1;
    public static final int PROGRESS_DATA = 2;

    private Uri mUri;
    private ContentResolver mResolver;
    private boolean mScanned;

    private Callback mCallback;

    public RestoreTask(Uri uri, ContentResolver resolver) {
        mUri = uri;
        mResolver = resolver;
        mScanned = false;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    @Override
    protected void onPreExecute() {
        MusicLibrary.getInstance().addObserver(mLibraryObserver);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        MusicLibrary.getInstance().removeObserver(mLibraryObserver);

        mCallback.onFinish(result);
    }

    @Override
    protected Boolean doInBackground(Void[] objects) {
        boolean result = false;

        BackupReader reader = new BackupReader();

        try {
            mResolver.openInputStream(mUri);

            BackupFile file = new BackupFile();
            result = reader.readFromStream(file, mResolver.openInputStream(mUri));
            if (result) {
                publishProgress(PROGRESS_PREFS);
                PreferenceManager.getInstance().restoreData(file);

                MusicLibrary.getInstance().prepareRestore();
                publishProgress(PROGRESS_SCAN);
                MusicLibrary.getInstance().postScanLibrary(false);

                synchronized (this) {
                    mScanned = false;
                    while (!mScanned) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                publishProgress(PROGRESS_DATA);
                MusicLibrary.getInstance().restoreData(file);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mCallback.onProgress(values[0]);
    }
}
