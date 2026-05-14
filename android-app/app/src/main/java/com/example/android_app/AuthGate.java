package com.example.android_app;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

/**
 * Blocks UI when no JWT. Needed because Android can restore any activity in task (not always Home).
 */
public final class AuthGate {

    private AuthGate() {
    }

    /**
     * @return false if redirected to AuthActivity (caller must return from onCreate without drawing UI).
     */
    public static boolean ensureSignedIn(Activity activity) {
        String apiBase = activity.getString(R.string.web_start_url).replace("5173/", "8080");
        try {
            AuthManager mgr = new AuthManager(activity, apiBase);
            if (!TextUtils.isEmpty(mgr.getToken())) {
                return true;
            }
        } catch (Exception ignored) {
        }
        Intent i = new Intent(activity, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
        return false;
    }
}
