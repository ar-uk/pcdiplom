package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    /** @deprecated Use {@link UserIdHelper#decodeJwtSub(String)}. */
    @Deprecated
    public static String decodeEmailFromJwt(String tok) {
        return UserIdHelper.decodeJwtSub(tok);
    }

    private LinearLayout buildsContainer;
    private String apiBase;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_profile);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        buildsContainer = findViewById(R.id.builds_container);
        apiBase = getString(R.string.web_start_url).replace("5173/", "8080");

        BottomNavHelper.attach(this, R.id.nav_profile);

        TextView handleView = findViewById(R.id.profile_handle);
        TextView usernameView = findViewById(R.id.profile_username);
        TextView emailView = findViewById(R.id.profile_email);

        try {
            AuthManager mgr = new AuthManager(this, apiBase);
            String token = mgr.getToken();
            if (TextUtils.isEmpty(token)) {
                Toast.makeText(this, R.string.profile_no_email, Toast.LENGTH_SHORT).show();
                return;
            }

            String storedUser = mgr.getStoredUsername();
            String storedMail = mgr.getStoredEmail();
            String sub = UserIdHelper.decodeJwtSub(token);

            String showUser = !TextUtils.isEmpty(storedUser) ? storedUser : sub;
            String showMail = !TextUtils.isEmpty(storedMail) ? storedMail : null;
            if (showMail == null && sub != null && sub.contains("@")) {
                showMail = sub;
            }

            if (!TextUtils.isEmpty(showUser)) {
                handleView.setText(String.format(Locale.US, "@%s", showUser.replaceFirst("^@", "")));
            } else {
                handleView.setVisibility(View.GONE);
            }

            usernameView.setText(!TextUtils.isEmpty(storedUser) ? storedUser : (sub != null && !sub.contains("@") ? sub : "—"));
            emailView.setText(!TextUtils.isEmpty(storedMail) ? storedMail : (showMail != null ? showMail : "—"));

            List<String> ids = UserIdHelper.userIdCandidates(mgr, token);
            if (ids.isEmpty()) {
                Toast.makeText(this, R.string.profile_no_identity, Toast.LENGTH_LONG).show();
                return;
            }
            fetchBuilds(ids, token);
        } catch (Exception e) {
            Toast.makeText(this, "Profile init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            wireProfileSettingsButton();
        }
    }

    private void wireProfileSettingsButton() {
        View settingsBtn = findViewById(R.id.profile_open_settings);
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> openProfileSettings());
        }
    }

    private void fetchBuilds(final List<String> userIds, final String token) {
        buildsContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Loading builds...");
        loading.setTextColor(getColor(R.color.text_secondary));
        buildsContainer.addView(loading);

        new Thread(() -> {
            try {
                Map<Long, JSONObject> byId = new HashMap<>();
                for (String userId : userIds) {
                    String url = apiBase + "/api/recommendation/manual-builds?userId=" + java.net.URLEncoder.encode(userId, "UTF-8");
                    Request.Builder rb = new Request.Builder().url(url).get();
                    if (!TextUtils.isEmpty(token)) {
                        rb.header("Authorization", "Bearer " + token);
                    }

                    try (Response r = client.newCall(rb.build()).execute()) {
                        if (!r.isSuccessful()) {
                            continue;
                        }
                        String text = r.body() != null ? r.body().string() : "";
                        JSONArray arr = text.trim().startsWith("[") ? new JSONArray(text) : new JSONArray();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            if (obj == null) {
                                continue;
                            }
                            long id = obj.optLong("id", -1L);
                            if (id >= 0L) {
                                byId.put(id, obj);
                            }
                        }
                    }
                }

                List<Long> keys = new ArrayList<>(byId.keySet());
                Collections.sort(keys, Collections.reverseOrder());
                JSONArray merged = new JSONArray();
                for (Long k : keys) {
                    merged.put(byId.get(k));
                }

                runOnUiThread(() -> showBuilds(merged));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Could not load builds: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_profile);
        wireProfileSettingsButton();
    }

    private void openProfileSettings() {
        startActivity(new Intent(ProfileActivity.this, ProfileSettingsActivity.class));
    }

    private void showBuilds(JSONArray arr) {
        buildsContainer.removeAllViews();
        if (arr.length() == 0) {
            TextView t = new TextView(this);
            t.setText("No builds found");
            t.setTextColor(getColor(R.color.text_secondary));
            buildsContainer.addView(t);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            String title = obj.optString("title", "Untitled");
            String id = String.valueOf(obj.opt("id"));

            View row = inflater.inflate(R.layout.item_profile_build, buildsContainer, false);
            TextView titleView = row.findViewById(R.id.build_title);
            TextView idView = row.findViewById(R.id.build_id);
            titleView.setText(title);
            idView.setText(String.format(Locale.US, "ID %s", id));
            buildsContainer.addView(row);
        }
    }
}
