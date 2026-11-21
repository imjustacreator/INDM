package app.indm.remote;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Saves a finished file from the PC into the phone's Downloads/INDM folder, one file at a time. */
public class SaveService extends Service {
    static final String EXTRA_URL = "url";
    static final String EXTRA_NAME = "name";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private int pending;

    @Override
    public void onCreate() {
        super.onCreate();
        Notifier.channels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String url = intent == null ? null : intent.getStringExtra(EXTRA_URL);
        final String name = Names.clean(intent == null ? null : intent.getStringExtra(EXTRA_NAME));
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifier.ID_SAVE, Notifier.saving(this, name, 0, -1), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(Notifier.ID_SAVE, Notifier.saving(this, name, 0, -1));
        }
        if (url == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        synchronized (lock) {
            pending++;
        }
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    save(url, name);
                } finally {
                    boolean last;
                    synchronized (lock) {
                        last = --pending == 0;
                    }
                    if (last) {
                        stopForeground(true);
                        stopSelf();
                    }
                }
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void save(String url, final String name) {
        final Sink sink = new Sink(name);
        try {
            Fetcher.get(url, new Fetcher.Opener() {
                @Override
                public OutputStream open(long length) throws IOException {
                    return sink.open();
                }
            }, new Fetcher.Progress() {
                @Override
                public void on(long done, long total) {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    try {
                        nm.notify(Notifier.ID_SAVE, Notifier.saving(SaveService.this, name, done, total));
                    } catch (RuntimeException ignored) {
                        // notifications not allowed
                    }
                }
            });
            sink.finish(true);
            Notifier.saved(this, name);
        } catch (Exception e) {
            sink.finish(false);
            Notifier.saveFailed(this, name);
        }
    }

    /** The place a file goes: MediaStore on Android 10+, the public Downloads folder before that. */
    private final class Sink {
        private final String name;
        private Uri uri;
        private File file;

        Sink(String name) {
            this.name = name;
        }

        OutputStream open() throws IOException {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, mime(name));
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/INDM");
                cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
                ContentResolver r = getContentResolver();
                uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new IOException("cannot create the file");
                OutputStream o = r.openOutputStream(uri);
                if (o == null) throw new IOException("cannot open the file");
                return o;
            }
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "INDM");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create the folder");
            file = unique(dir, name);
            return new FileOutputStream(file);
        }

        void finish(boolean ok) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    if (uri == null) return;
                    if (ok) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        getContentResolver().update(uri, cv, null, null);
                    } else {
                        getContentResolver().delete(uri, null, null);
                    }
                } else if (file != null) {
                    if (ok) MediaScannerConnection.scanFile(SaveService.this, new String[]{file.getAbsolutePath()}, null, null);
                    else if (!file.delete()) file.deleteOnExit();
                }
            } catch (RuntimeException ignored) {
                // nothing more can be done
            }
        }
    }

    private static File unique(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; i < 1000; i++) {
            f = new File(dir, stem + " (" + i + ")" + ext);
            if (!f.exists()) return f;
        }
        return new File(dir, stem + "_" + System.currentTimeMillis() + ext);
    }

    private static String mime(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (m != null) return m;
        }
        return "application/octet-stream";
    }
}
