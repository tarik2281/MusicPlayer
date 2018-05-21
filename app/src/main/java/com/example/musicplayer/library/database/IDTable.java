package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by 19tar on 06.09.2017.
 */

abstract class IDTable<IT extends AIterator> extends ObjectTable<IT> {
    private static final String ID_COLUMN = "id";
    private static final String[] ID_PROJECTION = { ID_COLUMN };

    private SQLiteDatabase mDB;

    public IDTable() {

    }

    @Override
    void setDatabase(SQLiteDatabase db) {
        super.setDatabase(db);
        mDB = db;
    }

    protected int getId(String[] selectionArgs, Enum... columns) {
        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < columns.length; ) {
            selection.append(columns[i].name()).append("=?");
            if (++i < columns.length)
                selection.append(" AND ");
        }
        Cursor c = mDB.query(getName(), ID_PROJECTION, selection.toString(), selectionArgs, null, null, null);
        int id = -1;
        if (c.moveToFirst())
            id = c.getInt(0);
        c.close();
        return id;
    }

    protected IT select(Selection selection) {
        return select(selection, null, null);
    }

    protected IT select(Selection selection, String order, String limit) {
        return select(ID_PROJECTION, selection, order, limit);
    }

    protected void update(int id, ContentValues values) {
        mDB.update(getName(), values, ID_COLUMN + "=?", new String[] { String.valueOf(id) });
    }

    public void delete(int id) {
        delete(ID_COLUMN + "=?", new String[] { String.valueOf(id) });
    }
}
