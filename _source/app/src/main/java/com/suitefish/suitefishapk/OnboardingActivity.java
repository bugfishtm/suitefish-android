package com.suitefish.suitefishapk;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.suitefish.suitefishapk.net.Http;
import com.suitefish.suitefishapk.net.ServerConfig;
import com.suitefish.suitefishapk.util.Bg;
import com.suitefish.suitefishapk.util.Prefs;

// Note: the insecure-TLS checkbox lets self-hosted servers with self-signed
// certificates pass the reachability check during setup.

/**
 * First-run setup: risk acknowledgement and a one-time server choice. This is the
 * launcher activity; once setup is complete it simply forwards to MainActivity.
 */
public class OnboardingActivity extends AppCompatActivity {

    private Prefs prefs;

    private MaterialCheckBox acceptRisk;
    private RadioButton radioDefault;
    private RadioButton radioCustom;
    private TextInputLayout customLayout;
    private TextInputEditText customInput;
    private MaterialCheckBox allowInsecure;
    private ProgressBar progress;
    private MaterialButton continueBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        // If setup already happened, skip straight to the main app.
        if (prefs.isSetupDone()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        // Edge-to-edge (enforced from targetSdk 35): keep content out of the system bars.
        View root = findViewById(R.id.onb_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        acceptRisk = findViewById(R.id.onb_accept_risk);
        radioDefault = findViewById(R.id.onb_radio_default);
        radioCustom = findViewById(R.id.onb_radio_custom);
        customLayout = findViewById(R.id.onb_custom_layout);
        customInput = findViewById(R.id.onb_custom_input);
        allowInsecure = findViewById(R.id.onb_allow_insecure);
        progress = findViewById(R.id.onb_progress);
        continueBtn = findViewById(R.id.onb_continue);

        radioDefault.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                customLayout.setEnabled(false);
                allowInsecure.setEnabled(false);
            }
        });
        radioCustom.setOnCheckedChangeListener((v, checked) -> {
            customLayout.setEnabled(checked);
            allowInsecure.setEnabled(checked);
            if (checked) customInput.requestFocus();
        });

        continueBtn.setOnClickListener(v -> onContinue());
    }

    private void onContinue() {
        if (!acceptRisk.isChecked()) {
            Toast.makeText(this, "Please accept the risk notice to continue.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (radioDefault.isChecked()) {
            // The default server always uses strict certificate validation.
            prefs.setAllowInsecureTls(false);
            Http.setAllowInsecureTls(false);
            prefs.setServer(false, null);
            finishSetup();
            return;
        }

        // Custom server path: validate then test reachability.
        String raw = customInput.getText() == null ? "" : customInput.getText().toString();
        final String host = ServerConfig.normalizeHost(raw);
        if (TextUtils.isEmpty(host)) {
            customLayout.setError(getString(R.string.server_invalid));
            return;
        }
        customLayout.setError(null);

        // Apply the chosen TLS mode before the reachability test so self-signed
        // hosts can be reached during setup.
        final boolean insecure = allowInsecure.isChecked();
        Http.setAllowInsecureTls(insecure);
        setBusy(true);

        final String testUrl = "https://" + host + "/_api/_android/fetch.php";
        Bg.run(() -> {
            // Reachable check: the fetch endpoint OR the bare host answering over HTTPS.
            boolean ok = Http.isReachable(testUrl) || Http.isReachable("https://" + host + "/");
            Bg.post(() -> {
                setBusy(false);
                if (ok) {
                    prefs.setAllowInsecureTls(insecure);
                    prefs.setServer(true, host);
                    finishSetup();
                } else {
                    customLayout.setError(getString(R.string.server_unreachable));
                }
            });
        });
    }

    private void finishSetup() {
        prefs.setSetupDone(true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        continueBtn.setEnabled(!busy);
        radioDefault.setEnabled(!busy);
        radioCustom.setEnabled(!busy);
        customInput.setEnabled(!busy);
        allowInsecure.setEnabled(!busy && radioCustom.isChecked());
    }
}
