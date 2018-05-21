package com.example.musicplayer.library.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 19tar on 06.09.2017.
 */

public class Helper extends SQLiteOpenHelper {

    private static final int VERSION = 14;
    private static final String NAME = "Library.db";

    private enum Tables {
        // simplifies adding new tables to this class

        artists(ArtistsTable.class),
        albums(AlbumsTable.class),
        genres(GenresTable.class),
        folders(FoldersTable.class),
        genreObjects(GenreObjectsTable.class),
        songs(SongsTable.class),
        playlists(PlaylistsTable.class),
        presets(PresetsTable.class),
        songStats(SongStatsTable.class);

        private final Class<? extends ObjectTable> mClass;

        Tables(Class<? extends ObjectTable> clazz) {
            mClass = clazz;
        }

        Class<? extends ObjectTable> getTableClass() {
            return mClass;
        }
    }

    private SQLiteDatabase mDB;

    private ObjectTable[] mTables;

    public Helper(Context context) {
        super(context, NAME, null, VERSION);
    }

    public void initialize() {
        mTables = new ObjectTable[Tables.values().length];
        for (int i = 0; i < Tables.values().length; i++) {
            try {
                // create all table instances
                mTables[i] = Tables.values()[i].getTableClass().newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        mDB = getWritableDatabase();
        mDB.enableWriteAheadLogging();

        for (ObjectTable mTable : mTables) {
            mTable.setDatabase(mDB);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (ObjectTable mTable : mTables) mTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // alter tables for new versions
    }

    public void createIndices() {
        for (ObjectTable mTable : mTables) mTable.createIndex();
    }

    public void dropIndices() {
        for (ObjectTable mTable : mTables) mTable.dropIndex();
    }

    public void beginTransaction() {
        mDB.beginTransactionNonExclusive();
    }

    public void setTransactionSuccessful() {
        mDB.setTransactionSuccessful();
    }

    public void endTransaction() {
        mDB.endTransaction();
    }

    public ArtistsTable getArtists() {
        return (ArtistsTable)mTables[Tables.artists.ordinal()];
    }

    public AlbumsTable getAlbums() {
        return (AlbumsTable)mTables[Tables.albums.ordinal()];
    }

    public GenresTable getGenres() {
        return (GenresTable) mTables[Tables.genres.ordinal()];
    }

    public GenreObjectsTable getGenreObjects() {
        return (GenreObjectsTable) mTables[Tables.genreObjects.ordinal()];
    }

    public FoldersTable getFolders() {
        return (FoldersTable)mTables[Tables.folders.ordinal()];
    }

    public SongsTable getSongs() {
        return (SongsTable) mTables[Tables.songs.ordinal()];
    }

    public PlaylistsTable getPlaylists() {
        return (PlaylistsTable)mTables[Tables.playlists.ordinal()];
    }

    public PresetsTable getPresets() {
        return (PresetsTable)mTables[Tables.presets.ordinal()];
    }

    public SongStatsTable getSongStats() {
        return (SongStatsTable) mTables[Tables.songStats.ordinal()];
    }
}
