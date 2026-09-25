package com.quiet.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/** Monotonic timing within a boot, bounded wall-clock recovery after a reboot. */
final class FocusSession {
    static final long MAX_DURATION = 60 * 60_000L;
    private static int boot(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }
    static void start(Context c, SharedPreferences p, int minutes) {
        start(p, minutes, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c));
    }
    static void start(SharedPreferences p, int minutes, long wall, long elapsed, int boot) {
        long duration = Math.max(1, Math.min(60, minutes)) * 60_000L;
        p.edit().putLong("focusUntil", wall + duration).putLong("focusElapsedEnd", elapsed + duration)
            .putLong("focusDuration", duration).putInt("focusBoot", boot).apply();
    }
    static long remaining(Context c, SharedPreferences p) {
        return remaining(p, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c));
    }
    static long remaining(SharedPreferences p, long wall, long elapsed, int boot) {
        long duration = Math.max(0, Math.min(MAX_DURATION, p.getLong("focusDuration", MAX_DURATION)));
        long remaining = p.getLong("focusUntil", 0) - wall;
        if (boot >= 0 && p.getInt("focusBoot", -2) == boot && p.contains("focusElapsedEnd"))
            remaining = p.getLong("focusElapsedEnd", 0) - elapsed;
        return Math.max(0, Math.min(duration, remaining));
    }
    static void end(SharedPreferences p) {
        p.edit().remove("focusUntil").remove("focusElapsedEnd").remove("focusDuration").remove("focusBoot").apply();
    }
}
