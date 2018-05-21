package com.example.musicplayer.playback;

import com.example.musicplayer.CountDownTimer;
import com.example.musicplayer.IObserver;
import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.TimeUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by 19tar on 15.09.2017.
 */

public class SleepTimer extends Observable<SleepTimer.ObserverData> {

    public interface Observer extends IObserver<ObserverData> { }

    public static class ObserverData {
        public enum Type {
            Tick, StateChanged, Finished, Enabled, Disabled
        }

        public final Type type;
        public final SleepTimer timer;

        private ObserverData(Type type, SleepTimer timer) {
            this.type = type;
            this.timer = timer;
        }
    }

    private static final String KEY_SLEEP_TIMER_TYPE = "sleep_timer_type"; // int
    private static final String KEY_SLEEP_TIMER_DURATION = "sleep_timer_duration"; // duration in minutes (int)
    private static final String KEY_SLEEP_TIMER_HOUR = "sleep_timer_hour"; // int
    private static final String KEY_SLEEP_TIMER_MINUTE = "sleep_timer_minute"; // int
    private static final String KEY_SLEEP_TIMER_REPEAT = "sleep_timer_repeat"; // boolean

    private static final int TIMER_INTERVAL = 1000 * 60; // interval every second (maybe every minute)

    public static final int TYPE_DURATION = 0;
    public static final int TYPE_ALARM = 1;

    private int mType;

    private boolean mAlarmRepeat;
    private int mAlarmHour;
    private int mAlarmMinute;

    private int mDurationMinutes;

    private CountDownTimer mTimer;
    private ObserverData mTimerTickData; // cache

    private Date mAlarmTime;

    public void initialize() {
        PreferenceManager prefs = PreferenceManager.getInstance();

        mType = prefs.getInt(KEY_SLEEP_TIMER_TYPE);
        mAlarmRepeat = prefs.getBoolean(KEY_SLEEP_TIMER_REPEAT);
        mAlarmHour = prefs.getInt(KEY_SLEEP_TIMER_HOUR);
        mAlarmMinute = prefs.getInt(KEY_SLEEP_TIMER_MINUTE);
        mDurationMinutes = prefs.getInt(KEY_SLEEP_TIMER_DURATION, 15);
    }

    public int getType() {
        return mType;
    }

    public boolean getAlarmRepeat() {
        return mAlarmRepeat;
    }

    public int getAlarmHour() {
        return mAlarmHour;
    }

    public int getAlarmMinute() {
        return mAlarmMinute;
    }

    public int getDurationMinutes() {
        return mDurationMinutes;
    }

    public boolean isEnabled() {
        return mTimer != null && !mTimer.finished();
    }

    public boolean isTimerRunning() {
        return mTimer != null && mTimer.isRunning();
    }

    public void setTimerState(boolean running) {
        if (mTimer != null) {
            if (running)
                mTimer.start();
            else
                mTimer.pause();
        }
    }

    // returns remaining time of the timer in milliseconds
    public long getRemainingTime() {
        if (mTimer != null)
            return mTimer.timeLeft();

        return 0;
    }

    // for alarm timer
    public final Date getAlarmTime() {
        if (mAlarmTime != null)
            return (Date)mAlarmTime.clone();

        return null;
    }

    public void cancelTimer() {
        cancelAll();

        callTimerEvent(ObserverData.Type.Disabled);
    }

    // duration in minutes
    public void startDurationTimer(int minutes) {
        cancelAll();

        mType = TYPE_DURATION;
        mDurationMinutes = minutes;

        savePreferences();

        startDurationTimer();
    }

    public void startAlarmTimer(int hour, int minute, boolean repeat) {
        cancelAll();

        mType = TYPE_ALARM;

        mAlarmHour = hour;
        mAlarmMinute = minute;
        mAlarmRepeat = repeat;

        savePreferences();

        startAlarmTimer();
    }

    public void startAlarmIfRepeat() {
        if (mType == TYPE_ALARM && mAlarmRepeat)
            startAlarmTimer();
    }

    private void startDurationTimer() {
        long millis = TimeUtil.getMillisForMinutes(mDurationMinutes);

        mTimer = new CountDownTimer(millis, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                callTimerTick();
            }

            @Override
            public void onStateChanged(boolean isRunning) {
                callTimerEvent(ObserverData.Type.StateChanged);
            }

            @Override
            public void onFinish() {
                callTimerEvent(ObserverData.Type.Finished);
                mTimer = null;
            }
        };

        startTimer();
    }

    private void savePreferences() {
        PreferenceManager prefs = PreferenceManager.getInstance();

        prefs.setInt(KEY_SLEEP_TIMER_TYPE, mType);
        prefs.setInt(KEY_SLEEP_TIMER_HOUR, mAlarmHour);
        prefs.setInt(KEY_SLEEP_TIMER_MINUTE, mAlarmMinute);
        prefs.setBoolean(KEY_SLEEP_TIMER_REPEAT, mAlarmRepeat);

        prefs.setInt(KEY_SLEEP_TIMER_DURATION, mDurationMinutes);
    }

    private void startAlarmTimer() {
        Calendar calendar = Calendar.getInstance();
        Calendar alarm = (Calendar)calendar.clone();

        alarm.set(Calendar.HOUR, mAlarmHour);
        alarm.set(Calendar.MINUTE, mAlarmMinute);
        alarm.set(Calendar.SECOND, 0);

        long millis = alarm.getTimeInMillis();

        if (alarm.before(calendar))
            millis += 1000L * 60 * 60 * 24; // add one day

        long duration = millis - calendar.getTimeInMillis();

        mAlarmTime = alarm.getTime();

        mTimer = new CountDownTimer(duration, duration) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onStateChanged(boolean isRunning) {

            }

            @Override
            public void onFinish() {
                callTimerEvent(ObserverData.Type.Finished);
            }
        };

        startTimer();
    }

    private void startTimer() {
        mTimer.start();
        callTimerEvent(ObserverData.Type.Enabled);
    }

    private void cancelAll() {
        if (mTimer != null)
            mTimer.cancel();

        mTimer = null;
    }

    private void callTimerTick() {
        if (mTimerTickData == null)
            mTimerTickData = new ObserverData(ObserverData.Type.Tick, this);

        notifyObservers(mTimerTickData);
    }

    private void callTimerEvent(ObserverData.Type type) {
        notifyObservers(new ObserverData(type, this));
    }
}
