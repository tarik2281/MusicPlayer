package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.Genre;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class GenresTable extends IDTable<GenresTable.Iterator> {
    public static class Iterator extends AIterator {

        private Genre.Builder mBuilder;

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);

            if (!idOnly)
                mBuilder = new Genre.Builder();
        }

        @Override
        protected Enum[] getColumns() { return Columns.values(); }

        public Genre getGenre() {
            return mBuilder.setId(getId())
                    .setName(getString(Columns.name)).build();
        }
    }

    public enum Columns {
        id, name, name_filter
    }

    public static final String NAME = "genres";
    private static String CREATE_CMD = "CREATE TABLE " + NAME + "(" +
            Columns.id + " INTEGER PRIMARY KEY, " +
            Columns.name + " TEXT, " +
            Columns.name_filter + " TEXT)";


    public GenresTable() {
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
        createIndex(Columns.name);
    }

    public int getId(String genre) {
        if (genre == null) genre = "";
        return getId(new String[] { genre }, Columns.name);
    }

    public int insert(String genre) {
        if (genre == null) genre = "";

        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), genre);
        values.put(Columns.name_filter.name(), getFilterText(genre));
        return insert(values);
    }

    // null for filter to get all
    public Iterator get(String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter))
            addFilter(filter, selection);

        return select(selection);
    }

    static void addFilter(String filter, Selection selection) {
        selection.addLikeConditions(NAME, getFilterText(filter), Columns.name_filter);
    }
}
