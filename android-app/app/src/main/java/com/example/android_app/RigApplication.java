package com.example.android_app;

import android.app.Application;

public class RigApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyPersistedNightMode(this);
    }
}
