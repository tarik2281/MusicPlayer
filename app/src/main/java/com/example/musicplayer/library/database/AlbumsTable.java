package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.Album;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class AlbumsTable extends IDTable<AlbumsTable.Iterator> {
    public static class Iterator extends AIterator {

        private Album.Builder mBuilder;

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);

            if (!idOnly)
                mBuilder = new Album.Builder();
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public Album getAlbum() {
            return mBuilder.setId(getId())
                    .setArtistId(getInt(Columns.artist_id))
                    .setTitle(getString(Columns.title))
                    .setArtistName(getString(Columns.artist))
                    .setHasVariousArtists(getBoolean(Columns.various_artists))
                    .setSongCount(getInt(Columns.song_count))
                    .build();
        }
    }

    public enum Columns {
        id, artist_id, title, artist, title_filter, artist_filter, various_artists, song_count
    }

    public static final String NAME = "albums";

    private static final String CREATE_CMD = "CREATE TABLE " + NAME + "(" +
            Columns.id + " INTEGER PRIMARY KEY, " +
            Columns.artist_id + " INTEGER, " +
            Columns.title + " TEXT, " +
            Columns.artist + " TEXT, " +
            Columns.title_filter + " TEXT, " +
            Columns.artist_filter + " TEXT, " +
            Columns.various_artists + " INTEGER, " +
            Columns.song_count + " INTEGER)";

    public AlbumsTable() {
        super();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected String getCreateCmd() {
        return CREATE_CMD;
    }

    @Override
    protected Iterator getIterator(Cursor c, boolean idOnly) {
        return new Iterator(c, idOnly);
    }

    @Override
    public void createIndex() {
        createIndex(Columns.title);
    }

    public int getId(int artistId, String title) {
        if (title == null) title = "";
        return getId(new String[] { title, String.valueOf(artistId) }, Columns.title, Columns.artist_id);
    }

    public int insert(int artistId, String artist, String title) {
        if (artist == null) artist = "";
        if (title == null) title = "";

        ContentValues values = new ContentValues();
        values.put(Columns.artist_id.name(), artistId);
        values.put(Columns.title.name(), title);
        values.put(Columns.artist.name(), artist);
        values.put(Columns.title_filter.name(), getFilterText(title));
        values.put(Columns.artist_filter.name(), getFilterText(artist));
        return insert(values);
    }

    public void update(int id, boolean variousArtists, int songCount) {
        ContentValues values = new ContentValues();
        values.put(Columns.various_artists.name(), variousArtists);
        values.put(Columns.song_count.name(), songCount);
        update(id, values);
    }

    public Iterator get(String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    public Iterator getForArtist(String filter, int artistId) {
        Selection selection = new Selection();

        selection.addEqualsStatement(NAME, String.valueOf(artistId), Columns.artist_id);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    static void addFilter(Selection selection, String filter) {
        selection.addLikeConditions(NAME, getFilterText(filter),
                Columns.title_filter, Columns.artist_filter);
    }
}
