package app.indm.remote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the address INDM shows in its QR code: http://192.168.x.x:38460/m/TOKEN/ (home networks only). */
final class PairLink {
    private PairLink() {}

    private static final Pattern P = Pattern.compile(
            "^http://(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}):(\\d{1,5})/m/([A-Za-z0-9_-]{8,64})/?$");

    /** Returns the cleaned address (always ending in a slash), or null when the text is not an INDM address. */
    static String parse(String text) {
        if (text == null) return null;
        Matcher m = P.matcher(text.trim());
        if (!m.matches()) return null;
        int[] o = new int[4];
        for (int i = 0; i < 4; i++) {
            o[i] = Integer.parseInt(m.group(i + 1));
            if (o[i] > 255) return null;
        }
        if (!isPrivate(o)) return null;
        int port = Integer.parseInt(m.group(5));
        if (port < 1 || port > 65535) return null;
        return "http://" + o[0] + "." + o[1] + "." + o[2] + "." + o[3] + ":" + port + "/m/" + m.group(6) + "/";
    }

    /** Same as parse, but a typed address may lack the http:// at the start. */
    static String parseLoose(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (!t.toLowerCase().startsWith("http://") && !t.toLowerCase().startsWith("https://")) t = "http://" + t;
        return parse(t);
    }

    private static boolean isPrivate(int[] o) {
        return o[0] == 10 || (o[0] == 172 && o[1] >= 16 && o[1] <= 31) || (o[0] == 192 && o[1] == 168);
    }
}
