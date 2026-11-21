package app.indm.remote;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Waits for INDM on the PC to say that a download finished or failed. It asks the PC "anything new since N?"
 * and the PC answers when something happens (or after 25 seconds), so it costs almost no battery.
 */
final class EventPoller implements Runnable {
    interface Listener {
        void onEvent(String kind, String id, String name, long size, String error);

        void onSeq(long seq);

        void onConnection(boolean ok);

        /** The PC refused the address (the code was reset); no point in trying again. */
        void onInvalid();
    }

    private static final class Refused extends IOException {
        Refused() {
            super("refused");
        }
    }

    private final String base;
    private final Listener listener;
    private volatile boolean stopped;
    private volatile HttpURLConnection current;
    private long since;

    EventPoller(String base, long since, Listener listener) {
        this.base = base;
        this.since = since;
        this.listener = listener;
    }

    void stop() {
        stopped = true;
        HttpURLConnection c = current;
        if (c != null) c.disconnect();
    }

    @Override
    public void run() {
        int fails = 0;
        while (!stopped) {
            try {
                if (since < 0) {
                    since = get("api/info", 8000).optLong("last", 0);
                    listener.onSeq(since);
                }
                JSONObject o = get("api/events?since=" + since + "&wait=25", 40000);
                fails = 0;
                listener.onConnection(true);
                long last = o.optLong("last", since);
                if (last < since) {                 // the PC program was restarted: its numbers began again
                    since = last;
                    listener.onSeq(since);
                }
                JSONArray a = o.optJSONArray("events");
                long max = since;
                if (a != null) {
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject e = a.getJSONObject(i);
                        long seq = e.optLong("seq", 0);
                        if (seq <= since) continue;
                        if (seq > max) max = seq;
                        listener.onEvent(e.optString("kind"), e.optString("id"), e.optString("name"),
                                e.optLong("size"), e.optString("error"));
                    }
                }
                if (max != since) {
                    since = max;
                    listener.onSeq(since);
                }
            } catch (Refused e) {
                if (!stopped) listener.onInvalid();
                return;
            } catch (Exception e) {
                if (stopped) return;
                fails++;
                listener.onConnection(false);
                try {
                    Thread.sleep(Math.min(60000L, 3000L * fails));
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private JSONObject get(String path, int readTimeout) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        current = c;
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(readTimeout);
            c.setUseCaches(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            int code = c.getResponseCode();
            if (code == 404 || code == 403) throw new Refused();
            if (code != 200) throw new IOException("HTTP " + code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > 1_000_000) throw new IOException("too big");
            }
            return new JSONObject(new String(bo.toByteArray(), "UTF-8"));
        } finally {
            current = null;
            c.disconnect();
        }
    }
}
