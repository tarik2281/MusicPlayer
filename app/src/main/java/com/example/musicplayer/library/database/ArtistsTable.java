package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.Artist;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class ArtistsTable extends IDTable<ArtistsTable.Iterator> {
    public static class Iterator extends AIterator {

        private Artist.Builder mBuilder;

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);

            if (!idOnly)
                mBuilder = new Artist.Builder();
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public Artist getArtist() {
            return mBuilder.setId(getId())
                    .setName(getString(Columns.name)).build();
        }
    }

    public enum Columns {
        id, name, name_filter
    }

    public static final String NAME = "artists";

    private static final String CREATE_CMD = "CREATE TABLE " + NAME + "(" +
            Columns.id + " INTEGER PRIMARY KEY, " +
            Columns.name + " TEXT, " +
            Columns.name_filter + " TEXT)";

    public ArtistsTable() {
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
    public void createIndex() {
        createIndex(Columns.name);
    }

    @Override
    protected ArtistsTable.Iterator getIterator(Cursor c, boolean idOnly) {
        return new ArtistsTable.Iterator(c, idOnly);
    }

    public int getId(String artist) {
        if (artist == null) artist = "";
        return getId(new String[] { artist }, Columns.name);
    }

    public int insert(String artist) {
        if (artist == null) artist = "";

        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), artist);
        values.put(Columns.name_filter.name(), getFilterText(artist));
        return insert(values);
    }

    public ArtistsTable.Iterator get(String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    static void addFilter(Selection selection, String filter) {
        selection.addLikeConditions(NAME, getFilterText(filter), ArtistsTable.Columns.name_filter);
    }
}
