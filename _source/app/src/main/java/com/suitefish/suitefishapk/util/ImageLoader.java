package com.suitefish.suitefishapk.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.net.Http;

import java.io.File;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Loads app icons with a persistent on-disk cache (icons are downloaded once and
 * reused). A tiny in-memory map avoids redecoding while scrolling.
 */
public final class ImageLoader {

    private static final Map<String, Bitmap> MEM =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ImageLoader() {
    }

    /** Cache directory for downloaded icons. */
    public static File cacheDir(File filesRoot) {
        File dir = new File(filesRoot, "images");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    /** Deterministic local filename for a given image URL. */
    public static File fileFor(File filesRoot, String url) {
        return new File(cacheDir(filesRoot), hash(url) + ".img");
    }

    /**
     * Ensures the image at {@code url} is cached locally; returns the local file path,
     * or null on failure. Safe to call from a background thread.
     */
    public static String ensureCached(File filesRoot, String url) {
        if (url == null || url.trim().isEmpty()) return null;
        try {
            File f = fileFor(filesRoot, url);
            if (f.exists() && f.length() > 0) return f.getAbsolutePath();
            Http.download(url, f);
            return f.exists() && f.length() > 0 ? f.getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Binds an icon into an ImageView. Uses the pre-resolved local path when present,
     * otherwise falls back to the placeholder. Decoding happens off the main thread.
     */
    public static void bind(ImageView view, String localPath) {
        view.setTag(localPath);
        if (localPath == null) {
            view.setImageResource(R.drawable.ic_placeholder);
            return;
        }
        Bitmap cached = MEM.get(localPath);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageResource(R.drawable.ic_placeholder);
        final String path = localPath;
        Bg.run(() -> {
            Bitmap bmp = decode(path);
            if (bmp == null) return;
            MEM.put(path, bmp);
            Bg.post(() -> {
                if (path.equals(view.getTag())) {
                    view.setImageBitmap(bmp);
                }
            });
        });
    }

    private static Bitmap decode(String path) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2; // icons are small; halve to save memory
            return BitmapFactory.decodeFile(path, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
