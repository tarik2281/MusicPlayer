package com.example.musicplayer.ui.fragments.options;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowArtistRequest;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.library.Sorting;

public class AlbumOptionsHandler extends MenuOptionsHandler<Album> {

    private boolean mShowArtist;

    public AlbumOptionsHandler() {
        super(Album.class);

        mShowArtist = true;
    }

    public void setShowArtist(boolean showArtist) {
        mShowArtist = showArtist;
    }

    @Override
    protected void onCreateMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.options_album, menu);

        menu.findItem(R.id.option_show_artist).setVisible(mShowArtist).setEnabled(mShowArtist);
    }

    @Override
    protected boolean onMenuItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_show_artist: {
                RequestManager.getInstance().pushRequest(new ShowArtistRequest(getItem().getArtistId()));
                return true;
            }
        }

        return super.onMenuItemSelected(item);
    }

    @Override
    protected Sorting getSorting() {
        return Sorting.Number;
    }
}
