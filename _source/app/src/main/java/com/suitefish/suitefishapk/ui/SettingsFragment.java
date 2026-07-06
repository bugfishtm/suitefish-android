package com.suitefish.suitefishapk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.suitefish.suitefishapk.MainActivity;
import com.suitefish.suitefishapk.OnboardingActivity;
import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.data.Repository;
import com.suitefish.suitefishapk.net.Http;
import com.suitefish.suitefishapk.net.ServerConfig;
import com.suitefish.suitefishapk.util.Bg;
import com.suitefish.suitefishapk.util.Prefs;

/**
 * Settings: view/change the catalog server (which wipes local data) and manage
 * local storage and the informational notices.
 */
public class SettingsFragment extends Fragment {

    private Prefs prefs;
    private Repository repo;
    private TextView currentServer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        prefs = new Prefs(requireContext());
        repo = new Repository(requireContext());

        currentServer = v.findViewById(R.id.settings_current_server);

        MaterialButton changeServer = v.findViewById(R.id.settings_change_server);
        MaterialSwitch allowInsecure = v.findViewById(R.id.settings_allow_insecure);
        MaterialButton wipe = v.findViewById(R.id.settings_wipe);

        allowInsecure.setChecked(prefs.isAllowInsecureTls());
        allowInsecure.setOnCheckedChangeListener((btn, checked) -> {
            if (!btn.isPressed()) return; // ignore programmatic changes
            if (checked) {
                confirmEnableInsecure(allowInsecure);
            } else {
                applyInsecure(false);
            }
        });
        MaterialButton showPrivacy = v.findViewById(R.id.settings_show_privacy);
        MaterialButton showRisk = v.findViewById(R.id.settings_show_risk);
        MaterialButton showInstallHelp = v.findViewById(R.id.settings_show_install_help);

        changeServer.setOnClickListener(view -> confirmChangeServer());
        wipe.setOnClickListener(view -> confirmWipe());
        showPrivacy.setOnClickListener(view -> Notices.showPrivacyInfo(requireContext(), prefs));
        showRisk.setOnClickListener(view -> Notices.showRisk(requireContext(), null));
        showInstallHelp.setOnClickListener(view -> Notices.showInstallHelp(requireContext()));

        updateServerLabel();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateServerLabel();
    }

    private void updateServerLabel() {
        String host = ServerConfig.from(prefs).displayHost();
        String label = prefs.isUsingCustomServer() ? host + " (custom)" : host + " (default)";
        currentServer.setText(getString(R.string.settings_current_server, label));
    }

    private void confirmEnableInsecure(MaterialSwitch toggle) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.allow_insecure_warn_title)
                .setMessage(R.string.allow_insecure_warn_body)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (d, w) -> toggle.setChecked(false))
                .setPositiveButton(R.string.allow_insecure_enable, (d, w) -> applyInsecure(true))
                .show();
    }

    private void applyInsecure(boolean allow) {
        prefs.setAllowInsecureTls(allow);
        Http.setAllowInsecureTls(allow);
    }

    private void confirmChangeServer() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_change_warning_title)
                .setMessage(R.string.settings_change_warning_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_change_server, (d, w) -> changeServer())
                .show();
    }

    /** Wipes all local data and preferences, then restarts into onboarding. */
    private void changeServer() {
        Bg.run(() -> {
            repo.wipeLocalData();
            prefs.clearAll(); // also resets the server choice and setup flag
            Bg.post(() -> restartTo(OnboardingActivity.class));
        });
    }

    private void confirmWipe() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_wipe_title)
                .setMessage(R.string.settings_wipe_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_wipe, (d, w) -> wipe())
                .show();
    }

    /** Clears catalog/images/downloads but keeps the server choice, then restarts. */
    private void wipe() {
        Bg.run(() -> {
            repo.wipeLocalData();
            Bg.post(() -> restartTo(MainActivity.class));
        });
    }

    private void restartTo(Class<?> target) {
        if (!isAdded()) return;
        Intent i = new Intent(requireContext(), target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finishAffinity();
    }
}
