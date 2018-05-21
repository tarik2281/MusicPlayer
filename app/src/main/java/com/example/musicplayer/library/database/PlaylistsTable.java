package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class PlaylistsTable extends IDTable<PlaylistsTable.Iterator> {

    public static class Iterator extends AIterator {

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public Playlist getPlaylist() {
            return new Playlist(getId(), getString(Columns.name),
                    getString(Columns.file_path));
        }

        public String getName() {
            return getString(Columns.name);
        }

        public boolean isAutomatic() {
            return getBoolean(Columns.automatic);
        }
    }

    public enum Columns {
        id, name, name_filter, file_path, automatic
    }

    private static final int FALSE = 0;
    private static final int TRUE = 1;

    private static final String NAME = "playlists";
    private static final String CREATE_CMD = "CREATE TABLE playlists(" +
            "id INTEGER PRIMARY KEY, name TEXT, name_filter TEXT, " +
            "file_path TEXT, automatic INTEGER)";

    public PlaylistsTable() {
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

    public int insert(String name, String importPath) {
        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), name);
        values.put(Columns.name_filter.name(), getFilterText(name));
        values.put(Columns.file_path.name(), importPath);
        values.put(Columns.automatic.name(), FALSE);
        return insert(values);
    }

    public int insertAutomatic(String name) {
        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), name);
        values.put(Columns.automatic.name(), TRUE);
        return insert(values);
    }

    public Iterator get(String filter) {
        Selection selection = new Selection();

        selection.addEqualsStatement(NAME, String.valueOf(FALSE), Columns.automatic);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    @Override
    public void deleteAll() {
        delete(Columns.automatic + "=?", new String[] { String.valueOf(FALSE) });
    }

    public void updateName(int id, String name) {
        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), name);
        values.put(Columns.name_filter.name(), getFilterText(name));
        update(id, values);
    }

    static void addFilter(Selection selection, String filter) {
        selection.addLikeConditions(NAME, getFilterText(filter), Columns.name_filter);
    }
}
