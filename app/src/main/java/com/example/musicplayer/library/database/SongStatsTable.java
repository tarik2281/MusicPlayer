package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by 19tar on 07.09.2017.
 */

public class SongStatsTable extends IDTable<SongStatsTable.Iterator> {
    public class Iterator extends AIterator {

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public long getDateAdded() {
            return getLong(Columns.date_added);
        }

        public int getPresetId() {
            return getInt(Columns.preset_id);
        }

        public int getPlayCount() {
            return getInt(Columns.play_count);
        }

        public long getLastPlayed() {
            return getLong(Columns.last_played);
        }
    }

    public enum Columns {
        id, date_added, preset_id, play_count, last_played
    }

    private static final String LIMIT = "100";

    public static final String NAME = "song_stats";
    private static final String CREATE_CMD = "CREATE TABLE song_stats(" +
            "id INTEGER PRIMARY KEY, date_added INTEGER, preset_id INTEGER," +
            " play_count INTEGER, last_played INTEGER)";

    public SongStatsTable() {
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

    public void insert(int songId) {
        ContentValues values = new ContentValues();
        values.put(Columns.id.name(), songId);
        values.put(Columns.date_added.name(), currentTime());
        values.put(Columns.preset_id.name(), 0);
        values.put(Columns.play_count.name(), 0);
        values.put(Columns.last_played.name(), 0);
        insert(values);
    }

    public void updateAll(int songId, long dateAdded, int presetId, int playCount,
                          long lastPlayed) {
        ContentValues values = new ContentValues();
        values.put(Columns.date_added.name(), dateAdded);
        values.put(Columns.preset_id.name(), presetId);
        values.put(Columns.play_count.name(), playCount);
        values.put(Columns.last_played.name(), lastPlayed);
        update(songId, values);
    }

    public void updatePresetId(int songId, int presetId) {
        ContentValues values = new ContentValues();
        values.put(Columns.preset_id.name(), presetId);
        update(songId, values);
    }

    public void updateSongPlayed(int id) {
        execSQL("UPDATE song_stats SET play_count = play_count + 1, last_played = ? WHERE id = ?",
                new Object[] { String.valueOf(currentTime()), String.valueOf(id) });
    }

    public Iterator getRecentlyAdded() {
        return select(null, "date_added DESC", LIMIT);
    }

    public Iterator getLastPlayed() {
        Selection selection = new Selection();
        selection.addEqualsStatement(NAME, String.valueOf(0), Columns.last_played, true);
        return select(selection, "last_played DESC", LIMIT);
    }

    public Iterator getMostPlayed() {
        Selection selection = new Selection();
        selection.addEqualsStatement(NAME, String.valueOf(0), Columns.play_count, true);
        return select(selection, "play_count DESC", LIMIT);
    }
}
