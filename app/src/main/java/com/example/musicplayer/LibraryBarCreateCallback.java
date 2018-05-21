package com.example.musicplayer;

import android.view.Menu;
import android.view.MenuInflater;

/**
 * Created by 19tarik97 on 10.09.16.
 */
public interface LibraryBarCreateCallback {
    boolean hasBarMenu();
    void onCreateBarMenu(MenuInflater inflater, Menu menu);
}
