package com.example.musicplayer.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import com.example.musicplayer.ui.activities.BaseActivity;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.activities.MainActivity;

import java.text.MessageFormat;

/**
 * Created by Tarik on 04.04.2016.
 */
public class PlaybackService extends Service implements MediaPlayer.CompletionListener,
        MediaPlayer.ErrorListener, AudioManager.OnAudioFocusChangeListener {

    @Override
    public void onError(int errorCode, Object[] args) {
        mCurrentSongError = true;
        // TODO: smart handling of invalid files
        if (mLastSongError) {
            PlaybackState.getInstance().setPlayingState(false);
            mLastSongError = false;
        }
        else {
            Toast.makeText(this, MessageFormat.format(getString(R.string.error_play_file_skip), args[0]), Toast.LENGTH_SHORT).show();
            PlaybackState.getInstance().nextSong(true);
        }
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BaseActivity.ACTION_ACTIVITY_CHANGED: {
                    Class<?> topActivity = BaseActivity.getActivityOnTop();
                    if (topActivity == null)
                        topActivity = MainActivity.class;

                    Intent launchIntent = new Intent(context, topActivity);

                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    mNotificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, launchIntent, 0));
                    commitNotification();
                    break;
                }
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    mWasPlaying = false;
                    PlaybackState.getInstance().setPlayingState(false);
                    break;
                case ACTION_CLOSE:
                    close();
                    break;
            }
        }
    }

    public static final String ACTION_CLOSE = "com.example.musicplayer.ACTION_CLOSE";

    //private static final int[] THEME_SMALL_LAYOUTS = { R.layout.player_widget_small_light,
    //        R.layout.player_widget_small_dark };
    //private static final int[] THEME_LAYOUTS = { R.layout.player_widget_light, R.layout.player_widget_dark };

    public static final String CHANNEL_ID = "media_playback_channel";
    private static final String ACTION_START_SERVICE = "start_playback_service";

    private static final int MEDIA_BUTTON_DELAY = 250; // in milliseconds
    private static final float SLEEP_FADE_DURATION = 1.0f;
    private static final float SLEEP_FADE_VOLUME = 0.0f;
    private static final float FADE_DURATION = 1.0f;
    private static final float FADE_VOLUME = 0.25f;
    private static final int FOCUS_UNKNOWN = 0;
    private static final int NOTIFICATION_ID = 1;
    private static final String MEDIA_SESSION_TAG = "MediaSession";

    private static boolean sIsRunning = false;
    private static PlaybackService sInstance = null;


    private MediaPlayer mMediaPlayer;
    private boolean mHasFocus;
    private boolean mIsPlaying;
    private boolean mWasPlaying;
    private int mLastFocusState;

    private AudioFocusRequest mFocusRequest;

    private boolean mShowArtLockscreen;
    private Bitmap mEmptyBitmap;
    private MediaSessionCompat mMediaSession;
    private MediaMetadataCompat.Builder mMetadataBuilder;
    private NotificationCompat.Builder mNotificationBuilder;
    private Notification mNotification;

    private boolean mLowerVolumeIncMessage;
    private boolean mFadeOnPlayPause;
    private int mMediaButtonPresses;

    private boolean mLastSongError;
    private boolean mCurrentSongError;

    private Receiver mReceiver;

    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler;

    private PlaybackState.Observer mPlaybackStateObserver = new PlaybackState.Observer() {
        @Override
        public void update(Observable sender, PlaybackState.ObserverData data) {
            switch (data.type) {
                case SongChanged:
                    setSong(data.song, data.forceChange);
                    if (PlaybackState.getInstance().isPlaying())
                        play();
                    break;
                case CoverLoaded:
                    updateAlbumArt(data.bitmap);
                    commitNotification();
                    commitMediaSession();
                    break;
                case FilterStateChanged:
                    mMediaPlayer.setNextData(null, data.filterState, false);
                    break;
                case PlayingStateChanged:
                    if (data.isPlaying)
                        play();
                    else
                        pause();
                    break;
                case ForceStopPlayback:
                    PlaybackState.getInstance().setCachedPosition(getCurrentPosition());
                    stop();
                    break;
                case RequestCurrentPosition:
                    PlaybackState.getInstance().setCachedPosition(getCurrentPosition());
                    break;
                case RequestSeek:
                    seekTo(data.position);
                    break;
                default:
                    break;
            }
        }
    };

    private SleepTimer.Observer mSleepTimerObserver = new SleepTimer.Observer() {
        @Override
        public void update(Observable sender, SleepTimer.ObserverData data) {
            switch (data.type) {
                case Finished:
                    fadeClose();
                    break;
            }
        }
    };

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    switch (data.key) {
                        case PreferenceManager.KEY_LOCKSCREEN_SHOW_ART:
                            mShowArtLockscreen = (boolean)data.value;
                            updateAlbumArt(PlaybackState.getInstance().getCover());
                            commitMediaSession();
                            break;
                        case PreferenceManager.KEY_LOWER_VOLUME_INCOMING_MESSAGE:
                            mLowerVolumeIncMessage = (boolean)data.value;

                            if (!mLowerVolumeIncMessage && mLastFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
                                mMediaPlayer.fadeVolume(1.0f, FADE_DURATION);

                            break;
                        case PreferenceManager.KEY_FADE_ON_PLAY_PAUSE:
                            mFadeOnPlayPause = (boolean)data.value;

                            break;
                    }
                    break;
            }
        }
    };

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                if (mMediaButtonPresses < 3 && event.getAction() == KeyEvent.ACTION_DOWN) {
                    mMediaButtonPresses++;

                    /*if (!mHeadsetMultiplePressEnabled) {
                        handleMediaButton();
                        return true;
                    }*/

                    mHandler.removeCallbacks(mMediaButtonCallback);
                    mHandler.postDelayed(mMediaButtonCallback, MEDIA_BUTTON_DELAY);
                }

                return true;
            }
            else
                return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onSkipToNext() {
            PlaybackState.getInstance().nextSong(true);
        }

        @Override
        public void onSkipToPrevious() {
            PlaybackState.getInstance().previousSong();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            PlaybackState.getInstance().setPlayingState(true);
        }

        @Override
        public void onPause() {
            PlaybackState.getInstance().setPlayingState(false);
        }

        @Override
        public void onStop() {
            PlaybackState.getInstance().setPlayingState(false);
        }
    };

    private Runnable mMediaButtonCallback = new Runnable() {
        @Override
        public void run() {

            //Log.d("PlaybackService", "run: long press = " + mMediaButtonLongPress);

            handleMediaButton();
        }
    };

    public PlaybackService() {
        mMediaPlayer = null;
        mHasFocus = false;
        mIsPlaying = false;
        mWasPlaying = false;
        mLastFocusState = FOCUS_UNKNOWN;
        mEmptyBitmap = null;
        mMediaSession = null;
        mMetadataBuilder = null;
        mNotificationBuilder = null;
        mNotification = null;
        mMediaButtonPresses = 0;
        mReceiver = null;
        mHandler = new Handler();
        mLastSongError = false;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static void createChannel(Service service) {
        NotificationManager manager = (NotificationManager)service.getSystemService(NOTIFICATION_SERVICE);

        String name = service.getString(R.string.playback_channel_name);
        //String desc = getString(R.string.playback_channel_desc);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        //channel.setDescription(desc);
        channel.setBypassDnd(true);
        channel.setImportance(importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onCreate() {
        PlaybackState state = PlaybackState.getInstance();

        if (!state.isInited()) {
            stopSelf();
            return;
        }

        PreferenceManager prefs = PreferenceManager.getInstance();

        sIsRunning = true;
        sInstance = this;

        state.addObserver(mPlaybackStateObserver);
        state.getSleepTimer().addObserver(mSleepTimerObserver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(this);

        requestFocus();

        PowerManager manager = (PowerManager)getSystemService(POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeLock");
        mWakeLock.setReferenceCounted(false);

        prefs.addObserver(mPreferenceObserver);
        mShowArtLockscreen = prefs.getBoolean(PreferenceManager.KEY_LOCKSCREEN_SHOW_ART, true);
        mLowerVolumeIncMessage = prefs.getBoolean(PreferenceManager.KEY_LOWER_VOLUME_INCOMING_MESSAGE);
        mFadeOnPlayPause = prefs.getBoolean(PreferenceManager.KEY_FADE_ON_PLAY_PAUSE);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setPackage(getPackageName());
        PendingIntent mediaButtonPending = PendingIntent.getBroadcast(this,
                0, mediaButtonIntent, 0);

        mEmptyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.standard_cover);
        mMediaSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG,
                new ComponentName(this, Receiver.class), mediaButtonPending);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(mMediaSessionCallback);
        mMetadataBuilder = new MediaMetadataCompat.Builder();

        //initNotificationViews(Integer.parseInt(prefs.getString(PreferenceManager.KEY_NOTIFICATION_THEME, "0")));

        mReceiver = new Receiver();
        IntentFilter receiverFilter = new IntentFilter();
        receiverFilter.addAction(BaseActivity.ACTION_ACTIVITY_CHANGED);
        receiverFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        //receiverFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        receiverFilter.addAction(ACTION_CLOSE);
        registerReceiver(mReceiver, receiverFilter);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.initialize(this);
        mMediaPlayer.setCompletionListener(this);
        mMediaPlayer.setErrorListener(this);

        if (state.getCurrentSong() != null)
            setSong(state.getCurrentSong(), false);
        if (state.isCoverCached())
            updateAlbumArt(state.getCover());

        seekTo(state.getCachedPosition());
        commitMediaSession();
        commitNotification();

        mMediaSession.setActive(true);
    }

    private void fadeClose() {
        mMediaPlayer.fadeVolume(SLEEP_FADE_VOLUME, SLEEP_FADE_DURATION);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, (long)SLEEP_FADE_DURATION * 1000);
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(this);

        Class<?> topActivity = BaseActivity.getActivityOnTop();
        if (topActivity == null)
            topActivity = MainActivity.class;
        Intent launchIntent = new Intent(this, topActivity);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPending = PendingIntent.getActivity(this,
                0, launchIntent, 0);

        PendingIntent closePending = PendingIntent.getBroadcast(this,
                0, new Intent(ACTION_CLOSE), 0);

        MediaStyle style = new MediaStyle()
                .setMediaSession(mMediaSession.getSessionToken())
                .setShowCancelButton(true)
                .setCancelButtonIntent(closePending)
                .setShowActionsInCompactView(0, 1, 2);

        int playPauseIcon = isPlaying() ? R.drawable.pause_button : R.drawable.play_button;
        String playPauseLabel = isPlaying() ? "Pause" : "Play"; // TODO: resource strings

        NotificationCompat.Action prevAction = new NotificationCompat.Action(R.drawable.previous_button,
                "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(playPauseIcon,
                playPauseLabel, MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        NotificationCompat.Action nextAction = new NotificationCompat.Action(R.drawable.next_button,
                "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        Bitmap cover = PlaybackState.getInstance().getCover();
        if (cover == null)
            cover = mEmptyBitmap;

        Song currentSong = PlaybackState.getInstance().getCurrentSong();

        String title = null;
        String artist = null;

        if (currentSong != null) {
            title = Song.getTitle(currentSong);
            artist = Song.getArtistText(currentSong);
        }

        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setDeleteIntent(closePending)
                .setSmallIcon(R.drawable.music_node)
                .setLargeIcon(cover)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(contentPending)
                .setStyle(style);
        mNotification = mNotificationBuilder.build();
        return mNotification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START_SERVICE.equals(intent.getAction())) {
            PlaybackState state = PlaybackState.getInstance();
            if (state.isInited())
                state.setPlayingState(true);
        }
        else {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onComplete() {
        PlaybackState state = PlaybackState.getInstance();
        MusicLibrary.getInstance().updateSongPlayed(state.getCurrentSong().getId());

        if (!state.nextSong(false)) {
            // no next song available
            close();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        PlaybackState state = PlaybackState.getInstance();

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mHasFocus = true;

                switch (mLastFocusState) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        mMediaSession.setActive(true);
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        state.setPlayingState(mWasPlaying);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if (mLowerVolumeIncMessage && mWasPlaying)
                            mMediaPlayer.fadeVolume(1.0f, FADE_DURATION);
                        break;
                }

                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mMediaSession.setActive(false);
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mHasFocus = false;
                mWasPlaying = state.isPlaying();
                state.setPlayingState(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mWasPlaying = state.isPlaying();

                if (mLowerVolumeIncMessage && mWasPlaying)
                    mMediaPlayer.fadeVolume(FADE_VOLUME, FADE_DURATION);

                break;
        }

        mLastFocusState = focusChange;
    }

    public void setSong(Song song, boolean forceChange) {
        mLastSongError = mCurrentSongError;
        mCurrentSongError = false;
        updateData(song);

        PlaybackState state = PlaybackState.getInstance();
        mMediaPlayer.setNextData(song.getInfo().getFilePath(), state.getFilterState(), forceChange);

        long position = state.getCachedPosition();
        if (position > 0)
            seekTo(position);
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void play() {
        mWakeLock.acquire();
        mMediaPlayer.play(mFadeOnPlayPause);
        mIsPlaying = true;

        requestFocus();
        updateState(-1);
        commitNotification();
    }

    private void requestFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC).build();
            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(this).build();
            mHasFocus = audioManager.requestAudioFocus(mFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            mHasFocus = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    public void pause() {
        PlaybackState.getInstance().setCachedPosition(mMediaPlayer.getCurrentPosition());
        mMediaPlayer.pause(mFadeOnPlayPause);
        mIsPlaying = false;
        updateState(-1);
        commitNotification();
        mWakeLock.release();
    }

    public void stop() {
        mMediaPlayer.stop();
    }

    public long getCurrentPosition() {
        if (mMediaPlayer == null)
            return 0;

        return mMediaPlayer.getCurrentPosition();
    }

    public void seekTo(long position) {
        if (mMediaPlayer == null)
            return;

        mMediaPlayer.seek(position);
        updateState(position);
    }

    public void close() {
        PlaybackState state = PlaybackState.getInstance();

        if (state.isInited()) {
            PreferenceManager prefs = PreferenceManager.getInstance();

            state.setCachedPosition(mMediaPlayer.getCurrentPosition());
            state.save();
            state.setPlayingState(false);

            mMediaPlayer.stop();
            mMediaPlayer.release();

            mMediaSession.setActive(false);
            mMediaSession.release();

            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mFocusRequest != null)
                    audioManager.abandonAudioFocusRequest(mFocusRequest);
            }
            else
                audioManager.abandonAudioFocus(this);

            unregisterReceiver(mReceiver);

            prefs.removeObserver(mPreferenceObserver);
            state.removeObserver(mPlaybackStateObserver);
            state.getSleepTimer().removeObserver(mSleepTimerObserver);
        }

        sInstance = null;
        sIsRunning = false;

        stopForeground(true);
        stopSelf();

        if (mEmptyBitmap != null)
            mEmptyBitmap.recycle();
    }


    private void updateState(long position) {
        if (position == -1)
            position = getCurrentPosition();

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_STOP |
                            PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState((isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED),
                        position, 1.0f).build();
        mMediaSession.setPlaybackState(playbackState);
    }

    private void updateAlbumArt(Bitmap bitmap) {
        Bitmap temp = null;

        if (bitmap != null && mShowArtLockscreen)
            temp = bitmap.copy(bitmap.getConfig(), false);

        mMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, temp);
    }

    private void updateData(Song song) {
        String titleText = Song.getTitle(song);
        String artistText = Song.getArtistText(song);

        mMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, titleText);
        mMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistText);
        mMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artistText);
        mMetadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getInfo().getDuration());
    }

    private void commitNotification() {
        if (mIsPlaying)
            startForeground(NOTIFICATION_ID, createNotification());
        else {
            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, createNotification());
            stopForeground(false);
        }
    }

    private void commitMediaSession() {
        MediaMetadataCompat metadata = mMetadataBuilder.build();

        mMediaSession.setMetadata(metadata);
    }

    private void handleMediaButton() {
        switch (mMediaButtonPresses) {
            case 1:
                PlaybackState.getInstance().togglePlayingState();
                break;
            case 2:
                PlaybackState.getInstance().nextSong(true);
                break;
            case 3:
                PlaybackState.getInstance().previousSong();
                break;
        }

        mMediaButtonPresses = 0;
    }

    public static void start(Context context) {
        MusicLibrary.getInstance().initialize(context);
        PlaybackState.getInstance().initialize(context);

        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_START_SERVICE);
        ContextCompat.startForegroundService(context, intent);
    }

    public static PlaybackService getInstance() {
        return sInstance;
    }

    public static boolean isRunning() {
        return sIsRunning;
    }

    private static boolean isLargeViewSupported() {
        return Build.VERSION.SDK_INT >= 16;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!PlaybackState.getInstance().isPlaying()) {
            close();
            super.onTaskRemoved(rootIntent);
            System.exit(0);
        }
        else
            super.onTaskRemoved(rootIntent);
    }
}
