package app.indm.remote;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** Keeps listening to the PC so a "download finished" notification can arrive even when the app is closed. */
public class NotifyService extends Service implements EventPoller.Listener {
    private EventPoller poller;
    private Thread thread;
    private String base = "";
    private volatile boolean connected;

    @Override
    public void onCreate() {
        super.onCreate();
        Notifier.channels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        enterForeground(connected);
        String b = Prefs.base(this);
        if (b.isEmpty() || !Prefs.notifyOn(this)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (poller == null || !b.equals(base)) {
            stopPoller();
            base = b;
            poller = new EventPoller(b, Prefs.seq(this), this);
            thread = new Thread(poller, "indm-listen");
            thread.setDaemon(true);
            thread.start();
        }
        return START_STICKY;
    }

    private void enterForeground(boolean ok) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifier.ID_STATUS, Notifier.status(this, ok), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(Notifier.ID_STATUS, Notifier.status(this, ok));
        }
    }

    private void stopPoller() {
        if (poller != null) poller.stop();
        if (thread != null) thread.interrupt();
        poller = null;
        thread = null;
    }

    @Override
    public void onDestroy() {
        stopPoller();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---- EventPoller.Listener (called from the listening thread)

    @Override
    public void onEvent(String kind, String id, String name, long size, String error) {
        if ("completed".equals(kind)) Notifier.finished(this, name);
        else if ("error".equals(kind)) Notifier.failed(this, name, error);
    }

    @Override
    public void onSeq(long seq) {
        Prefs.setSeq(this, seq);
    }

    @Override
    public void onConnection(boolean ok) {
        if (ok == connected) return;
        connected = ok;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            nm.notify(Notifier.ID_STATUS, Notifier.status(this, ok));
        } catch (RuntimeException ignored) {
            // notifications not allowed
        }
    }

    @Override
    public void onInvalid() {
        Prefs.setNotify(this, false);
        Notifier.invalid(this);
        stopForeground(true);
        stopSelf();
    }
}
