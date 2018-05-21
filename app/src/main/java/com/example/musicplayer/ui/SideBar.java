package com.example.musicplayer.ui;

import android.view.Menu;

import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.fragments.library.AlbumGridFragment;
import com.example.musicplayer.ui.fragments.library.ArtistListFragment;
import com.example.musicplayer.ui.fragments.library.FolderListFragment;
import com.example.musicplayer.ui.fragments.library.GenreListFragment;
import com.example.musicplayer.ui.fragments.library.PlaylistListFragment;
import com.example.musicplayer.ui.fragments.library.SongListFragment;

/**
 * Created by 19tarik97 on 05.01.17.
 */

public class SideBar {
    public static final int ARTIST_FRAGMENT = 0;
    public static final int ALBUM_FRAGMENT = 1;
    public static final int SONG_FRAGMENT = 2;
    public static final int GENRE_FRAGMENT = 3;
    public static final int FOLDER_FRAGMENT = 4;
    public static final int PLAYLIST_FRAGMENT = 5;

    private int[] mItems;
    private int mStartingPosition;

    public int[] getItems() {
        if (mItems == null)
            loadItems();

        return mItems;
    }

    public int getStartingPosition() {
        return mStartingPosition;
    }

    public void loadItems() {
        PreferenceManager prefs = PreferenceManager.getInstance();
        String layoutString = prefs.getString(PreferenceManager.KEY_START_PAGE_LAYOUT);
        int startingItem = prefs.getInt(PreferenceManager.KEY_START_PAGE_LAUNCH);
        String[] itemStrings = layoutString.split(";");
        mItems = new int[itemStrings.length];

        for (int i = 0; i < itemStrings.length; i++) {
            mItems[i] = Integer.parseInt(itemStrings[i]);

            if (mItems[i] == startingItem) {
                mStartingPosition = i;
            }
        }
    }

    public void toMenu(Menu menu) {
        for (int item : getItems()) {
            menu.add(0, getIdForItem(item), 0, getTitleForItem(item))
                    .setIcon(getDrawableForItem(item));
        }
    }

    public static int getDrawableForItem(int item) {
        switch (item) {
            case ARTIST_FRAGMENT:
                return R.drawable.ic_mic_black_36dp;
            case ALBUM_FRAGMENT:
                return R.drawable.ic_album_black_36dp;
            case SONG_FRAGMENT:
                return R.drawable.music_node;
            case GENRE_FRAGMENT:
                return R.drawable.ic_straighten_black_36dp;
            case FOLDER_FRAGMENT:
                return R.drawable.ic_folder_black_36dp;
            case PLAYLIST_FRAGMENT:
                return R.drawable.music_playlist;
        }

        return 0;
    }

    public static int getIdForItem(int item) {
        switch (item) {
            case ARTIST_FRAGMENT:
                return R.id.item_artists;
            case ALBUM_FRAGMENT:
                return R.id.item_albums;
            case SONG_FRAGMENT:
                return R.id.item_songs;
            case GENRE_FRAGMENT:
                return R.id.item_genres;
            case FOLDER_FRAGMENT:
                return R.id.item_folders;
            case PLAYLIST_FRAGMENT:
                return R.id.item_playlists;
        }

        return 0;
    }

    public static int getTitleForItem(int item) {
        switch (item) {
            case ARTIST_FRAGMENT:
                return R.string.bar_item_artists;
            case ALBUM_FRAGMENT:
                return R.string.bar_item_albums;
            case SONG_FRAGMENT:
                return R.string.bar_item_songs;
            case GENRE_FRAGMENT:
                return R.string.bar_item_genres;
            case FOLDER_FRAGMENT:
                return R.string.bar_item_folders;
            case PLAYLIST_FRAGMENT:
                return R.string.bar_item_playlists;
        }

        return 0;
    }

    public static Class getClassForItem(int item) {
        switch (item) {
            case ARTIST_FRAGMENT:
                return ArtistListFragment.class;
            case ALBUM_FRAGMENT:
                return AlbumGridFragment.class;
            case SONG_FRAGMENT:
                return SongListFragment.class;
            case GENRE_FRAGMENT:
                return GenreListFragment.class;
            case FOLDER_FRAGMENT:
                return FolderListFragment.class;
            case PLAYLIST_FRAGMENT:
                return PlaylistListFragment.class;
        }

        return null;
    }

    public static int getItemById(int id) {
        switch (id) {
            case R.id.item_artists:
                return ARTIST_FRAGMENT;
            case R.id.item_albums:
                return ALBUM_FRAGMENT;
            case R.id.item_songs:
                return SONG_FRAGMENT;
            case R.id.item_genres:
                return GENRE_FRAGMENT;
            case R.id.item_folders:
                return FOLDER_FRAGMENT;
            case R.id.item_playlists:
                return PLAYLIST_FRAGMENT;
        }

        return 0;
    }
}
