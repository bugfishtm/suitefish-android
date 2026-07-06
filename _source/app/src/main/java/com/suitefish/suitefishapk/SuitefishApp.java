package com.suitefish.suitefishapk;

import android.app.Application;

import com.suitefish.suitefishapk.net.Http;
import com.suitefish.suitefishapk.util.Prefs;

/**
 * Application entry point. Applies the persisted TLS setting before any network call.
 */
public class SuitefishApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Http.setAllowInsecureTls(new Prefs(this).isAllowInsecureTls());
    }
}
