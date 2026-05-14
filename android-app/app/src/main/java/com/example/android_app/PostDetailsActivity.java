package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostDetailsActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_post_details);

        long postId = getIntent().getLongExtra(DiscoveryActivity.EXTRA_POST_ID, -1L);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.post_details_title);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        BottomNavHelper.attach(this, R.id.nav_social);

        TextView title = findViewById(R.id.post_detail_title);
        TextView meta = findViewById(R.id.post_detail_meta);
        TextView body = findViewById(R.id.post_detail_body);

        if (postId < 0) {
            title.setText(R.string.post_details_title);
            body.setText(R.string.discovery_empty);
            return;
        }

        String apiBase = getString(R.string.web_start_url).replace("5173/", "8080").replaceAll("/+$", "");
        String tok = null;
        try {
            tok = new AuthManager(this, apiBase).getToken();
        } catch (Exception ignored) {
        }
        final String authToken = tok;

        new Thread(() -> {
            try {
                Request.Builder rb = new Request.Builder()
                        .url(apiBase + "/community/posts/" + postId)
                        .get();
                if (!TextUtils.isEmpty(authToken)) {
                    rb.header("Authorization", "Bearer " + authToken);
                }
                try (Response r = client.newCall(rb.build()).execute()) {
                    String text = r.body() != null ? r.body().string() : "{}";
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("HTTP " + r.code());
                    }
                    JSONObject o = new JSONObject(text);
                    String t = o.optString("title", "");
                    String b = o.optString("body", "");
                    String author = o.optString("authorUserId", "");
                    int score = o.optInt("score", 0);
                    runOnUiThread(() -> {
                        title.setText(t);
                        body.setText(b);
                        meta.setText(String.format(java.util.Locale.US, "%s · score %d", author, score));
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(PostDetailsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_social);
    }
}
