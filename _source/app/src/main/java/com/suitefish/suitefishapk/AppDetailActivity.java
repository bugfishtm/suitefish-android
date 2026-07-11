package com.suitefish.suitefishapk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.suitefish.suitefishapk.data.Repository;
import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.ui.Notices;
import com.suitefish.suitefishapk.util.Bg;
import com.suitefish.suitefishapk.util.ImageLoader;
import com.suitefish.suitefishapk.util.InstallUtils;

import java.io.File;
import java.io.IOException;

/**
 * App details, with download/install, update and uninstall actions. All state-changing
 * actions delegate to the system install/uninstall routines.
 */
public class AppDetailActivity extends AppCompatActivity {

    private static final String EXTRA_PACKAGE = "extra_package";

    private Repository repo;
    private AppItem item;

    private ImageView icon;
    private TextView name, version, category, installedVersion, packageName, description;
    private TextView badgeInstalled, badgeUpdate;
    private MaterialButton primary, uninstall, installHelp;
    private ProgressBar progress;

    public static void start(Context ctx, String pkg) {
        Intent i = new Intent(ctx, AppDetailActivity.class);
        i.putExtra(EXTRA_PACKAGE, pkg);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        // Edge-to-edge (enforced from targetSdk 35): keep content out of the system bars.
        View root = findViewById(R.id.detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
        }

        repo = new Repository(this);

        icon = findViewById(R.id.detail_icon);
        name = findViewById(R.id.detail_name);
        version = findViewById(R.id.detail_version);
        category = findViewById(R.id.detail_category);
        installedVersion = findViewById(R.id.detail_installed_version);
        packageName = findViewById(R.id.detail_package);
        description = findViewById(R.id.detail_description);
        badgeInstalled = findViewById(R.id.detail_badge_installed);
        badgeUpdate = findViewById(R.id.detail_badge_update);
        primary = findViewById(R.id.detail_primary);
        uninstall = findViewById(R.id.detail_uninstall);
        installHelp = findViewById(R.id.detail_install_help);
        progress = findViewById(R.id.detail_progress);

        installHelp.setOnClickListener(v -> Notices.showInstallHelp(this));
        uninstall.setOnClickListener(v -> confirmUninstall());
        primary.setOnClickListener(v -> confirmDownload());

        String pkg = getIntent().getStringExtra(EXTRA_PACKAGE);
        loadItem(pkg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reflect any install/uninstall that happened via the system UI.
        if (item != null) loadItem(item.packageName);
    }

    private void loadItem(String pkg) {
        Bg.run(() -> {
            AppItem it = repo.db().getByPackage(pkg);
            if (it != null) repo.decorateInstallState(it);
            Bg.post(() -> {
                if (isFinishing()) return;
                if (it == null) {
                    Toast.makeText(this, "App not found in catalog.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                item = it;
                bind();
            });
        });
    }

    private void bind() {
        ImageLoader.bind(icon, item.cachedImagePath);
        name.setText(item.name != null ? item.name : item.packageName);
        version.setText(getString(R.string.detail_version, safe(item.version)));
        category.setText(getString(R.string.detail_category,
                item.category != null ? item.category : "-"));
        packageName.setText(getString(R.string.detail_package, item.packageName));
        description.setText(item.description != null ? item.description : "");

        badgeInstalled.setVisibility(item.installed ? View.VISIBLE : View.GONE);
        badgeUpdate.setVisibility(item.updateAvailable ? View.VISIBLE : View.GONE);

        if (item.installed) {
            installedVersion.setVisibility(View.VISIBLE);
            installedVersion.setText(getString(R.string.detail_installed_version,
                    safe(item.installedVersion)));
            uninstall.setVisibility(View.VISIBLE);
        } else {
            installedVersion.setVisibility(View.GONE);
            uninstall.setVisibility(View.GONE);
        }

        if (item.updateAvailable) {
            primary.setVisibility(View.VISIBLE);
            primary.setText(R.string.detail_update);
        } else if (item.installed) {
            // Installed and current: no download action needed.
            primary.setVisibility(View.GONE);
        } else {
            primary.setVisibility(View.VISIBLE);
            primary.setText(R.string.detail_download);
        }
    }

    private void confirmDownload() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_online_title)
                .setMessage(R.string.confirm_download_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> download())
                .show();
    }

    private void download() {
        setBusy(true);
        final AppItem it = item;
        Bg.run(() -> {
            try {
                File apk = repo.downloadApk(it);
                Bg.post(() -> {
                    if (isFinishing()) return;
                    setBusy(false);
                    // The system installer handles the "unknown sources" prompt itself.
                    if (!InstallUtils.canRequestInstalls(this)) {
                        Notices.showInstallHelp(this);
                    }
                    InstallUtils.installApk(this, apk);
                });
            } catch (IOException e) {
                // Server unreachable / offline: warn the user; nothing local is changed.
                Bg.post(() -> {
                    if (isFinishing()) return;
                    setBusy(false);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.download_offline_title)
                            .setMessage(R.string.download_offline_body)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        });
    }

    private void confirmUninstall() {
        // Always routes through the system uninstall routine.
        InstallUtils.uninstall(this, item.packageName);
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        primary.setEnabled(!busy);
        uninstall.setEnabled(!busy);
    }

    private static String safe(String s) {
        return s == null ? "?" : s;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
