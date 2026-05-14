package com.example.android_app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Bottom tabs for the main shell (Home / Builds / AI / Social / Profile).
 */
public final class BottomNavHelper {

    private BottomNavHelper() {
    }

    public static void attach(@NonNull final Activity activity, int checkedMenuId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) {
            return;
        }
        ColorStateList indicator = ColorStateList.valueOf(
                ContextCompat.getColor(activity, R.color.bottom_nav_active_indicator));
        ColorStateList ripple = ColorStateList.valueOf(
                ContextCompat.getColor(activity, R.color.bottom_nav_ripple));
        nav.setItemActiveIndicatorColor(indicator);
        nav.setItemRippleColor(ripple);
        nav.setSaveEnabled(false);
        // Programmatic selection must not fire OnItemSelectedListener, or it would start
        // e.g. ProfileActivity from ProfileSettingsActivity and hide the screen we just opened.
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(checkedMenuId);
        nav.setOnItemSelectedListener(navListener(activity));
    }

    /**
     * Re-apply the selected tab when the activity resumes so it always matches this screen.
     */
    public static void syncSelection(@NonNull Activity activity, int checkedMenuId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) {
            return;
        }
        if (nav.getSelectedItemId() == checkedMenuId) {
            return;
        }
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(checkedMenuId);
        nav.setOnItemSelectedListener(navListener(activity));
    }

    private static NavigationBarView.OnItemSelectedListener navListener(@NonNull final Activity activity) {
        return item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                launch(activity, HomeActivity.class);
                return true;
            }
            if (id == R.id.nav_builds) {
                launch(activity, BuilderActivity.class);
                return true;
            }
            if (id == R.id.nav_ai) {
                launch(activity, AiBuilderActivity.class);
                return true;
            }
            if (id == R.id.nav_social) {
                launch(activity, DiscoveryActivity.class);
                return true;
            }
            if (id == R.id.nav_profile) {
                launch(activity, ProfileActivity.class);
                return true;
            }
            return false;
        };
    }

    private static void launch(Activity from, Class<? extends Activity> dest) {
        if (dest.isInstance(from)) {
            return;
        }
        Intent i = new Intent(from, dest);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        from.startActivity(i);
    }
}
