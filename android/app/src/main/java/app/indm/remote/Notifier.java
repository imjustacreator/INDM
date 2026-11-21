package app.indm.remote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** All notifications of the app in one place. */
final class Notifier {
    private Notifier() {}

    static final String CH_STATUS = "status";
    static final String CH_DONE = "done";
    static final int ID_STATUS = 1;
    static final int ID_SAVE = 2;
    private static int next = 100;

    static void channels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CH_STATUS, c.getString(R.string.ch_status),
                NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CH_DONE, c.getString(R.string.ch_done),
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    private static Notification.Builder builder(Context c, String channel) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel)
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat);
        return b;
    }

    private static PendingIntent open(Context c, Intent i) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(c, 0, i, flags);
    }

    private static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return open(c, i);
    }

    private static PendingIntent openDownloads(Context c) {
        Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return open(c, i);
    }

    /** The permanent notification of the listening service. */
    static Notification status(Context c, boolean connected) {
        Notification.Builder b = builder(c, CH_STATUS)
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(c.getString(connected ? R.string.status_ok : R.string.status_wait))
                .setOngoing(true)
                .setContentIntent(openApp(c));
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
        return b.build();
    }

    /** Shown while a file is being saved. total < 0 means the size is unknown. */
    static Notification saving(Context c, String name, long done, long total) {
        Notification.Builder b = builder(c, CH_STATUS)
                .setContentTitle(c.getString(R.string.saving))
                .setContentText(name)
                .setOngoing(true)
                .setContentIntent(openDownloads(c));
        if (total > 0) b.setProgress(1000, (int) Math.min(1000, done * 1000 / total), false);
        else b.setProgress(0, 0, true);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
        return b.build();
    }

    private static void post(Context c, Notification n) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            nm.notify(next++, n);
        } catch (RuntimeException ignored) {
            // no permission to show notifications: nothing else to do
        }
    }

    static void finished(Context c, String name) {
        post(c, builder(c, CH_DONE).setContentTitle(c.getString(R.string.done_title)).setContentText(name)
                .setAutoCancel(true).setContentIntent(openApp(c)).build());
    }

    static void failed(Context c, String name, String error) {
        String text = error == null || error.isEmpty() ? name : name + "\n" + error;
        post(c, builder(c, CH_DONE).setContentTitle(c.getString(R.string.fail_title)).setContentText(name)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true).setContentIntent(openApp(c)).build());
    }

    static void saved(Context c, String name) {
        post(c, builder(c, CH_DONE).setContentTitle(c.getString(R.string.saved)).setContentText(name)
                .setAutoCancel(true).setContentIntent(openDownloads(c)).build());
    }

    static void saveFailed(Context c, String name) {
        post(c, builder(c, CH_DONE).setContentTitle(c.getString(R.string.save_failed)).setContentText(name)
                .setAutoCancel(true).setContentIntent(openApp(c)).build());
    }

    static void invalid(Context c) {
        post(c, builder(c, CH_DONE).setContentTitle(c.getString(R.string.app_name))
                .setContentText(c.getString(R.string.invalid))
                .setAutoCancel(true).setContentIntent(openApp(c)).build());
    }
}
