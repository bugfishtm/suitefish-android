package com.suitefish.suitefishapk.net;

import android.content.Context;

import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.util.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level access to the Suitefish catalog API and the file downloads.
 */
public class ApiClient {

    private final Context ctx;
    private final ServerConfig server;

    public ApiClient(Context ctx, ServerConfig server) {
        this.ctx = ctx.getApplicationContext();
        this.server = server;
    }

    /** GET fetch.php (with the device's API language) and parse the JSON array of apps. */
    public List<AppItem> fetchCatalog() throws IOException {
        String body = Http.getString(server.fetchUrl(LanguageUtils.apiLanguageCode()));
        return parse(body);
    }

    static List<AppItem> parse(String body) throws IOException {
        List<AppItem> out = new ArrayList<>();
        try {
            JSONArray arr = extractArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                AppItem it = new AppItem();
                it.packageName = optStr(o, "app_package");
                it.name = optStr(o, "app_name");
                it.category = optStr(o, "app_category");
                it.version = optStr(o, "app_version");
                it.description = optStr(o, "app_description");
                it.imageUrl = optStr(o, "app_image_url");
                it.downloadUrl = optStr(o, "app_download_url");
                if (it.packageName != null && !it.packageName.trim().isEmpty()) {
                    out.add(it);
                }
            }
        } catch (JSONException e) {
            throw new IOException("Malformed catalog response", e);
        }
        return out;
    }

    /** Accepts either a bare JSON array or an object wrapping one (e.g. {"apps":[...]}). */
    private static JSONArray extractArray(String body) throws JSONException, IOException {
        String s = body == null ? "" : body.trim();
        if (s.startsWith("[")) {
            return new JSONArray(s);
        }
        if (s.startsWith("{")) {
            JSONObject obj = new JSONObject(s);
            for (String key : new String[]{"apps", "data", "results", "list"}) {
                JSONArray a = obj.optJSONArray(key);
                if (a != null) return a;
            }
        }
        throw new IOException("Response is not a JSON array");
    }

    private static String optStr(JSONObject o, String key) {
        if (o.isNull(key)) return null;
        String v = o.optString(key, null);
        return v == null ? null : v.trim();
    }

    // ---- Files ----

    /** Directory where downloaded APKs are stored (app-scoped external files). */
    public File downloadsDir() {
        File base = ctx.getExternalFilesDir(null);
        if (base == null) base = ctx.getFilesDir();
        File dir = new File(base, "downloads");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    /** Downloads an APK for the given item to local storage and returns the file. */
    public File downloadApk(AppItem item) throws IOException {
        if (item.downloadUrl == null) throw new IOException("No download URL");
        String safePkg = item.packageName == null ? "app" : item.packageName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String safeVer = item.version == null ? "0" : item.version.replaceAll("[^a-zA-Z0-9._-]", "_");
        File dest = new File(downloadsDir(), safePkg + "_" + safeVer + ".apk");
        return Http.download(item.downloadUrl, dest);
    }
}
