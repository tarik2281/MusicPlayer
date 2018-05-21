package com.example.musicplayer;

import android.app.Application;
import android.text.format.DateFormat;

import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.request.GlobalRequestReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

/**
 * Created by 19tarik97 on 16.10.16.
 */
public class MusicApplication extends Application {

    private static boolean sNativeLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.getInstance().initialize(this);
        CacheManager.getInstance().initialize(this);
        GlobalRequestReceiver.getInstance().register();
        loadNative();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                handleUncaughtException(thread, ex);
            }
        });
    }

    private void loadNative() {
        try {
            System.loadLibrary("MusicPlayer");
            Decoder.initialize();
            sNativeLoaded = true;
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static boolean isNativeLoaded() {
        return sNativeLoaded;
    }

    private void handleUncaughtException(Thread thread, Throwable ex) {
        ex.printStackTrace();

        File external = getExternalFilesDir(null);
        File logsDir = new File(external, "logs");
        logsDir.mkdir();

        if (logsDir.exists()) {
            PrintStream stream = null;
            String fileName = "Crash_" + DateFormat.format("dd_MM_yyyy__HH_mm_ss", new Date()) + ".txt";

            File file = new File(logsDir, fileName);
            if (!file.exists()) {
                try {
                    stream = new PrintStream(new FileOutputStream(file));

                    ex.printStackTrace(stream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (stream != null)
                        stream.close();
                }
            }
        }

        System.exit(1);
    }
}
