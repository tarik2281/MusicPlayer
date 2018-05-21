package com.example.musicplayer.playback;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.example.musicplayer.PreferenceManager;

public class HeadsetService extends Service {

    public class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case PreferenceManager.ACTION_PREFERENCE_CHANGE: {
                    String key = intent.getStringExtra(PreferenceManager.EXTRA_PREFERENCE_KEY);
                    String value = intent.getStringExtra(PreferenceManager.EXTRA_PREFERENCE_VALUE);

                    if (key.equals(PreferenceManager.KEY_START_PLAYBACK_HEADSET))
                        mStartHeadsetPlugged = Boolean.parseBoolean(value);
                    else if (key.equals(PreferenceManager.KEY_START_PLAYBACK_BLUETOOTH))
                        mStartBluetoothPlugged = Boolean.parseBoolean(value);

                    if (!mStartHeadsetPlugged && !mStartBluetoothPlugged)
                        close();

                    break;
                }
                case Intent.ACTION_HEADSET_PLUG: {
                    if (!mInitializing && mStartHeadsetPlugged) {
                        int plugState = intent.getIntExtra("state", -1);

                        PlaybackState state = PlaybackState.getInstance();

                        if (plugState == 1) {
                            if (!state.isInited())
                                state.initialize(context);

                            state.setPlayingState(true);
                        }
                    }

                    mInitializing = false;

                    break;
                }
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
                    if (mStartBluetoothPlugged) {
                        int plugState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);

                        PlaybackState state = PlaybackState.getInstance();

                        switch (plugState) {
                            case BluetoothHeadset.STATE_CONNECTED:
                                if (!state.isInited())
                                    state.initialize(context);

                                state.setPlayingState(true);
                                break;
                        }
                    }

                    break;
                }
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED: {
                    if (mStartBluetoothPlugged) {
                        int plugState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);

                        PlaybackState state = PlaybackState.getInstance();

                        switch (plugState) {
                            case BluetoothA2dp.STATE_CONNECTED:
                                if (!state.isInited())
                                    state.initialize(context);

                                state.setPlayingState(true);
                                break;
                        }
                    }

                    break;
                }
            }
        }
    }

    private boolean mInitializing = false;
    private Receiver mReceiver;

    private boolean mStartHeadsetPlugged;
    private boolean mStartBluetoothPlugged;

    public HeadsetService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager prefs = PreferenceManager.getInstance();

        if (!prefs.isInited())
            prefs.initialize(this);

        mStartHeadsetPlugged = prefs.getBoolean(PreferenceManager.KEY_START_PLAYBACK_HEADSET);
        mStartBluetoothPlugged = prefs.getBoolean(PreferenceManager.KEY_START_PLAYBACK_BLUETOOTH);

        mInitializing = true;

        mReceiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PreferenceManager.ACTION_PREFERENCE_CHANGE);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        if (!mStartHeadsetPlugged && !mStartBluetoothPlugged)
            close();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        /*Intent restartService = new Intent(getApplicationContext(), HeadsetService.class);
        restartService.setPackage(getPackageName());
        PendingIntent restartPending = PendingIntent.getService(getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartPending);*/

    }

    private void close() {
        unregisterReceiver(mReceiver);
        mReceiver = null;

        stopSelf();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, HeadsetService.class);
        context.startService(intent);
    }
}
