package com.example.android_app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthManager {
    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "auth_username";
    private static final String KEY_EMAIL = "auth_email";
    private final SharedPreferences prefs;
    private final OkHttpClient client = new OkHttpClient();
    private final String apiBase;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface Callback {
        void onSuccess(String token);

        void onError(String message);
    }

    public AuthManager(Context ctx, String apiBaseUrl) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        this.prefs = EncryptedSharedPreferences.create(ctx, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        this.apiBase = apiBaseUrl.replaceAll("/+$", "");
    }

    @Nullable
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USERNAME).remove(KEY_EMAIL).apply();
    }

    @Nullable
    public String getStoredUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    @Nullable
    public String getStoredEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    private void persistProfileFields(JSONObject data) {
        if (data == null) {
            return;
        }
        String username = data.optString("username", "").trim();
        String email = data.optString("email", "").trim();
        android.content.SharedPreferences.Editor ed = prefs.edit();
        if (!username.isEmpty()) {
            ed.putString(KEY_USERNAME, username);
        }
        if (!email.isEmpty()) {
            ed.putString(KEY_EMAIL, email);
        }
        ed.apply();
    }

    public void login(final String email, final String password, final Callback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                Request request = new Request.Builder()
                        .url(apiBase + "/auth/login")
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response res = client.newCall(request).execute()) {
                    String text = res.body() != null ? res.body().string() : "";
                    if (res.code() == 202) {
                        cb.onError("2FA required — finish sign-in on web");
                        return;
                    }
                    if (res.isSuccessful()) {
                        JSONObject data = text.isEmpty() ? new JSONObject() : new JSONObject(text);
                        String token = data.optString("token", null);
                        if (token == null || token.isEmpty()) {
                            cb.onError("Login succeeded but no token returned");
                        } else {
                            saveToken(token);
                            persistProfileFields(data);
                            cb.onSuccess(token);
                        }
                    } else {
                        String msg = "Login failed: " + res.code();
                        try {
                            JSONObject err = text.isEmpty() ? null : new JSONObject(text);
                            if (err != null && err.has("message")) {
                                msg = err.getString("message");
                            }
                        } catch (Exception ignored) {
                        }
                        cb.onError(msg);
                    }
                }
            } catch (IOException | RuntimeException e) {
                cb.onError(e.getMessage());
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    public void register(final String username, final String email, final String password, final Callback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("email", email);
                body.put("password", password);

                Request request = new Request.Builder()
                        .url(apiBase + "/auth/register")
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response res = client.newCall(request).execute()) {
                    String text = res.body() != null ? res.body().string() : "";
                    if (res.isSuccessful()) {
                        JSONObject data = text.isEmpty() ? new JSONObject() : new JSONObject(text);
                        String token = data.optString("token", null);
                        if (token == null || token.isEmpty()) {
                            cb.onError("Register succeeded but no token returned");
                        } else {
                            saveToken(token);
                            persistProfileFields(data);
                            cb.onSuccess(token);
                        }
                    } else {
                        String msg = "Register failed: " + res.code();
                        try {
                            JSONObject err = text.isEmpty() ? null : new JSONObject(text);
                            if (err != null && err.has("message")) {
                                msg = err.getString("message");
                            }
                        } catch (Exception ignored) {
                        }
                        cb.onError(msg);
                    }
                }
            } catch (IOException | RuntimeException e) {
                cb.onError(e.getMessage());
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }
}
