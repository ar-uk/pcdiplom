package com.example.android_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DiscoveryActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "postId";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    private MaterialToolbar toolbar;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private TextView errorView;
    private android.widget.LinearLayout postsContainer;
    private ChipGroup tagGroup;
    private TextInputEditText searchInput;

    private String apiBase;
    private String token;
    private String currentUserId;

    private String sortParam = "new";
    private String tagSlugFilter;

    private final List<JSONObject> lastLoaded = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_discovery);

        apiBase = getString(R.string.web_start_url).replace("5173/", "8080").replaceAll("/+$", "");

        try {
            AuthManager mgr = new AuthManager(this, apiBase);
            token = mgr.getToken();
            currentUserId = UserIdHelper.canonicalUserId(mgr, token);
            if (currentUserId.isEmpty()) {
                currentUserId = null;
            }
        } catch (Exception e) {
            token = null;
            currentUserId = null;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_social);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.inflateMenu(R.menu.discovery_toolbar);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenu);
        MenuItem newPost = toolbar.getMenu().findItem(R.id.action_new_post);
        if (newPost != null) {
            newPost.setVisible(!TextUtils.isEmpty(token));
        }

        swipeRefresh = findViewById(R.id.discovery_refresh);
        errorView = findViewById(R.id.discovery_error);
        postsContainer = findViewById(R.id.posts_container);
        tagGroup = findViewById(R.id.discovery_tag_group);
        searchInput = findViewById(R.id.discovery_search);

        swipeRefresh.setOnRefreshListener(this::loadPosts);

        findViewById(R.id.sort_new).setOnClickListener(v -> {
            sortParam = "new";
            loadPosts();
        });
        findViewById(R.id.sort_hot).setOnClickListener(v -> {
            sortParam = "hot";
            loadPosts();
        });

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                renderPosts(lastLoaded);
            }
        });

        BottomNavHelper.attach(this, R.id.nav_social);

        loadTagsThenPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_social);
    }

    private boolean onToolbarMenu(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadPosts();
            return true;
        }
        if (id == R.id.action_new_post) {
            showCreateDialog();
            return true;
        }
        return false;
    }

    private void loadTagsThenPosts() {
        swipeRefresh.setRefreshing(true);
        new Thread(() -> {
            try {
                String url = apiBase + "/community/tags";
                Request.Builder rb = new Request.Builder().url(url).get();
                addAuth(rb);
                try (Response r = client.newCall(rb.build()).execute()) {
                    String text = r.body() != null ? r.body().string() : "[]";
                    JSONArray arr = new JSONArray(text.trim().startsWith("[") ? text : "[]");
                    runOnUiThread(() -> {
                        buildTagChips(arr);
                        loadPosts();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    buildTagChips(new JSONArray());
                    loadPosts();
                });
            }
        }).start();
    }

    private void buildTagChips(JSONArray tagsFromApi) {
        tagGroup.removeAllViews();
        Chip all = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        all.setText(R.string.discovery_filter_all);
        all.setCheckable(true);
        all.setChecked(true);
        all.setTag("");
        all.setOnClickListener(v -> {
            tagSlugFilter = null;
            clearTagChecksExcept(all);
            all.setChecked(true);
            loadPosts();
        });
        tagGroup.addView(all);

        for (int i = 0; i < tagsFromApi.length(); i++) {
            JSONObject t = tagsFromApi.optJSONObject(i);
            if (t == null) {
                continue;
            }
            String slug = t.optString("slug", "");
            String label = t.optString("displayName", slug);
            if (slug.isEmpty()) {
                continue;
            }
            Chip c = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            c.setText(label);
            c.setCheckable(true);
            c.setTag(slug);
            final Chip self = c;
            c.setOnClickListener(v -> {
                tagSlugFilter = slug;
                clearTagChecksExcept(self);
                self.setChecked(true);
                loadPosts();
            });
            tagGroup.addView(c);
        }
    }

    private void clearTagChecksExcept(Chip keep) {
        for (int i = 0; i < tagGroup.getChildCount(); i++) {
            View v = tagGroup.getChildAt(i);
            if (v instanceof Chip && v != keep) {
                ((Chip) v).setChecked(false);
            }
        }
    }

    private void loadPosts() {
        runOnUiThread(() -> swipeRefresh.setRefreshing(true));
        new Thread(() -> {
            try {
                Uri.Builder ub = Uri.parse(apiBase + "/community/posts").buildUpon();
                ub.appendQueryParameter("sort", sortParam);
                if (!TextUtils.isEmpty(tagSlugFilter)) {
                    ub.appendQueryParameter("tag", tagSlugFilter);
                }
                Request.Builder rb = new Request.Builder().url(ub.build().toString()).get();
                addAuth(rb);
                try (Response r = client.newCall(rb.build()).execute()) {
                    String text = r.body() != null ? r.body().string() : "[]";
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("HTTP " + r.code() + ": " + text);
                    }
                    JSONArray arr = new JSONArray(text.trim().startsWith("[") ? text : "[]");
                    lastLoaded.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null) {
                            lastLoaded.add(o);
                        }
                    }
                    runOnUiThread(() -> {
                        errorView.setVisibility(View.GONE);
                        renderPosts(lastLoaded);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    errorView.setText(e.getMessage());
                    errorView.setVisibility(View.VISIBLE);
                    postsContainer.removeAllViews();
                });
            } finally {
                runOnUiThread(() -> swipeRefresh.setRefreshing(false));
            }
        }).start();
    }

    private void renderPosts(List<JSONObject> posts) {
        String q = searchInput.getText() != null ? searchInput.getText().toString().trim().toLowerCase(Locale.US) : "";
        postsContainer.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        int shown = 0;
        for (JSONObject o : posts) {
            String title = o.optString("title", "");
            String body = o.optString("body", "");
            if (!q.isEmpty()) {
                if (!title.toLowerCase(Locale.US).contains(q) && !body.toLowerCase(Locale.US).contains(q)) {
                    continue;
                }
            }
            View row = inf.inflate(R.layout.item_discovery_post, postsContainer, false);
            TextView tTitle = row.findViewById(R.id.post_title);
            TextView tBody = row.findViewById(R.id.post_body);
            TextView tMeta = row.findViewById(R.id.post_meta);
            View manage = row.findViewById(R.id.post_manage);

            tTitle.setText(title);
            tBody.setText(body);
            int score = o.optInt("score", 0);
            int comments = o.optInt("commentCount", 0);
            String author = o.optString("authorUserId", "?");
            tMeta.setText(String.format(Locale.US, "%s · %d · %d comments", author, score, comments));

            long postId = o.optLong("id", -1);
            boolean owner = currentUserId != null && currentUserId.equals(normalizeUserId(author));
            manage.setVisibility(owner ? View.VISIBLE : View.GONE);
            if (owner) {
                manage.setOnClickListener(v -> showManageMenu(v, postId, title, body));
            }

            row.setOnClickListener(v -> openPost(postId));
            postsContainer.addView(row);
            shown++;
        }
        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.discovery_empty);
            empty.setTextSize(15f);
            empty.setTextColor(getColor(R.color.on_surface_variant));
            postsContainer.addView(empty);
        }
    }

    private void openPost(long postId) {
        if (postId < 0) {
            return;
        }
        Intent i = new Intent(this, PostDetailsActivity.class);
        i.putExtra(EXTRA_POST_ID, postId);
        startActivity(i);
    }

    private void showManageMenu(View anchor, long postId, String title, String body) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, R.string.discovery_edit_post);
        pm.getMenu().add(0, 2, 0, R.string.discovery_delete_post);
        pm.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showEditDialog(postId, title, body);
                return true;
            }
            if (item.getItemId() == 2) {
                confirmDelete(postId);
                return true;
            }
            return false;
        });
        pm.show();
    }

    private void showEditDialog(long postId, String title, String body) {
        TextInputLayout lt = new TextInputLayout(this);
        lt.setHint(getString(R.string.discovery_edit_title));
        TextInputEditText etTitle = new TextInputEditText(this);
        etTitle.setText(title);
        lt.addView(etTitle);
        TextInputLayout lb = new TextInputLayout(this);
        lb.setHint(getString(R.string.discovery_edit_body));
        TextInputEditText etBody = new TextInputEditText(this);
        etBody.setMinLines(4);
        etBody.setText(body);
        lb.addView(etBody);

        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        box.addView(lt);
        box.addView(lb);

        new AlertDialog.Builder(this)
                .setTitle(R.string.discovery_edit_post)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.discovery_save, (d, w) -> {
                    String nt = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String nb = etBody.getText() != null ? etBody.getText().toString().trim() : "";
                    if (nt.isEmpty() || nb.isEmpty()) {
                        Toast.makeText(this, R.string.discovery_edit_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    patchPost(postId, nt, nb);
                })
                .show();
    }

    private void patchPost(long postId, String title, String body) {
        new Thread(() -> {
            try {
                JSONObject patch = new JSONObject();
                patch.put("editorUserId", currentUserId);
                patch.put("title", title);
                patch.put("body", body);
                Request.Builder rb = new Request.Builder()
                        .url(apiBase + "/community/posts/" + postId)
                        .patch(RequestBody.create(patch.toString(), JSON));
                addAuth(rb);
                try (Response r = client.newCall(rb.build()).execute()) {
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("HTTP " + r.code());
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(DiscoveryActivity.this, R.string.discovery_saved, Toast.LENGTH_SHORT).show();
                        loadPosts();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(DiscoveryActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void confirmDelete(long postId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.discovery_delete_post)
                .setMessage(R.string.discovery_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> deletePost(postId))
                .show();
    }

    private void deletePost(long postId) {
        new Thread(() -> {
            try {
                String url = apiBase + "/community/posts/" + postId + "?userId=" + java.net.URLEncoder.encode(currentUserId, "UTF-8");
                Request.Builder rb = new Request.Builder().url(url).delete();
                addAuth(rb);
                try (Response r = client.newCall(rb.build()).execute()) {
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("HTTP " + r.code());
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(DiscoveryActivity.this, R.string.discovery_deleted, Toast.LENGTH_SHORT).show();
                        loadPosts();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(DiscoveryActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showCreateDialog() {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, R.string.discovery_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        TextInputLayout lt = new TextInputLayout(this);
        lt.setHint(getString(R.string.discovery_new_title));
        TextInputEditText etTitle = new TextInputEditText(this);
        lt.addView(etTitle);
        TextInputLayout lb = new TextInputLayout(this);
        lb.setHint(getString(R.string.discovery_new_body));
        TextInputEditText etBody = new TextInputEditText(this);
        etBody.setMinLines(4);
        lb.addView(etBody);

        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        box.addView(lt);
        box.addView(lb);

        new AlertDialog.Builder(this)
                .setTitle(R.string.discovery_new_post)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.discovery_publish, (d, w) -> {
                    String nt = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String nb = etBody.getText() != null ? etBody.getText().toString().trim() : "";
                    if (nt.isEmpty() || nb.isEmpty()) {
                        Toast.makeText(this, R.string.discovery_edit_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createPost(nt, nb);
                })
                .show();
    }

    private void createPost(String title, String body) {
        new Thread(() -> {
            try {
                JSONObject o = new JSONObject();
                o.put("authorUserId", currentUserId);
                o.put("title", title);
                o.put("body", body);
                Request.Builder rb = new Request.Builder()
                        .url(apiBase + "/community/posts")
                        .post(RequestBody.create(o.toString(), JSON));
                addAuth(rb);
                try (Response r = client.newCall(rb.build()).execute()) {
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("HTTP " + r.code());
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(DiscoveryActivity.this, R.string.discovery_created, Toast.LENGTH_SHORT).show();
                        loadPosts();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(DiscoveryActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addAuth(Request.Builder rb) {
        if (!TextUtils.isEmpty(token)) {
            rb.header("Authorization", "Bearer " + token);
        }
    }

    private static String normalizeUserId(String userId) {
        return userId == null ? "" : userId.trim().toLowerCase(Locale.US);
    }
}
