package com.suitefish.suitefishapk.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.util.VersionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Local, offline catalog of apps. One row per package (the newest version seen).
 */
public class CatalogDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "suitefish_catalog.db";
    private static final int DB_VERSION = 1;

    private static final String T = "apps";
    private static final String C_PKG = "package";
    private static final String C_NAME = "name";
    private static final String C_CAT = "category";
    private static final String C_VER = "version";
    private static final String C_DESC = "description";
    private static final String C_IMG = "image_url";
    private static final String C_DL = "download_url";
    private static final String C_IMGPATH = "image_path";

    private static volatile CatalogDb INSTANCE;

    private CatalogDb(Context ctx) {
        super(ctx.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    /**
     * Process-wide singleton. A single {@link SQLiteOpenHelper} keeps all reads and
     * writes on one connection, so a background sync and a UI read cannot race into a
     * "database is locked" exception.
     */
    public static CatalogDb getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (CatalogDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CatalogDb(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T + " (" +
                C_PKG + " TEXT PRIMARY KEY, " +
                C_NAME + " TEXT, " +
                C_CAT + " TEXT, " +
                C_VER + " TEXT, " +
                C_DESC + " TEXT, " +
                C_IMG + " TEXT, " +
                C_DL + " TEXT, " +
                C_IMGPATH + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T);
        onCreate(db);
    }

    /**
     * Replaces the whole catalog with the given items, keeping only the highest
     * version per package. Existing cached image paths are preserved when the URL
     * for that package's image is unchanged.
     */
    public void replaceAll(List<AppItem> items) {
        // Preserve previously resolved image paths keyed by image URL.
        Map<String, String> imagePathByUrl = new LinkedHashMap<>();
        SQLiteDatabase rdb = getReadableDatabase();
        try (Cursor c = rdb.query(T, new String[]{C_IMG, C_IMGPATH}, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String url = c.getString(0);
                String path = c.getString(1);
                if (url != null && path != null) imagePathByUrl.put(url, path);
            }
        }

        // Dedup by package, keeping the newest version.
        Map<String, AppItem> latest = new LinkedHashMap<>();
        for (AppItem it : items) {
            if (it.packageName == null || it.packageName.trim().isEmpty()) continue;
            AppItem cur = latest.get(it.packageName);
            if (cur == null || VersionUtils.compare(it.version, cur.version) > 0) {
                latest.put(it.packageName, it);
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(T, null, null);
            for (AppItem it : latest.values()) {
                ContentValues v = new ContentValues();
                v.put(C_PKG, it.packageName);
                v.put(C_NAME, it.name);
                v.put(C_CAT, it.category);
                v.put(C_VER, it.version);
                v.put(C_DESC, it.description);
                v.put(C_IMG, it.imageUrl);
                v.put(C_DL, it.downloadUrl);
                String keptPath = it.imageUrl != null ? imagePathByUrl.get(it.imageUrl) : null;
                v.put(C_IMGPATH, keptPath);
                db.insertWithOnConflict(T, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateImagePath(String packageName, String path) {
        ContentValues v = new ContentValues();
        v.put(C_IMGPATH, path);
        getWritableDatabase().update(T, v, C_PKG + "=?", new String[]{packageName});
    }

    public int count() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + T, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /**
     * Query the catalog with optional case-insensitive text search and category filter.
     *
     * @param query    search over name / package / description; null or empty = all
     * @param category exact category filter; null or empty = all
     */
    public List<AppItem> query(@Nullable String query, @Nullable String category) {
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            String like = "%" + query.trim() + "%";
            where.append("(").append(C_NAME).append(" LIKE ? OR ")
                    .append(C_PKG).append(" LIKE ? OR ")
                    .append(C_DESC).append(" LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (category != null && !category.trim().isEmpty()) {
            if (where.length() > 0) where.append(" AND ");
            where.append(C_CAT).append(" = ?");
            args.add(category);
        }
        SQLiteDatabase db = getReadableDatabase();
        List<AppItem> out = new ArrayList<>();
        try (Cursor c = db.query(T, null,
                where.length() > 0 ? where.toString() : null,
                args.isEmpty() ? null : args.toArray(new String[0]),
                null, null, C_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                out.add(fromCursor(c));
            }
        }
        return out;
    }

    @Nullable
    public AppItem getByPackage(String packageName) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(T, null, C_PKG + "=?", new String[]{packageName},
                null, null, null)) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    public List<String> categories() {
        SQLiteDatabase db = getReadableDatabase();
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Cursor c = db.query(true, T, new String[]{C_CAT}, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String cat = c.getString(0);
                if (cat != null && !cat.trim().isEmpty()) set.add(cat.trim());
            }
        }
        return new ArrayList<>(set);
    }

    /** All package names currently in the catalog. */
    public List<String> allPackages() {
        SQLiteDatabase db = getReadableDatabase();
        List<String> out = new ArrayList<>();
        try (Cursor c = db.query(T, new String[]{C_PKG}, null, null, null, null, null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }

    public void wipe() {
        getWritableDatabase().delete(T, null, null);
    }

    private static AppItem fromCursor(Cursor c) {
        AppItem it = new AppItem();
        it.packageName = c.getString(c.getColumnIndexOrThrow(C_PKG));
        it.name = c.getString(c.getColumnIndexOrThrow(C_NAME));
        it.category = c.getString(c.getColumnIndexOrThrow(C_CAT));
        it.version = c.getString(c.getColumnIndexOrThrow(C_VER));
        it.description = c.getString(c.getColumnIndexOrThrow(C_DESC));
        it.imageUrl = c.getString(c.getColumnIndexOrThrow(C_IMG));
        it.downloadUrl = c.getString(c.getColumnIndexOrThrow(C_DL));
        it.cachedImagePath = c.getString(c.getColumnIndexOrThrow(C_IMGPATH));
        return it;
    }
}
