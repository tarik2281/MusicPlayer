package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class GenreObjectsTable extends ObjectTable<GenreObjectsTable.Iterator> {
    public static class Iterator extends AIterator {

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }
    }

    public enum Columns {
        object_id, object_type, genre_id
    }

    public static final String NAME = "genreobjects";

    private static String CREATE_CMD = "CREATE TABLE " + NAME + "(" +
            Columns.object_id + " INTEGER, " +
            Columns.object_type + " INTEGER, " +
            Columns.genre_id + " INTEGER, PRIMARY KEY(" +
            Columns.object_id + ", " + Columns.object_type + ", " + Columns.genre_id + "))";

    public GenreObjectsTable() {
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

    public void delete(int objectId, int objectType, int genreId) {
        delete(Columns.object_id + "=? AND " + Columns.object_type + "=? AND " + Columns.genre_id + "=?",
                new String[] { String.valueOf(objectId), String.valueOf(objectType), String.valueOf(genreId) });
    }

    public void deleteGenre(int genreId) {
        delete(Columns.genre_id + "=?", new String[] { String.valueOf(genreId) });
    }

    public void insert(int objectId, int objectType, int genreId) {
        ContentValues values = new ContentValues();
        values.put(Columns.object_id.name(), objectId);
        values.put(Columns.object_type.name(), objectType);
        values.put(Columns.genre_id.name(), genreId);
        insert(values);
    }

    public Iterator getArtists(int genreId, String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter)) {
            addJoin(selection, genreId, ArtistsTable.NAME, LibraryObject.ARTIST);
            ArtistsTable.addFilter(selection, filter);
            return rawQuery(selection);
        }
        else {
            selection.addEqualsStatement(NAME, String.valueOf(LibraryObject.ARTIST), Columns.object_type);
            selection.addConjunction();
            selection.addEqualsStatement(NAME, String.valueOf(genreId), Columns.genre_id);
            return select(new String[] { Columns.object_id.name() }, selection);
        }
    }

    public Iterator getAlbums(int genreId, String filter) {
        Selection selection = new Selection();

        if (!Util.stringIsEmpty(filter)) {
            addJoin(selection, genreId, AlbumsTable.NAME, LibraryObject.ALBUM);
            AlbumsTable.addFilter(selection, filter);
            return rawQuery(selection);
        }
        else {
            selection.addEqualsStatement(NAME, String.valueOf(LibraryObject.ALBUM), Columns.object_type);
            selection.addConjunction();
            selection.addEqualsStatement(NAME, String.valueOf(genreId), Columns.genre_id);
            return select(new String[] { Columns.object_id.name() }, selection);
        }
    }

    // for joining with artists and albums
    static void addJoin(Selection selection, int genreId, String table, int objectType) {
        selection.getBuilder().append("SELECT object_id FROM genreobjects JOIN ")
                .append(table).append(" ON object_id=id WHERE object_type=? AND genre_id=?");
        selection.addArg(String.valueOf(objectType));
        selection.addArg(String.valueOf(genreId));
    }
}
