package com.suitefish.suitefishapk.ui;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.net.ServerConfig;
import com.suitefish.suitefishapk.util.Prefs;

/**
 * Central place for the legally-relevant notices so the wording stays consistent
 * wherever they are shown (onboarding, every launch, and Settings).
 */
public final class Notices {

    public interface Callback {
        void onResult(boolean accepted);
    }

    private Notices() {
    }

    /** "Use at your own risk" notice. */
    public static void showRisk(Context ctx, @Nullable Runnable onAck) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.risk_title)
                .setMessage(R.string.risk_body)
                .setCancelable(onAck == null)
                .setPositiveButton(R.string.privacy_accept, (d, w) -> {
                    if (onAck != null) onAck.run();
                });
        b.show();
    }

    /**
     * Privacy notice shown on every launch. The body differs for default vs custom
     * server. The callback reports accept (true) or exit (false).
     */
    public static void showPrivacy(Context ctx, Prefs prefs, Callback cb) {
        String message;
        if (prefs.isUsingCustomServer()) {
            message = ctx.getString(R.string.privacy_body_custom,
                    ServerConfig.from(prefs).displayHost());
        } else {
            message = ctx.getString(R.string.privacy_body_default);
        }
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.privacy_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.privacy_accept, (d, w) -> cb.onResult(true))
                .setNegativeButton(R.string.privacy_decline, (d, w) -> cb.onResult(false))
                .show();
    }

    /** Read-only privacy notice (from Settings). */
    public static void showPrivacyInfo(Context ctx, Prefs prefs) {
        String message = prefs.isUsingCustomServer()
                ? ctx.getString(R.string.privacy_body_custom, ServerConfig.from(prefs).displayHost())
                : ctx.getString(R.string.privacy_body_default);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.privacy_title)
                .setMessage(message)
                .setPositiveButton(R.string.privacy_accept, null)
                .show();
    }

    /** How to allow installs from untrusted sources. */
    public static void showInstallHelp(Context ctx) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.install_help_title)
                .setMessage(R.string.install_help_body)
                .setPositiveButton(R.string.install_help_got_it, null)
                .show();
    }
}
