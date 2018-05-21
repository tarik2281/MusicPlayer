package com.example.musicplayer.ui.activities;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.musicplayer.AppCompatPreferenceActivity;
import com.example.musicplayer.ConfirmPreference;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.playback.HeadsetService;
import com.example.musicplayer.ui.dialogs.BackupDialog;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    /*public static class BackupProgressDialog extends DialogFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setCancelable(false);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setTitle("Backup");
            dialog.setMessage("Backing up data...");
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            return dialog;
        }
    }*/

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferencesRestored:
                    recreate();
                    break;
                case PreferenceChange:
                    if ("app_theme".equals(data.key))
                        recreate();
                    break;
                /*case BackupStateChanged:
                    if (data.state && !mStopped) {
                        if (getFragmentManager().findFragmentByTag("backup_dialog") == null)
                            new BackupProgressDialog().show(getFragmentManager(), "backup_progress_dialog");
                    }
                    else if (!data.state && !mStopped) {
                        BackupProgressDialog dialog = (BackupProgressDialog)getFragmentManager().findFragmentByTag("backup_progress_dialog");
                        if (dialog != null)
                            dialog.dismiss();
                    }
                    break;*/
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.musicplayer.PreferenceManager prefManager = com.example.musicplayer.PreferenceManager.getInstance();

        prefManager.addObserver(mPreferenceObserver);

        prefManager.applyCurrentTheme(this);

        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        com.example.musicplayer.PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return getResources().getBoolean(R.bool.isTablet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || AppearanceFragment.class.getName().equals(fragmentName)
                || LibraryFragment.class.getName().equals(fragmentName)
                || MiscFragment.class.getName().equals(fragmentName)
                || InfoFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static abstract class BaseFragment extends PreferenceFragment {

        private void setListSummary(ListPreference preference, String value) {
            CharSequence[] values = preference.getEntryValues();

            int index = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    index = i;
                    break;
                }
            }

            preference.setSummary(preference.getEntries()[index]);
        }

        private Preference.OnPreferenceChangeListener mPreferenceListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PreferenceManager.getInstance().notifyPreferenceChange(preference.getKey(), newValue);

                return true;
            }
        };

        private Preference.OnPreferenceChangeListener mListPreferenceListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (PreferenceManager.getInstance().getString(preference.getKey()).equals(newValue.toString()))
                    return false;

                setListSummary((ListPreference)preference, newValue.toString());

                PreferenceManager.getInstance().notifyPreferenceChange(preference.getKey(), newValue);

                return true;
            }
        };

        public void bindPreference(String key) {
            findPreference(key).setOnPreferenceChangeListener(mPreferenceListener);
        }

        public void bindListPreference(String key) {
            ListPreference preference = (ListPreference)findPreference(key);
            setListSummary(preference, PreferenceManager.getInstance().getString(preference.getKey()));
            preference.setOnPreferenceChangeListener(mListPreferenceListener);
        }
    }

    public static class AppearanceFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appearance);

            bindListPreference(PreferenceManager.KEY_APP_THEME);

            Preference editStartLayout = findPreference("edit_start_layout");
            editStartLayout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    LayoutEditorActivity.start(getActivity());
                    return true;
                }
            });
            if (getResources().getBoolean(R.bool.isTablet)) {
                PreferenceCategory categoryGeneral = (PreferenceCategory)findPreference("category_general");
                PreferenceCategory categoryPlayback = (PreferenceCategory)findPreference("category_playback");

                categoryGeneral.removePreference(findPreference(PreferenceManager.KEY_SIDEBAR_PORTRAIT_POS));
                categoryGeneral.removePreference(findPreference(PreferenceManager.KEY_SIDEBAR_TITLES_VISIBLE));

                categoryPlayback.removePreference(findPreference(PreferenceManager.KEY_PLAYER_ALBUM_COVER_SIZE));
            }
            else {

                bindListPreference(PreferenceManager.KEY_SIDEBAR_PORTRAIT_POS);
                bindPreference(PreferenceManager.KEY_SIDEBAR_TITLES_VISIBLE);
                bindListPreference(PreferenceManager.KEY_PLAYER_ALBUM_COVER_SIZE);
            }

            bindPreference(PreferenceManager.KEY_SHOW_PARENT_FOLDERS);
            bindPreference(PreferenceManager.KEY_SHOW_TIME_REMAINING);
            bindPreference(PreferenceManager.KEY_LOCKSCREEN_SHOW_ART);
        }
    }

    public static class LibraryFragment extends BaseFragment {

        private static final int REQUEST_IMPORT_PLAYLIST = 15;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_library);

            findPreference(PreferenceManager.KEY_LIBRARY_IGNORE_SHORT_SONGS).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PreferenceManager.getInstance().notifyPreferenceChange(preference.getKey(), newValue);
                    MusicLibrary.getInstance().postScanLibrary(false);
                    return true;
                }
            });
            bindPreference(PreferenceManager.KEY_LIBRARY_IGNORE_SHORT_SONGS);

            findPreference("hidden_songs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    HiddenSongsActivity.start(getActivity());
                    return true;
                }
            });

            findPreference("import_playlist").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = FilePickerActivity.importIntent(getActivity(), FilePickerActivity.TYPE_PLAYLIST);
                    startActivityForResult(intent, REQUEST_IMPORT_PLAYLIST);
                    return true;
                }
            });

            bindPreference(PreferenceManager.KEY_USE_SONG_FILE_NAMES);

            bindPreference(PreferenceManager.KEY_AUTOMATIC_PLAYLISTS);

            findPreference("scanned_directories").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), IgnoreFoldersActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            /*((ConfirmPreference)findPreference("library_rescan_library")).setCallback(new ConfirmPreference.Callback() {
                @Override
                public void onConfirm() {
                    MusicLibrary.getInstance().postScanLibrary(true, null, null, null);
                }
            });

            ((ConfirmPreference)findPreference("library_delete_album_art_cache")).setCallback(new ConfirmPreference.Callback() {
                @Override
                public void onConfirm() {
                    MusicLibrary.getInstance().invalidateAlbumArtCache();
                }
            });*/
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            switch (requestCode) {
                case REQUEST_IMPORT_PLAYLIST: {
                    if (resultCode == RESULT_OK) {
                        MusicLibrary.getInstance().importPlaylist(data.getStringExtra(FilePickerActivity.KEY_FILEPATH));
                    }

                    break;
                }
            }
        }
    }

    public static class MiscFragment extends BaseFragment {

        private Preference.OnPreferenceChangeListener mHeadsetChange = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean)newValue)
                    HeadsetService.start(getActivity());

                PreferenceManager.getInstance().notifyPreferenceChange(preference.getKey(), newValue);

                return true;
            }
        };

        private static final int REQUEST_SELECT_DIRS = 1;
        private static final int REQUEST_RESTORE = 2;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_misc);

            bindHeadsetPreference(PreferenceManager.KEY_START_PLAYBACK_HEADSET);
            bindHeadsetPreference(PreferenceManager.KEY_START_PLAYBACK_BLUETOOTH);
            bindListPreference(PreferenceManager.KEY_PLAYLIST_ADD_DUPLICATE);

            bindPreference(PreferenceManager.KEY_FADE_ON_PLAY_PAUSE);
            bindPreference(PreferenceManager.KEY_LOWER_VOLUME_INCOMING_MESSAGE);

            findPreference("backup_data").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    BackupDialog.newInstance().show(getFragmentManager(), "backup_dialog");
                    return true;
                }
            });

            ((ConfirmPreference)findPreference("restore_data")).setCallback(new ConfirmPreference.Callback() {
                @Override
                public void onConfirm() {
                    // TODO: change restore dialog
                    //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    //intent.setType("application/octet-stream");
                    Intent intent = FilePickerActivity.importIntent(getActivity(), FilePickerActivity.TYPE_BACKUP);
                    startActivityForResult(intent, REQUEST_RESTORE);
                }
            });

            Preference writableDirs = findPreference("select_writable_dirs");
            if (Build.VERSION.SDK_INT >= 21) {
                writableDirs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, REQUEST_SELECT_DIRS);
                        return true;
                    }
                });
            }
            else {
                getPreferenceScreen().removePreference(writableDirs);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_SELECT_DIRS) {
                if (resultCode == RESULT_OK) {
                    Uri treeUri = data.getData();

                    StorageManager.getInstance().setSDCardRoot(treeUri);
                }
            }
            else if (requestCode == REQUEST_RESTORE) {
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    PreferenceManager.getInstance().restore(uri);
                }
            }
        }

        private void bindHeadsetPreference(String key) {
            Preference preference = findPreference(key);
            preference.setOnPreferenceChangeListener(mHeadsetChange);
        }
    }

    public static class InfoFragment extends BaseFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_info);
            //TODO: open source licenses dialog
            findPreference("open_source_licenses").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.pref_title_open_source_licenses);
                    builder.setPositiveButton(R.string.dialog_button_ok, null);

                    WebView web = new WebView(getActivity());
                    web.loadUrl("file:///android_res/raw/licenses.html");
                    web.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                            return true;
                        }

                        @TargetApi(Build.VERSION_CODES.N)
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                            startActivity(intent);
                            return true;
                        }
                    });
                    builder.setView(web);
                    builder.show();
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, SettingsActivity.class);
        context.startActivity(starter);
    }
}
