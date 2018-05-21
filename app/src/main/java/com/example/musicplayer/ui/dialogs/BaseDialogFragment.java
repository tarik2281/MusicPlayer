package com.example.musicplayer.ui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by 19tarik97 on 18.12.16.
 */

public class BaseDialogFragment extends DialogFragment {

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        Fragment fragment = getParentFragment();
        if (fragment instanceof OnDialogDismissListener)
            ((OnDialogDismissListener)fragment).onDialogDismiss(this, getTag());

        Activity activity = getActivity();
        if (activity instanceof OnDialogDismissListener)
            ((OnDialogDismissListener)activity).onDialogDismiss(this, getTag());
    }
}
