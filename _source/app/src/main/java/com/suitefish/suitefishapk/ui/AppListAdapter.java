package com.suitefish.suitefishapk.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suitefish.suitefishapk.R;
import com.suitefish.suitefishapk.model.AppItem;
import com.suitefish.suitefishapk.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared list adapter used by both the Store and Installed screens.
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.VH> {

    public interface OnItemClick {
        void onClick(AppItem item);
    }

    private final List<AppItem> items = new ArrayList<>();
    private final OnItemClick clickListener;

    public AppListAdapter(OnItemClick clickListener) {
        this.clickListener = clickListener;
    }

    public void setItems(List<AppItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppItem it = items.get(position);
        h.name.setText(it.name != null ? it.name : it.packageName);
        String cat = it.category != null ? it.category : "";
        h.meta.setText(cat.isEmpty()
                ? ("v" + safe(it.version))
                : (cat + " · v" + safe(it.version)));

        h.badgeInstalled.setVisibility(it.installed ? View.VISIBLE : View.GONE);
        h.badgeUpdate.setVisibility(it.updateAvailable ? View.VISIBLE : View.GONE);

        ImageLoader.bind(h.icon, it.cachedImagePath);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(it);
        });
    }

    private static String safe(String s) {
        return s == null ? "?" : s;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView meta;
        final TextView badgeInstalled;
        final TextView badgeUpdate;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.item_icon);
            name = v.findViewById(R.id.item_name);
            meta = v.findViewById(R.id.item_meta);
            badgeInstalled = v.findViewById(R.id.item_badge_installed);
            badgeUpdate = v.findViewById(R.id.item_badge_update);
        }
    }
}
