package com.example.musicplayer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Created by 19tarik97 on 09.09.16.
 */
public class ConfirmPreference extends DialogPreference {
    public interface Callback {
        void onConfirm();
    }

    private Callback mCallback;

    public ConfirmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (mCallback != null && positiveResult)
            mCallback.onConfirm();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
