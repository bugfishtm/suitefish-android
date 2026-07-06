package com.suitefish.suitefishapk.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.suitefish.suitefishapk.AppDetailActivity;
import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.data.Repository;
import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.util.Bg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Installed view: shows only catalog apps that are installed on the device. Works
 * fully offline from the local catalog; the "Check for updates" button is the only
 * action that contacts the server (after an explicit confirmation).
 */
public class InstalledFragment extends Fragment implements CatalogRefreshable {

    private Repository repo;

    private RecyclerView list;
    private AppListAdapter adapter;
    private TextView empty;
    private ProgressBar progress;
    private TextInputEditText search;

    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_installed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = new Repository(requireContext());

        list = v.findViewById(R.id.installed_list);
        empty = v.findViewById(R.id.installed_empty);
        progress = v.findViewById(R.id.installed_progress);
        search = v.findViewById(R.id.installed_search);
        MaterialButton checkBtn = v.findViewById(R.id.installed_check_updates);

        adapter = new AppListAdapter(item -> AppDetailActivity.start(requireContext(), item.packageName));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                currentQuery = s.toString();
                reload();
            }
        });

        checkBtn.setOnClickListener(view -> confirmCheckUpdates());
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void refreshCatalog() {
        reload();
    }

    /** Loads installed catalog apps from the local database (no network). */
    private void reload() {
        final String q = currentQuery;
        Bg.run(() -> {
            List<AppItem> all = repo.db().query(q, null);
            repo.decorateInstallState(all);
            List<AppItem> installed = new ArrayList<>();
            for (AppItem it : all) {
                if (it.installed) installed.add(it);
            }
            Bg.post(() -> {
                if (!isAdded()) return;
                adapter.setItems(installed);
                boolean isEmpty = installed.isEmpty();
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                empty.setText(!q.isEmpty()
                        ? getString(R.string.empty_search)
                        : getString(R.string.empty_installed));
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void confirmCheckUpdates() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_online_title)
                .setMessage(R.string.confirm_update_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> checkUpdates())
                .show();
    }

    /** Contacts the server, refreshes the catalog, and reports how many updates exist. */
    private void checkUpdates() {
        setBusy(true);
        Bg.run(() -> {
            try {
                repo.syncCatalog();
                List<AppItem> all = repo.db().query(null, null);
                repo.decorateInstallState(all);
                int updates = 0;
                for (AppItem it : all) {
                    if (it.updateAvailable) updates++;
                }
                final int count = updates;
                Bg.post(() -> {
                    if (!isAdded()) return;
                    setBusy(false);
                    String msg = count > 0
                            ? getString(R.string.updates_found, count)
                            : getString(R.string.updates_none);
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    reload();
                });
            } catch (IOException e) {
                // Offline / unreachable: keep the existing local catalog untouched.
                Bg.post(() -> {
                    if (!isAdded()) return;
                    setBusy(false);
                    Toast.makeText(requireContext(),
                            getString(R.string.sync_offline), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }
}
