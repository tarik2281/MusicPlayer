package com.example.musicplayer.library;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;

import com.example.musicplayer.TimeUtil;
import com.example.musicplayer.backup.BackupFile;
import com.example.musicplayer.io.Metadata;
import com.example.musicplayer.ui.AlbumArtCache;
import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.IObserver;
import com.example.musicplayer.io.MediaTag;
import com.example.musicplayer.Observable;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.io.PlaylistFile;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.database.AlbumsTable;
import com.example.musicplayer.library.database.ArtistsTable;
import com.example.musicplayer.library.database.FoldersTable;
import com.example.musicplayer.library.database.GenreObjectsTable;
import com.example.musicplayer.library.database.GenresTable;
import com.example.musicplayer.library.database.Helper;
import com.example.musicplayer.library.database.PlaylistsTable;
import com.example.musicplayer.library.database.PresetsTable;
import com.example.musicplayer.library.database.SongStatsTable;
import com.example.musicplayer.library.database.SongsTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by 19tarik97 on 21.03.16.
 */
public class MusicLibrary extends Observable<MusicLibrary.ObserverData> {

    public interface Observer extends IObserver<ObserverData> { }

    public static class ObserverData {
        public enum Type {
            LibraryUpdated, PlaylistsUpdated, SongPlayedUpdated,
            ScanStateChanged, SongsRemoved, SongsEdited, PresetsUpdated, SongUnhidden
        }

        public final Type type;
        public final int songId;
        public final boolean scanState;
        public final Collection<Song> songs;

        private ObserverData(Type type) {
            this.type = type;
            this.songId = 0;
            this.scanState = false;
            this.songs = null;
        }

        private ObserverData(Type type, int songId) {
            this.type = type;
            this.songId = songId;
            this.scanState = false;
            this.songs = null;
        }

        private ObserverData(Type type, boolean scanState) {
            this.type = type;
            this.songId = 0;
            this.scanState = scanState;
            this.songs = null;
        }

        private ObserverData(Type type, Collection<Song> songs) {
            this.type = type;
            this.songs = songs;
            this.songId = 0;
            this.scanState = false;
        }
    }

    private class ScanTask {

        private boolean mCancelRequested;

        private Set<String> mStoragePaths;
        private String[] mUnavailables;

        private long mLeastSongDuration;
        private boolean mNewLibrary;
        private MediaTag mMediaTag;
        private Metadata.Builder mBuilder;

        private int mNumProcessedSongs;
        private DataUpdater mUpdater;
        private Set<String> mPlaylistPaths;

        private Handler mHandler;

        private long mLastUpdateTime = 0;

        private TreeSet<Playlist> mPlaylists;
        private ArraySet<Integer> mRemovedSongs;
        private SparseArrayCompat<Folder> mFoldersClone;
        private SparseArrayCompat<Song> mSongsClone;

        private boolean mTransactionActive = false;

        private Runnable mMainRunnable = new Runnable() {
            @Override
            public void run() {
                mMediaTag = new MediaTag();
                mBuilder = new com.example.musicplayer.io.Metadata.Builder();
                mNumProcessedSongs = 0;
                mUpdater = new DataUpdater();
                mPlaylistPaths = new TreeSet<>();

                if (mNewLibrary) {
                    //mDBHelper.dropNonSettingTables();
                    mDBHelper.dropIndices();
                    mDBHelper.getArtists().deleteAll();
                    mDBHelper.getAlbums().deleteAll();
                    mDBHelper.getGenres().deleteAll();
                    mDBHelper.getGenreObjects().deleteAll();
                    mDBHelper.getFolders().deleteAll();
                    mDBHelper.getSongs().deleteAll();

                    mFolders.clear();
                    mGenres.clear();
                    mArtists.clear();
                    mAlbums.clear();
                    mSongs.clear();
                }

                mFoldersClone = mFolders.clone();
                mSongsClone = mSongs.clone();
                mRemovedSongs = new ArraySet<>();
                mPlaylists = getEditablePlaylists(null);

                for (String path : mStoragePaths) {
                    scanDirectory(0, new File(path), false);
                }

                // remove non existing folders
                for (int i = 0; i < mFoldersClone.size(); i++) {
                    Folder folder = mFoldersClone.valueAt(i);

                    removeFolder(folder.getId());
                }

                // remove non existing songs
                for (int i = 0; i < mSongsClone.size(); i++) {
                    Song song = mSongsClone.valueAt(i);
                    boolean remove = true;

                    if (mUnavailables != null) {
                        for (String storage : mUnavailables)
                            if (song.getInfo().getFilePath().startsWith(storage))
                                remove = false;
                    }

                    if (remove)
                        removeSong(song, false);
                }

                mUpdater.doUpdate();

                mDBHelper.createIndices();

                endTransaction();

                for (String playlist : mPlaylistPaths) {
                    if (!isPlaylistImported(playlist))
                        importPlaylist(playlist, false);
                }

                saveImportedPlaylists();

                mUpdater.release();
                mMediaTag.release();

                Log.i("Library", "scan finishing");
                finishTask();
            }
        };

        private Runnable mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                callLibraryUpdated();
            }
        };

        private Runnable mFinishRunnable = new Runnable() {
            @Override
            public void run() {
                for (Playlist playlist : mPlaylists) {
                    if (!playlist.isLoaded())
                        playlist.load();

                    for (int id : mRemovedSongs)
                        playlist.removeSongById(id);

                    playlist.save();
                    playlist.notifyChanged();
                }

                if (mNumProcessedSongs > 0)
                    callLibraryUpdated();

                if (mScanTask != ScanTask.this)
                    mScanTask.start();
                else {
                    mScanTask = null;
                    callScanStateChanged(false);
                }
            }
        };

        public ScanTask(boolean newLibrary) {
            mNewLibrary = newLibrary;
            mCancelRequested = false;

            mHandler = new Handler();
        }

        public void start() {
            mStoragePaths = StorageManager.getInstance().getAllStorages();
            mUnavailables = StorageManager.getInstance().getUnavailableStorages();

            PreferenceManager prefs = PreferenceManager.getInstance();
            mLeastSongDuration = prefs.getInt(PreferenceManager.KEY_LIBRARY_IGNORE_SHORT_SONGS, 0);

            new Thread(mMainRunnable).start();
        }

        private void publishProgress() {
            mHandler.post(mUpdateRunnable);
        }

        private void finishTask() {
            mHandler.post(mFinishRunnable);
        }

        private void removeSong(Song song, boolean removeClone) {
            mUpdater.queueAllButFolder(song);

            MusicLibrary.this.removeSong(song.getId());
            if (removeClone)
                mSongsClone.remove(song.getId());

            mRemovedSongs.add(song.getId());
        }

        private void removeFolderSongs(Folder folder) {
            SongsTable.Iterator it = mDBHelper.getSongs().getForObject(LibraryObject.FOLDER, folder.getId(), null, false);

            if (it.getSize() > 0)
                beginTransaction();

            while (it.next()) {
                removeSong(getSongById(it.getId()), true);
                mNumProcessedSongs++;
            }
            it.close();
        }

        private void updateSong(Song song, com.example.musicplayer.io.Metadata metadata, long lastModified) {
            String relevantArtist = metadata.getRelevantArtist();
            int artistId = getArtistIdOrAdd(relevantArtist);
            int genreId = getGenreIdOrAdd(metadata.getGenre());
            int albumId = getAlbumIdOrAdd(artistId, relevantArtist, metadata.getAlbum());

            Song.Info info = song.getInfo();

            if (artistId != info.getArtistId()) {
                mUpdater.queueArtist(info.getArtistId());
                mUpdater.queueArtist(artistId);
            }

            if (genreId != info.getGenreId()) {
                mUpdater.queueGenre(info.getGenreId());
                mUpdater.queueGenre(genreId);
            }

            // to update the album thumbnail
            mUpdater.queueAlbum(info.getAlbumId());
            if (albumId != info.getAlbumId()) {
                mUpdater.queueAlbum(albumId);
            }

            info.setUpdate(artistId, albumId, genreId, metadata, lastModified);
            mDBHelper.getSongs().updateSongInfo(song.getId(), info);
        }

        private void beginTransaction() {
            if (!mTransactionActive) {
                setUpdateTime();
                mTransactionActive = true;
                mDBHelper.beginTransaction();
            }
        }

        private void endTransaction() {
            if (mTransactionActive) {
                mTransactionActive = false;
                mDBHelper.setTransactionSuccessful();
                mDBHelper.endTransaction();
            }
        }

        private void scanDirectory(int parentId, File directory, boolean noMedia) {
            String name = parentId == 0 ? Util.getCanonicalPath(directory) : directory.getName();
            int id = mDBHelper.getFolders().getId(parentId, name);
            Folder folder = null;

            if (!noMedia) {
                File noMediaFile = new File(directory, ".nomedia");
                noMedia = noMediaFile.exists();
            }

            if (id == -1) {
                beginTransaction();
                Folder.IgnoreType ignore = Folder.IgnoreType.NotIgnore;
                if (noMedia)
                    ignore = Folder.IgnoreType.Ignore;

                folder = addFolder(parentId, Util.getCanonicalPath(directory), ignore);
                id = folder.getId();
            }
            else {
                mFoldersClone.remove(id);
                folder = getFolderById(id);
                if (!noMedia && folder.getIgnoreType() == Folder.IgnoreType.Ignore) {
                    beginTransaction();
                    folder.setIgnored(Folder.IgnoreType.NotIgnore);
                    mDBHelper.getFolders().updateIgnored(id, Folder.IgnoreType.NotIgnore);
                }
                else if (noMedia && folder.getIgnoreType() == Folder.IgnoreType.NotIgnore) {
                    beginTransaction();
                    folder.setIgnored(Folder.IgnoreType.Ignore);
                    mDBHelper.getFolders().updateIgnored(id, Folder.IgnoreType.Ignore);
                }

                if (folder.isIgnored())
                    removeFolderSongs(folder);
            }

            File[] files = directory.listFiles();
            if (files == null)
                return;

            for (File file : files) {
                if (isCancelled())
                    break;

                if (mTransactionActive && SystemClock.currentThreadTimeMillis() - mLastUpdateTime >= MAX_UPDATE_DURATION)
                    update();

                String absPath = file.getAbsolutePath();
                MediaTag.FileType type;

                if (file.isDirectory()) {
                    scanDirectory(id, file, noMedia);
                }
                else if (!folder.isIgnored() && PlaylistFile.hasPlaylistExtension(absPath)) {
                    String filePath = Util.getCanonicalPath(file);

                    mPlaylistPaths.add(filePath);
                }
                else if (!folder.isIgnored() && (type = MediaTag.getFileType(absPath)) != MediaTag.FileType.None) {
                    String filePath = Util.getCanonicalPath(file);

                    int songId = -1;
                    Song song = null;

                    if (!mNewLibrary) {
                        songId = mDBHelper.getSongs().getId(id, file.getName());
                        song = getSongById(songId);
                    }

                    if (song != null && song.getInfo().getLastModified() == file.lastModified()) {
                        mSongsClone.remove(song.getId());
                        continue;
                    }

                    if (mMediaTag.open(type, absPath)) {
                        long duration = mMediaTag.getProperty(MediaTag.Properties.Duration);
                        if (TimeUtil.getSecondsForMillis(duration) < mLeastSongDuration) {
                            mMediaTag.close();
                            continue;
                        }

                        com.example.musicplayer.io.Metadata metadata = mBuilder.fromMediaTag(mMediaTag).build();

                        beginTransaction();

                        if (song == null) {
                            // add new song
                            song = addSong(id, metadata, filePath, file.lastModified());
                            mUpdater.queueAllButFolder(song);
                        } else {
                            // song file has changed so update it
                            mSongsClone.remove(song.getId());
                            updateSong(song, metadata, file.lastModified());
                        }

                        mMediaTag.close();

                        setUpdateTime();

                        if (++mNumProcessedSongs >= UPDATE_FREQUENCY) {
                            update();
                        }
                    }
                }
            }

            updateFolder(id);
        }

        private void update() {
            mUpdater.doUpdate();

            endTransaction();

            publishProgress();
            mNumProcessedSongs = 0;
        }

        private void setUpdateTime() {
            mLastUpdateTime = SystemClock.currentThreadTimeMillis();
        }

        public void cancel() {
            synchronized (ScanTask.this) {
                mCancelRequested = true;
            }
        }

        private boolean isCancelled() {
            synchronized (ScanTask.this) {
                return mCancelRequested;
            }
        }
    }

    class DataUpdater {
        private ArraySet<Integer> mUpdatedFolders;
        private ArraySet<Integer> mUpdatedArtists;
        private ArraySet<Integer> mUpdatedGenres;
        private ArraySet<Integer> mUpdatedAlbums;

        private Decoder mDecoder;

        public DataUpdater() {
            this(0);
        }

        public DataUpdater(int capacity) {
            mUpdatedFolders = new ArraySet<>(capacity);
            mUpdatedArtists = new ArraySet<>(capacity);
            mUpdatedGenres = new ArraySet<>(capacity);
            mUpdatedAlbums = new ArraySet<>(capacity);
        }

        public void queueAllButFolder(Song song) {
            queueArtist(song.getInfo().getArtistId());
            queueGenre(song.getInfo().getGenreId());
            queueAlbum(song.getInfo().getAlbumId());
        }

        public void queueFolder(int id) {
            mUpdatedFolders.add(id);
        }

        public void queueArtist(int id) {
            mUpdatedArtists.add(id);
        }

        public void queueGenre(int id) {
            mUpdatedGenres.add(id);
        }

        public void queueAlbum(int id) {
            mUpdatedAlbums.add(id);
        }

        public void doUpdate() {
            for (int id : mUpdatedFolders)
                updateFolder(id);

            updateArtists(mUpdatedArtists);
            updateGenres(mUpdatedGenres);
            updateAlbums(mUpdatedAlbums);

            if (!mUpdatedAlbums.isEmpty()) {
                if (mDecoder == null)
                    mDecoder = Thumbnails.getInstance().getDecoder();

                Thumbnails.getInstance().saveAll(mUpdatedAlbums, mDecoder);
            }

            mUpdatedFolders.clear();
            mUpdatedArtists.clear();
            mUpdatedAlbums.clear();
            mUpdatedGenres.clear();
        }

        public void release() {
            if (mDecoder != null)
                mDecoder.release();
        }
    }

    // refresh album thumbnails for all albums
    private class AlbumArtTask extends AsyncTask<Void, Void, Void> {

        Collection<Album> albums;
        int processedAlbums = 0;

        @Override
        protected Void doInBackground(Void... params) {
            MediaTag tag = new MediaTag();
            Decoder decoder = Thumbnails.getInstance().getDecoder();

            Thumbnails.getInstance().deleteAll();

            for (Album album : albums) {
                Thumbnails.getInstance().save(album.getId(), decoder, tag);

                if (++processedAlbums >= UPDATE_FREQUENCY) {
                    publishProgress();
                    processedAlbums = 0;
                }
            }

            tag.release();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (processedAlbums > 0)
                callLibraryUpdated();

            mAlbumArtTask = null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            callLibraryUpdated();
        }

        @Override
        protected void onPreExecute() {
            AlbumArtCache.getInstance().clearCache();
            albums = getAllAlbums(null, Sorting.ID, false);
        }
    }

    private class EditTask extends AsyncTask<Object, Void, Void> {

        private DocumentFile mRoot;
        private Collection<Song> mSongs;
        private DataUpdater mUpdater;

        public EditTask(Collection<Song> songs) {
            mSongs = songs;
        }

        @Override
        protected void onPreExecute() {
            mRoot = StorageManager.getInstance().getSDCardRoot();
        }

        @Override
        protected Void doInBackground(Object... params) {
            com.example.musicplayer.io.Metadata metadata = (com.example.musicplayer.io.Metadata)params[0];
            boolean setPreset = (boolean)params[1];
            int presetId = (int)params[2];
            boolean setAlbumArt = (boolean)params[3];
            Uri albumArtUri = (Uri)params[4];
            MediaTag.ImageType imageType = (MediaTag.ImageType) params[5];

            mUpdater = new DataUpdater(mSongs.size());

            int genreId = -1;
            if (metadata.getGenre() != null) {
                genreId = getGenreIdOrAdd(metadata.getGenre());
                mUpdater.queueGenre(genreId);
            }

            String relevantArtist = null;
            int artistId = -1;
            if (!Util.stringIsEmpty(metadata.getAlbumArtist())) {
                relevantArtist = metadata.getAlbumArtist();
                artistId = getArtistIdOrAdd(relevantArtist);
                mUpdater.queueArtist(artistId);
            }

            int albumId = -1;
            if (artistId != -1 && metadata.getAlbum() != null) {
                albumId = getAlbumIdOrAdd(artistId, metadata.getAlbumArtist(), metadata.getAlbum());
                mUpdater.queueAlbum(albumId);
            }

            MediaTag tag = new MediaTag();

            mDBHelper.beginTransaction();

            for (Song song : mSongs) {
                Song.Info info = song.getInfo();
                String sRelevantArtist = relevantArtist;
                int sArtistId = artistId;
                int sAlbumId = albumId;

                if (sArtistId == -1) {
                    if (metadata.getAlbumArtist() != null) {
                        // delete album artist
                        if (metadata.getArtist() != null) {
                            sRelevantArtist = metadata.getArtist();
                            sArtistId = getArtistIdOrAdd(sRelevantArtist);
                            mUpdater.queueArtist(sArtistId);
                        }
                        else {
                            sRelevantArtist = info.getArtist();
                            sArtistId = getArtistIdOrAdd(sRelevantArtist);
                            mUpdater.queueArtist(sArtistId);
                        }
                    }
                    else if (info.getAlbumArtist() == null) {
                        if (metadata.getArtist() != null) {
                            sRelevantArtist = metadata.getArtist();
                            sArtistId = getArtistIdOrAdd(sRelevantArtist);
                            mUpdater.queueArtist(sArtistId);
                        }
                        else {
                            sRelevantArtist = info.getArtist();
                        }
                    }
                    else
                        sRelevantArtist = info.getAlbumArtist();
                }

                if (sAlbumId == -1) {
                    if (sArtistId != -1) {
                        sAlbumId = getAlbumIdOrAdd(sArtistId, sRelevantArtist,
                                metadata.getAlbum() != null ? metadata.getAlbum() : info.getAlbum());
                        mUpdater.queueAlbum(sAlbumId);
                    }
                    else if (metadata.getAlbum() != null) {
                        sAlbumId = getAlbumIdOrAdd(info.getArtistId(), sRelevantArtist, metadata.getAlbum());
                        mUpdater.queueAlbum(sAlbumId);
                    }
                }

                if (genreId != -1)
                    mUpdater.queueGenre(info.getGenreId());

                if (sArtistId != -1)
                    mUpdater.queueArtist(info.getArtistId());

                if (sAlbumId != -1)
                    mUpdater.queueAlbum(info.getAlbumId());

                info.setEdit(sArtistId, sAlbumId, genreId, metadata);

                if (setPreset) {
                    song.setPresetId(presetId);
                    mDBHelper.getSongStats().updatePresetId(song.getId(), presetId);
                }

                String filePath = info.getFilePath();
                File file = new File(filePath);
                boolean tagOpen = false;

                if (file.exists() && !file.canWrite()) {
                    DocumentFile documentFile = StorageManager.getInstance().getSDCardFile(mRoot, filePath, false);
                    if (documentFile != null) {
                        int fd = StorageManager.getInstance().openAndDetachFd(documentFile.getUri(), "rw");
                        if (fd != 0)
                            tagOpen = tag.open(MediaTag.getFileType(filePath), fd);
                    }
                }
                else {
                    tagOpen = tag.open(filePath);
                }

                if (tagOpen) {
                    tag.setMetadata(MediaTag.Metadata.Title, info.getTitle());
                    tag.setMetadata(MediaTag.Metadata.Artist, info.getArtist());
                    tag.setMetadata(MediaTag.Metadata.AlbumArtist, info.getAlbumArtist());
                    tag.setMetadata(MediaTag.Metadata.Album, info.getAlbum());
                    tag.setMetadata(MediaTag.Metadata.Genre, info.getGenre());

                    String titleNumber = null;
                    if (info.getTitleNumber() > 0 || info.getNumTitles() > 0) {
                        titleNumber = String.valueOf(Math.max(0, info.getTitleNumber()));
                        if (info.getNumTitles() > 0)
                            titleNumber += "/" + String.valueOf(info.getNumTitles());
                    }
                    tag.setMetadata(MediaTag.Metadata.TrackNumber, titleNumber);

                    String discNumber = null;
                    if (info.getDiscNumber() > 0 || info.getNumDiscs() > 0) {
                        discNumber = String.valueOf(Math.max(0, info.getDiscNumber()));
                        if (info.getNumDiscs() > 0)
                            discNumber += "/" + String.valueOf(info.getNumDiscs());
                    }
                    tag.setMetadata(MediaTag.Metadata.DiscNumber, discNumber);

                    String year = null;
                    if (info.getYear() > 0) {
                        year = String.valueOf(info.getYear());
                    }
                    tag.setMetadata(MediaTag.Metadata.Year, year);

                    if (setAlbumArt) {
                        if (albumArtUri == null)
                            tag.removeAlbumArt();
                        else {
                            int fd = StorageManager.getInstance().openAndDetachFd(albumArtUri, "r");
                            if (fd != 0)
                                tag.setAlbumArt(fd, imageType);
                        }
                    }

                    if (!tag.save()) {
                        Log.e("MusicLibrary", "Could not save media tag (DocumentFile)");
                    }
                    tag.close();

                    info.setLastModified(file.lastModified());
                }

                mDBHelper.getSongs().updateSongInfo(song.getId(), song.getInfo());
            }

            mDBHelper.setTransactionSuccessful();
            mDBHelper.endTransaction();

            mUpdater.doUpdate();
            mUpdater.release();

            tag.release();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            notifyObservers(new ObserverData(ObserverData.Type.SongsEdited, mSongs));

            callLibraryUpdated();
        }
    }

    private class RemoveTask extends AsyncTask<Object, Void, Void> {

        private TreeSet<Playlist> mPlaylists;
        private Collection<Song> mSongs;
        private DataUpdater mUpdater;

        public RemoveTask(Collection<Song> songs) {
            mSongs = songs;
            mPlaylists = getEditablePlaylists(null);
        }

        @Override
        protected void onPreExecute() {
            for (Song song : mSongs) {
                for (Playlist playlist : mPlaylists)
                    playlist.removeSongById(song.getId());
            }
        }

        @Override
        protected Void doInBackground(Object... params) {
            boolean deleteFiles = (boolean)params[0];

            mUpdater = new DataUpdater(mSongs.size());

            StorageManager storages = StorageManager.getInstance();
            DocumentFile root = storages.getSDCardRoot();

            for (Song song : mSongs) {
                boolean res = true;

                if (deleteFiles) {
                    File file = new File(song.getInfo().getFilePath());
                    if (file.exists() && !file.delete()) {
                        res = false;

                        DocumentFile documentFile = storages.getSDCardFile(root, song.getInfo().getFilePath(), false);
                        if (documentFile != null)
                            res = documentFile.delete();
                    }

                    if (res) {
                        removeSong(song.getId());
                    }
                }

                if (!deleteFiles || !res) {
                    song.setHidden(true);
                    mDBHelper.getSongs().updateHidden(song.getId(), true);
                }

                mUpdater.queueAllButFolder(song);
                mUpdater.queueFolder(song.getInfo().getFolderId());
            }

            mUpdater.doUpdate();
            mUpdater.release();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            notifyObservers(new ObserverData(ObserverData.Type.SongsRemoved, mSongs));

            callLibraryUpdated();
        }
    }

    private class UnhideTask extends AsyncTask<Void, Void, Void> {

        private List<Song> mSongs;

        public UnhideTask(List<Song> songs) {
            mSongs = songs;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DataUpdater updater = new DataUpdater(mSongs.size());

            mDBHelper.beginTransaction();

            for (Song song : mSongs) {
                song.setHidden(false);
                mDBHelper.getSongs().updateHidden(song.getId(), false);

                int artistId = getArtistIdOrAdd(song.getInfo().getRelevantArtist());
                int albumId = getAlbumIdOrAdd(artistId, song.getInfo().getRelevantArtist(), song.getInfo().getAlbum());
                int genreId = getGenreIdOrAdd(song.getInfo().getGenre());

                song.getInfo().updateIds(artistId, albumId, genreId);

                mDBHelper.getSongs().updateSongInfo(song.getId(), song.getInfo());

                updater.queueAllButFolder(song);
                updater.queueFolder(song.getInfo().getFolderId());
            }

            updater.doUpdate();
            updater.release();

            mDBHelper.setTransactionSuccessful();
            mDBHelper.endTransaction();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            callLibraryUpdated();
        }
    }

    private static MusicLibrary sSingleton;

    public static MusicLibrary getInstance() {
        if (sSingleton == null) {
            sSingleton = new MusicLibrary();
        }

        return sSingleton;
    }

    private static final String KEY_LIBRARY_SETUP = "music_library_setup";

    private static final String KEY_RECENTLY_ADDED = "recently_added";
    private static final String KEY_MOST_PLAYED = "most_played";
    private static final String KEY_LAST_PLAYED = "last_played";

    public static final int INFORMATION_NUMSONGS = 0;
    public static final int INFORMATION_DURATION = 1;
    public static final int INFORMATION_SIZE = 2;

    private static final int UPDATE_FREQUENCY = 40; // TODO: optimize update frequency
    private static final int MAX_UPDATE_DURATION = 500; // in milliseconds
    private static final String PLAYLISTS_FOLDER = "playlists";

    private boolean mInitialized;
    private Context mContext;
    private Helper mDBHelper;
    private File mPlaylistsFolder;

    private ScanTask mScanTask;
    private AlbumArtTask mAlbumArtTask;

    private SparseArrayCompat<Folder> mFolders;
    private SparseArrayCompat<Genre> mGenres;
    private SparseArrayCompat<Artist> mArtists;
    private SparseArrayCompat<Album> mAlbums;
    private SparseArrayCompat<Song> mSongs;
    private SparseArrayCompat<Playlist> mPlaylists;
    private SparseArrayCompat<EqualizerPreset> mPresets;

    private boolean mUseFileName;

    private List<Song> mUnhidingSongs;

    private Set<String> mImportedPlaylists;
    private RecentlyAddedPlaylist mRecentsPlaylist;
    private MostPlayedPlaylist mMostsPlaylist;
    private LastPlayedPlaylist mLastsPlaylist;

    private String mUnknownGenre;
    private String mUnknownArtist;
    private String mUnknownAlbum;

    private PreferenceManager.Observer mPrefsObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferencesRestored:
                    updateAutoPlaylists(PreferenceManager.getInstance().getStringSet(PreferenceManager.KEY_AUTOMATIC_PLAYLISTS));
                    callPlaylistsUpdated();
                    break;
                case PreferenceChange:

                    switch (data.key) {
                        case PreferenceManager.KEY_AUTOMATIC_PLAYLISTS:
                            updateAutoPlaylists((Set<String>)data.value);
                            callPlaylistsUpdated();
                            break;
                        case PreferenceManager.KEY_USE_SONG_FILE_NAMES:
                            mUseFileName = (boolean)data.value;
                            PlaybackState.getInstance().refreshSong();
                            break;
                    }

                    break;
            }
        }
    };

    private MusicLibrary() {
        mInitialized = false;
        mContext = null;
        mDBHelper = null;
        mPlaylistsFolder = null;
        mGenres = null;
        mArtists = null;
        mAlbums = null;
        mSongs = null;
        mPlaylists = null;
        mFolders = null;
    }

    public void initialize(Context context) {
        if (!mInitialized) {
            Thumbnails.getInstance().initialize(context);

            mContext = context.getApplicationContext();
            mDBHelper = new Helper(mContext);
            mDBHelper.initialize();

            File dir = mContext.getFilesDir();

            mPlaylistsFolder = new File(dir, PLAYLISTS_FOLDER);
            mPlaylistsFolder.mkdirs();

            mUnknownGenre = mContext.getString(R.string.unknown_genre);
            mUnknownArtist = mContext.getString(R.string.unknown_artist);
            mUnknownAlbum = mContext.getString(R.string.unknown_album);

            mInitialized = true;

            PreferenceManager prefs = PreferenceManager.getInstance();

            // TODO: Cache manager
            boolean setup = prefs.getBoolean(KEY_LIBRARY_SETUP, false);

            if (!setup)
                setupLibrary();

            load();

            loadPreferences();

            prefs.addObserver(mPrefsObserver);
        }
    }

    private void setupLibrary() {
        mDBHelper.beginTransaction();
        // setup playlists
        mDBHelper.getPlaylists().insertAutomatic(RecentlyAddedPlaylist.NAME);
        mDBHelper.getPlaylists().insertAutomatic(MostPlayedPlaylist.NAME);
        mDBHelper.getPlaylists().insertAutomatic(LastPlayedPlaylist.NAME);

        // setup presets
        PresetsParser parser = new PresetsParser();
        parser.open(mContext);

        while (parser.next()) {
            mDBHelper.getPresets().insert(parser.getName(), parser.getBandGains(), true);
        }

        parser.close();

        mDBHelper.setTransactionSuccessful();
        mDBHelper.endTransaction();
        PreferenceManager.getInstance().setBoolean(KEY_LIBRARY_SETUP, true);
    }

    public void deleteAll() {
        //mDBHelper.delete();

        mFolders.clear();
        mGenres.clear();
        mArtists.clear();
        mAlbums.clear();
        mSongs.clear();

        mPresets.clear();

        for (int i = 0; i < mPlaylists.size(); i++) {
            mPlaylists.valueAt(i).delete();
        }

        mPlaylists.clear();
    }

    private void updateAutoPlaylists(Set<String> values) {
        mRecentsPlaylist.setEnabled(values == null || values.contains(KEY_RECENTLY_ADDED));
        mMostsPlaylist.setEnabled(values == null || values.contains(KEY_MOST_PLAYED));
        mLastsPlaylist.setEnabled(values == null || values.contains(KEY_LAST_PLAYED));
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public boolean isScanning() {
        return mScanTask != null;
    }

    public void load() {
        FoldersTable.Iterator folderCursor = mDBHelper.getFolders().readAll();
        mFolders = new SparseArrayCompat<>(folderCursor.getSize());
        while (folderCursor.next()) {
            Folder folder = folderCursor.getFolder();
            mFolders.put(folder.getId(), folder);
        }
        folderCursor.close();

        GenresTable.Iterator genreCursor = mDBHelper.getGenres().readAll();
        mGenres = new SparseArrayCompat<>(genreCursor.getSize());
        while (genreCursor.next()) {
            Genre genre = genreCursor.getGenre();
            mGenres.put(genre.getId(), genre);
        }
        genreCursor.close();

        ArtistsTable.Iterator artistCursor = mDBHelper.getArtists().readAll();
        mArtists = new SparseArrayCompat<>(artistCursor.getSize());
        while (artistCursor.next()) {
            Artist artist = artistCursor.getArtist();
            mArtists.put(artist.getId(), artist);
        }
        artistCursor.close();

        AlbumsTable.Iterator albumCursor = mDBHelper.getAlbums().readAll();
        mAlbums = new SparseArrayCompat<>(albumCursor.getSize());
        while (albumCursor.next()) {
            Album album = albumCursor.getAlbum();
            mAlbums.put(album.getId(), album);
        }
        albumCursor.close();

        SongsTable.Iterator songCursor = mDBHelper.getSongs().readAll();
        mSongs = new SparseArrayCompat<>(songCursor.getSize());
        while (songCursor.next()) {
            Song song = songCursor.getSong();
            mSongs.put(song.getId(), song);
        }
        songCursor.close();

        SongStatsTable.Iterator songInfoCursor = mDBHelper.getSongStats().readAll();
        while (songInfoCursor.next()) {
            Song song = mSongs.get(songInfoCursor.getId());
            if (song != null) {
                song.setPresetId(songInfoCursor.getPresetId());
            }
        }

        loadPresets();

        loadPlaylists();
    }

    private void loadPreferences() {
        PreferenceManager prefs = PreferenceManager.getInstance();

        mUseFileName = prefs.getBoolean(PreferenceManager.KEY_USE_SONG_FILE_NAMES);

        if (mImportedPlaylists == null)
            mImportedPlaylists = new ArraySet<>(prefs.getStringSet(
                PreferenceManager.KEY_IMPORTED_PLAYLISTS));
        else {
            mImportedPlaylists.clear();
            mImportedPlaylists.addAll(prefs.getStringSet(PreferenceManager.KEY_IMPORTED_PLAYLISTS));
        }
        updateAutoPlaylists(prefs.getStringSet(PreferenceManager.KEY_AUTOMATIC_PLAYLISTS));
    }

    private void loadPresets() {
        PresetsTable.Iterator cursor = mDBHelper.getPresets().readAll();
        if (mPresets == null)
            mPresets = new SparseArrayCompat<>(cursor.getSize());
        else
            mPresets.clear();
        while (cursor.next()) {
            EqualizerPreset preset = cursor.getPreset();

            if (preset.isPrebuilt()) {
                preset.mResourceName = preset.getName();
                int resourceId = mContext.getResources().getIdentifier(preset.mResourceName,
                        "string", mContext.getPackageName());
                if (resourceId != 0)
                    preset.setName(mContext.getString(resourceId));
            }

            mPresets.put(preset.getId(), preset);
        }
    }

    private void loadPlaylists() {
        PlaylistsTable.Iterator cursor = mDBHelper.getPlaylists().readAll();
        if (mPlaylists == null)
            mPlaylists = new SparseArrayCompat<>(cursor.getSize() + 3);
        else
            mPlaylists.clear();
        while (cursor.next()) {
            Playlist playlist = null;
            String name = cursor.getName();

            if (cursor.isAutomatic()) {
                switch (name) {
                    case RecentlyAddedPlaylist.NAME:
                        playlist = mRecentsPlaylist = new RecentlyAddedPlaylist(cursor.getId(),
                                mContext.getString(RecentlyAddedPlaylist.RESOURCE_ID));
                        break;
                    case LastPlayedPlaylist.NAME:
                        playlist = mLastsPlaylist = new LastPlayedPlaylist(cursor.getId(),
                                mContext.getString(LastPlayedPlaylist.RESOURCE_ID));
                        break;
                    case MostPlayedPlaylist.NAME:
                        playlist = mMostsPlaylist = new MostPlayedPlaylist(cursor.getId(),
                                mContext.getString(MostPlayedPlaylist.RESOURCE_ID));
                        break;
                }
            }
            else
                playlist = cursor.getPlaylist();

            mPlaylists.put(playlist.getId(), playlist);
        }
        cursor.close();
    }

    public String getGenreString(String genre) {
        return genre == null ? mUnknownGenre : genre;
    }

    public String getArtistString(String artist) {
        return artist == null ? mUnknownArtist : artist;
    }

    public String getAlbumString(String album) {
        return album == null ? mUnknownAlbum : album;
    }

    public boolean getUseFileName() {
        return mUseFileName;
    }

    public void postScanLibrary(boolean newLibrary) {
        if (mScanTask != null) {
            mScanTask.cancel();

            mScanTask = new ScanTask(newLibrary);
        }
        else {
            mScanTask = new ScanTask(newLibrary);
            mScanTask.start();
            callScanStateChanged(true);
        }
    }

    public void cancelScan() {
        if (mScanTask != null)
            mScanTask.cancel();
    }

    public void invalidateAlbumArtCache() {
        if (mAlbumArtTask == null) {
            mAlbumArtTask = new AlbumArtTask();
            mAlbumArtTask.execute();
        }
    }


    public Folder getFolderById(int id) {
        return mFolders.get(id);
    }

    public Genre getGenreById(int id) {
        return mGenres.get(id);
    }

    public Artist getArtistById(int id) {
        return mArtists.get(id);
    }

    public Album getAlbumById(int id) {
        return mAlbums.get(id);
    }

    public Song getSongById(int id) {
        if (id == -1)
            return null;

        return mSongs.get(id);
    }

    public Playlist getPlaylistById(int id) {
        return mPlaylists.get(id);
    }

    public EqualizerPreset getPresetById(int id) {
        return mPresets.get(id);
    }

    public LibraryObject getLibraryObject(int type, int id) {
        switch (type) {
            case LibraryObject.FOLDER:
                return getFolderById(id);
            case LibraryObject.GENRE:
                return getGenreById(id);
            case LibraryObject.ARTIST:
                return getArtistById(id);
            case LibraryObject.ALBUM:
                return getAlbumById(id);
            case LibraryObject.PLAYLIST:
                return getPlaylistById(id);
            case LibraryObject.SONG:
                return getSongById(id);
        }

        return null;
    }


    // returns all folders including ignored folders
    public TreeSet<Folder> getAllFolders() {
        Comparator<Folder> comparator = new Folder.FolderComparator(Sorting.Name, false);

        TreeSet<Folder> folders = new TreeSet<>(comparator);

        for (int i = 0; i < mFolders.size(); i++)
            folders.add(mFolders.valueAt(i));

        return folders;
    }

    // no ignored folders
    public TreeSet<Folder> getAllFolders(String filter, Sorting sorting, boolean reversed) {
        Comparator<Folder> compare = new Folder.FolderComparator(sorting, reversed);

        TreeSet<Folder> sorted;

        if (Util.stringIsEmpty(filter)) {
            sorted = new TreeSet<>(compare);
            for (int i = 0; i < mFolders.size(); i++) {
                Folder folder = mFolders.valueAt(i);
                if (!folder.isIgnored() && folder.getSongCount() > 0)
                    sorted.add(folder);
            }
        }
        else {
            FoldersTable.Iterator iterator = mDBHelper.getFolders().get(filter);

            sorted = foldersCursorToSet(iterator, compare);

            iterator.close();
        }

        return sorted;
    }

    public TreeSet<Genre> getAllGenres(String filter, Sorting sorting, boolean reversed) {
        Comparator<Genre> compare = new Genre.GenreComparator(sorting, reversed);

        TreeSet<Genre> sorted;

        if (Util.stringIsEmpty(filter)) {
            sorted = new TreeSet<>(compare);
            for (int i = 0; i < mGenres.size(); i++)
                sorted.add(mGenres.valueAt(i));
        }
        else {
            GenresTable.Iterator iterator = mDBHelper.getGenres().get(filter);

            sorted = genresCursorToSet(iterator, compare);

            iterator.close();
        }

        return sorted;
    }

    public TreeSet<Artist> getAllArtists(String filter, Sorting sorting, boolean reversed) {
        Comparator<Artist> compare = new Artist.ArtistComparator(sorting, reversed);

        TreeSet<Artist> sorted;

        if (Util.stringIsEmpty(filter)) {
            sorted = new TreeSet<>(compare);
            for (int i = 0; i < mArtists.size(); i++)
                sorted.add(mArtists.valueAt(i));
        }
        else {
            ArtistsTable.Iterator iterator = mDBHelper.getArtists().get(filter);

            sorted = artistsCursorToSet(iterator, compare);

            iterator.close();
        }

        return sorted;
    }

    public TreeSet<Album> getAllAlbums(String filter, Sorting sorting, boolean reversed) {
        Comparator<Album> compare = new Album.AlbumComparator(sorting, reversed);

        TreeSet<Album> sorted;

        if (Util.stringIsEmpty(filter)) {
            sorted = new TreeSet<>(compare);
            for (int i = 0; i < mAlbums.size(); i++)
                sorted.add(mAlbums.valueAt(i));
        }
        else {
            AlbumsTable.Iterator iterator = mDBHelper.getAlbums().get(filter);

            sorted = albumsCursorToSet(iterator, compare);

            iterator.close();
        }

        return sorted;
    }

    public TreeSet<Song> getAllSongs(String filter, Sorting sorting, boolean reversed) {
        Comparator<Song> compare = new Song.SongComparator(sorting, reversed);
        TreeSet<Song> sorted;

        if (Util.stringIsEmpty(filter)) {
            sorted = new TreeSet<>(compare);
            for (int i = 0; i < mSongs.size(); i++) {
                Song song = mSongs.valueAt(i);
                if (!song.isHidden())
                    sorted.add(song);
            }
        }
        else {
            SongsTable.Iterator iterator = mDBHelper.getSongs().get(filter, mUseFileName);

            sorted = songsCursorToSet(iterator, compare);

            iterator.close();
        }

        return sorted;
    }

    public TreeSet<Playlist> getAllPlaylists() {
        Comparator<Playlist> compare = new Playlist.PlaylistComparator();

        TreeSet<Playlist> sorted = new TreeSet<>(compare);

        for (int i = 0; i < mPlaylists.size(); i++) {
            Playlist playlist = mPlaylists.valueAt(i);
            if (playlist.isEnabled())
                sorted.add(playlist);
        }

        return sorted;
    }

    public TreeSet<Playlist> getEditablePlaylists(String filter) {
        Comparator<Playlist> compare = new Playlist.PlaylistComparator();

        TreeSet<Playlist> sorted = new TreeSet<>(compare);

        if (!Util.stringIsEmpty(filter)) {
            PlaylistsTable.Iterator iterator = mDBHelper.getPlaylists().get(filter);

            while (iterator.next()) {
                sorted.add(getPlaylistById(iterator.getId()));
            }

            iterator.close();
        }
        else {
            for (int i = 0; i < mPlaylists.size(); i++) {
                Playlist item = mPlaylists.valueAt(i);
                if (item.isMutable()) {
                    sorted.add(item);
                }
            }
        }

        return sorted;
    }

    public ArrayList<EqualizerPreset> getPresets(@Nullable ArrayList<EqualizerPreset> dest) {
        if (dest == null)
            dest = new ArrayList<>(mPresets.size());
        else
            dest.clear();

        for (int i = 0; i < mPresets.size(); i++) {
            dest.add(mPresets.valueAt(i));
        }

        Collections.sort(dest);

        return dest;
    }

    public void setFoldersIgnored(Folder[] folders, boolean[] ignored) {
        if (folders == null || ignored == null)
            throw new NullPointerException("setFoldersIgnored: folders and ignored may not be null");

        mDBHelper.beginTransaction();

        for (int i = 0; i < folders.length; i++) {
            Folder folder = folders[i];
            Folder.IgnoreType type = ignored[i] ? Folder.IgnoreType.UserIgnore : Folder.IgnoreType.UserNotIgnore;
            folder.setIgnored(type);
            mDBHelper.getFolders().updateIgnored(folder.getId(), type);
        }

        mDBHelper.setTransactionSuccessful();
        mDBHelper.endTransaction();

        postScanLibrary(false);
    }

    public TreeSet<Folder> getRootFolders() {
        Comparator<Folder> compare = new Folder.FolderComparator(Sorting.Name, false);

        FoldersTable.Iterator cursor = mDBHelper.getFolders().get(0, null);

        TreeSet<Folder> folders = new TreeSet<>(compare);

        while (cursor.next()) {
            Folder folder = getFolderById(cursor.getId());
            folders.add(folder);
        }

        cursor.close();

        return folders;
    }

    public TreeSet<Folder> getSubfoldersAll(int parentId) {
        Comparator<Folder> compare = new Folder.FolderComparator(Sorting.Name, false);
        FoldersTable.Iterator cursor = mDBHelper.getFolders().get(parentId, null);
        TreeSet<Folder> folders = new TreeSet<>(compare);

        while (cursor.next()) {
            Folder folder = getFolderById(cursor.getId());
            folders.add(folder);
        }

        cursor.close();

        return folders;
    }

    public TreeSet<Folder> getSubfoldersForFolder(int id, String filter, Sorting sorting, boolean reversed) {
        Comparator<Folder> compare = new Folder.FolderComparator(sorting, reversed);

        FoldersTable.Iterator cursor = mDBHelper.getFolders().get(id, filter);

        TreeSet<Folder> folders = foldersCursorToSet(cursor, compare);

        cursor.close();

        return folders;
    }

    public TreeSet<Artist> getArtistsForGenre(int id, String filter, Sorting sorting, boolean reversed) {
        Comparator<Artist> compare = new Artist.ArtistComparator(sorting, reversed);

        GenreObjectsTable.Iterator cursor = mDBHelper.getGenreObjects().getArtists(id, filter);

        TreeSet<Artist> artists = new TreeSet<>(compare);
        while (cursor.next())
            artists.add(getArtistById(cursor.getId()));
        cursor.close();

        return artists;
    }

    public TreeSet<Album> getAlbumsForGenre(int id, String filter, Sorting sorting, boolean reversed) {
        Comparator<Album> compare = new Album.AlbumComparator(sorting, reversed);

        GenreObjectsTable.Iterator cursor = mDBHelper.getGenreObjects().getAlbums(id, filter);

        TreeSet<Album> albums = new TreeSet<>(compare);
        while (cursor.next())
            albums.add(getAlbumById(cursor.getId()));
        cursor.close();

        return albums;
    }

    // id, title
    public TreeSet<Album> getAlbumsForArtist(int id, String filter, Sorting sorting, boolean reversed) {
        Comparator<Album> compare = new Album.AlbumComparator(sorting, reversed);

        AlbumsTable.Iterator cursor = mDBHelper.getAlbums().getForArtist(filter, id);

        TreeSet<Album> albums = albumsCursorToSet(cursor, compare);

        cursor.close();

        return albums;
    }

    public List<Song> getSongsForPlaylist(Playlist playlist, Sorting sorting, boolean reversed) {
        if (sorting == Sorting.Custom || sorting == Sorting.ID) {
            return playlist.getSongList();
        } else {
            Comparator<Song> compare = new Song.SongComparator(sorting, reversed);

            ArrayList<Song> songs = new ArrayList<>(playlist.getSongList());
            Collections.sort(songs, compare);

            return songs;
        }
    }

    public Collection<Song> getSongsForObject(LibraryObject object, String filter, Sorting sorting, boolean reversed) {
        if (object == null)
            return getSongsForObject(LibraryObject.UNKNOWN, 0, filter, sorting, reversed);

        if (object.getType() == LibraryObject.SONG)
            return Collections.singleton((Song)object);

        return getSongsForObject(object.getType(), object.getId(), filter, sorting, reversed);
    }

    public Collection<Song> getSongsForObject(int type, int id, String filter, Sorting sorting, boolean reversed) {
        switch (type) {

            case LibraryObject.FOLDER:
            case LibraryObject.GENRE:
            case LibraryObject.ARTIST:
                if (sorting == Sorting.Default)
                    sorting = Sorting.Title;
                break;
            case LibraryObject.ALBUM:
                if (sorting == Sorting.Default)
                    sorting = Sorting.Number;
                break;

            case LibraryObject.SONG:
                return Collections.singleton(getSongById(id));
            case LibraryObject.PLAYLIST:
                return getSongsForPlaylist(getPlaylistById(id), sorting, reversed);
            case LibraryObject.UNKNOWN:
                if (sorting == Sorting.Default)
                    sorting = Sorting.Title;
                return getAllSongs(filter, sorting, reversed);
        }

        Comparator<Song> compare = new Song.SongComparator(sorting, reversed);

        SongsTable.Iterator cursor = mDBHelper.getSongs().getForObject(type, id, filter, mUseFileName);

        TreeSet<Song> sorted = songsCursorToSet(cursor, compare);

        cursor.close();

        return sorted;
    }

    public void getRecentlyAddedSongs(List<Song> songs) {
        addCursorToList(songs, mDBHelper.getSongStats().getRecentlyAdded());
    }

    public void getLastPlayedSongs(List<Song> songs) {
        addCursorToList(songs, mDBHelper.getSongStats().getLastPlayed());
    }

    public void getMostPlayedSongs(List<Song> songs) {
        addCursorToList(songs, mDBHelper.getSongStats().getMostPlayed());
    }

    private void addCursorToList(List<Song> songs, SongStatsTable.Iterator cursor) {
        while (cursor.next())
            songs.add(getSongById(cursor.getId()));

        cursor.close();
    }

    public TreeSet<Song> getHiddenSongs(String filter, Sorting sorting, boolean reversed) {
        Comparator<Song> compare = new Song.SongComparator(sorting, reversed);

        TreeSet<Song> sorted = new TreeSet<>(compare);

        if (Util.stringIsEmpty(filter)) {
            for (int i = 0; i < mSongs.size(); i++) {
                Song song = mSongs.valueAt(i);
                if (song.isHidden() && (mUnhidingSongs == null || !mUnhidingSongs.contains(song)))
                    sorted.add(song);
            }
        }
        else {
            SongsTable.Iterator iterator = mDBHelper.getSongs().get(filter, mUseFileName);

            while (iterator.next()) {
                Song song = getSongById(iterator.getId());
                if (song.isHidden() && (mUnhidingSongs == null || !mUnhidingSongs.contains(song)))
                    sorted.add(song);
            }

            iterator.close();
        }

        return sorted;
    }

    /**
     * 0: num songs
     * 1: duration in milliseconds
     * 2: size in bytes
     * @param object
     * @return
     */
    public long[] getSongsInformation(LibraryObject object) {
        long[] ret = new long[3];

        Collection<Song> songs = getSongsForObject(object, null, Sorting.ID, false);

        ret[0] = songs.size();

        for (Song song : songs) {
            ret[1] += song.getInfo().getDuration();
            ret[2] += new File(song.getInfo().getFilePath()).length();
        }

        return ret;
    }

    public void queueUnhideSong(Song song) {
        if (mUnhidingSongs == null)
            mUnhidingSongs = new ArrayList<>();

        mUnhidingSongs.add(song);
        notifyObservers(new ObserverData(ObserverData.Type.SongUnhidden, song.getId()));
    }

    public void unhideSongsAsync() {
        if (mUnhidingSongs == null)
            return;

        UnhideTask task = new UnhideTask(mUnhidingSongs);
        task.execute();
    }

    public void updateSongPlayed(int id) {
        mDBHelper.beginTransaction();
        mDBHelper.getSongStats().updateSongPlayed(id);
        mDBHelper.setTransactionSuccessful();
        mDBHelper.endTransaction();

        notifyObservers(new ObserverData(ObserverData.Type.SongPlayedUpdated, id));
    }

    public void postEditSongs(Collection<Song> songs, com.example.musicplayer.io.Metadata metadata, boolean setPreset, int presetId,
                              boolean setAlbumArt, Uri albumArtUri, MediaTag.ImageType imageType) {
        EditTask task = new EditTask(songs);
        task.execute(metadata, setPreset, presetId, setAlbumArt, albumArtUri, imageType);
    }

    public void postRemoveSongs(Collection<Song> songs, boolean deleteFiles) {
        RemoveTask task = new RemoveTask(songs);
        task.execute(deleteFiles);
    }


    /**
     *
     * @param name
     * @param importPath path to the file from which this playlist is imported, null if not imported
     * @return
     */
    public Playlist createPlaylist(String name, String importPath) {
        Playlist playlist = addPlaylist(name, importPath);
        mPlaylists.put(playlist.getId(), playlist);

        callPlaylistsUpdated();

        return playlist;
    }

    private Playlist addPlaylist(String name, String importPath) {
        int id = mDBHelper.getPlaylists().insert(name, importPath);
        return new Playlist(id, name, importPath);
    }

    public void renamePlaylist(int id, String name) {
        Playlist playlist = getPlaylistById(id);
        playlist.setName(name);
        mDBHelper.getPlaylists().updateName(id, name);
        callPlaylistsUpdated();
    }

    public void deletePlaylist(int id) {
        int index = mPlaylists.indexOfKey(id);
        if (index < 0)
            return;

        Playlist playlist = mPlaylists.valueAt(index);
        playlist.delete();

        mDBHelper.getPlaylists().delete(id);
        mPlaylists.removeAt(index);

        callPlaylistsUpdated();
    }

    public void importPlaylist(String filePath) {
        importPlaylist(filePath, true);
    }

    private void importPlaylist(String filePath, boolean save) {
        PlaylistFile file = PlaylistFile.parseFile(filePath);

        if (file == null)
            return;

        mImportedPlaylists.add(filePath);
        if (save)
            saveImportedPlaylists();

        Playlist playlist = createPlaylist(file.getName(), filePath);

        for (PlaylistFile.Entry entry : file.getEntryList()) {
            String entryPath = entry.filePath;
            if (entry.relativePath)
                entryPath = file.getDirectoryPath() + '/' + entryPath;

            File songFile = new File(entryPath);
            if (songFile.exists()) {
                String songPath = Util.getCanonicalPath(songFile);

                int songId = mDBHelper.getSongs().getIdByFilePath(songPath);
                if (songId != -1) {
                    Song song = getSongById(songId);
                    playlist.getSongList().add(song);
                }
            }
        }

        playlist.save();
    }

    private boolean isPlaylistImported(String filePath) {
        return mImportedPlaylists.contains(filePath);
    }

    private void saveImportedPlaylists() {
        PreferenceManager.getInstance().setStringSet(PreferenceManager.KEY_IMPORTED_PLAYLISTS, mImportedPlaylists);
    }

    File getPlaylistFile(int id) {
        return new File(mPlaylistsFolder, String.valueOf(id));
    }


    public EqualizerPreset createPreset(String name, String bandGains) {
        int id = mDBHelper.getPresets().insert(name, bandGains, false);
        EqualizerPreset preset = new EqualizerPreset(id, name, bandGains, false);
        mPresets.put(id, preset);
        callPresetsUpdated();

        return preset;
    }

    public void updatePresetName(int id, String name) {
        EqualizerPreset preset = getPresetById(id);
        if (!preset.isPrebuilt()) {
            preset.setName(name);
            mDBHelper.getPresets().updateName(id, name);
        }
    }

    public void updatePresetGains(int id, String gains) {
        EqualizerPreset preset = getPresetById(id);
        preset.setBandGains(gains);
        mDBHelper.getPresets().updateGains(id, gains);
    }

    public void restorePresetDefault(int id) {
        EqualizerPreset preset = getPresetById(id);

        if (!preset.isPrebuilt())
            return; // or throw

        PresetsParser parser = new PresetsParser();
        parser.open(mContext);

        do {
            parser.next();
        }
        while (parser.getName() != null && !preset.mResourceName.equals(parser.getName()));

        preset.setBandGains(parser.getBandGains());
        mDBHelper.getPresets().updateGains(id, preset.getGainsStr());
    }

    public void removePreset(int id) {
        int index = mPresets.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getPresets().delete(id);
        mPresets.removeAt(index);
        callPresetsUpdated();
    }


    public void backupData(BackupFile file) {
        SongStatsTable.Iterator statsCursor = mDBHelper.getSongStats().readAll();
        while (statsCursor.next()) {
            Song song = getSongById(statsCursor.getId());
            file.addSongStat(song.getInfo().getFilePath(),
                    statsCursor.getDateAdded(), statsCursor.getPresetId(),
                    statsCursor.getPlayCount(), statsCursor.getLastPlayed(), song.isHidden());
        }

        for (int i = 0; i < mPresets.size(); i++) {
            EqualizerPreset preset = mPresets.valueAt(i);

            String name = preset.isPrebuilt() ? preset.mResourceName : preset.getName();
            file.addPreset(preset.getId(), name, preset.getGainsStr(), preset.isPrebuilt());
        }

        for (int i = 0; i < mPlaylists.size(); i++) {
            Playlist playlist = mPlaylists.valueAt(i);

            if (playlist.isMutable()) {
                BackupFile.Playlist bPlaylist = file.addPlaylist(playlist.getName(), playlist.getImportPath());

                for (Song song : playlist.getSongList()) {
                    bPlaylist.addSong(song.getInfo().getFilePath());
                }
            }
        }
    }

    public void prepareRestore() {
        mDBHelper.beginTransaction();

        mPresets.clear();
        mDBHelper.getPresets().deleteAll();

        mPlaylists.clear();
        mDBHelper.getPlaylists().deleteAll();

        mDBHelper.setTransactionSuccessful();
        mDBHelper.endTransaction();

        loadPreferences();
    }

    public void restoreData(BackupFile file) {
        ArrayList<Song> hiddenSongs = new ArrayList<>();

        mDBHelper.beginTransaction();

        for (BackupFile.SongStat stat : file.getSongStats()) {
            int songId = mDBHelper.getSongs().getIdByFilePath(stat.getSongPath());
            if (songId != -1) {
                mDBHelper.getSongStats().updateAll(songId, stat.getDateAdded(), stat.getPresetId(),
                        stat.getPlayCount(), stat.getLastPlayed());

                Song song = getSongById(songId);
                song.setPresetId(stat.getPresetId());

                if (song.isHidden() != stat.isHidden()) {
                    if (stat.isHidden()) {
                        // hide song
                        hiddenSongs.add(song);
                    }
                    else {
                        // unhide song
                        queueUnhideSong(song);
                    }
                }
            }
        }

        for (BackupFile.Preset preset : file.getPresets()) {
            mDBHelper.getPresets().insertId(preset.getId(), preset.getName(),
                    preset.getBandGains(), preset.isPrebuilt());
        }
        loadPresets();

        for (BackupFile.Playlist playlist : file.getPlaylists()) {
            Playlist pl = addPlaylist(playlist.getName(), playlist.getImportPath());
            pl.load();

            for (String song : playlist.getSongList()) {
                int id = mDBHelper.getSongs().getIdByFilePath(song);
                if (id != -1)
                    pl.getSongList().add(getSongById(id));
            }

            pl.save();
        }
        loadPlaylists();

        mDBHelper.setTransactionSuccessful();
        mDBHelper.endTransaction();

        unhideSongsAsync();
        if (hiddenSongs.size() > 0)
            postRemoveSongs(hiddenSongs, false);

        // TODO: invalidate on main thread
        mRecentsPlaylist.invalidate();
        mLastsPlaylist.invalidate();
        mMostsPlaylist.invalidate();
    }

    /*private String getExportPath(Song song) {
        String root = Util.getStorageRoot(song.getInfo().getFilePath());

        if (root.length() == song.getInfo().getFilePath().length())
            return song.getInfo().getFilePath();

        return song.getInfo().getFilePath().substring(root.length() + 1);
    }*/

    private void callLibraryUpdated() {
        notifyObservers(new ObserverData(ObserverData.Type.LibraryUpdated));
    }

    private void callPlaylistsUpdated() {
        notifyObservers(new ObserverData(ObserverData.Type.PlaylistsUpdated));
    }

    private void callPresetsUpdated() {
        notifyObservers(new ObserverData(ObserverData.Type.PresetsUpdated));
    }

    private void callScanStateChanged(boolean scanState) {
        notifyObservers(new ObserverData(ObserverData.Type.ScanStateChanged, scanState));
    }


    private TreeSet<Folder> foldersCursorToSet(FoldersTable.Iterator cursor, Comparator<Folder> compare) {
        TreeSet<Folder> set = new TreeSet<>(compare);

        while (cursor.next()) {
            Folder folder = getFolderById(cursor.getId());
            if (!folder.isIgnored() && folder.getSongCount() > 0) // TODO: song count
                set.add(folder);
        }

        return set;
    }

    private TreeSet<Genre> genresCursorToSet(GenresTable.Iterator cursor, Comparator<Genre> compare) {
        TreeSet<Genre> set = new TreeSet<>(compare);

        while (cursor.next()) {
            Genre item = getGenreById(cursor.getId());
            set.add(item);
        }

        return set;
    }

    private TreeSet<Artist> artistsCursorToSet(ArtistsTable.Iterator cursor, Comparator<Artist> compare) {
        TreeSet<Artist> set = new TreeSet<>(compare);

        while (cursor.next()) {
            Artist item = getArtistById(cursor.getId());
            set.add(item);
        }

        return set;
    }

    private TreeSet<Album> albumsCursorToSet(AlbumsTable.Iterator cursor, Comparator<Album> compare) {
        TreeSet<Album> set = new TreeSet<>(compare);

        while (cursor.next()) {
            Album item = getAlbumById(cursor.getId());
            set.add(item);
        }

        return set;
    }

    private TreeSet<Song> songsCursorToSet(SongsTable.Iterator cursor, Comparator<Song> compare) {
        TreeSet<Song> set = new TreeSet<>(compare);

        while (cursor.next()) {
            Song item = getSongById(cursor.getId());
            if (!item.isHidden())
                set.add(item);
        }

        return set;
    }


    private int getArtistIdOrAdd(String name) {
        if (Util.stringIsEmpty(name))
            name = null;

        int id = mDBHelper.getArtists().getId(name);
        if (id == -1) {
            id = mDBHelper.getArtists().insert(name);
            Artist artist = new Artist(id, name);
            mArtists.put(id, artist);
        }
        return id;
    }

    private int getGenreIdOrAdd(String name) {
        if (Util.stringIsEmpty(name))
            name = null;

        int id = mDBHelper.getGenres().getId(name);
        if (id == -1) {
            id = mDBHelper.getGenres().insert(name);
            Genre genre = new Genre(id, name);
            mGenres.put(id, genre);
        }
        return id;
    }

    private int getAlbumIdOrAdd(int artistId, String artist, String title) {
        if (Util.stringIsEmpty(artist))
            artist = null;
        if (Util.stringIsEmpty(title))
            title = null;

        int id = mDBHelper.getAlbums().getId(artistId, title);
        if (id == -1) {
            id = mDBHelper.getAlbums().insert(artistId, artist, title);
            Album album = new Album(id, artistId, artist, title);
            mAlbums.put(id, album);
        }
        return id;
    }

    private Folder addFolder(int parentId, String fullPath, Folder.IgnoreType ignored) {
        String path = null;
        String name = null;

        if (parentId == 0) {
            path = "/";
            name = fullPath;
        }
        else {
            path = Folder.getPathFromFullPath(fullPath);
            name = Folder.getNameFromFullPath(fullPath);
        }
        int id = mDBHelper.getFolders().insert(parentId, name, path, ignored);
        Folder folder = new Folder(id, parentId, path, name, ignored);
        mFolders.put(id, folder);
        return folder;
    }

    private Song addSong(int folderId, Metadata metadata, String filePath, long lastModified) {
        String relevantArtist = metadata.getRelevantArtist();
        int artistId = getArtistIdOrAdd(relevantArtist);
        int genreId = getGenreIdOrAdd(metadata.getGenre());
        int albumId = getAlbumIdOrAdd(artistId, relevantArtist, metadata.getAlbum());

        Song.Info info = new Song.Info(artistId, albumId, genreId, folderId,
                metadata, filePath, lastModified);
        int id = mDBHelper.getSongs().insert(info);
        mDBHelper.getSongStats().insert(id);
        Song song = new Song(id, info);
        mSongs.put(id, song);
        return song;
    }

    private void removeFolder(int id) {
        int index = mFolders.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getFolders().delete(id);
        mFolders.removeAt(index);
    }

    private void removeGenre(int id) {
        int index = mGenres.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getGenres().delete(id);
        mGenres.removeAt(index);
    }

    private void removeArtist(int id) {
        int index = mArtists.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getArtists().delete(id);
        mArtists.removeAt(index);
    }

    private void removeAlbum(int id) {
        int index = mAlbums.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getAlbums().delete(id);
        mAlbums.removeAt(index);

        Thumbnails.getInstance().delete(id);
    }

    private void removeSong(int id) {
        int index = mSongs.indexOfKey(id);
        if (index < 0)
            return;

        mDBHelper.getSongs().delete(id);
        mDBHelper.getSongStats().delete(id);
        mSongs.removeAt(index);
    }

    private void updateFolder(int id) {
        SongsTable.Iterator cursor = mDBHelper.getSongs().getForObject(LibraryObject.FOLDER, id, null, false);
        FoldersTable.Iterator folderCursor = mDBHelper.getFolders().get(id, null);

        Folder folder = getFolderById(id);
        if (folder.getSongCount() != cursor.getSize()) {
            folder.setSongCount(cursor.getSize());
            mDBHelper.getFolders().updateSongCount(id, cursor.getSize());
        }
        if (folder.getFolderCount() != folderCursor.getSize()) {
            folder.setFolderCount(folderCursor.getSize());
            mDBHelper.getFolders().updateFolderCount(id, folderCursor.getSize());
        }

        cursor.close();
    }

    private void updateArtist(int id) {
        if (mDBHelper.getSongs().getForObject(LibraryObject.ARTIST, id, null, false).closeEmpty())
            removeArtist(id);
    }

    private void updateGenre(int id) {
        SongsTable.Iterator cursor = mDBHelper.getSongs().getForObject(LibraryObject.GENRE, id, null, false);

        mDBHelper.getGenreObjects().deleteGenre(id);

        if (cursor.getSize() == 0)
            removeGenre(id);
        else {
            while (cursor.next()) {
                Song.Info info = getSongById(cursor.getId()).getInfo();
                mDBHelper.getGenreObjects().insert(info.getArtistId(), LibraryObject.ARTIST, id);
                mDBHelper.getGenreObjects().insert(info.getAlbumId(), LibraryObject.ALBUM, id);
            }
        }

        cursor.close();
    }

    private void updateAlbum(int id) {
        SongsTable.Iterator cursor = mDBHelper.getSongs().getForObject(LibraryObject.ALBUM, id, null, false);

        if (cursor.getSize() == 0) {
            removeAlbum(id);
        }
        else {
            Album album = getAlbumById(id);

            boolean variousArtists = false;
            int songCount = 0;
            while (cursor.next()) {
                Song.Info info = getSongById(cursor.getId()).getInfo();

                if (!variousArtists && info.getArtist() != null && !info.getArtist().equals(album.getArtist()))
                    variousArtists = true;

                songCount++;
            }

            album.setHasVariousArtists(variousArtists);
            album.setSongCount(songCount);
            mDBHelper.getAlbums().update(id, variousArtists, songCount);
        }

        cursor.close();
    }

    private void updateArtists(Collection<Integer> artists) {
        for (int id : artists)
            updateArtist(id);
    }

    private void updateGenres(Collection<Integer> genres) {
        for (int id : genres)
            updateGenre(id);
    }

    private void updateAlbums(Collection<Integer> albums) {
        for (int id : albums)
            updateAlbum(id);
    }
}
