package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.example.musicplayer.backup.BackupFile;
import com.example.musicplayer.backup.BackupWriter;
import com.example.musicplayer.backup.RestoreService;
import com.example.musicplayer.library.MusicLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Created by Tarik on 06.08.2016.
 */
public class PreferenceManager extends Observable<PreferenceManager.ObserverData> {

    public interface Observer extends IObserver<ObserverData> { }

    public static class ObserverData {

        public enum Type {
            PreferenceChange, PreferencesRestored
        }

        public final Type type;
        public final String key;
        public final Object value;

        public ObserverData(Type type) {
            this.type = type;
            this.key = null;
            this.value = null;
        }

        public ObserverData(String key, Object value) {
            this.type = Type.PreferenceChange;
            this.key = key;
            this.value = value;
        }
    }

    private static final String BACKUPS_DIR = "backups";

    public static final int[] THEME_RESOURCES = { R.style.LightTheme, R.style.DarkTheme };

    public static final String ACTION_PREFERENCE_CHANGE = "com.example.musicplayer.PREFERENCE_CHANGE";
    public static final String EXTRA_PREFERENCE_KEY = "key";
    public static final String EXTRA_PREFERENCE_VALUE = "value";

    public static final String KEY_IMPORTED_PLAYLISTS = "imported_playlists";

    public static final String KEY_AUTOMATIC_PLAYLISTS = "automatic_playlists";
    public static final String KEY_START_PAGE_LAYOUT = "start_page_layout";
    public static final String KEY_START_PAGE_LAUNCH = "start_page_launch";

    public static final String KEY_LIBRARY_IGNORE_SHORT_SONGS = "library_ignore_short_songs";
    public static final String KEY_SHOW_PARENT_FOLDERS = "show_parent_folders";
    public static final String KEY_PLAYER_ALBUM_COVER_SIZE = "player_album_cover_size";
    public static final String KEY_ALBUM_COVER_SIZE = "album_cover_size";

    public static final String KEY_FADE_ON_PLAY_PAUSE = "fade_on_play_pause";
    public static final String KEY_LOWER_VOLUME_INCOMING_MESSAGE = "lower_volume_incoming_message";
    public static final String KEY_SIDEBAR_PORTRAIT_POS = "sidebar_portrait_pos";
    public static final String KEY_SIDEBAR_TITLES_VISIBLE = "sidebar_titles_visible";

    public static final String KEY_USE_SONG_FILE_NAMES = "use_song_file_names";

    public static final String KEY_PLAYLIST_ADD_DUPLICATE = "playlist_add_duplicate";

    public static final String KEY_SHOW_TIME_REMAINING = "show_time_remaining";

    public static final String KEY_LOCKSCREEN_SHOW_ART = "lockscreen_show_art";
    public static final String KEY_APP_THEME = "app_theme";

    public static final String KEY_START_PLAYBACK_HEADSET = "start_playback_headset";
    public static final String KEY_START_PLAYBACK_BLUETOOTH = "start_playback_bluetooth";
    public static final String KEY_REWIND_ON_PREVIOUS = "rewind_on_previous";

    private static final String KEY_DEFAULT_VALUES_SET = "default_values_set";

    public static final String THEME_LIGHT = "0";
    public static final String THEME_DARK = "1";

    public static final String COVER_FULL_SIZED = "0";
    public static final String COVER_SMALL = "1";

    private static PreferenceManager sSingleton;

    public static PreferenceManager getInstance() {
        if (sSingleton == null)
            sSingleton = new PreferenceManager();

        return sSingleton;
    }

    private boolean mInited;
    private Context mContext;

    private SharedPreferences mDefaultPreferences;

    public void initialize(Context context) {
        if (!isInited()) {
            mContext = context.getApplicationContext();

            mDefaultPreferences = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(mContext);

            //mDefaultPreferences.edit().clear().apply();

            if (mDefaultPreferences.getString(KEY_START_PAGE_LAYOUT, null) == null) {
                mDefaultPreferences.edit()
                        .putString(KEY_START_PAGE_LAYOUT, "0;1;2;3;4;5")
                        .putInt(KEY_START_PAGE_LAUNCH, 0).apply();
            }

            if (mDefaultPreferences.getString(KEY_ALBUM_COVER_SIZE, null) == null) {
                mDefaultPreferences.edit().putString(KEY_ALBUM_COVER_SIZE, "1024").apply();
            }

            Util.deleteDirectory(getBackupsDir());

            if (!mDefaultPreferences.getBoolean(KEY_DEFAULT_VALUES_SET, false)) {
                android.preference.PreferenceManager.setDefaultValues(mContext, R.xml.pref_appearance, true);
                android.preference.PreferenceManager.setDefaultValues(mContext, R.xml.pref_library, true);
                android.preference.PreferenceManager.setDefaultValues(mContext, R.xml.pref_misc, true);
                android.preference.PreferenceManager.setDefaultValues(mContext, R.xml.pref_info, true);

                mDefaultPreferences.edit().putBoolean(KEY_DEFAULT_VALUES_SET, true).apply();
            }

            mInited = true;
        }
    }

    public boolean isInited() {
        return mInited;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mDefaultPreferences.getBoolean(key, defaultValue);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return mDefaultPreferences.getString(key, defaultValue);
    }

    public int parseInt(String key) {
        String pref = getString(key);
        if (!Util.stringIsEmpty(pref))
            return Integer.parseInt(pref);

        return 0;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return mDefaultPreferences.getInt(key, defaultValue);
    }

    public void setString(String key, String value) {
        setString(key, value, false);
    }

    public void setString(String key, String value, boolean silent) {
        mDefaultPreferences.edit().putString(key, value).apply();

        if (!silent)
            notifyPreferenceChange(key, value);
    }

    public Set<String> getStringSet(String key) {
        return mDefaultPreferences.getStringSet(key, null);
    }

    public void setStringSet(String key, Set<String> value) {
        mDefaultPreferences.edit().putStringSet(key, value).apply();
    }

    public void setInt(String key, int value) {
        setInt(key, value, false);
    }

    public void setInt(String key, int value, boolean silent) {
        mDefaultPreferences.edit().putInt(key, value).apply();

        if (!silent)
            notifyPreferenceChange(key, value);
    }

    public void setBoolean(String key, boolean value) {
        setBoolean(key, value, false);
    }

    public void setBoolean(String key, boolean value, boolean silent) {
        mDefaultPreferences.edit().putBoolean(key, value).apply();

        if (!silent)
            notifyPreferenceChange(key, value);
    }

    public void setAppTheme(String value) {
        setString(KEY_APP_THEME, value);
    }

    public void setAlbumCoverType(String value) {
        setString(KEY_PLAYER_ALBUM_COVER_SIZE, value);
    }

    public void notifyPreferenceChange(String key, Object value) {
        if (mContext != null) {
            Intent intent = new Intent(ACTION_PREFERENCE_CHANGE);
            intent.putExtra(EXTRA_PREFERENCE_KEY, key);
            intent.putExtra(EXTRA_PREFERENCE_VALUE, value.toString());
            mContext.sendBroadcast(intent);
        }

        notifyObservers(new ObserverData(key, value));
    }

    public void applyCurrentTheme(Context context) {
        int themeIndex = Integer.parseInt(getString(KEY_APP_THEME, "0"));
        context.setTheme(THEME_RESOURCES[themeIndex]);
    }

    @SuppressWarnings("unchecked")
    public void backupData(BackupFile file) {
        Map<String, ?> entries = mDefaultPreferences.getAll();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Integer) {
                file.addIntPref(entry.getKey(), (int)value);
            }
            else if (value instanceof Boolean) {
                file.addBoolPref(entry.getKey(), (boolean)value);
            }
            else if (value instanceof Set) {
                file.addStringSetPref(entry.getKey(), (Set<String>)value);
            }
            else if (value instanceof String) {
                file.addStringPref(entry.getKey(), (String)value);
            }
        }
    }

    public void restoreData(BackupFile file) {
        SharedPreferences.Editor editor = mDefaultPreferences.edit();

        for (BackupFile.Preference pref : file.getPreferences()) {
            switch (pref.getType()) {
                case BackupFile.TYPE_INT:
                    editor.putInt(pref.getKey(), pref.getIntValue());
                    break;
                case BackupFile.TYPE_BOOL:
                    editor.putBoolean(pref.getKey(), pref.getBoolValue());
                    break;
                case BackupFile.TYPE_SET:
                    editor.putStringSet(pref.getKey(), pref.getStringSetValue());
                    break;
                case BackupFile.TYPE_STRING:
                    editor.putString(pref.getKey(), pref.getStringValue());
                    break;
            }
        }

        editor.apply();
    }

    private File getBackupsDir() {
        return new File(mContext.getCacheDir(), BACKUPS_DIR);
    }

    public File makeBackupFile(String name) {
        File backupsDir = getBackupsDir();
        backupsDir.mkdirs();
        File file = new File(backupsDir, name + BackupFile.EXTENSION);

        BackupFile backupFile = new BackupFile();
        backupData(backupFile);
        MusicLibrary.getInstance().backupData(backupFile);

        BackupWriter writer = new BackupWriter(backupFile);
        try {
            writer.saveToStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return file;
    }

    public void restore(Uri uri) {
        RestoreService.start(mContext, uri);
    }
}
