package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.EqualizerPreset;

/**
 * Created by 19tar on 06.09.2017.
 */

public class PresetsTable extends IDTable<PresetsTable.Iterator> {

    public static class Iterator extends AIterator {

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public EqualizerPreset getPreset() {
            return new EqualizerPreset(getId(), getString(Columns.name),
                    getString(Columns.band_gains), getBoolean(Columns.prebuilt));
        }
    }

    public enum Columns {
        id, name, band_gains, prebuilt
    }

    public static final String NAME = "presets";
    public static final String CREATE_CMD = "CREATE TABLE presets(" +
            "id INTEGER PRIMARY KEY, name TEXT, band_gains TEXT, prebuilt INTEGER)";

    public PresetsTable() {
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

    public int insert(String name, String bandGains, boolean prebuilt) {
        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), name);
        values.put(Columns.band_gains.name(), bandGains);
        values.put(Columns.prebuilt.name(), prebuilt);
        return insert(values);
    }

    public void insertId(int id, String name, String bandGains, boolean prebuilt) {
        ContentValues values = new ContentValues();
        values.put(Columns.id.name(), id);
        values.put(Columns.name.name(), name);
        values.put(Columns.band_gains.name(), bandGains);
        values.put(Columns.prebuilt.name(), prebuilt);
        insert(values);
    }

    public void updateName(int id, String name) {
        ContentValues values = new ContentValues();
        values.put(Columns.name.name(), name);
        update(id, values);
    }

    public void updateGains(int id, String bandGains) {
        ContentValues values = new ContentValues();
        values.put(Columns.band_gains.name(), bandGains);
        update(id, values);
    }
}
