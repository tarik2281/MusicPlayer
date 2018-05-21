package com.example.musicplayer;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.example.musicplayer.playback.PlaybackList;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.ui.dialogs.KeepQueueDialog;

/**
 * Created by 19tar on 21.09.2017.
 */

public class KeepQueueDelegate {

    public static final String KEY_KEEP_QUEUE_TYPE = "keep_queue_type";
    public static final String TAG_DIALOG = "keep_queue_dialog";

    public enum Type {
        Ask, Keep, Delete
    }

    private Type mType;
    private boolean mShouldKeep;

    public KeepQueueDelegate() {
        mType = Type.values()[CacheManager.getInstance().getInteger(KEY_KEEP_QUEUE_TYPE, Type.Ask.ordinal())];
    }

    public boolean checkKeepQueue(Fragment fragment) {
        PlaybackList list = PlaybackState.getInstance().getPlaybackList();
        if (list != null && !list.isQueueEmpty() && mType == Type.Ask) {
            KeepQueueDialog.newInstance().show(fragment.getChildFragmentManager(), TAG_DIALOG);
            return false;
        }

        return true;
    }

    public boolean shouldKeepQueue() {
        switch (mType) {
            case Keep:
                return true;
            case Delete:
                return false;
            case Ask:
                return mShouldKeep;
        }

        return false;
    }

    public boolean onDialogDismiss(DialogFragment dialog, String tag) {
        if (TAG_DIALOG.equals(tag)) {
            KeepQueueDialog keepQueueDialog = (KeepQueueDialog)dialog;
            mShouldKeep = keepQueueDialog.getResult();

            if (keepQueueDialog.shouldRemember()) {
                mType = mShouldKeep ? Type.Keep : Type.Delete;
                CacheManager.getInstance().setInteger(KEY_KEEP_QUEUE_TYPE, mType.ordinal());
            }

            return true;
        }

        return false;
    }
}
