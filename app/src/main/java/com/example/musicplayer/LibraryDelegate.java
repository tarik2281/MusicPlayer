package com.example.musicplayer;

import android.support.v7.view.ActionMode;

/**
 * Created by 19tarik97 on 10.09.16.
 */
public interface LibraryDelegate {
    ActionMode startActionMode(ActionMode.Callback callback);

    void setHighlightedItem(int position);

    void invalidateTitle();
    void invalidateBarMenu();
}
