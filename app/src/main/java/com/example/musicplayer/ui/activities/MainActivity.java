package com.example.musicplayer.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.CacheManager;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.LibraryBarCreateCallback;
import com.example.musicplayer.LibraryDelegate;
import com.example.musicplayer.MemoryManager;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowAlbumRequest;
import com.example.musicplayer.request.ShowArtistRequest;
import com.example.musicplayer.request.ShowFolderRequest;
import com.example.musicplayer.request.ShowGenreRequest;
import com.example.musicplayer.request.ShowPlaylistRequest;
import com.example.musicplayer.ui.AlbumArtCache;
import com.example.musicplayer.ui.DragLayout;
import com.example.musicplayer.ui.dialogs.OnDialogDismissListener;
import com.example.musicplayer.ui.dialogs.SleepTimerDialog;
import com.example.musicplayer.ui.dialogs.UnavailableStorageDialog;
import com.example.musicplayer.ui.fragments.library.AlbumFragment;
import com.example.musicplayer.ui.fragments.library.ArtistFragment;
import com.example.musicplayer.ui.fragments.library.FolderFragment;
import com.example.musicplayer.ui.fragments.FragmentCreateListener;
import com.example.musicplayer.ui.fragments.library.GenreFragment;
import com.example.musicplayer.ui.fragments.library.LibraryFragment;
import com.example.musicplayer.ui.fragments.MainFragment;
import com.example.musicplayer.ui.fragments.MenuFragment;
import com.example.musicplayer.ui.fragments.PaneFragment;
import com.example.musicplayer.ui.fragments.PlayerFragment;
import com.example.musicplayer.ui.fragments.library.PlaylistFragment;
import com.example.musicplayer.ui.fragments.TwoPaneLayout;

public class MainActivity extends BaseActivity implements DragLayout.Callback,
        TwoPaneLayout.Callback, RequestManager.Receiver, LibraryBarCreateCallback,
        LibraryBar.Callback, FragmentCreateListener, OnDialogDismissListener {

    // TODO: library mode and folder mode
    // TODO: open layout request

    public static final String EXTRA_OPEN_LAYOUT = "open_layout";

    private static final String TAG_FRAGMENT = "player_fragment";
    private static final String KEY_PANE_LAYOUT = "pane_layout";

    private static final int REQUEST_INTRO = 1234;

    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL = 1;

    private View mSongView; // layout for song title and artist
    private TextView mSongTitleText;
    private TextView mSongArtistText;
    private TextView mTitleView; // library title

    private CharSequence mTitleText;

    private PlayerFragment mPlayerFragment;
    private PaneFragment mActiveFragment;

    private DragLayout mDragLayout;
    private TwoPaneLayout mPaneLayout;

    private int mSideBarGravity;

    private boolean mIsTablet;
    private boolean mIsLandscape;
    private boolean mIsRunning;
    private RequestManager.Request mTempRequest;
    private Handler mHandler = new Handler();
    private boolean mOpenLayoutRequest;

    private Runnable mOpenLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            mDragLayout.open();
        }
    };

    private Runnable mRequestRunnable = new Runnable() {
        @Override
        public void run() {
            handleRequest(mTempRequest);
            mTempRequest = null;
        }
    };

    private MusicLibrary.Observer mLibraryObserver = new MusicLibrary.Observer() {
        @Override
        public void update(Observable sender, MusicLibrary.ObserverData data) {
            switch (data.type) {
                case ScanStateChanged:
                    if (data.scanState)
                        mDragLayout.showProgressBar();
                    else
                        mDragLayout.dismissProgressBar();

                    break;
            }
        }
    };

    private PlaybackState.Observer mPlaybackStateObserver = new PlaybackState.Observer() {
        @Override
        public void update(Observable sender, PlaybackState.ObserverData data) {
            switch (data.type) {
                case SongChanged:
                    setSong(data.song);
                    break;
            }
        }
    };

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (PreferenceManager.KEY_PLAYER_ALBUM_COVER_SIZE.equals(data.key) ||
                            (PreferenceManager.KEY_SIDEBAR_PORTRAIT_POS.equals(data.key) && !isLandscape()))
                        recreate();
                    else if (PreferenceManager.KEY_START_PAGE_LAYOUT.equals(data.key))
                        mPaneLayout.goToStart();
                    break;
            }
        }
    };

    private LibraryDelegate mLibraryDelegate = new LibraryDelegate() {
        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            return startSupportActionMode(callback);
        }

        @Override
        public void setHighlightedItem(int position) {
            mPlayerFragment.setHighlightedBarItem(position);
        }

        @Override
        public void invalidateTitle() {
            MainActivity.this.invalidateTitle();
        }

        @Override
        public void invalidateBarMenu() {
            mPlayerFragment.invalidateBarMenu();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mOpenLayoutRequest = savedInstanceState.getBoolean(EXTRA_OPEN_LAYOUT, false);
        else
            mOpenLayoutRequest = getIntent().getBooleanExtra(EXTRA_OPEN_LAYOUT, false);

        loadPrefs();

        initSystems();
        initActionBar();
        initViews(savedInstanceState);

        PreferenceManager.getInstance().addObserver(mPreferenceObserver);
        RequestManager.getInstance().registerReceiver(this);

        if (savedInstanceState == null) {
            if (CacheManager.getInstance().getBoolean(IntroActivity.CACHE_INTRO_DONE, false)) {
                if (checkPermissions()) {
                    checkStorages();
                }
            }
            else {
                if (isTablet())
                    IntroTabletActivity.startResult(this, REQUEST_INTRO);
                else
                    IntroActivity.startResult(this, REQUEST_INTRO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_INTRO) {
            if (resultCode == RESULT_OK) {
                if (checkPermissions()) {
                    checkStorages();
                }
            }
            else {
                // intro was not done
                finish();
            }
        }
    }

    private void checkStorages() {
        StorageManager.getInstance().initialize(this);
        String[] storages = StorageManager.getInstance().getUnavailableStorages();
        if (!StorageManager.getInstance().unavailablesChecked()) {
            if (storages != null) {
                UnavailableStorageDialog.newInstance(storages).show(getSupportFragmentManager(), "unavailable_storages");
                return;
            }
            else {
                StorageManager.getInstance().updateUnavailables(null, null);
            }
        }

        MusicLibrary.getInstance().postScanLibrary(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
        RequestManager.getInstance().unregisterReceiver(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPaneLayout != null)
            outState.putParcelable(KEY_PANE_LAYOUT, mPaneLayout.saveState());

        outState.putBoolean(EXTRA_OPEN_LAYOUT, mOpenLayoutRequest);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mOpenLayoutRequest = intent.getBooleanExtra(EXTRA_OPEN_LAYOUT, false);
        if (mIsRunning)
            handleOpenLayoutRequest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkStorages();
                }
                else {
                    Toast.makeText(this, R.string.toast_error_access_not_granted, Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_base, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_settings: {
                SettingsActivity.start(this);
                return true;
            }
            case R.id.option_sleep_timer: {
                SleepTimerDialog.newInstance().show(getSupportFragmentManager(), "sleep_timer_dialog");
                return true;
            }
            case android.R.id.home:
                if (mPaneLayout.backAvailable()) {
                    mPaneLayout.goBack();
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mTempRequest != null) {
            mHandler.post(mRequestRunnable);
            closeDragLayout();
        }

        if (MusicLibrary.getInstance().isScanning())
            mDragLayout.showProgressBar();
        else
            mDragLayout.dismissProgressBar();

        setSong(PlaybackState.getInstance().getCurrentSong());

        registerObservers();

        handleOpenLayoutRequest();

        mIsRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        mIsRunning = false;

        unregisterObservers();
    }

    @Override
    public void onBackPressed() {
        if (mDragLayout.isOpen()) {
            mDragLayout.close();
        }
        else if (mPaneLayout.backAvailable()) {
            mPaneLayout.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            MemoryManager.getInstance().trimMemory();
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);

        boolean showAlways = showSongViewAlways();

        mTitleText = title;

        if (mTitleView != null)
            mTitleView.setText(title);

        if (showAlways != showSongViewAlways())
            updateSongViewVisible();
    }

    /**
     * DragLayout callbacks
     */
    @Override
    public void onDragStarted() {
        if (!isTablet()) {
            float playerOpacity = mDragLayout.isOpen() ? 1.0f : 0.0f;
            float contentOpacity = 1.0f - playerOpacity;

            if (!showSongViewAlways()) {
                mSongView.setVisibility(View.VISIBLE);
                mTitleView.setVisibility(View.VISIBLE);

                mSongView.setAlpha(playerOpacity);
                mTitleView.setAlpha(contentOpacity);
            }

            LibraryBar bar = mPlayerFragment.getLibraryBar();
            if (bar != null) {
                bar.setVisibility(View.VISIBLE);
                bar.setAlpha(contentOpacity);
            }
        }

        mPlayerFragment.setPlayTimeAllowed(false);
    }

    @Override
    public void onDragEnded() {
        if (mDragLayout.isOpen()) {
            mPlayerFragment.updatePlayTime(PlaybackState.getInstance().requestCurrentPosition());
            mPlayerFragment.setPlayTimeAllowed(true);
        }

        if (!isTablet()) {
            int playerVisibility;
            int contentVisibility;

            if (mDragLayout.isOpen()) {
                playerVisibility = View.VISIBLE;
                contentVisibility = View.INVISIBLE;
            }
            else {
                playerVisibility = View.INVISIBLE;
                contentVisibility = View.VISIBLE;
            }

            if (!showSongViewAlways()) {
                mSongView.setVisibility(playerVisibility);
                mTitleView.setVisibility(contentVisibility);
            }

            LibraryBar bar = mPlayerFragment.getLibraryBar();
            if (bar != null) {
                bar.setVisibility(contentVisibility);
            }
        }
    }

    @Override
    public void onDragPositionChanged(float relX) {
        if (!isTablet()) {
            float playerOpacity = 1.0f - relX;

            if (!showSongViewAlways()) {
                mSongView.setAlpha(playerOpacity);
                mTitleView.setAlpha(relX);
            }

            LibraryBar bar = mPlayerFragment.getLibraryBar();
            if (bar != null) {
                bar.setAlpha(relX);
            }
        }
    }

    @Override
    public void onDragLayoutToggled(boolean isOpen) {
        boolean visibility = showSongViewAlways() || isOpen;
        mPlayerFragment.setMenuVisibility(visibility);
        mActiveFragment.setMenuVisibility(!visibility);
    }

    /**
     * TwoPaneLayout callbacks
     */
    @Override
    public void onActiveFragmentChanged(PaneFragment fragment) {
        mActiveFragment = fragment;

        mPlayerFragment.setHighlightedBarItem(-1);
        mPlayerFragment.invalidateBarMenu();
        if (fragment instanceof LibraryFragment && ((LibraryFragment)fragment).hasBarMenu()) {
            mPlayerFragment.setHighlightedBarItem(((LibraryFragment)fragment).getHighlightedBarItem());
        }

        setTitle(fragment.getTitle(getResources()));
        setIcon(fragment.getTitleIcon());

        showBackButton(mPaneLayout.backAvailable());
    }

    /**
     * LibraryBarCreate callbacks
     */
    @Override
    public boolean hasBarMenu() {
        if (mActiveFragment instanceof LibraryBarCreateCallback) {
            return ((LibraryBarCreateCallback)mActiveFragment).hasBarMenu();
        }

        return false;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        if (mActiveFragment instanceof LibraryBarCreateCallback) {
            ((LibraryBarCreateCallback)mActiveFragment).onCreateBarMenu(inflater, menu);
        }
    }

    /**
     * LibraryBar callbacks
     */
    @Override
    public void onBarItemClick(LibraryBar.Item item) {
        if (mActiveFragment instanceof LibraryBar.Callback) {
            ((LibraryBar.Callback)mActiveFragment).onBarItemClick(item);
        }
    }

    @Override
    public boolean onBarItemLongClick(LibraryBar.Item item) {
        if (mActiveFragment instanceof LibraryBar.Callback) {
            return ((LibraryBar.Callback)mActiveFragment).onBarItemLongClick(item);
        }

        return false;
    }

    /**
     * RequestManager callbacks
     */
    @Override
    public boolean onReceiveRequest(RequestManager.Request request) {
        int type = request.getType();

        if (type == ShowFolderRequest.TYPE ||
                type == ShowArtistRequest.TYPE ||
                type == ShowAlbumRequest.TYPE ||
                type == ShowGenreRequest.TYPE ||
                type == ShowPlaylistRequest.TYPE) {

            if (!mIsRunning) {
                mTempRequest = request;
                return true;
            }
            else {
                return handleRequest(request);
            }
        }
        else if (request.getType() == RequestManager.OpenPlayerRequest.TYPE) {
            mDragLayout.open();
            return true;
        }

        return false;
    }

    @Override
    public void onCreateFragment(Class<?> fragmentClass, Fragment fragment) {
        if (fragment instanceof LibraryFragment)
            ((LibraryFragment)fragment).setDelegate(mLibraryDelegate);
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof UnavailableStorageDialog) {
            UnavailableStorageDialog d = (UnavailableStorageDialog)dialog;
            StorageManager.getInstance().updateUnavailables(d.getUnavailables(), d.getResult());
            MusicLibrary.getInstance().postScanLibrary(false);
        }
    }

    /**
     * internal functions
     */
    private void initSystems() {
        MusicLibrary.getInstance().initialize(this);
        PlaybackState.getInstance().initialize(this);
        AlbumArtCache.getInstance().initialize(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            View view = getLayoutInflater().inflate(R.layout.actionbar_layout, null);
            mSongView = view.findViewById(R.id.layout_song_view);
            mSongTitleText = (TextView) view.findViewById(R.id.text_title);
            mSongArtistText = (TextView) view.findViewById(R.id.text_artist);
            mTitleView = (TextView) view.findViewById(R.id.text_library_title);

            mTitleView.setText(mTitleText);

            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(view);
            actionBar.setDisplayShowCustomEnabled(true);
        }
    }

    private void initViews(Bundle savedInstanceState) {
        mPaneLayout = new TwoPaneLayout(this);
        mPaneLayout.setSinglePane(!isTablet());
        mPaneLayout.setCallback(this);
        mPaneLayout.initialize(getSupportFragmentManager());

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);

        mDragLayout = new DragLayout(this);
        mDragLayout.setGravity(mSideBarGravity);
        int offsetRes = isTablet() ? R.dimen.drag_view_offset_tablet : R.dimen.drag_view_offset;
        mDragLayout.setOffsetResource(offsetRes);
        mDragLayout.setCallback(this);
        mDragLayout.setContentView(mPaneLayout);
        mDragLayout.setDragView(container);

        if (savedInstanceState == null) {
            mPlayerFragment = PlayerFragment.newInstance(isTablet(), mSideBarGravity);
            getSupportFragmentManager().beginTransaction().add(R.id.container, mPlayerFragment, TAG_FRAGMENT).commit();

            PaneFragment fragment = isTablet() ? new MenuFragment() : new MainFragment();
            mPaneLayout.pushFragment(fragment);
        }
        else {
            mPlayerFragment = (PlayerFragment)getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
            mPlayerFragment.setGravity(mSideBarGravity);

            mPaneLayout.restoreState(savedInstanceState.getParcelable(KEY_PANE_LAYOUT));
            mActiveFragment = mPaneLayout.getActiveFragment();
        }

        setContentView(mDragLayout);

        mDragLayout.initialize();
    }

    private void loadPrefs() {
        PreferenceManager prefs = PreferenceManager.getInstance();

        mIsTablet = getResources().getBoolean(R.bool.isTablet);
        mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        mSideBarGravity = DragLayout.GRAVITY_BOTTOM;
        if (!isTablet() && !isLandscape())
            mSideBarGravity = prefs.parseInt(PreferenceManager.KEY_SIDEBAR_PORTRAIT_POS);
    }

    private void handleOpenLayoutRequest() {
        if (mOpenLayoutRequest) {
            mOpenLayoutRequest = false;
            mHandler.postDelayed(mOpenLayoutRunnable, 500);
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_WRITE_EXTERNAL);
            }

            return false;
        }

        return true;
    }

    private void registerObservers() {
        MusicLibrary.getInstance().addObserver(mLibraryObserver);
        PlaybackState.getInstance().addObserver(mPlaybackStateObserver);
    }

    private void unregisterObservers() {
        MusicLibrary.getInstance().removeObserver(mLibraryObserver);
        PlaybackState.getInstance().removeObserver(mPlaybackStateObserver);
    }

    private boolean isTablet() {
        return mIsTablet;
    }

    private boolean isLandscape() {
        return mIsLandscape;
    }

    private boolean showSongViewAlways() {
        return mTitleText == null;
    }

    private void updateSongViewVisible() {
        boolean songVisible = showSongViewAlways() || mDragLayout.isOpen();

        int songVisibility;
        int titleVisibility;
        float songAlpha;
        float titleAlpha;

        if (songVisible) {
            songVisibility = View.VISIBLE;
            titleVisibility = View.INVISIBLE;
            songAlpha = 1.0f;
            titleAlpha = 0.0f;
        }
        else {
            songVisibility = View.INVISIBLE;
            titleVisibility = View.VISIBLE;
            songAlpha = 0.0f;
            titleAlpha = 1.0f;
        }

        mSongView.setVisibility(songVisibility);
        mTitleView.setVisibility(titleVisibility);
        mSongView.setAlpha(songAlpha);
        mTitleView.setAlpha(titleAlpha);
        mPlayerFragment.setMenuVisibility(songVisible);
    }

    private void setSong(Song song) {
        if (song != null) {
            mSongTitleText.setText(Song.getTitle(song));
            mSongArtistText.setText(Song.getArtistText(song));
        }
    }

    private void setIcon(@DrawableRes int icon) {
        Drawable drawable = null;
        if (icon != 0) {
            drawable = ResourcesCompat.getDrawable(getResources(), icon, null);
            drawable.setColorFilter(Util.getAttrColor(this, R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);

            int size = getResources().getDimensionPixelSize(R.dimen.icon_size);
            drawable.setBounds(0, 0, size, size);
        }

        mTitleView.setCompoundDrawables(drawable, null, null, null);
    }

    private void invalidateTitle() {
        if (mActiveFragment != null) {
            setTitle(mActiveFragment.getTitle(getResources()));
        }
    }

    private void closeDragLayout() {
        if (mIsRunning)
            mDragLayout.close();
        else
            mDragLayout.closeForced();
    }

    private boolean handleRequest(RequestManager.Request request) {
        int type = request.getType();

        if (type == ShowFolderRequest.TYPE) {
            ShowFolderRequest folderRequest = (ShowFolderRequest)request;

            closeDragLayout();
            mPlayerFragment.dismissQueue();

            TwoPaneLayout.Entry nextEntry = mPaneLayout.findNextEntry(request.getSenderFragment());
            if (nextEntry != null && nextEntry.getFragment().getType() == FolderFragment.TYPE) {
                ((FolderFragment)nextEntry.getFragment()).setLibraryObject(LibraryObject.FOLDER, folderRequest.getFolderId());
                invalidateTitle();
            }
            else {
                FolderFragment fragment = new FolderFragment();
                fragment.setLibraryObject(LibraryObject.FOLDER, folderRequest.getFolderId());
                if (nextEntry != null)
                    nextEntry.setFragment(fragment);
                else
                    mPaneLayout.pushFragment(fragment);
            }

            return true;
        }
        else if (type == ShowGenreRequest.TYPE) {
            ShowGenreRequest genreRequest = (ShowGenreRequest)request;

            closeDragLayout();
            mPlayerFragment.dismissQueue();

            TwoPaneLayout.Entry nextEntry = mPaneLayout.findNextEntry(request.getSenderFragment());
            if (nextEntry != null && nextEntry.getFragment().getType() == GenreFragment.TYPE) {
                ((GenreFragment)nextEntry.getFragment()).setLibraryObject(LibraryObject.GENRE, genreRequest.getGenreId());
                invalidateTitle();
            }
            else {
                GenreFragment fragment = new GenreFragment();
                fragment.setLibraryObject(LibraryObject.GENRE, genreRequest.getGenreId());
                if (nextEntry != null)
                    nextEntry.setFragment(fragment);
                else
                    mPaneLayout.pushFragment(fragment);
            }

            return true;
        }
        else if (type == ShowArtistRequest.TYPE) {
            ShowArtistRequest artistRequest = (ShowArtistRequest)request;

            closeDragLayout();
            mPlayerFragment.dismissQueue();

            TwoPaneLayout.Entry nextEntry = mPaneLayout.findNextEntry(request.getSenderFragment());
            if (nextEntry != null && nextEntry.getFragment().getType() == ArtistFragment.TYPE) {
                ((ArtistFragment)nextEntry.getFragment()).setLibraryObject(LibraryObject.ARTIST, artistRequest.getArtistId());
                invalidateTitle();
            }
            else {
                ArtistFragment fragment = new ArtistFragment();
                fragment.setLibraryObject(LibraryObject.ARTIST, artistRequest.getArtistId());
                if (nextEntry != null)
                    nextEntry.setFragment(fragment);
                else
                    mPaneLayout.pushFragment(fragment);
            }

            return true;
        }
        else if (type == ShowAlbumRequest.TYPE) {
            ShowAlbumRequest albumRequest = (ShowAlbumRequest)request;

            closeDragLayout();
            mPlayerFragment.dismissQueue();

            TwoPaneLayout.Entry nextEntry = mPaneLayout.findNextEntry(request.getSenderFragment());
            if (nextEntry != null && nextEntry.getFragment().getType() == AlbumFragment.TYPE) {
                ((AlbumFragment)nextEntry.getFragment()).setLibraryObject(LibraryObject.ALBUM, albumRequest.getAlbumId());
                invalidateTitle();
            }
            else {
                AlbumFragment fragment = new AlbumFragment();
                fragment.setLibraryObject(LibraryObject.ALBUM, albumRequest.getAlbumId());
                if (nextEntry != null)
                    nextEntry.setFragment(fragment);
                else
                    mPaneLayout.pushFragment(fragment);
            }

            return true;
        }
        else if (type == ShowPlaylistRequest.TYPE) {
            ShowPlaylistRequest playlistRequest = (ShowPlaylistRequest)request;

            closeDragLayout();
            mPlayerFragment.dismissQueue();

            TwoPaneLayout.Entry nextEntry = mPaneLayout.findNextEntry(request.getSenderFragment());
            if (nextEntry != null && nextEntry.getFragment().getType() == PlaylistFragment.TYPE) {
                ((PlaylistFragment)nextEntry.getFragment()).setLibraryObject(LibraryObject.PLAYLIST, playlistRequest.getPlaylistId());
                invalidateTitle();
            }
            else {
                PlaylistFragment fragment = new PlaylistFragment();
                fragment.setLibraryObject(LibraryObject.PLAYLIST, playlistRequest.getPlaylistId());
                if (nextEntry != null)
                    nextEntry.setFragment(fragment);
                else
                    mPaneLayout.pushFragment(fragment);
            }

            return true;
        }

        return false;
    }
}
