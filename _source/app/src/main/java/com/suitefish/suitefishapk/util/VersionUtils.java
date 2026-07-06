package com.suitefish.suitefishapk.util;

/**
 * Dotted numeric version comparison ("1.2.3" style). Non-numeric segments are
 * compared lexicographically as a fallback so malformed versions never crash.
 */
public final class VersionUtils {

    private VersionUtils() {
    }

    /**
     * @return negative if a &lt; b, 0 if equal, positive if a &gt; b.
     */
    public static int compare(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        String[] pa = a.trim().split("\\.");
        String[] pb = b.trim().split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String sa = i < pa.length ? pa[i].trim() : "0";
            String sb = i < pb.length ? pb[i].trim() : "0";
            Integer ia = tryInt(sa);
            Integer ib = tryInt(sb);
            int cmp;
            if (ia != null && ib != null) {
                cmp = Integer.compare(ia, ib);
            } else {
                cmp = sa.compareTo(sb);
            }
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    /** @return true if remote is strictly newer than installed. */
    public static boolean isNewer(String remote, String installed) {
        return compare(remote, installed) > 0;
    }

    private static Integer tryInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
