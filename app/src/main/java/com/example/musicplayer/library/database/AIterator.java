package com.example.musicplayer.library.database;

import android.database.Cursor;

/**
 * Created by 19tar on 06.09.2017.
 */

abstract class AIterator {
    private Cursor mCursor;
    private boolean mFirst;
    private int[] mColumnIndices;

    public AIterator(Cursor cursor, boolean idOnly) {
        mCursor = cursor;
        mFirst = false;

        Enum[] columns = getColumns();

        int itemsCount = columns.length;
        if (itemsCount > 1 && idOnly)
            itemsCount = 1;

        mColumnIndices = new int[itemsCount];

        for (int i = 0; i < itemsCount; i++)
            mColumnIndices[i] = mCursor.getColumnIndex(columns[i].name());
    }

    public void reset() {
        mFirst = false;
    }

    // checks if this cursor is empty and then closes it
    public boolean closeEmpty() {
        boolean empty = !next();
        close();
        return empty;
    }

    public boolean next() {
        if (!mFirst) {
            mFirst = true;
            return mCursor.moveToFirst();
        }
        else
            return mCursor.moveToNext();
    }

    public void close() {
        mCursor.close();
    }

    // should be first column in all tables
    public int getId() {
        return getInt(0);
    }

    public int getSize() {
        return mCursor.getCount();
    }

    protected abstract Enum[] getColumns();// {
//            return null;
//        }

    protected String getString(Enum e) {
        return getString(e.ordinal());
    }

    protected String getString(int colIndex) {
        return mCursor.getString(mColumnIndices[colIndex]);
    }

    protected boolean getBoolean(Enum e) {
        int value = getInt(e);
        return value != 0;
    }

    protected int getInt(Enum e) {
        return getInt(e.ordinal());
    }

    protected int getInt(int colIndex) {
        return mCursor.getInt(mColumnIndices[colIndex]);
    }

    protected long getLong(Enum e) {
        return getLong(e.ordinal());
    }

    protected long getLong(int colIndex) {
        return mCursor.getLong(mColumnIndices[colIndex]);
    }
}
