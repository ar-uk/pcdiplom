package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ProfileSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_profile_settings);

        String apiBase = getString(R.string.web_start_url).replace("5173/", "8080");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        BottomNavHelper.attach(this, R.id.nav_profile);

        try {
            AuthManager mgr = new AuthManager(this, apiBase);
            String u = mgr.getStoredUsername();
            String e = mgr.getStoredEmail();
            android.widget.TextView usernameField = findViewById(R.id.settings_account_username);
            android.widget.TextView emailField = findViewById(R.id.settings_account_email);
            if (!TextUtils.isEmpty(u)) {
                usernameField.setText(u);
            } else {
                String sub = UserIdHelper.decodeJwtSub(mgr.getToken());
                usernameField.setText(!TextUtils.isEmpty(sub) && !sub.contains("@") ? sub : "—");
            }
            if (!TextUtils.isEmpty(e)) {
                emailField.setText(e);
            } else {
                String sub = UserIdHelper.decodeJwtSub(mgr.getToken());
                emailField.setText(sub != null && sub.contains("@") ? sub : "—");
            }
        } catch (Exception ignored) {
        }

        MaterialSwitch lightThemeSwitch = findViewById(R.id.settings_theme_light);
        lightThemeSwitch.setChecked(ThemeHelper.isLightTheme(this));
        lightThemeSwitch.setOnClickListener(v -> {
            boolean useLight = lightThemeSwitch.isChecked();
            ThemeHelper.setLightTheme(ProfileSettingsActivity.this, useLight);
            ThemeHelper.restartFromThemeChange(ProfileSettingsActivity.this);
        });

        MaterialButton signOut = findViewById(R.id.settings_sign_out);
        signOut.setOnClickListener(v -> {
            try {
                new AuthManager(ProfileSettingsActivity.this, apiBase).clearToken();
            } catch (Exception ignored) {
            }
            Intent i = new Intent(ProfileSettingsActivity.this, AuthActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finishAffinity();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_profile);
    }
}
