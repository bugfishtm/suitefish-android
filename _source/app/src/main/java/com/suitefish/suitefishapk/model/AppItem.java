package com.suitefish.suitefishapk.model;

/**
 * A single catalog entry as returned by fetch.php.
 */
public class AppItem {
    public String packageName;
    public String name;
    public String category;
    public String version;
    public String description;
    public String imageUrl;
    public String downloadUrl;

    /** Local absolute path of the cached image, resolved at query time. May be null. */
    public String cachedImagePath;

    // Install state, filled in by the UI layer from PackageManager.
    public boolean installed;
    public String installedVersion;
    public boolean updateAvailable;

    public AppItem() {
    }

    public AppItem(String packageName, String name, String category, String version,
                   String description, String imageUrl, String downloadUrl) {
        this.packageName = packageName;
        this.name = name;
        this.category = category;
        this.version = version;
        this.description = description;
        this.imageUrl = imageUrl;
        this.downloadUrl = downloadUrl;
    }
}
