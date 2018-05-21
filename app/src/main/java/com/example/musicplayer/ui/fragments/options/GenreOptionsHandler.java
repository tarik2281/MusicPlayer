package com.example.musicplayer.ui.fragments.options;

import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Genre;
import com.example.musicplayer.library.Sorting;

/**
 * Created by 19tarik97 on 12.02.16.
 */
public class GenreOptionsHandler extends MenuOptionsHandler<Genre> {

    public GenreOptionsHandler() {
        super(Genre.class);
    }

    @Override
    protected void onCreateMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.options_genre, menu);
    }

    @Override
    protected Sorting getSorting() {
        return Sorting.Name;
    }
}
