package com.suitefish.suitefishapk.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suitefish.suitefishapk.AppDetailActivity;
import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.data.Repository;
import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.util.Bg;
import com.suitefish.suitefishapk.util.Prefs;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Store view: lists the locally-stored catalog (no online fetch unless the user
 * presses Sync). Supports live search and category filtering.
 */
public class StoreFragment extends Fragment implements CatalogRefreshable {

    private Repository repo;
    private Prefs prefs;

    private RecyclerView list;
    private AppListAdapter adapter;
    private TextView empty;
    private ProgressBar progress;
    private TextView lastSync;
    private TextInputEditText search;
    private Spinner categorySpinner;

    private String currentQuery = "";
    private String currentCategory = null; // null = all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_store, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = new Repository(requireContext());
        prefs = new Prefs(requireContext());

        list = v.findViewById(R.id.store_list);
        empty = v.findViewById(R.id.store_empty);
        progress = v.findViewById(R.id.store_progress);
        lastSync = v.findViewById(R.id.store_last_sync);
        search = v.findViewById(R.id.store_search);
        categorySpinner = v.findViewById(R.id.store_category);
        MaterialButton syncBtn = v.findViewById(R.id.store_sync);

        adapter = new AppListAdapter(this::openDetail);
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

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String sel = (String) parent.getItemAtPosition(pos);
                currentCategory = (pos == 0) ? null : sel; // index 0 is "All categories"
                reload();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        syncBtn.setOnClickListener(view -> sync());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCatalog();
    }

    /** Reload list, categories and the last-synced label from local data. */
    @Override
    public void refreshCatalog() {
        refreshCategories();
        reload();
        updateLastSync();
    }

    private void updateLastSync() {
        long ts = prefs.getLastSync();
        String when = ts == 0
                ? getString(R.string.never)
                : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(ts));
        lastSync.setText(getString(R.string.last_synced, when));
    }

    private void refreshCategories() {
        Bg.run(() -> {
            List<String> cats = new ArrayList<>();
            cats.add(getString(R.string.filter_all));
            cats.addAll(repo.db().categories());
            Bg.post(() -> {
                if (!isAdded()) return;
                ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, cats);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                int keep = 0;
                if (currentCategory != null) {
                    int idx = cats.indexOf(currentCategory);
                    if (idx > 0) keep = idx;
                }
                categorySpinner.setAdapter(a);
                categorySpinner.setSelection(keep);
            });
        });
    }

    private void reload() {
        final String q = currentQuery;
        final String cat = currentCategory;
        Bg.run(() -> {
            List<AppItem> data = repo.db().query(q, cat);
            repo.decorateInstallState(data);
            Bg.post(() -> {
                if (!isAdded()) return;
                adapter.setItems(data);
                boolean isEmpty = data.isEmpty();
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                empty.setText(!q.isEmpty() || cat != null
                        ? getString(R.string.empty_search)
                        : getString(R.string.empty_store));
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void sync() {
        setBusy(true);
        Bg.run(() -> {
            try {
                int n = repo.syncCatalog();
                Bg.post(() -> {
                    if (!isAdded()) return;
                    setBusy(false);
                    Toast.makeText(requireContext(), getString(R.string.sync_done, n),
                            Toast.LENGTH_SHORT).show();
                    refreshCategories();
                    reload();
                    updateLastSync();
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

    private void openDetail(AppItem item) {
        AppDetailActivity.start(requireContext(), item.packageName);
    }
}
