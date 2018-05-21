package com.example.musicplayer;

import java.util.Locale;

/**
 * Created by 19tar on 17.09.2017.
 */

public final class TimeUtil {

    private static final long HOUR_IN_MS = 3600 * 1000;
    private static final long MIN_IN_MS = 60 * 1000;
    private static final long SEC_IN_MS = 1000;

    private TimeUtil() {
        // no instances allowed
    }


    /**
     * Converts the given duration to a string representation like '01:30:15' (hours, minutes, seconds)
     * @param duration in milliseconds
     * @return the string representation of the duration
     */
    public static String durationToString(long duration) {
        String sign = duration < 0 ? "-" : "";

        duration = Math.abs(duration);

        int hours = (int)Math.floor(duration / HOUR_IN_MS);
        duration -= hours * HOUR_IN_MS;
        int mins = (int)Math.floor(duration / MIN_IN_MS);
        duration -= mins * MIN_IN_MS;
        int secs = (int)Math.floor(duration / SEC_IN_MS);

        String time;
        if (hours > 0)
            time = String.format(Locale.US, "%s%d:%02d:%02d", sign, hours, mins, secs);
        else
            time = String.format(Locale.US, "%s%02d:%02d", sign, mins, secs);

        return time;
    }

    // result will be rounded down
    public static long getSecondsForMillis(long ms) {
        return (long)Math.floor((double)ms / SEC_IN_MS);
    }

    // result will be rounded up
    public static long getMinutesForMillis(long ms) {
        return (long)Math.ceil((double)ms / MIN_IN_MS);
    }

    public static long getMillisForMinutes(long minutes) {
        return minutes * MIN_IN_MS;
    }
}
