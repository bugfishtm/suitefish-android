package com.suitefish.suitefishapk.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Thin wrapper over SharedPreferences for all persisted settings.
 */
public final class Prefs {

    private static final String FILE = "suitefish_prefs";

    private static final String KEY_SETUP_DONE = "setup_done";
    private static final String KEY_USE_CUSTOM = "use_custom_server";
    private static final String KEY_CUSTOM_HOST = "custom_host"; // host[:port], no scheme
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_ALLOW_INSECURE_TLS = "allow_insecure_tls";

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isSetupDone() {
        return sp.getBoolean(KEY_SETUP_DONE, false);
    }

    public void setSetupDone(boolean done) {
        sp.edit().putBoolean(KEY_SETUP_DONE, done).apply();
    }

    public boolean isUsingCustomServer() {
        return sp.getBoolean(KEY_USE_CUSTOM, false);
    }

    /** Persist the server choice. host is only used when custom==true. */
    public void setServer(boolean custom, String host) {
        sp.edit()
                .putBoolean(KEY_USE_CUSTOM, custom)
                .putString(KEY_CUSTOM_HOST, host == null ? "" : host)
                .apply();
    }

    /** host[:port] for a custom server, or empty when using the default. */
    public String getCustomHost() {
        return sp.getString(KEY_CUSTOM_HOST, "");
    }

    /**
     * When true, HTTPS connections accept untrusted / self-signed certificates.
     * Off by default. Intended for self-hosted servers; see the in-app warning.
     */
    public boolean isAllowInsecureTls() {
        return sp.getBoolean(KEY_ALLOW_INSECURE_TLS, false);
    }

    public void setAllowInsecureTls(boolean allow) {
        sp.edit().putBoolean(KEY_ALLOW_INSECURE_TLS, allow).apply();
    }

    public long getLastSync() {
        return sp.getLong(KEY_LAST_SYNC, 0L);
    }

    public void setLastSync(long ts) {
        sp.edit().putLong(KEY_LAST_SYNC, ts).apply();
    }

    /** Wipe every stored preference (used on server change / reset). */
    public void clearAll() {
        sp.edit().clear().apply();
    }
}
