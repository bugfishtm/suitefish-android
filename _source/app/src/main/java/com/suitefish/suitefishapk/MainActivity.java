package com.suitefish.suitefishapk;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.suitefish.suitefishapk.data.Repository;
import com.suitefish.suitefishapk.ui.AboutFragment;
import com.suitefish.suitefishapk.ui.CatalogRefreshable;
import com.suitefish.suitefishapk.ui.InstalledFragment;
import com.suitefish.suitefishapk.ui.Notices;
import com.suitefish.suitefishapk.ui.SettingsFragment;
import com.suitefish.suitefishapk.ui.StoreFragment;
import com.suitefish.suitefishapk.util.Bg;
import com.suitefish.suitefishapk.util.Prefs;

import java.io.IOException;

/**
 * Bottom-nav host for the four sections. Shows the privacy notice on every launch
 * and kicks off one automatic catalog sync per launch (after the notice is accepted).
 */
public class MainActivity extends AppCompatActivity {

    private Prefs prefs;
    private Repository repo;
    private Fragment currentFragment;
    private boolean privacyAcceptedThisSession = false;
    private boolean privacyDialogShowing = false;
    private boolean autoSyncStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        repo = new Repository(this);

        // Safety net: if somehow launched before setup, go back to onboarding.
        if (!prefs.isSetupDone()) {
            startActivity(new android.content.Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_installed) {
                show(new InstalledFragment(), "installed");
            } else if (id == R.id.nav_store) {
                show(new StoreFragment(), "store");
            } else if (id == R.id.nav_settings) {
                show(new SettingsFragment(), "settings");
            } else if (id == R.id.nav_about) {
                show(new AboutFragment(), "about");
            }
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_installed); // default view
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Guard against the setup-redirect path where the activity is already finishing.
        if (isFinishing() || !prefs.isSetupDone()) {
            return;
        }
        // Privacy notice is required on every launch. Guard against stacking a second
        // dialog if the app is backgrounded and resumed before the user answers.
        if (!privacyAcceptedThisSession && !privacyDialogShowing) {
            privacyDialogShowing = true;
            Notices.showPrivacy(this, prefs, accepted -> {
                privacyDialogShowing = false;
                if (accepted) {
                    privacyAcceptedThisSession = true;
                    maybeAutoSync();
                } else {
                    finishAffinity(); // user declined -> leave the app
                }
            });
        }
    }

    /**
     * One automatic catalog refresh per launch. On success the visible catalog
     * fragment is reloaded; if the server is offline the local catalog is left
     * untouched and the user is notified.
     */
    private void maybeAutoSync() {
        if (autoSyncStarted) return;
        autoSyncStarted = true;
        Bg.run(() -> {
            try {
                repo.syncCatalog();
                Bg.post(() -> {
                    if (isFinishing()) return;
                    if (currentFragment instanceof CatalogRefreshable) {
                        ((CatalogRefreshable) currentFragment).refreshCatalog();
                    }
                });
            } catch (IOException e) {
                Bg.post(() -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, R.string.sync_offline, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void show(Fragment f, String tag) {
        currentFragment = f;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f, tag)
                .commit();
    }
}
