package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.example.musicplayer.Util;

import java.text.Normalizer;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by 19tar on 06.09.2017.
 */

abstract class ObjectTable<IT extends AIterator> {

    private static Pattern sFilterPattern = Pattern.compile("[^\\p{ASCII}]");

    private SQLiteDatabase mDB;

    public ObjectTable() {

    }

    protected abstract String getName();
    protected abstract String getCreateCmd();
    protected abstract IT getIterator(Cursor c, boolean idOnly);

    void setDatabase(SQLiteDatabase db) {
        mDB = db;
    }

    public void createTable(SQLiteDatabase db) {
        db.execSQL(getCreateCmd());
    }

    public void createIndex() {

    }

    public void dropIndex() {
        mDB.execSQL("DROP INDEX IF EXISTS " + getIndexName());
    }

    protected void createIndex(Enum... columns) {
        String name = getName() + "_idx";
        StringBuilder builder = new StringBuilder("CREATE INDEX IF NOT EXISTS ");

        builder.append(name).append(" ON ").append(getName()).append("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0)
                builder.append(", ");

            builder.append(columns[i].name());
        }
        builder.append(")");

        mDB.execSQL(builder.toString());
    }

    private String getIndexName() {
        return getName() + "_idx";
    }

    protected int insert(ContentValues values) {
        return (int)mDB.insertWithOnConflict(getName(), null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public IT readAll() {
        Cursor c = mDB.query(getName(), null, null, null, null, null, null);
        return getIterator(c, false);
    }

    protected IT select(@Nullable String[] projection, Selection selection) {
        return select(projection, selection, null, null);
    }

    protected IT select(@Nullable String[] projection, Selection selection, String order, String limit) {
        try {
            mDB.yieldIfContendedSafely();
        }
        catch (IllegalStateException e) {
            //e.printStackTrace();
        }

        String select = null;
        String[] args = null;
        if (selection != null) {
            select = selection.getSelection();
            args = selection.getArgs();
        }

        mDB.beginTransactionNonExclusive();
        Cursor c = mDB.query(getName(), projection, select, args, null, null, order, limit);
        mDB.setTransactionSuccessful();
        mDB.endTransaction();

        return getIterator(c, true);
    }

    protected IT rawQuery(Selection selection) {
        try {
            //mDB.yieldIfContendedSafely();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mDB.beginTransactionNonExclusive();
        Cursor c = mDB.rawQuery(selection.getSelection(), selection.getArgs());
        mDB.setTransactionSuccessful();
        mDB.endTransaction();

        return getIterator(c, true);
    }

    protected void execSQL(String sql, Object[] args) {
        if (args == null)
            mDB.execSQL(sql);
        else
            mDB.execSQL(sql, args);
    }

    public void deleteAll() {
        delete(null, null);
    }

    protected void delete(String selection, String[] args) {
        mDB.delete(getName(), selection, args);
    }


    static String getFilterText(String text) {
        if (Util.stringIsEmpty(text))
            return null;

        return sFilterPattern.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("").toUpperCase();
    }

    static long currentTime() {
        return new Date().getTime() / 1000;
    }
}
