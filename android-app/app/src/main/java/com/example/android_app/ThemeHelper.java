package com.example.android_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {

    public static final String PREF_NAME = "rig_ui_prefs";
    private static final String KEY_LIGHT_THEME = "use_light_theme";

    private ThemeHelper() {
    }

    public static boolean isLightTheme(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LIGHT_THEME, false);
    }

    public static void setLightTheme(Context context, boolean useLight) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LIGHT_THEME, useLight)
                .apply();
        applyNightMode(useLight);
    }

    /** Call from {@link android.app.Application#onCreate()} before any activity. */
    public static void applyPersistedNightMode(Context context) {
        applyNightMode(isLightTheme(context));
    }

    private static void applyNightMode(boolean useLight) {
        AppCompatDelegate.setDefaultNightMode(
                useLight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
        );
    }

    /** Restart app task so all activities pick up the new configuration. */
    public static void restartFromThemeChange(Activity activity) {
        Intent intent = new Intent(activity, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finishAffinity();
    }
}
