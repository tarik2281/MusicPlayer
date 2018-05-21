package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.Folder;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class FoldersTable extends IDTable<FoldersTable.Iterator> {
    public static class Iterator extends AIterator {

        private Folder.Builder mBuilder;

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);

            if (!idOnly)
                mBuilder = new Folder.Builder();
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public Folder getFolder() {
            return mBuilder.setId(getId())
                    .setPath(getInt(Columns.parent_id), getString(Columns.path))
                    .setName(getString(Columns.name))
                    .setIgnoreType(Folder.IgnoreType.values()[getInt(Columns.ignored)])
                    .setSongCount(getInt(Columns.song_count))
                    .setFolderCount(getInt(Columns.folder_count)).build();
        }
    }

    public enum Columns {
        id, parent_id, name, path, name_filter, path_filter, song_count, folder_count, ignored
    }

    public static final String NAME = "folders";
    private static final String CREATE_CMD = "CREATE TABLE " + NAME + "(" +
            Columns.id + " INTEGER PRIMARY KEY, " +
            Columns.parent_id + " INTEGER, " + // DEFAULT 0 REFERENCES folders(id) ON DELETE SET DEFAULT, " +
            Columns.name + " TEXT, " +
            Columns.path + " TEXT, " +
            Columns.name_filter + " TEXT, " +
            Columns.path_filter + " TEXT, " +
            Columns.song_count + " INTEGER, " +
            Columns.folder_count + " INTEGER, " +
            Columns.ignored + " INTEGER)";

    public FoldersTable() {
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

    public int getId(int parentId, String name) {
        return getId(new String[] { String.valueOf(parentId), name }, Columns.parent_id, Columns.name);
    }

    public int insert(int parentId, String name, String path, Folder.IgnoreType ignored) {
        ContentValues values = new ContentValues();
        values.put(Columns.parent_id.name(), parentId);
        values.put(Columns.name.name(), name);
        values.put(Columns.path.name(), path);
        values.put(Columns.name_filter.name(), getFilterText(name));
        values.put(Columns.path_filter.name(), getFilterText(path));
        values.put(Columns.song_count.name(), 0);
        values.put(Columns.folder_count.name(), 0);
        values.put(Columns.ignored.name(), ignored.ordinal());
        return insert(values);
    }

    public Iterator get(String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    public Iterator get(int parentId, String filter) {
        Selection selection = new Selection();

        selection.addEqualsStatement(NAME, String.valueOf(parentId), Columns.parent_id);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter);

        return select(selection);
    }

    public void updateIgnored(int id, Folder.IgnoreType ignore) {
        ContentValues values = new ContentValues();
        values.put(Columns.ignored.name(), ignore.ordinal());
        update(id, values);
    }

    public void updateSongCount(int id, int songCount) {
        ContentValues values = new ContentValues();
        values.put(Columns.song_count.name(), songCount);
        update(id, values);
    }

    public void updateFolderCount(int id, int folderCount) {
        ContentValues values = new ContentValues();
        values.put(Columns.folder_count.name(), folderCount);
        update(id, values);
    }

    static void addFilter(Selection selection, String filter) {
        selection.addLikeConditions(NAME, getFilterText(filter),
                Columns.name_filter, Columns.path_filter);
    }
}
