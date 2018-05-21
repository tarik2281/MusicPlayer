package com.example.musicplayer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.widget.RemoteViews;

import com.example.musicplayer.library.Song;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.ui.activities.BaseActivity;
import com.example.musicplayer.ui.activities.MainActivity;

/**
 * Implementation of App Widget functionality.
 */
public class PlayerWidget extends AppWidgetProvider {

    private static boolean sInitialized;
    private static Bitmap sBitmap;
    private static Canvas sCanvas;
    private static Rect sRect;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            initAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static int getThemeLayout() {
        return R.layout.player_widget_light;

    }

     static void initAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

         RemoteViews views = new RemoteViews(context.getPackageName(), getThemeLayout());

         Class<?> topActivity = BaseActivity.getActivityOnTop();
         Intent activityIntent = new Intent(context, (topActivity == null) ? MainActivity.class : topActivity);
         activityIntent.putExtra(MainActivity.EXTRA_OPEN_LAYOUT, true);
         activityIntent.setAction("com.example.musicplayer.open_layout");
         PendingIntent activityPending = PendingIntent.getActivity(context, 0, activityIntent, 0);
         Intent prevIntent = new Intent(PlaybackState.ACTION_PREV);
         PendingIntent prevPending = PendingIntent.getBroadcast(context, 0, prevIntent, 0);
         Intent playIntent = new Intent(PlaybackState.ACTION_PLAY);
         PendingIntent playPending = PendingIntent.getBroadcast(context, 0, playIntent, 0);
         Intent nextIntent = new Intent(PlaybackState.ACTION_NEXT);
         PendingIntent nextPending = PendingIntent.getBroadcast(context, 0, nextIntent, 0);

         views.setOnClickPendingIntent(R.id.layout_widget, activityPending);
         views.setOnClickPendingIntent(R.id.button_previous, prevPending);
         views.setOnClickPendingIntent(R.id.button_play_pause, playPending);
         views.setOnClickPendingIntent(R.id.button_next, nextPending);

         PlaybackState state = PlaybackState.getInstance();

         updateViewState(context, views);

         if (state.isCoverCached())
            updateViewData(context, views);

         appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void updateViewData(Context context, RemoteViews views) {
        if (!sInitialized) {
            int size = Util.DPItoPX(88, context);
            sBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            sCanvas = new Canvas(sBitmap);
            sRect = new Rect(0, 0, size, size);
            sInitialized = true;
        }

        PlaybackState state = PlaybackState.getInstance();

        Song song = state.getCurrentSong();

        if (song != null) {
            String titleText = Song.getTitle(song);
            String artistText = Song.getArtistText(song);

            views.setTextViewText(R.id.text_title, titleText);
            views.setTextViewText(R.id.text_artist, artistText);

            Bitmap bitmap = state.getCover();
            if (bitmap == null) {
                views.setImageViewResource(R.id.cover_view, R.drawable.standard_cover);
            }
            else {
                sCanvas.drawBitmap(bitmap, null, sRect, null);
                views.setImageViewBitmap(R.id.cover_view, sBitmap);
            }
        }
    }

    static void updateViewState(Context context, RemoteViews views) {
        PlaybackState state = PlaybackState.getInstance();

        int res = state.isPlaying() ? R.drawable.pause_button : R.drawable.play_button;
        views.setImageViewResource(R.id.button_play_pause, res);
    }

    static void updateWidgetData(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), getThemeLayout());

        updateViewData(context, views);

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    static void updateWidgetState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), getThemeLayout());

        updateViewState(context, views);

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    static void updateWidgetActivity(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), getThemeLayout());

        Class<?> topActivity = BaseActivity.getActivityOnTop();
        Intent activityIntent = new Intent(context, topActivity == null ? MainActivity.class : topActivity);
        activityIntent.putExtra(MainActivity.EXTRA_OPEN_LAYOUT, true);
        activityIntent.setAction("com.example.musicplayer.open_layout");
        PendingIntent activityPending = PendingIntent.getActivity(context, 0, activityIntent, 0);

        views.setOnClickPendingIntent(R.id.layout_widget, activityPending);

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PreferenceManager.getInstance().isInited())
            return;

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PlayerWidget.class));

        switch (intent.getAction()) {
            case PlaybackState.ACTION_COVER_LOADED:
                for (int id : ids)
                    updateWidgetData(context, manager, id);
                break;
            case PlaybackState.ACTION_STATE_CHANGED:
                for (int id : ids)
                    updateWidgetState(context, manager, id);
                break;
            case BaseActivity.ACTION_ACTIVITY_CHANGED:
                for (int id : ids)
                    updateWidgetActivity(context, manager, id);
                break;
            default:
                super.onReceive(context, intent);
                break;
        }

    }


}

