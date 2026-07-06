package com.suitefish.suitefishapk.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal background/main-thread dispatch so we avoid pulling in extra libraries.
 */
public final class Bg {

    private static final ExecutorService POOL = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Bg() {
    }

    public static void run(Runnable r) {
        POOL.execute(r);
    }

    public static void post(Runnable r) {
        MAIN.post(r);
    }
}
