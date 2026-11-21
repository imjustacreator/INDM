package app.indm.remote;

/** Makes a file name safe to save on the phone. */
final class Names {
    private Names() {}

    static String clean(String name) {
        String s = name == null ? "" : name;
        s = s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        while (s.startsWith(".")) s = s.substring(1);
        s = s.trim();
        if (s.length() > 120) {
            int dot = s.lastIndexOf('.');
            String ext = dot > 0 && s.length() - dot <= 12 ? s.substring(dot) : "";
            s = s.substring(0, 120 - ext.length()) + ext;
        }
        return s.isEmpty() ? "file" : s;
    }
}
