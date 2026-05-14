package com.example.android_app;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Resolves stable user ids for API calls. Auth JWT {@code sub} is the account username;
 * legacy tokens may use email as {@code sub}. Stored username/email from login/register
 * are used to merge manual-build lists and for display.
 */
public final class UserIdHelper {

    private UserIdHelper() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.US);
    }

    /**
     * Reads JWT payload {@code sub} (preferred), then optional {@code email} claim.
     */
    public static String decodeJwtSub(String token) {
        try {
            if (token == null) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod > 0) {
                StringBuilder sb = new StringBuilder(payload);
                for (int i = 0; i < 4 - mod; i++) {
                    sb.append("=");
                }
                payload = sb.toString();
            }
            byte[] decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
            String json = new String(decoded);
            JSONObject obj = new JSONObject(json);
            if (obj.has("sub")) {
                return obj.optString("sub", null);
            }
            if (obj.has("email")) {
                return obj.optString("email", null);
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Distinct ids to query for manual builds (username-keyed and legacy email-keyed rows).
     */
    public static List<String> userIdCandidates(AuthManager mgr, String token) throws Exception {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        addIfPresent(set, mgr.getStoredUsername());
        addIfPresent(set, mgr.getStoredEmail());
        addIfPresent(set, decodeJwtSub(token));
        return new ArrayList<>(set);
    }

    /**
     * Primary id for new writes: stored username, else JWT sub, else stored email.
     */
    public static String canonicalUserId(AuthManager mgr, String token) throws Exception {
        String u = normalize(mgr.getStoredUsername());
        if (!u.isEmpty()) {
            return u;
        }
        String sub = normalize(decodeJwtSub(token));
        if (!sub.isEmpty()) {
            return sub;
        }
        return normalize(mgr.getStoredEmail());
    }

    private static void addIfPresent(LinkedHashSet<String> set, String raw) {
        String n = normalize(raw);
        if (!n.isEmpty()) {
            set.add(n);
        }
    }
}
