package com.example.musicplayer.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.playback.PlaybackService;

public class RestoreService extends Service implements RestoreTask.Callback {

    //private static final String CHANNEL_ID = "restore_data_channel";

    private static final int NOTIFICATION_ID = Util.HashFNV1a32("RestoreProgress");
    private static final int NOTIFICATION_FINISH_ID = Util.HashFNV1a32("RestoreFinish");

    private NotificationCompat.Builder mBuilder;
    private RestoreTask mTask;

    public RestoreService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            PlaybackService.createChannel(this);

        mBuilder = new NotificationCompat.Builder(this, PlaybackService.CHANNEL_ID);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.drawable.music_node);
        mBuilder.setProgress(0, 0, true);
        mBuilder.setContentTitle(getString(R.string.service_title_restore_progress));

        mBuilder.setContentText(getString(R.string.service_restore_progress_prepare));

        commitNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mTask == null) {
            Uri uri = intent.getData();

            mTask = new RestoreTask(uri, getContentResolver());
            mTask.setCallback(this);
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return START_STICKY;
    }

    @Override
    public void onProgress(int progress) {
        int stringRes = 0;

        switch (progress) {
            case RestoreTask.PROGRESS_PREFS:
                stringRes = R.string.service_restore_progress_prefs;
                break;
            case RestoreTask.PROGRESS_SCAN:
                stringRes = R.string.service_restore_progress_scan;
                break;
            case RestoreTask.PROGRESS_DATA:
                stringRes = R.string.service_restore_progress_data;
                break;
        }

        mBuilder.setContentText(getString(stringRes));

        commitNotification();
    }

    @Override
    public void onFinish(boolean result) {
        Notification n = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.music_node)
                .setContentTitle(getString(R.string.service_title_restore_result))
                .setContentText(getString(result ? R.string.service_restore_result_success : R.string.service_restore_result_error))
                .setAutoCancel(true).build();

        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_FINISH_ID, n);

        close();
    }

    private void close() {
        stopForeground(true);
        stopSelf();
    }

    private void commitNotification() {
        startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    public static void start(Context context, Uri uri) {
        Intent intent = new Intent(context, RestoreService.class);
        intent.setData(uri);
        context.startService(intent);
    }
}
