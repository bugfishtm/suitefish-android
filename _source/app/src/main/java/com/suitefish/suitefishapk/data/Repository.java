package com.suitefish.suitefishapk.data;

import android.content.Context;

import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.net.ApiClient;
import com.suitefish.suitefishapk.net.ServerConfig;
import com.suitefish.suitefishapk.util.ImageLoader;
import com.suitefish.suitefishapk.util.InstallUtils;
import com.suitefish.suitefishapk.util.Prefs;
import com.suitefish.suitefishapk.util.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Coordinates the network API, the local catalog database and the on-disk caches.
 * All heavy methods are blocking and must be called from a background thread.
 */
public class Repository {

    private final Context ctx;
    private final CatalogDb db;
    private final Prefs prefs;

    public Repository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.db = CatalogDb.getInstance(this.ctx);
        this.prefs = new Prefs(this.ctx);
    }

    public CatalogDb db() {
        return db;
    }

    private ApiClient api() {
        return new ApiClient(ctx, ServerConfig.from(prefs));
    }

    /**
     * Fetches the catalog from the active server, stores it locally, then downloads
     * any not-yet-cached icons. Returns the number of stored apps.
     */
    public int syncCatalog() throws IOException {
        List<AppItem> remote = api().fetchCatalog();
        db.replaceAll(remote);
        cacheImages();
        prefs.setLastSync(System.currentTimeMillis());
        return db.count();
    }

    /** Downloads and records local paths for any catalog icons not yet cached. */
    private void cacheImages() {
        File root = ctx.getFilesDir();
        for (AppItem it : db.query(null, null)) {
            if (it.imageUrl == null || it.imageUrl.isEmpty()) continue;
            if (it.cachedImagePath != null && new File(it.cachedImagePath).exists()) continue;
            String path = ImageLoader.ensureCached(root, it.imageUrl);
            if (path != null) {
                db.updateImagePath(it.packageName, path);
            }
        }
    }

    /** Download an APK for a catalog item (blocking). */
    public File downloadApk(AppItem item) throws IOException {
        return api().downloadApk(item);
    }

    /**
     * Fills installed / installedVersion / updateAvailable on each item from the
     * platform package manager.
     */
    public void decorateInstallState(List<AppItem> items) {
        for (AppItem it : items) {
            String iv = InstallUtils.installedVersion(ctx, it.packageName);
            it.installed = iv != null;
            it.installedVersion = iv;
            it.updateAvailable = iv != null && VersionUtils.isNewer(it.version, iv);
        }
    }

    public void decorateInstallState(AppItem it) {
        String iv = InstallUtils.installedVersion(ctx, it.packageName);
        it.installed = iv != null;
        it.installedVersion = iv;
        it.updateAvailable = iv != null && VersionUtils.isNewer(it.version, iv);
    }

    /**
     * Deletes every piece of local state: catalog rows, cached icons and downloaded
     * APKs. Does not touch installed apps or the server preference.
     */
    public void wipeLocalData() {
        db.wipe();
        deleteRecursive(ImageLoader.cacheDir(ctx.getFilesDir()));
        File extBase = ctx.getExternalFilesDir(null);
        if (extBase != null) deleteRecursive(new File(extBase, "downloads"));
        deleteRecursive(new File(ctx.getFilesDir(), "downloads"));
        prefs.setLastSync(0L);
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
