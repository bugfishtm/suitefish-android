package com.suitefish.suitefishapk.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.annotation.SuppressLint;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Tiny HTTPS client built on HttpURLConnection. HTTPS is required for every call;
 * plain-HTTP URLs are rejected before a connection is opened.
 *
 * Certificate validation is on by default. It can be disabled process-wide via
 * {@link #setAllowInsecureTls(boolean)} for users who self-host with an untrusted
 * or self-signed certificate. When disabled, the bypass is applied per-connection
 * (never installed as the JVM default), so it only affects this client.
 */
public final class Http {

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    /** Process-wide toggle for accepting untrusted certificates. Default: secure. */
    private static volatile boolean allowInsecureTls = false;
    private static volatile SSLSocketFactory insecureFactory;
    private static final HostnameVerifier ACCEPT_ALL_HOSTS = (hostname, session) -> true;

    private Http() {
    }

    /** Enable/disable acceptance of untrusted TLS certificates for future connections. */
    public static void setAllowInsecureTls(boolean allow) {
        allowInsecureTls = allow;
    }

    public static boolean isAllowInsecureTls() {
        return allowInsecureTls;
    }

    private static HttpsURLConnection open(String url) throws IOException {
        if (url == null || !url.toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw new IOException("Only HTTPS URLs are allowed: " + url);
        }
        HttpURLConnection raw = (HttpURLConnection) new URL(url).openConnection();
        if (!(raw instanceof HttpsURLConnection)) {
            raw.disconnect();
            throw new IOException("Connection is not HTTPS: " + url);
        }
        HttpsURLConnection c = (HttpsURLConnection) raw;
        if (allowInsecureTls) {
            // Per-connection bypass only; the JVM default trust manager is untouched.
            SSLSocketFactory f = insecureFactory();
            if (f != null) {
                c.setSSLSocketFactory(f);
                c.setHostnameVerifier(ACCEPT_ALL_HOSTS);
            }
        }
        c.setConnectTimeout(CONNECT_TIMEOUT);
        c.setReadTimeout(READ_TIMEOUT);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Suitefish-Android");
        c.setRequestProperty("Accept", "application/json, */*");
        return c;
    }

    /**
     * Lazily builds (and caches) a trust-all SSL socket factory. This deliberately
     * skips certificate validation and is ONLY reachable when the user has explicitly
     * enabled "Allow untrusted certificates" (off by default). The lint warnings for
     * the empty trust manager are suppressed because that is the intended behaviour.
     */
    @SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
    private static SSLSocketFactory insecureFactory() {
        SSLSocketFactory f = insecureFactory;
        if (f != null) return f;
        synchronized (Http.class) {
            if (insecureFactory != null) return insecureFactory;
            try {
                TrustManager[] trustAll = new TrustManager[]{
                        new X509TrustManager() {
                            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        }
                };
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, trustAll, new SecureRandom());
                insecureFactory = ctx.getSocketFactory();
            } catch (Exception e) {
                insecureFactory = null;
            }
            return insecureFactory;
        }
    }

    /** GET a URL and return the body as a UTF-8 string. Throws on non-2xx. */
    public static String getString(String url) throws IOException {
        HttpsURLConnection c = open(url);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            try (InputStream in = new BufferedInputStream(c.getInputStream())) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
                return new String(bos.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }

    /** Download a URL to a file. Returns the destination file. Throws on non-2xx. */
    public static File download(String url, File dest) throws IOException {
        HttpsURLConnection c = open(url);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            try (InputStream in = new BufferedInputStream(c.getInputStream());
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                out.flush();
            }
            return dest;
        } finally {
            c.disconnect();
        }
    }

    /**
     * Opens a HEAD/GET connection just to confirm the host answers over HTTPS.
     * Any 2x/3x/4xx response counts as "reachable"; only connection failures fail.
     */
    public static boolean isReachable(String url) {
        HttpsURLConnection c = null;
        try {
            c = open(url);
            c.setRequestMethod("GET");
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            int code = c.getResponseCode();
            return code > 0;
        } catch (IOException e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
