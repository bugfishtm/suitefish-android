package com.suitefish.suitefishapk.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.suitefish.suitefishapk.R;

/**
 * About: open-source and author information (Bugfish) plus outbound links. No private
 * contact details are shown.
 */
public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        TextView versionView = v.findViewById(R.id.about_version);
        versionView.setText(getString(R.string.about_version_fmt, appVersion()));

        link(v, R.id.about_website, R.string.about_website_url);
        link(v, R.id.about_youtube, R.string.about_youtube_url);
        link(v, R.id.about_repo, R.string.about_repo_url);
    }

    private void link(View root, int viewId, int urlRes) {
        MaterialButton b = root.findViewById(viewId);
        b.setOnClickListener(v -> openUrl(getString(urlRes)));
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app available to open: " + url,
                    Toast.LENGTH_LONG).show();
        }
    }

    private String appVersion() {
        try {
            PackageInfo pi = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            return pi.versionName != null ? pi.versionName : "1.0";
        } catch (Exception e) {
            return "1.0";
        }
    }
}
