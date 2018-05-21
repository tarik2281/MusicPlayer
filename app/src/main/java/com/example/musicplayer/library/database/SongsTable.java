package com.example.musicplayer.library.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.Util;

/**
 * Created by 19tar on 06.09.2017.
 */

public class SongsTable extends IDTable<SongsTable.Iterator> {

    public static class Iterator extends AIterator {

        private Song.Builder mBuilder;

        public Iterator(Cursor cursor, boolean idOnly) {
            super(cursor, idOnly);

            if (!idOnly)
                mBuilder = new Song.Builder();
        }

        @Override
        protected Enum[] getColumns() {
            return Columns.values();
        }

        public Song getSong() {
            return mBuilder.setId(getId())
                    .setTitle(getString(Columns.title))
                    .setArtistId(getInt(Columns.artist_id))
                    .setArtist(getString(Columns.artist))
                    .setAlbumArtist(getString(Columns.albumartist))
                    .setAlbum(getInt(Columns.album_id), getString(Columns.album))
                    .setGenre(getInt(Columns.genre_id), getString(Columns.genre))
                    .setTitleNumber(getInt(Columns.titlenumber), getInt(Columns.numtitles))
                    .setDiscNumber(getInt(Columns.discnumber), getInt(Columns.numdiscs))
                    .setYear(getInt(Columns.year))
                    .setDuration(getLong(Columns.duration))
                    .setFolderId(getInt(Columns.folder_id))
                    .setFilePath(getString(Columns.filepath))
                    .setLastModified(getLong(Columns.last_modified))
                    .setIsHidden(getBoolean(Columns.is_hidden)).build();
        }
    }

    public enum Columns {
        id, title, artist, artist_id, albumartist, album, album_id, genre,
        genre_id, title_filter, artist_filter, albumartist_filter, album_filter, genre_filter,
        filename_filter, titlenumber, numtitles, discnumber, numdiscs, year, duration, folder_id,
        filepath, filename, last_modified, is_hidden
    }

    public static final String NAME = "songs";
    public static final String CREATE_CMD = "CREATE TABLE songs(id INTEGER PRIMARY KEY, " +
            "title TEXT, artist TEXT, artist_id INTEGER, albumartist TEXT, " +
            "album TEXT, album_id INTEGER, genre TEXT, genre_id INTEGER, title_filter TEXT, " +
            "artist_filter TEXT, albumartist_filter TEXT, album_filter TEXT, genre_filter TEXT, " +
            "filename_filter TEXT, titlenumber INTEGER, numtitles INTEGER, discnumber INTEGER, " +
            "numdiscs INTEGER, year INTEGER, duration INTEGER, folder_id INTEGER, filepath TEXT, " +
            "filename TEXT, last_modified INTEGER, is_hidden INTEGER)";

    private static final int FALSE = 0;
    private static final int TRUE = 1;

    public SongsTable() {
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
        createIndex(Columns.filename);
    }

    public int getId(int folderId, String fileName) {
        return getId(new String[] { String.valueOf(folderId), fileName }, Columns.folder_id, Columns.filename);
    }

    public int getIdByFilePath(String filePath) {
        return getId(new String[] { filePath }, Columns.filepath);
    }

    public int insert(Song.Info info) {
        ContentValues values = new ContentValues();
        addInfoValues(info, values);

        values.put(Columns.is_hidden.name(), false);
        return insert(values);
    }

    public void updateHidden(int id, boolean hidden) {
        ContentValues values = new ContentValues();
        values.put(Columns.is_hidden.name(), hidden);
        update(id, values);
    }

    public void updateSongInfo(int id, Song.Info info) {
        ContentValues values = new ContentValues();
        addInfoValues(info, values);
        update(id, values);
    }

    public Iterator get(String filter, boolean useFileName) {
        Selection selection = new Selection();

        selection.addEqualsStatement(NAME, String.valueOf(FALSE), Columns.is_hidden);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter, useFileName);

        return select(selection);
    }

    public Iterator getForObject(LibraryObject object, String filter, boolean useFileName) {
        return getForObject(object.getType(), object.getId(), filter, useFileName);
    }

    public Iterator getForObject(int objectType, int objectId, String filter, boolean useFileName) {
        Selection selection = new Selection();

        Columns column = null;
        switch (objectType) {
            case LibraryObject.ARTIST:
                column = Columns.artist_id;
                break;
            case LibraryObject.ALBUM:
                column = Columns.album_id;
                break;
            case LibraryObject.GENRE:
                column = Columns.genre_id;
                break;
            case LibraryObject.FOLDER:
                column = Columns.folder_id;
                break;
        }

        selection.addEqualsStatement(NAME, String.valueOf(objectId), column);
        selection.addConjunction();
        selection.addEqualsStatement(NAME, String.valueOf(FALSE), Columns.is_hidden);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter, useFileName);

        return select(selection);
    }

    public Iterator getHidden(String filter, boolean useFileName) {
        Selection selection = new Selection();

        selection.addEqualsStatement(NAME, String.valueOf(TRUE), Columns.is_hidden);

        if (!Util.stringIsEmpty(filter))
            addFilter(selection, filter, useFileName);

        return select(selection);
    }

    static void addFilter(Selection selection, String filter, boolean useFileName) {
        String filterText = getFilterText(filter);

        selection.addConjunction();

        Columns column = Columns.title_filter;
        if (useFileName)
            column = Columns.filename_filter;

        selection.beginCond();

        selection.addLikeStatement(NAME, filterText, column);

        selection.addDisjunction();

        selection.addLikeStatement(NAME, filterText, Columns.album_filter);

        selection.addDisjunction();

        selection.beginCond();
        // artist_filter is null
        selection.addNullCheck(NAME, Columns.artist_filter, false);
        selection.addConjunction();
        selection.addLikeStatement(NAME, filterText, Columns.albumartist_filter);
        selection.endCond();

        selection.addDisjunction();

        selection.beginCond();
        // artist_filter is not null
        selection.addNullCheck(NAME, Columns.artist_filter, true);
        selection.addConjunction();;
        selection.addLikeStatement(NAME, filterText, Columns.artist_filter);
        selection.endCond();

        selection.endCond();
    }

    private void addInfoValues(Song.Info info, ContentValues values) {
        String fileName = Util.getFileNameExtension(info.getFilePath());
        values.put(Columns.title.name(), info.getTitle());
        values.put(Columns.artist_id.name(), info.getArtistId());
        values.put(Columns.artist.name(), info.getArtist());
        values.put(Columns.albumartist.name(), info.getAlbumArtist());
        values.put(Columns.album_id.name(), info.getAlbumId());
        values.put(Columns.album.name(), info.getAlbum());
        values.put(Columns.genre.name(), info.getGenre());
        values.put(Columns.genre_id.name(), info.getGenreId());
        values.put(Columns.title_filter.name(), getFilterText(info.getTitle()));
        values.put(Columns.artist_filter.name(), getFilterText(info.getArtist()));
        values.put(Columns.albumartist_filter.name(), getFilterText(info.getAlbumArtist()));
        values.put(Columns.album_filter.name(), getFilterText(info.getAlbum()));
        values.put(Columns.genre_filter.name(), getFilterText(info.getGenre()));
        values.put(Columns.filename_filter.name(), getFilterText(info.getFileName()));
        values.put(Columns.titlenumber.name(), info.getTitleNumber());
        values.put(Columns.numtitles.name(), info.getNumTitles());
        values.put(Columns.discnumber.name(), info.getDiscNumber());
        values.put(Columns.numdiscs.name(), info.getNumDiscs());
        values.put(Columns.year.name(), info.getYear());
        values.put(Columns.duration.name(), info.getDuration());
        values.put(Columns.folder_id.name(), info.getFolderId());
        values.put(Columns.filepath.name(), info.getFilePath());
        values.put(Columns.filename.name(), fileName);
        values.put(Columns.last_modified.name(), info.getLastModified());
    }
}
