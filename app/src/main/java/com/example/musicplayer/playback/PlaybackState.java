package com.example.musicplayer.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import com.example.musicplayer.IObserver;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.io.MediaTag;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Tarik on 03.04.2016.
 */
public class PlaybackState extends Observable<PlaybackState.ObserverData> {

    public enum RepeatMode {
        None, All, Single
    }

    public interface Observer extends IObserver<ObserverData> { }

    public static class ObserverData {
        public enum Type {
            SongChanged, CoverLoaded, FilterStateChanged, PlayingStateChanged, ForceStopPlayback,
            PlaybackListChanged, RequestCurrentPosition, RequestSeek
        }

        public final Type type;
        public final Song song;
        public final boolean forceChange; // clear playback buffers
        public final Bitmap bitmap;
        public final int color;
        public final FilterState filterState;
        public final boolean isPlaying;
        public final PlaybackList playbackList;
        public final long position;

        private ObserverData(Type type) {
            this(type, null, false, null, 0, null, false, null, 0);
        }

        private ObserverData(Song song, boolean forceChange) {
            this(Type.SongChanged, song, forceChange, null, 0, null, false, null, 0);
        }

        private ObserverData(Bitmap bitmap, int color) {
            this(Type.CoverLoaded, null, false, bitmap, color, null, false, null, 0);
        }

        private ObserverData(FilterState state) {
            this(Type.FilterStateChanged, null, false, null, 0, state, false, null, 0);
        }

        private ObserverData(boolean isPlaying) {
            this(Type.PlayingStateChanged, null, false, null, 0, null, isPlaying, null, 0);
        }

        private ObserverData(PlaybackList playbackList) {
            this(Type.PlaybackListChanged, null, false, null, 0, null, false, playbackList, 0);
        }

        private ObserverData(long position) {
            this(Type.RequestSeek, null, false, null, 0, null, false, null, position);
        }

        private ObserverData(Type type, Song song, boolean forceChange, Bitmap bitmap, int color,
                             FilterState state, boolean isPlaying, PlaybackList list, long position) {
            this.type = type;
            this.song = song;
            this.forceChange = forceChange;
            this.bitmap = bitmap;
            this.color = color;
            this.filterState = state;
            this.isPlaying = isPlaying;
            this.playbackList = list;
            this.position = position;
        }
    }

    public static class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            PlaybackState state = PlaybackState.getInstance();

            switch (intent.getAction()) {
                case ACTION_PLAY:
                    state.initialize(context);
                    state.togglePlayingState();
                    break;
                case ACTION_NEXT:
                    if (state.isInited())
                        state.nextSong(true);
                    break;
                case ACTION_PREV:
                    if (state.isInited())
                        state.previousSong();
                    break;
            }
        }
    }

    private class CoverTask extends AsyncTask<Song, Void, Bitmap> {

        private Palette mPalette;

        @Override
        protected Bitmap doInBackground(Song... params) {
            if (isCancelled())
                return null;

            Song song = params[0];

            mMediaTag.open(song.getInfo().getFilePath());

            Bitmap result = mDecoder.readAlbumArt(mMediaTag);

            if (result != null) {
                Palette.Builder builder = new Palette.Builder(result);
                mPalette = builder.generate();
            }

            mMediaTag.close();

            return result;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled())
                return;

            mIsCached = true;
            mResultBitmap = bitmap;
            if (mPalette != null)
                mResultColor = mPalette.getLightVibrantColor(Color.GRAY);
            else
                mResultColor = Color.GRAY;

            callCoverLoaded(bitmap, mResultColor);

            mCoverTask = null;
        }
    }

    private class SaveTask extends AsyncTask<Object[], Void, Void> {

        @Override
        protected Void doInBackground(Object[]... params) {
            saveInternal(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mSaveTask = null;
        }
    }

    private static PlaybackState sSingleton;

    public static PlaybackState getInstance() {
        if (sSingleton == null)
            sSingleton = new PlaybackState();

        return sSingleton;
    }

    public static final String ACTION_PLAY = "com.example.musicplayer.ACTION_PLAY";
    public static final String ACTION_NEXT = "com.example.musicplayer.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.musicplayer.ACTION_PREV";
    public static final String ACTION_COVER_LOADED = "com.example.musicplayer.ACTION_COVER_LOADED";
    public static final String ACTION_STATE_CHANGED = "com.example.musicplayer.ACTION_STATE_CHANGED";

    private static final int REWIND_THRESHOLD = 3000; // in milliseconds

    // for future updates in PlaybackState
    private static final int VERSION = 1;

    private static final String FILENAME = "cache";
    private static final int AUTO_SAVE_DELAY = 2000; // in milliseconds

    private Runnable mSaveRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSaveTask != null)
                mSaveTask.cancel(false);

            mSaveTask = new SaveTask();
            mSaveTask.execute(getSaveArgs(true));
        }
    };

    private Context mContext;
    private boolean mInited;

    private Song mCurrentSong;
    private long mCachedPosition;
    private boolean mFilterSetBySong;
    private FilterState mFilterState;
    private FilterState mOldFilterState; // for restoring when set by song
    private PlaybackList mPlaybackList;
    private RepeatMode mRepeatMode;
    private boolean mShuffleEnabled;
    private boolean mIsPlaying;
    private boolean mRewindOnPrevious;

    private SleepTimer mSleepTimer;

    private MediaTag mMediaTag;
    private Decoder mDecoder;
    private CoverTask mCoverTask;
    private boolean mIsCached;
    private Bitmap mResultBitmap;
    private int mResultColor;

    private Handler mHandler;
    private SaveTask mSaveTask;

    private MusicLibrary.Observer mLibraryObserver = new MusicLibrary.Observer() {
        @Override
        public void update(Observable sender, MusicLibrary.ObserverData data) {
            switch (data.type) {
                case SongsEdited:
                    if (data.songs != null && data.songs.contains(mCurrentSong)) {
                        refreshSong();
                    }

                    break;
                case SongsRemoved:
                    if (mPlaybackList != null)
                        mPlaybackList.removeSongs(data.songs);

                    if (data.songs != null && data.songs.contains(mCurrentSong)) {
                        mCurrentSong = null;
                        nextSong(true);
                    }

                    break;
            }
        }
    };

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (PreferenceManager.KEY_REWIND_ON_PREVIOUS.equals(data.key))
                        mRewindOnPrevious = (boolean)data.value;
                    break;
            }
        }
    };

    private PlaybackState() {
        mContext = null;
        mInited = false;
        mCurrentSong = null;
        mCachedPosition = 0;
        mFilterSetBySong = false;
        mFilterState = null;
        mOldFilterState = null;
        mPlaybackList = null;
        mRepeatMode = RepeatMode.None;
        mShuffleEnabled = false;
        mIsPlaying = false;

        mMediaTag = null;
        mDecoder = null;
        mCoverTask = null;
        mIsCached = false;
        mResultBitmap = null;
        mResultColor = Color.GRAY;
    }

    public void initialize(Context context) {
        if (!mInited) {
            mContext = context.getApplicationContext();
            PreferenceManager prefs = PreferenceManager.getInstance();
            MusicLibrary lib = MusicLibrary.getInstance();

            prefs.initialize(mContext);
            lib.initialize(mContext);
            lib.addObserver(mLibraryObserver);
            prefs.addObserver(mPreferenceObserver);

            mRewindOnPrevious = prefs.getBoolean(PreferenceManager.KEY_REWIND_ON_PREVIOUS, true);

            if (prefs.getBoolean(PreferenceManager.KEY_START_PLAYBACK_HEADSET) ||
                    prefs.getBoolean(PreferenceManager.KEY_START_PLAYBACK_BLUETOOTH))
                HeadsetService.start(mContext);

            // TODO: album cover size
            int size = prefs.parseInt(PreferenceManager.KEY_ALBUM_COVER_SIZE);

            mMediaTag = new MediaTag();

            mDecoder = new Decoder();
            mDecoder.setVideoSize(size, size);

            mFilterState = FilterState.Factory.getFilterState();

            mHandler = new Handler();

            restore();

            mSleepTimer = new SleepTimer();
            mSleepTimer.initialize();

            mInited = true;
        }
    }

    public boolean isInited() {
        return mInited;
    }

    public boolean nextSong(boolean forceNext) {
        if (mPlaybackList == null)
            return false;

        Song next;

        if (mCurrentSong != null && !forceNext && mRepeatMode == RepeatMode.Single)
            next = mCurrentSong;
        else
            next = mPlaybackList.nextSong();

        if (next == null) {
            switch (mRepeatMode) {
                case None:
                    return false;
                case All:
                    mPlaybackList.repeat();
                    next = mPlaybackList.nextSong();

                    if (next == null)
                        return false;

                    break;
            }
        }

        if (mCurrentSong != null && mCurrentSong != next && !mCurrentSong.isHidden())
            mPlaybackList.addToHistoryLast(mCurrentSong);

        setCurrentSong(next, forceNext);
        return true;
    }

    public boolean previousSong() {
        if (mPlaybackList == null)
            return false;

        if (!mPlaybackList.hasPreviousSong() || (mRewindOnPrevious && mCurrentSong != null &&
                requestCurrentPosition() > REWIND_THRESHOLD)) {
            seekTo(0);
            return true;
        }

        Song previous = mPlaybackList.previousSong();

        if (previous == null)
            return false;

        if (mCurrentSong != null && !mCurrentSong.isHidden())
            mPlaybackList.addToNextSongsFirst(mCurrentSong);

        setCurrentSong(previous, true);
        return true;
    }

    public boolean skipSongs(int numSongs) {
        if (numSongs < 0 || mPlaybackList == null)
            return false;

        if (mCurrentSong != null)
            mPlaybackList.addToHistoryLast(mCurrentSong);

        mCurrentSong = null;

        mPlaybackList.skipSongs(numSongs);

        return nextSong(true);
    }

    public long requestCurrentPosition() {
        notifyObservers(new ObserverData(ObserverData.Type.RequestCurrentPosition));

        return getCachedPosition();
    }

    @Nullable
    public Song getCurrentSong() {
        return mCurrentSong;
    }

    public long getCachedPosition() {
        return mCachedPosition;
    }

    public PlaybackList getPlaybackList() {
        return mPlaybackList;
    }

    public FilterState getFilterState() {
        return mFilterState;
    }

    public RepeatMode getRepeatMode() {
        return mRepeatMode;
    }

    public boolean isShuffled() {
        return mShuffleEnabled;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public boolean isCoverCached() {
        return mIsCached;
    }

    public Bitmap getCover() {
        return mResultBitmap;
    }

    public int getCoverColor() {
        return mResultColor;
    }

    public SleepTimer getSleepTimer() {
        return mSleepTimer;
    }

    public void setCurrentSong(Song song, boolean forceChange) {
        if (song == null)
            return;

        mCurrentSong = song;
        mCachedPosition = 0;

        if (mCoverTask != null)
            mCoverTask.cancel(false);

        mIsCached = false;
        mCoverTask = new CoverTask();
        mCoverTask.execute(song);

        EqualizerPreset preset = MusicLibrary.getInstance().getPresetById(song.getPresetId());
        if (preset != null) {
            applyPreset(preset, true);
        }
        else if (mFilterSetBySong) {
            setFilterState(mOldFilterState, false);
        }

        callSongChanged(mCurrentSong, forceChange);
        requestAutoSave();
    }

    public void refreshSong() {
        if (mCurrentSong == null)
            return;

        if (mCoverTask != null)
            mCoverTask.cancel(false);

        mIsCached = false;

        mCoverTask = new CoverTask();
        mCoverTask.execute(mCurrentSong);

        callSongChanged(mCurrentSong, false);
    }

    public void setCachedPosition(long position) {
        mCachedPosition = position;
    }

    public void seekTo(long position) {
        setCachedPosition(position);

        notifyObservers(new ObserverData(position));
    }

    public void setPlaybackList(PlaybackList list, boolean keepQueue) {
        if (mPlaybackList != null && keepQueue)
            mPlaybackList.transferQueueTo(list);

        mCurrentSong = null;
        mPlaybackList = list;

        mShuffleEnabled = mPlaybackList.isShuffled();
    }

    public void applyPreset(EqualizerPreset preset, boolean shouldEnable) {
        applyPreset(preset, false, shouldEnable);
    }

    public void applyPreset(EqualizerPreset preset) {
        applyPreset(preset, false, false);
    }

    public void setFilterState(FilterState state) {
        setFilterState(state, false);
    }

    public void setPlayingState(boolean isPlaying) {
        if (isPlaying && !PlaybackService.isRunning()) {
            if (mCurrentSong == null) {
                setPlaybackList(PlaybackList.fromObject(LibraryObject.UNKNOWN, 0, Sorting.Title, false, isShuffled(), null), false);
                nextSong(true);
            }

            mSleepTimer.startAlarmIfRepeat();

            PlaybackService.start(mContext);
            return;
        }

        boolean temp = mIsPlaying;
        mIsPlaying = isPlaying;

        if (temp != isPlaying)
            callPlayingStateChanged(isPlaying);
    }

    public void togglePlayingState() {
        setPlayingState(!isPlaying());
    }

    public void forceStopPlayback() {
        notifyObservers(new ObserverData(ObserverData.Type.ForceStopPlayback));
    }

    public void toggleShuffling() {
        mShuffleEnabled = !mShuffleEnabled;
        requestAutoSave();

        if (mPlaybackList == null)
            return;

        mPlaybackList.setIsShuffled(mCurrentSong, mShuffleEnabled);
    }

    public void toggleRepeatMode() {
        switch (mRepeatMode) {
            case None:
                mRepeatMode = RepeatMode.All;
                break;
            case All:
                mRepeatMode = RepeatMode.Single;
                break;
            case Single:
                mRepeatMode = RepeatMode.None;
                break;
        }

        requestAutoSave();
    }

    public void restore() {
        MusicLibrary lib = MusicLibrary.getInstance();

        DataInputStream stream = null;

        try {
            stream = new DataInputStream(mContext.openFileInput(FILENAME));

            int version = stream.readInt();

            setCurrentSong(lib.getSongById(stream.readInt()), false);
            mCachedPosition = stream.readLong();
            mRepeatMode = RepeatMode.values()[stream.readInt()];
            mShuffleEnabled = stream.readBoolean();

            mFilterSetBySong = stream.readBoolean();
            if (mFilterSetBySong) {
                mOldFilterState = FilterState.Factory.getFilterState();
                boolean oldStateEnabled = stream.readBoolean();
                for (int i = 0; i < FilterState.NUM_EQ_ARGS; i++) {
                    mOldFilterState.getEqualizerGains()[i] = stream.readFloat();
                }
                mOldFilterState.setEqualizer(oldStateEnabled, mOldFilterState.getEqualizerGains(),
                        mOldFilterState.getBassGain(), mOldFilterState.getTrebleGain());
            }

            boolean stateEnabled = stream.readBoolean();
            for (int i = 0; i < FilterState.NUM_EQ_ARGS; i++) {
                mFilterState.getEqualizerGains()[i] = stream.readFloat();
            }
            mFilterState.setEqualizer(stateEnabled, mFilterState.getEqualizerGains(),
                    mFilterState.getBassGain(), mFilterState.getTrebleGain());

            int objectId = stream.readInt();
            int objectType = stream.readInt();
            Sorting sorting = Sorting.values()[stream.readInt()];
            boolean reversed = stream.readBoolean();

            LibraryObject object = null;
            switch (objectType) {
                case LibraryObject.GENRE:
                    object = lib.getGenreById(objectId);
                    break;
                case LibraryObject.ARTIST:
                    object = lib.getArtistById(objectId);
                    break;
                case LibraryObject.ALBUM:
                    object = lib.getAlbumById(objectId);
                    break;
                case LibraryObject.PLAYLIST:
                    object = lib.getPlaylistById(objectId);
                    break;
                case LibraryObject.SONG:
                case LibraryObject.UNKNOWN:
                    break;
            }

            mPlaybackList = new PlaybackList(object, sorting, reversed);

            int numQueue = stream.readInt();
            for (int i = 0; i < numQueue; i++) {
                Song song = lib.getSongById(stream.readInt());
                if (song != null)
                    mPlaybackList.addToQueueLast(song, false);
            }

            int numNextSongs = stream.readInt();
            for (int i = 0; i < numNextSongs; i++) {
                Song song = lib.getSongById(stream.readInt());
                if (song != null)
                    mPlaybackList.addToNextSongsLast(song);
            }

            int numHistory = stream.readInt();
            for (int i = 0; i < numHistory; i++) {
                Song song = lib.getSongById(stream.readInt());
                if (song != null)
                    mPlaybackList.addToHistoryLast(song);
            }

            while (mCurrentSong == null && !mPlaybackList.getNextSongsList().isEmpty()) {
                nextSong(true);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        saveInternal(getSaveArgs(false));
    }

    private void callSongChanged(Song song, boolean forceChange) {
        notifyObservers(new ObserverData(song, forceChange));
    }

    private void callCoverLoaded(Bitmap bitmap, int color) {
        if (isInited()) {
            Intent intent = new Intent(ACTION_COVER_LOADED);
            mContext.sendBroadcast(intent);
        }

        notifyObservers(new ObserverData(bitmap, color));
    }

    private void callFilterStateChanged(FilterState state) {
        notifyObservers(new ObserverData(state));
    }

    private void callPlayingStateChanged(boolean isPlaying) {
        if (isInited()) {
            Intent intent = new Intent(ACTION_STATE_CHANGED);
            mContext.sendBroadcast(intent);
        }

        notifyObservers(new ObserverData(isPlaying));
    }

    private void applyPreset(EqualizerPreset preset, boolean bySong, boolean shouldEnable) {
        FilterState nextState = FilterState.Factory.getFilterState();
        nextState.setEqualizer(shouldEnable || bySong || mFilterState.isEqualizerEnabled(), preset.getBandGains(),
                mFilterState.getBassGain(), mFilterState.getTrebleGain());

        setFilterState(nextState, bySong);
    }

    private void setFilterState(FilterState state, boolean bySong) {
        if (bySong && !mFilterSetBySong) {
            mOldFilterState = mFilterState;
            mFilterState = null;
        }

        mFilterSetBySong = bySong;

        if (mFilterState != null)
            mFilterState.release();

        mFilterState = state;

        callFilterStateChanged(mFilterState);
        requestAutoSave();
    }

    public void requestAutoSave() {
        mHandler.removeCallbacks(mSaveRunnable);
        mHandler.postDelayed(mSaveRunnable, AUTO_SAVE_DELAY);
    }

    private synchronized void saveInternal(Object[] args) {
        DataOutputStream stream = null;

        try {
            stream = new DataOutputStream(mContext.openFileOutput(FILENAME, 0));

            // version
            stream.writeInt(VERSION);
            // current song id
            stream.writeInt((int)args[0]);
            // cached position
            stream.writeLong((long)args[1]);
            // repeat mode
            stream.writeInt((int)args[2]);
            // shuffle enabled
            stream.writeBoolean((boolean)args[3]);

            // filter state
            boolean filterSetBySong = (boolean)args[4];
            stream.writeBoolean(filterSetBySong);
            if (filterSetBySong) {
                stream.writeBoolean((boolean)args[5]);
                float[] gains = (float[])args[6];
                for (int i = 0; i < FilterState.NUM_EQ_ARGS; i++) {
                    float gain = gains != null ? gains[i] : 0.0f;
                    stream.writeFloat(gain);
                }
            }
            // equalizer enabled
            stream.writeBoolean((boolean)args[7]);
            // equalizer gains
            float[] gains = (float[])args[8];
            for (int i = 0; i < FilterState.NUM_EQ_ARGS; i++) {
                float gain = gains != null ? gains[i] : 0.0f;
                stream.writeFloat(gain);
            }

            // played object from PlaybackList
            LibraryObject object = (LibraryObject)args[9];
            if (object != null) {
                stream.writeInt(object.getId());
                stream.writeInt(object.getType());
            }
            else {
                stream.writeInt(-1);
                stream.writeInt(LibraryObject.UNKNOWN);
            }
            // sorting ordinal
            stream.writeInt((int)args[10]);
            // sorting reversed
            stream.writeBoolean((boolean)args[11]);

            List<Song> queue = (List<Song>)args[12];
            if (queue != null) {
                stream.writeInt(queue.size());
                for (Song song : queue)
                    if (song != null)
                        stream.writeInt(song.getId());
            }
            else {
                stream.writeInt(0);
            }

            List<Song> nextSongs = (List<Song>)args[13];
            if (nextSongs != null) {
                stream.writeInt(nextSongs.size());
                for (Song song : nextSongs)
                    if (song != null)
                        stream.writeInt(song.getId());
            }
            else {
                stream.writeInt(0);
            }

            List<Song> history = (List<Song>)args[14];
            if (history != null) {
                stream.writeInt(history.size());
                for (Song song : history)
                    if (song != null)
                        stream.writeInt(song.getId());
            }
            else {
                stream.writeInt(0);
            }

            stream.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Object[] getSaveArgs(boolean offthread) {
        return new Object[] {
                (mCurrentSong != null ? mCurrentSong.getId() : 0),
                mCachedPosition,
                mRepeatMode.ordinal(),
                mShuffleEnabled,
                mFilterSetBySong,
                (mOldFilterState != null && mOldFilterState.isEqualizerEnabled()),
                (mOldFilterState != null ? (offthread ? Arrays.copyOf(mOldFilterState.getEqualizerGains(), mOldFilterState.getEqualizerGains().length) : mOldFilterState.getEqualizerGains()) : null),
                (mFilterState != null && mFilterState.isEqualizerEnabled()),
                (mFilterState != null ? (offthread ? Arrays.copyOf(mFilterState.getEqualizerGains(), mFilterState.getEqualizerGains().length) : mFilterState.getEqualizerGains()) : null),
                (mPlaybackList != null ? mPlaybackList.getPlayedObject() : null),
                (mPlaybackList != null ? mPlaybackList.getSorting().ordinal() : 0),
                (mPlaybackList != null && mPlaybackList.isSortingReversed()),
                (mPlaybackList != null && mPlaybackList.getQueue() != null ? (offthread ? mPlaybackList.getQueue().clone() : mPlaybackList.getQueue()) : null),
                (mPlaybackList != null ? (offthread ? mPlaybackList.getNextSongsList().clone() : mPlaybackList.getNextSongsList()) : null),
                (mPlaybackList != null ? (offthread ? mPlaybackList.getHistory().clone() : mPlaybackList.getHistory()) : null)
        };
    }
}
