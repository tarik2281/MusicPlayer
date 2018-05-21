package com.example.musicplayer.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicplayer.playback.SleepTimer;
import com.example.musicplayer.TimeUtil;
import com.example.musicplayer.ui.ColorTransitionDrawable;
import com.example.musicplayer.ui.CoverView;
import com.example.musicplayer.ui.DragLayout;
import com.example.musicplayer.ui.activities.EqualizerActivity;
import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.LibraryBarCreateCallback;
import com.example.musicplayer.Observable;
import com.example.musicplayer.playback.PlaybackService;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.ui.activities.QueueActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.fragments.options.SongOptionsHandler;

import java.text.MessageFormat;

/**
 * Created by Tarik on 07.06.2016.
 */
public class PlayerFragment extends Fragment implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, LibraryBar.Callback,
        PopupMenu.OnMenuItemClickListener {

    private static final String KEY_LIBBAR_GRAVITY = "LIBBAR_GRAVITY";
    private static final String TABLET_KEY = "tablet";

    private static final String TAG_HANDLER = "HANDLER";

    private static final double SEEK_BAR_SCALING = 100.0;
    private static final double UPDATE_FREQUENCY = SEEK_BAR_SCALING / 2.0;
    private static final int NUM_BITMAPS = 2;

    private static final int COVER_SIZE_FULL = 0;
    private static final int COVER_SIZE_SMALL = 1;

    private Handler mHandler;
    //private PlaybackService.Binding mBinding;

    private boolean mIsTablet;

    private boolean mShowSmallCover;
    private ColorTransitionDrawable mColorDrawable;
    private CoverView mCoverView;
    private CoverView mCoverViewSmall;

    private TextView mTitleText;
    private TextView mArtistText;

    private ImageButton mQueueButton;
    private ImageButton mNextButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mPreviousButton;
    private ImageButton mShuffleButton;
    private ImageButton mRepeatButton;
    private ImageButton mOptionsButton;

    private ImageButton mPlayPauseButtonSmall;
    private ImageButton mPreviousButtonSmall;
    private ImageButton mNextButtonSmall;

    private SongOptionsHandler mOptionsHandler;

    private View mSongView;

    private LibraryBar mLibraryBar;
    private LibraryBar.Item mBarPlayButton;

    private SeekBar mPlayTimeBar;
    private boolean mSeeking;

    private long mSongDuration;
    private boolean mShowTimeRemaining;
    private TextView mPositionText;
    private TextView mDurationText;

    private Button mTimerButton;

    private boolean mPlayTimeAllowed;

    private int mHighlightedPosition = -1;

    //private QueueFragment mQueueFragment;

    private static Bitmap[] sBitmaps;
    private static Canvas[] sCanvases;
    private static Rect sBitmapRect;
    private static int sCurrentBitmap;
    private static Bitmap sEmptyBitmap;
    private static long sCurrentSongId = -1;

    private Runnable mUpdateTimeCallback = new Runnable() {
        @Override
        public void run() {
            updatePlayTime(PlaybackState.getInstance().requestCurrentPosition());
            mHandler.postDelayed(this, (long)UPDATE_FREQUENCY);
            //if (mBinding.isBound()) {
            //    PlaybackService service = mBinding.getService();
            //    updatePlayTime(service.getCurrentPosition());
            //    mHandler.postDelayed(this, (long)UPDATE_FREQUENCY);
            //}
        }
    };

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    switch (data.key) {
                        case PreferenceManager.KEY_SHOW_TIME_REMAINING:
                            mShowTimeRemaining = (boolean)data.value;
                            updateDuration();
                            break;
                        case PreferenceManager.KEY_SIDEBAR_TITLES_VISIBLE:
                            if (mLibraryBar != null)
                                mLibraryBar.setTitlesVisible((boolean)data.value);
                            break;
                        //case PreferenceManager.KEY_PLAYER_ALBUM_COVER_SIZE:
                        //    setShowSmallCover((boolean)data.value);
                        //    break;
                    }
            }
        }
    };

    private PlaybackState.Observer mPlaybackStateObserver = new PlaybackState.Observer() {
        @Override
        public void update(Observable sender, PlaybackState.ObserverData data) {
            switch (data.type) {
                case SongChanged:
                    sCurrentSongId = 0;
                    setSong(data.song);
                    updatePlayTime(PlaybackState.getInstance().getCachedPosition());
                    mSeeking = false;
                    break;
                case CoverLoaded:
                    mCoverView.enqueueBitmap(drawNextBitmap(data.bitmap, PlaybackState.getInstance().getCurrentSong().getId()));
                    if (mCoverViewSmall != null)
                        mCoverViewSmall.enqueueBitmap(getCurrentBitmap());
                    if (mColorDrawable != null)
                        mColorDrawable.enqueueColor(data.color);
                    break;
                case PlayingStateChanged:
                    //if (!mBinding.isBound())
                    //    PlaybackService.bind(getActivity(), mBinding);

                    updatePlayingState(data.isPlaying);
                    break;
            }
        }
    };

    private SleepTimer.Observer mSleepTimerObserver = new SleepTimer.Observer() {
        @Override
        public void update(Observable sender, SleepTimer.ObserverData data) {
            switch (data.type) {
                case Enabled:
                case Disabled:
                case Finished:
                case StateChanged:
                case Tick:
                    updateSleepTimer();
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        //mBinding = new PlaybackService.Binding();
        //mBinding.setCallback(this);

        Bundle args = getArguments();
        if (args != null)
            mIsTablet = args.getBoolean(TABLET_KEY);

        setHasOptionsMenu(!mIsTablet);

        initializeBitmaps(getActivity());
        PreferenceManager prefs = PreferenceManager.getInstance();

        mShowTimeRemaining = prefs.getBoolean(PreferenceManager.KEY_SHOW_TIME_REMAINING, false);
        mShowSmallCover = prefs.parseInt(PreferenceManager.KEY_PLAYER_ALBUM_COVER_SIZE) == COVER_SIZE_SMALL;
        prefs.addObserver(mPreferenceObserver);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId;
        if (mIsTablet)
            layoutId = R.layout.layout_player_tablet;
        else if (mShowSmallCover)
            layoutId = R.layout.layout_player_small;
        else
            layoutId = R.layout.layout_player;

        View v = inflater.inflate(layoutId, container, false);

        if (mIsTablet || mShowSmallCover) {
            mColorDrawable = new ColorTransitionDrawable();
            mColorDrawable.setOverlayColor(Util.getAttrColor(getContext(), android.R.attr.windowBackground));
            Util.setBackground(v, mColorDrawable);
        }

        if (mIsTablet) {
            mCoverViewSmall = (CoverView)v.findViewById(R.id.cover_view_small);
            mSongView = v.findViewById(R.id.layout_song_view);
            mTitleText = (TextView)v.findViewById(R.id.text_title);
            mArtistText = (TextView)v.findViewById(R.id.text_artist);
            mOptionsButton = (ImageButton)v.findViewById(R.id.options_button);
            mPlayPauseButtonSmall = (ImageButton)v.findViewById(R.id.button_play_pause_small);
            mPreviousButtonSmall = (ImageButton)v.findViewById(R.id.button_previous_small);
            mNextButtonSmall = (ImageButton)v.findViewById(R.id.button_next_small);

            mOptionsButton.setOnClickListener(this);
            mPlayPauseButtonSmall.setOnClickListener(this);
            mNextButtonSmall.setOnClickListener(this);
            mPreviousButtonSmall.setOnClickListener(this);
        }
        else {
            boolean titlesVisible = PreferenceManager.getInstance().getBoolean(
                    PreferenceManager.KEY_SIDEBAR_TITLES_VISIBLE, true);

            mLibraryBar = (LibraryBar)v.findViewById(R.id.library_bar);
            mLibraryBar.setTitlesVisible(titlesVisible);
            mLibraryBar.setDimItemBackground(mShowSmallCover);

            int gravity = getArguments().getInt(KEY_LIBBAR_GRAVITY);

            int alignLeft = 0;
            int alignRight = 0;
            int alignTop = 0;
            int alignBottom = 0;

            switch (gravity) {
                case DragLayout.GRAVITY_LEFT:
                    alignRight = 1;
                    break;
                case DragLayout.GRAVITY_RIGHT:
                    alignLeft = 1;
                    break;
                case DragLayout.GRAVITY_TOP:
                    alignBottom = 1;
                    break;
                case DragLayout.GRAVITY_BOTTOM:
                    alignTop = 1;
                    break;
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mLibraryBar.getLayoutParams();

            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, alignLeft);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, alignRight);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, alignTop);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, alignBottom);

            if (getActivity() instanceof LibraryBar.Callback)
                mLibraryBar.setCallback((LibraryBar.Callback)getActivity());

            invalidateBarMenu();
        }

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mCoverView = (CoverView)v.findViewById(R.id.cover_view);

        //if (!mIsTablet) {
        //    mCoverView.setVisibility(mShowSmallCover ? View.GONE : View.VISIBLE);
        //    mCoverViewSmall.setVisibility(mShowSmallCover ? View.VISIBLE : View.GONE);
        //}

        mQueueButton = (ImageButton)v.findViewById(R.id.button_queue);
        mNextButton = (ImageButton)v.findViewById(R.id.button_next);
        mPlayPauseButton = (ImageButton)v.findViewById(R.id.button_play_pause);
        mPreviousButton = (ImageButton)v.findViewById(R.id.button_previous);
        mShuffleButton = (ImageButton)v.findViewById(R.id.button_shuffle);
        mRepeatButton = (ImageButton)v.findViewById(R.id.button_repeat);
        View equalizerButton = v.findViewById(R.id.button_equalizer);

        if (equalizerButton != null)
            equalizerButton.setOnClickListener(this);

        mPlayTimeBar = (SeekBar)v.findViewById(R.id.bar_play_time);

        mPositionText = (TextView)v.findViewById(R.id.text_position);
        mDurationText = (TextView)v.findViewById(R.id.text_duration);

        mTimerButton = (Button)v.findViewById(R.id.button_timer);
        mTimerButton.setOnClickListener(this);

        mQueueButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPlayPauseButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mShuffleButton.setOnClickListener(this);
        mRepeatButton.setOnClickListener(this);

        //v.findViewById(R.id.button_equalizer).setOnClickListener(this);

        if (mPlayTimeBar != null)
            mPlayTimeBar.setOnSeekBarChangeListener(this);

        mOptionsHandler = (SongOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

        if (mOptionsHandler == null) {
            mOptionsHandler = new SongOptionsHandler();
            mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
        }

        mOptionsHandler.setShowUnhide(false);
        mOptionsHandler.setHasOptionsMenu(!mIsTablet);
        mOptionsHandler.setMenuVisibility(isMenuVisible());

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        PlaybackState state = PlaybackState.getInstance();

        setSong(state.getCurrentSong());
        updatePlayTime(state.getCachedPosition());
        updatePlayingState(state.isPlaying());
        updateShuffleState();
        updateRepeatMode();

        if (state.isCoverCached()) {
            drawNextBitmap(state.getCover(), state.getCurrentSong().getId());

            if (mColorDrawable != null)
                mColorDrawable.setCurrentColor(state.getCoverColor());
        }
        else if (state.getCurrentSong() == null) {
            drawNextBitmap(null, 0);

            if (mColorDrawable != null)
                mColorDrawable.setCurrentColor(state.getCoverColor());
        }

        if (mCoverView != null)
            mCoverView.setCurrentBitmap(getCurrentBitmap());
        if (mCoverViewSmall != null)
            mCoverViewSmall.setCurrentBitmap(getCurrentBitmap());

        updateSleepTimer();

        state.addObserver(mPlaybackStateObserver);
        state.getSleepTimer().addObserver(mSleepTimerObserver);

        //PlaybackService.bind(getActivity(), mBinding);
    }

    @Override
    public void onStop() {
        super.onStop();

        //mBinding.unbind();

        PlaybackState state = PlaybackState.getInstance();
        state.removeObserver(mPlaybackStateObserver);
        state.getSleepTimer().removeObserver(mSleepTimerObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if (mIsTablet)
            setShowSongOptions(menuVisible);
        else if (mOptionsHandler != null)
            mOptionsHandler.setMenuVisibility(menuVisible);
    }

    @Override
    public void onBarItemClick(LibraryBar.Item item) {
        switch (item.getId()) {
            case R.id.item_play_pause:
                PlaybackState.getInstance().togglePlayingState();
                break;
            case R.id.item_previous:
                PlaybackState.getInstance().previousSong();
                break;
            case R.id.item_next:
                PlaybackState.getInstance().nextSong(true);
                break;
        }
    }

    @Override
    public boolean onBarItemLongClick(LibraryBar.Item item) {
        if (item.getId() == R.id.item_play_pause) {
            RequestManager.getInstance().pushRequest(new RequestManager.OpenPlayerRequest());
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_play_pause_small:
            case R.id.button_play_pause:
                PlaybackState.getInstance().togglePlayingState();
                break;
            case R.id.button_previous_small:
            case R.id.button_previous:
                PlaybackState.getInstance().previousSong();
                break;
            case R.id.button_next_small:
            case R.id.button_next:
                PlaybackState.getInstance().nextSong(true);
                break;
            case R.id.button_shuffle:
                PlaybackState.getInstance().toggleShuffling();
                updateShuffleState();
                break;
            case R.id.button_repeat:
                PlaybackState.getInstance().toggleRepeatMode();
                updateRepeatMode();
                break;
            case R.id.button_queue:
                showQueue();
                break;
            case R.id.button_equalizer:
                EqualizerActivity.start(getContext());
                break;
            case R.id.button_timer: {
                PopupMenu popup = new PopupMenu(getContext(), mTimerButton);
                popup.inflate(R.menu.options_sleep_timer);

                SleepTimer timer = PlaybackState.getInstance().getSleepTimer();

                Menu menu = popup.getMenu();

                boolean start = timer.getType() == SleepTimer.TYPE_DURATION && !timer.isTimerRunning();
                boolean pause = timer.getType() == SleepTimer.TYPE_DURATION && timer.isTimerRunning();

                menu.findItem(R.id.option_start).setVisible(start).setEnabled(start);
                menu.findItem(R.id.option_pause).setVisible(pause).setEnabled(pause);

                popup.setOnMenuItemClickListener(this);
                popup.show();

                break;
            }
            case R.id.options_button:
                mOptionsHandler.showPopup(mOptionsButton);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.option_start:
                PlaybackState.getInstance().getSleepTimer().setTimerState(true);
                return true;
            case R.id.option_pause:
                PlaybackState.getInstance().getSleepTimer().setTimerState(false);
                return true;
            case R.id.option_cancel:
                PlaybackState.getInstance().getSleepTimer().cancelTimer();
                return true;
        }

        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mSeeking) {
            long time = fromSeekBarValue(mPlayTimeBar.getProgress());

            PlaybackState.getInstance().seekTo(time);
            //if (mBinding.isBound()) {
            //    PlaybackService service = mBinding.getService();
            //    service.seekTo(time);
            //}
            //else
            //    PlaybackState.getInstance().setCachedPosition(time);

            updatePlayTime(time);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        stopPlayTime();
        mSeeking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startPlayTime();
        mSeeking = false;
    }

    @Nullable
    public LibraryBar getLibraryBar() {
        return mLibraryBar;
    }

    private long getCurrentPosition() {
        //if (mBinding.isBound())
        //    return mBinding.getService().getCurrentPosition();
        //else
            return PlaybackState.getInstance().getCachedPosition();
    }

    public void setGravity(int gravity) {
        getArguments().putInt(KEY_LIBBAR_GRAVITY, gravity);
    }

    public void setPlayTimeAllowed(boolean allowed) {
        mPlayTimeAllowed = allowed;

        if (mPlayTimeAllowed)
            startPlayTime();
        else
            stopPlayTime();
    }

    private void setShowSmallCover(boolean show) {
        if (mIsTablet)
            return;

        mShowSmallCover = show;

        if (mLibraryBar != null)
            mLibraryBar.setDimItemBackground(mShowSmallCover);

        if (getView() != null) {
            if (mColorDrawable == null && mShowSmallCover) {
                mColorDrawable = new ColorTransitionDrawable();
                mColorDrawable.setOverlayColor(Util.getAttrColor(getContext(), android.R.attr.windowBackground));

                Util.setBackground(getView(), mColorDrawable);
            }

            if (mCoverView != null)
                mCoverView.setVisibility(mShowSmallCover ? View.GONE : View.VISIBLE);
            if (mCoverViewSmall != null)
                mCoverViewSmall.setVisibility(mShowSmallCover ? View.VISIBLE : View.GONE);
        }
    }

    private void setShowSongOptions(boolean show) {
        if (mSongView == null)
            return;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mSongView.getLayoutParams();

        if (show) {
            params.addRule(RelativeLayout.LEFT_OF, R.id.options_button);
            mPlayPauseButtonSmall.setVisibility(View.GONE);
            mNextButtonSmall.setVisibility(View.GONE);
            mPreviousButtonSmall.setVisibility(View.GONE);
            mOptionsButton.setVisibility(View.VISIBLE);
        }
        else {
            params.addRule(RelativeLayout.LEFT_OF, R.id.button_previous_small);
            mPlayPauseButtonSmall.setVisibility(View.VISIBLE);
            mNextButtonSmall.setVisibility(View.VISIBLE);
            mPreviousButtonSmall.setVisibility(View.VISIBLE);
            mOptionsButton.setVisibility(View.GONE);
        }

        mSongView.setLayoutParams(params);
    }

    public void setHighlightedBarItem(int position) {
        mHighlightedPosition = position;

        if (mHighlightedPosition != -1)
            mHighlightedPosition++;

        if (getLibraryBar() != null)
            getLibraryBar().setHighlightedItem(mHighlightedPosition);
    }

    @SuppressLint("RestrictedApi")
    public void invalidateBarMenu() {
        if (mLibraryBar != null) {

            MenuBuilder menu = new MenuBuilder(getContext());

            LibraryBarCreateCallback callback = null;
            if (getActivity() instanceof LibraryBarCreateCallback)
                callback = (LibraryBarCreateCallback)getActivity();

            int iconRes = PlaybackState.getInstance().isPlaying() ? R.drawable.pause_button : R.drawable.play_button;

            if (callback != null && callback.hasBarMenu()) {
                menu.add(0, R.id.item_play_pause, 0, "").setIcon(iconRes);
                callback.onCreateBarMenu(getActivity().getMenuInflater(), menu);
            }
            else {
                menu.add(0, R.id.item_previous, 0, "Previous").setIcon(R.drawable.previous_button);
                menu.add(0, R.id.item_play_pause, 0, "").setIcon(iconRes);
                menu.add(0, R.id.item_next, 0, "Next").setIcon(R.drawable.next_button);
            }

            mLibraryBar.setMenu(menu);
            mBarPlayButton = mLibraryBar.getItemById(R.id.item_play_pause);
            mBarPlayButton.setCallback(this);

            LibraryBar.Item previousButton = mLibraryBar.getItemById(R.id.item_previous);
            if (previousButton != null)
                previousButton.setCallback(this);

            LibraryBar.Item nextButton = mLibraryBar.getItemById(R.id.item_next);
            if (nextButton != null)
                nextButton.setCallback(this);

            mLibraryBar.setHighlightedItem(mHighlightedPosition);
        }
    }

    public void dismissQueue() {
        //if (mQueueFragment != null)
        //    mQueueFragment.dismiss();
    }

    private void setSong(Song song) {
        if (song == null)
            return;

        if (mTitleText != null) {
            mTitleText.setText(Song.getTitle(song));
            mArtistText.setText(Song.getArtistText(song));
        }

        mSongDuration = song.getInfo().getDuration();
        updateDuration();

        if (mPlayTimeBar != null)
            mPlayTimeBar.setMax(toSeekBarValue(mSongDuration));

        if (mOptionsHandler != null)
            mOptionsHandler.setItem(song);
    }

    private void updatePlayingState(boolean isPlaying) {
        int buttonRes;
        int smallButtonRes;

        if (isPlaying) {
            buttonRes = R.drawable.pause_button_big;
            smallButtonRes = R.drawable.pause_button;
            startPlayTime();
        }
        else {
            buttonRes = R.drawable.play_button_big;
            smallButtonRes = R.drawable.play_button;
            stopPlayTime();
        }

        if (mPlayPauseButton != null)
            mPlayPauseButton.setImageResource(buttonRes);
        if (mBarPlayButton != null)
            mBarPlayButton.setIcon(smallButtonRes);
        if (mPlayPauseButtonSmall != null)
            mPlayPauseButtonSmall.setImageResource(smallButtonRes);
    }

    private void startPlayTime() {
        if (mPlayTimeAllowed && PlaybackState.getInstance().isPlaying())
            mHandler.postDelayed(mUpdateTimeCallback, (long)UPDATE_FREQUENCY);
    }

    private void stopPlayTime() {
        mHandler.removeCallbacks(mUpdateTimeCallback);
    }

    public void updatePlayTime(long ms) {
        if (mPlayTimeBar != null) {
            mPlayTimeBar.setProgress(toSeekBarValue(ms));

            mPositionText.setText(TimeUtil.durationToString(ms));

            if (mShowTimeRemaining)
                updateDuration();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDuration() {
        if (mShowTimeRemaining)
            mDurationText.setText('-' + TimeUtil.durationToString(mSongDuration - getCurrentPosition()));
        else
            mDurationText.setText(TimeUtil.durationToString(mSongDuration));
    }

    private void updateShuffleState() {
        if (mShuffleButton != null) {
            boolean shuffle = PlaybackState.getInstance().isShuffled();
            mShuffleButton.setBackgroundResource(shuffle ?
                    R.drawable.background_image_button_highlight : R.drawable.background_image_button);
            mShuffleButton.setColorFilter(Util.getAttrColor(getContext(), shuffle ? R.attr.colorAccentInverse : R.attr.colorAccent));
        }
    }

    private void updateRepeatMode() {
        if (mRepeatButton != null) {
            switch (PlaybackState.getInstance().getRepeatMode()) {
                case None:
                    mRepeatButton.setBackgroundResource(R.drawable.background_image_button);
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_black_36dp);
                    mRepeatButton.setColorFilter(Util.getAttrColor(getContext(), R.attr.colorAccent));
                    break;
                case Single:
                    mRepeatButton.setBackgroundResource(R.drawable.background_image_button_highlight);
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_one_black_36dp);
                    mRepeatButton.setColorFilter(Util.getAttrColor(getContext(), R.attr.colorAccentInverse));
                    break;
                case All:
                    mRepeatButton.setBackgroundResource(R.drawable.background_image_button_highlight);
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_black_36dp);
                    mRepeatButton.setColorFilter(Util.getAttrColor(getContext(), R.attr.colorAccentInverse));
                    break;
            }
        }
    }

    private void updateSleepTimer() {
        SleepTimer timer = PlaybackState.getInstance().getSleepTimer();

        if (timer.isEnabled()) {
            mTimerButton.setVisibility(View.VISIBLE);

            String formatString = null;
            String timeString = null;
            switch (timer.getType()) {
                case SleepTimer.TYPE_ALARM:
                    formatString = getResources().getString(R.string.sleep_timer_text_alarm);

                    timeString = DateFormat.getTimeFormat(getContext()).format(timer.getAlarmTime());
                    break;
                case SleepTimer.TYPE_DURATION:
                    formatString = getResources().getString(R.string.sleep_timer_text_duration);
                    timeString = String.valueOf(TimeUtil.getMinutesForMillis(timer.getRemainingTime()));
                    break;
            }

            mTimerButton.setText(MessageFormat.format(formatString, timeString));

            int res = timer.isTimerRunning() ? R.drawable.ic_timer_black_24dp : R.drawable.ic_timer_off_black_24dp;
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), res, null);

            int colorAccent = Util.getAttrColor(getContext(), R.attr.colorAccent);
            drawable.setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
            mTimerButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
        else {
            mTimerButton.setVisibility(View.INVISIBLE);
        }
    }

    private void showQueue() {
        //if (mIsTablet) {
        //    if (mQueueFragment == null)
        //        mQueueFragment = QueueFragment.newInstance(true);
        //    mQueueFragment.show(getFragmentManager(), "queue");
        //}
        //else
            QueueActivity.start(getActivity());
    }


    public static PlayerFragment newInstance(boolean isTablet, int libBarGravity) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putBoolean(TABLET_KEY, isTablet);
        args.putInt(KEY_LIBBAR_GRAVITY, libBarGravity);
        fragment.setArguments(args);
        return fragment;
    }


    private static int toSeekBarValue(long ms) {
        return (int)Math.floor(ms / SEEK_BAR_SCALING);
    }

    private static long fromSeekBarValue(int value) {
        return (long)(value * SEEK_BAR_SCALING);
    }

    private static void initializeBitmaps(Context context) {
        if (sBitmaps == null) {
            PreferenceManager prefs = PreferenceManager.getInstance();
            int size = prefs.parseInt(PreferenceManager.KEY_ALBUM_COVER_SIZE);

            sBitmapRect = new Rect(0, 0, size, size);

            sBitmaps = new Bitmap[NUM_BITMAPS];
            sCanvases = new Canvas[NUM_BITMAPS];

            int resource = R.drawable.standard_cover_medium;

            switch (size) {
                case 512:
                    resource = R.drawable.standard_cover_low;
                    break;
                case 1024:
                    resource = R.drawable.standard_cover_medium;
                    break;
                case 2048:
                    resource = R.drawable.standard_cover_high;
                    break;
            }

            sEmptyBitmap = BitmapFactory.decodeResource(context.getResources(), resource);

            for (int i = 0; i < NUM_BITMAPS; i++) {
                sBitmaps[i] = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
                sCanvases[i] = new Canvas(sBitmaps[i]);
            }

            sCurrentBitmap = 0;
        }
    }

    private static Bitmap getCurrentBitmap() {
        return sBitmaps != null ? sBitmaps[sCurrentBitmap] : null;
    }

    private static Bitmap drawNextBitmap(Bitmap bitmap, long songId) {
        if (sBitmaps != null) {
            if (songId == sCurrentSongId)
                return sBitmaps[sCurrentBitmap];

            sCurrentSongId = songId;

            sCurrentBitmap = (sCurrentBitmap + 1) % NUM_BITMAPS;

            if (bitmap != null)
                sCanvases[sCurrentBitmap].drawBitmap(bitmap, 0, 0, null);
            else
                sCanvases[sCurrentBitmap].drawBitmap(sEmptyBitmap, null, sBitmapRect, null);

            return sBitmaps[sCurrentBitmap];
        }

        return null;
    }

    private static void releaseBitmaps() {
        if (sBitmaps != null) {
            for (int i = 0; i < NUM_BITMAPS; i++) {
                sBitmaps[i].recycle();
                sBitmaps[i] = null;
                sCanvases[i] = null;
            }

            sBitmaps = null;
            sCanvases = null;
        }
    }
}
