package com.suitefish.suitefishapk.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Helpers around the platform package manager and the system install/uninstall UI.
 */
public final class InstallUtils {

    private InstallUtils() {
    }

    /** @return the installed versionName for a package, or null if not installed. */
    public static String installedVersion(Context ctx, String packageName) {
        if (packageName == null) return null;
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(packageName, 0);
            return pi.versionName != null ? pi.versionName : "";
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static boolean isInstalled(Context ctx, String packageName) {
        return installedVersion(ctx, packageName) != null;
    }

    /**
     * Launches the system installer for a downloaded APK via FileProvider.
     * The system routine handles the actual install and any "unknown sources" prompt.
     */
    public static void installApk(Context ctx, File apk) {
        Uri uri = FileProvider.getUriForFile(
                ctx, ctx.getPackageName() + ".fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    /**
     * Triggers the system uninstall routine for a package. The OS shows its own prompt.
     */
    public static void uninstall(Context ctx, String packageName) {
        Intent intent;
        Uri pkg = Uri.parse("package:" + packageName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            intent = new Intent(Intent.ACTION_DELETE, pkg);
        } else {
            intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, pkg);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    /** Whether this app may currently install APKs (Android O+ per-app setting). */
    public static boolean canRequestInstalls(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ctx.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
}
