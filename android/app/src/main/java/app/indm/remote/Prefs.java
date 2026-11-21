package app.indm.remote;

import android.content.Context;
import android.content.SharedPreferences;

/** The few things the app remembers: the PC address, whether notifications are on, the last event number. */
final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("indm", Context.MODE_PRIVATE);
    }

    static String base(Context c) {
        return sp(c).getString("base", "");
    }

    /** A new PC address also forgets the old event number. */
    static void setBase(Context c, String base) {
        sp(c).edit().putString("base", base).putLong("seq", -1).apply();
    }

    static void forget(Context c) {
        sp(c).edit().remove("base").putLong("seq", -1).putBoolean("notify", false).apply();
    }

    static boolean notifyOn(Context c) {
        return sp(c).getBoolean("notify", false);
    }

    static void setNotify(Context c, boolean on) {
        sp(c).edit().putBoolean("notify", on).apply();
    }

    static long seq(Context c) {
        return sp(c).getLong("seq", -1);
    }

    static void setSeq(Context c, long seq) {
        sp(c).edit().putLong("seq", seq).apply();
    }

    static boolean askedBattery(Context c) {
        return sp(c).getBoolean("asked_battery", false);
    }

    static void setAskedBattery(Context c) {
        sp(c).edit().putBoolean("asked_battery", true).apply();
    }
}
