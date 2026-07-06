package com.suitefish.suitefishapk.ui;

/**
 * Implemented by fragments that show catalog data so the host can tell them to
 * reload after a background (auto) sync completes.
 */
public interface CatalogRefreshable {
    void refreshCatalog();
}
