package com.twotoasters.chron;

import android.app.Application;

import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class ChronApp extends Application {

    protected static ChronApp instance;

    public static ChronApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }
    }
}
