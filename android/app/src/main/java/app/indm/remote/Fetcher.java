package app.indm.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Copies one finished file from the PC (over the home network) into whatever the opener gives us. */
final class Fetcher {
    private Fetcher() {}

    interface Opener {
        OutputStream open(long length) throws IOException;
    }

    interface Progress {
        void on(long done, long total);
    }

    static long get(String url, Opener opener, Progress progress) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            c.setUseCaches(false);
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            long total = c.getContentLength();
            InputStream in = c.getInputStream();
            OutputStream out = opener.open(total);
            long done = 0, lastReport = 0;
            try {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    long now = System.currentTimeMillis();
                    if (progress != null && now - lastReport > 500) {
                        lastReport = now;
                        progress.on(done, total);
                    }
                }
                out.flush();
            } finally {
                out.close();
            }
            if (total >= 0 && done != total) throw new IOException("cut off");
            if (progress != null) progress.on(done, total);
            return done;
        } finally {
            c.disconnect();
        }
    }
}
