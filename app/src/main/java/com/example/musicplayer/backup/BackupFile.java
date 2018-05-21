package com.example.musicplayer.backup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by 19tar on 04.10.2017.
 */

public class BackupFile {

    static final String TAG_ROOT = "Backup";
    static final String TAG_PREFS = "Preferences";
    static final String TAG_STATS = "SongStats";
    static final String TAG_PRESETS = "Presets";
    static final String TAG_PLAYLISTS = "Playlists";

    static final String TAG_ITEM = "Item";
    static final String TAG_SONG_PATH = "SongPath";

    static final String TAG_TYPE = "Type";
    static final String TAG_KEY = "Key";
    static final String TAG_VALUE = "Value";

    static final String TAG_DATE_ADDED = "DateAdded";
    static final String TAG_PRESET_ID = "PresetID";
    static final String TAG_PLAY_COUNT = "PlayCount";
    static final String TAG_LAST_PLAYED = "LastPlayed";
    static final String TAG_IS_HIDDEN = "IsHidden";

    static final String TAG_NAME = "Name";
    static final String TAG_GAINS = "BandGains";
    static final String TAG_PREBUILT = "Prebuilt";

    static final String TAG_IMPORT_PATH = "ImportPath";
    static final String TAG_SONGS = "Songs";

    public static final String EXTENSION = ".mpbackup";

    public static final String TYPE_STRING = "string";
    public static final String TYPE_INT = "int";
    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_SET = "set";

    private static final String STRING_SEPARATOR = ";";

    private ArrayList<Preference> mPreferences;
    private ArrayList<SongStat> mSongStats;
    private ArrayList<Preset> mPresets;
    private ArrayList<Playlist> mPlaylists;

    public BackupFile() {
        mPreferences = new ArrayList<>();
        mSongStats = new ArrayList<>();
        mPresets = new ArrayList<>();
        mPlaylists = new ArrayList<>();
    }

    public static boolean hasBackupExtension(String filePath) {
        return filePath.toLowerCase().endsWith(EXTENSION);
    }

    public void addStringPref(String key, String value) {
        Preference preference = new Preference(TYPE_STRING, key, value);
        mPreferences.add(preference);
    }

    public void addIntPref(String key, int value) {
        mPreferences.add(new Preference(TYPE_INT, key, String.valueOf(value)));
    }

    public void addBoolPref(String key, boolean value) {
        mPreferences.add(new Preference(TYPE_BOOL, key, String.valueOf(value)));
    }

    public void addStringSetPref(String key, Set<String> value) {
        StringBuilder builder = new StringBuilder();

        for (String val : value) {
            if (builder.length() > 0)
                builder.append(STRING_SEPARATOR);

            builder.append(val);
        }

        mPreferences.add(new Preference(TYPE_SET, key, builder.toString()));
    }

    void addPreference(String type, String key, String value) {
        mPreferences.add(new Preference(type, key, value));
    }

    public void addSongStat(String path, long dateAdded, int preset, int playCount, long lastPlayed, boolean hidden) {
        mSongStats.add(new SongStat(path, dateAdded, preset, playCount, lastPlayed, hidden));
    }

    public void addPreset(int id, String name, String gains, boolean prebuilt) {
        mPresets.add(new Preset(id, name, gains, prebuilt));
    }

    public Playlist addPlaylist(String name, String importPath) {
        Playlist playlist = new Playlist(name, importPath);
        mPlaylists.add(playlist);
        return playlist;
    }

    void addPlaylist(String name, String importPath, Collection<String> songs) {
        mPlaylists.add(new Playlist(name, importPath, songs));
    }

    public List<Preference> getPreferences() {
        return Collections.unmodifiableList(mPreferences);
    }

    public List<SongStat> getSongStats() {
        return Collections.unmodifiableList(mSongStats);
    }

    public List<Preset> getPresets() {
        return Collections.unmodifiableList(mPresets);
    }

    public List<Playlist> getPlaylists() {
        return Collections.unmodifiableList(mPlaylists);
    }

    public static class Preference {
        private String mType;
        private String mKey;
        private String mValue;

        public Preference(String type, String key, String value) {
            mType = type;
            mKey = key;
            mValue = value;
        }

        public String getType() {
            return mType;
        }

        public String getKey() {
            return mKey;
        }

        public String getStringValue() {
            return mValue;
        }

        public int getIntValue() {
            return Integer.parseInt(mValue);
        }

        public boolean getBoolValue() {
            return Boolean.parseBoolean(mValue);
        }

        public Set<String> getStringSetValue() {
            return new HashSet<>(Arrays.asList(mValue.split(STRING_SEPARATOR)));
        }
    }

    public static class SongStat {
        private String mPath;
        private long mDateAdded;
        private int mPresetId;
        private int mPlayCount;
        private long mLastPlayed;
        private boolean mHidden;

        public SongStat(String song, long dateAdded, int presetId, int playCount, long lastPlayed, boolean hidden) {
            mPath = song;
            mDateAdded = dateAdded;
            mPresetId = presetId;
            mPlayCount = playCount;
            mLastPlayed = lastPlayed;
            mHidden = hidden;
        }

        public String getSongPath() {
            return mPath;
        }

        public long getDateAdded() {
            return mDateAdded;
        }

        public int getPresetId() {
            return mPresetId;
        }

        public int getPlayCount() {
            return mPlayCount;
        }

        public long getLastPlayed() {
            return mLastPlayed;
        }

        public boolean isHidden() {
            return mHidden;
        }
    }

    public static class Preset {
        private int mId;
        private String mName; // resource name for prebuilts
        private String mBandGains;
        private boolean mPrebuilt;

        public Preset(int id, String name, String gains, boolean prebuilt) {
            mId = id;
            mName = name;
            mBandGains = gains;
            mPrebuilt = prebuilt;
        }

        public int getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        public String getBandGains() {
            return mBandGains;
        }

        public boolean isPrebuilt() {
            return mPrebuilt;
        }
    }

    public static class Playlist {
        private String mName;
        private String mImportPath;
        private ArrayList<String> mSongs;

        public Playlist(String name, String importPath) {
            mName = name;
            mImportPath = importPath;
            mSongs = new ArrayList<>();
        }

        public Playlist(String name, String importPath, Collection<String> songs) {
            mName = name;
            mImportPath = importPath;
            mSongs = new ArrayList<>(songs);
        }

        public String getName() {
            return mName;
        }

        public String getImportPath() {
            return mImportPath;
        }

        public void addSong(String path) {
            mSongs.add(path);
        }

        public List<String> getSongList() {
            return Collections.unmodifiableList(mSongs);
        }
    }
}
