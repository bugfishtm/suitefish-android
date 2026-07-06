package com.suitefish.suitefishapk.net;

import androidx.annotation.NonNull;

import com.suitefish.suitefishapk.util.Prefs;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolves the active catalog server and builds API endpoint URLs.
 *
 * HTTPS is always enforced. A custom server is entered as a bare host with an
 * optional ":port"; the scheme is never accepted from the user.
 */
public final class ServerConfig {

    /** Default catalog host. The default server fetches from store.suitefish.com. */
    public static final String DEFAULT_HOST = "store.suitefish.com";

    private static final String API_PATH = "/_api/_android/";

    // host (domain or IPv4) with an optional :port. Scheme is intentionally rejected.
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "^(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" + // domain
                    "|(?:\\d{1,3}\\.){3}\\d{1,3})" +                                    // or IPv4
                    "(?::(\\d{1,5}))?$");                                                // optional :port

    private final String host; // host[:port], no scheme

    private ServerConfig(String host) {
        this.host = host;
    }

    public static ServerConfig from(Prefs prefs) {
        if (prefs.isUsingCustomServer()) {
            String h = prefs.getCustomHost();
            if (h != null && !h.isEmpty()) {
                return new ServerConfig(h);
            }
        }
        return new ServerConfig(DEFAULT_HOST);
    }

    /** Base of the Android API, e.g. https://example.com:8443/_api/_android/ */
    public String apiBase() {
        return "https://" + host + API_PATH;
    }

    /** Fetch endpoint including the API language code, e.g. fetch.php?lang=de */
    public String fetchUrl(String langCode) {
        String lang = (langCode == null || langCode.isEmpty()) ? "en" : langCode;
        return apiBase() + "fetch.php?lang=" + lang;
    }

    @NonNull
    public String displayHost() {
        return "https://" + host;
    }

    public String rawHost() {
        return host;
    }

    // ---- Validation & normalization of user-entered custom hosts ----

    /**
     * Normalizes user input into a bare host[:port]. Strips a leading http(s):// and
     * any trailing path/slash. Returns null if the value is not a valid online host.
     */
    public static String normalizeHost(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;

        // Reject/strip a scheme. Plain http is not allowed (HTTPS enforced).
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")) {
            return null; // explicit non-HTTPS scheme is rejected outright
        }
        if (lower.startsWith("https://")) {
            s = s.substring("https://".length());
        }

        // Drop anything after the first slash (path/query).
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        s = s.trim();
        if (s.isEmpty()) return null;

        java.util.regex.Matcher m = HOST_PATTERN.matcher(s);
        if (!m.matches()) return null;

        // Validate the optional port range.
        String portStr = m.group(1);
        if (portStr != null) {
            try {
                int port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return s;
    }
}
