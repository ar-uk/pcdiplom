package com.example.android_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BuilderActivity extends AppCompatActivity {

    private static final String[][] CATEGORIES = new String[][]{
            {"cpu", "CPU", "/api/parsed/cpu"},
            {"gpu", "GPU", "/api/parsed/video-card"},
            {"motherboard", "Motherboard", "/api/parsed/motherboard"},
            {"psu", "PSU", "/api/parsed/power-supply"},
            {"storage", "Storage", "/api/parsed/internal-hard-drive"},
            {"ram", "RAM", "/api/parsed/memory"},
            {"case", "Case", "/api/parsed/pc-case"},
            {"cooling", "Cooling", "/api/parsed/cpu-cooler"}
    };

    private static final int CATALOG_PAGE_SIZE = 24;

    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, List<Part>> partsByCategory = new HashMap<>();
    private final Map<String, Part> selectedParts = new HashMap<>();

    private LinearLayout partsContainer;
    private LinearLayout categoryButtons;
    private LinearLayout selectedList;
    private LinearLayout partsPagination;
    private TextInputEditText buildName;
    private TextInputEditText searchInput;
    private MaterialButton saveBtn;
    private MaterialButton toggleOrder;
    private MaterialButton pagePrev;
    private MaterialButton pageNext;
    private TextView pageMeta;
    private TextView compatHint;
    private TextView totalEstimate;
    private ProgressBar selectionProgressBar;
    private TextView selectionProgressLabel;
    private TextView selectionPercentLabel;

    private String apiBase;
    private String token;
    private String activeCategoryKey = "cpu";
    private boolean priceAscending = true;
    private int catalogPage = 0;

    static class Part {
        String id;
        String name;
        double price;
        String category;
        String socket;
        String ramType;
        String formFactor;
        Double wattage;
        String retailer;
        List<String> supportedSockets = new ArrayList<>();
        JSONObject raw;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.ensureSignedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_builder);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.builder_title);
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        partsContainer = findViewById(R.id.parts_container);
        categoryButtons = findViewById(R.id.category_buttons);
        selectedList = findViewById(R.id.selected_list);
        buildName = findViewById(R.id.build_name);
        searchInput = findViewById(R.id.search_input);
        saveBtn = findViewById(R.id.save_build);
        toggleOrder = findViewById(R.id.toggle_order);
        partsPagination = findViewById(R.id.parts_pagination);
        pagePrev = findViewById(R.id.parts_page_prev);
        pageNext = findViewById(R.id.parts_page_next);
        pageMeta = findViewById(R.id.parts_page_meta);
        compatHint = findViewById(R.id.parts_compat_hint);
        totalEstimate = findViewById(R.id.total_estimate);
        selectionProgressBar = findViewById(R.id.selection_progress_bar);
        selectionProgressLabel = findViewById(R.id.selection_progress_label);
        selectionPercentLabel = findViewById(R.id.selection_percent_label);

        apiBase = getString(R.string.web_start_url).replace("5173/", "8080");

        try {
            AuthManager mgr = new AuthManager(this, apiBase);
            token = mgr.getToken();
        } catch (Exception e) {
            token = null;
        }

        saveBtn.setEnabled(false);
        updatePriceToggleLabel();

        setupCategoryButtons();
        loadCategoryParts(CATEGORIES[0][0]);
        refreshSelectedList();

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    catalogPage = 0;
                    refreshCatalogUi();
                }
            });
        }

        if (toggleOrder != null) {
            toggleOrder.setOnClickListener(v -> {
                priceAscending = !priceAscending;
                catalogPage = 0;
                updatePriceToggleLabel();
                refreshCatalogUi();
            });
        }

        if (pagePrev != null) {
            pagePrev.setOnClickListener(v -> {
                if (catalogPage > 0) {
                    catalogPage--;
                    refreshCatalogUi();
                }
            });
        }
        if (pageNext != null) {
            pageNext.setOnClickListener(v -> {
                catalogPage++;
                refreshCatalogUi();
            });
        }

        saveBtn.setOnClickListener(v -> saveBuild());

        if (buildName != null) {
            buildName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateSaveButtonState();
                }
            });
        }

        BottomNavHelper.attach(this, R.id.nav_builds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.syncSelection(this, R.id.nav_builds);
    }

    private void updatePriceToggleLabel() {
        if (toggleOrder != null) {
            toggleOrder.setText(priceAscending ? getString(R.string.builder_price_asc) : getString(R.string.builder_price_desc));
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void setupCategoryButtons() {
        categoryButtons.removeAllViews();
        final int outStyle = com.google.android.material.R.attr.materialButtonOutlinedStyle;
        for (final String[] cat : CATEGORIES) {
            MaterialButton b = new MaterialButton(this, null, outStyle);
            b.setTag(cat[0]);
            b.setText(cat[1]);
            b.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> loadCategoryParts(cat[0]));
            categoryButtons.addView(b);
        }
        updateCategoryButtonStyles();
    }

    private void updateCategoryButtonStyles() {
        for (int i = 0; i < categoryButtons.getChildCount(); i++) {
            View child = categoryButtons.getChildAt(i);
            if (!(child instanceof MaterialButton)) {
                continue;
            }
            MaterialButton b = (MaterialButton) child;
            boolean active = Objects.equals(b.getTag(), activeCategoryKey);
            b.setAlpha(active ? 1f : 0.62f);
        }
    }

    private void loadCategoryParts(final String categoryKey) {
        activeCategoryKey = categoryKey;
        catalogPage = 0;
        if (searchInput != null && searchInput.getText() != null) {
            searchInput.setText("");
        }
        updateCategoryButtonStyles();

        partsContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Loading parts...");
        loading.setTextSize(16f);
        loading.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        partsContainer.addView(loading);

        if (partsPagination != null) {
            partsPagination.setVisibility(View.GONE);
        }
        if (compatHint != null) {
            compatHint.setVisibility(View.GONE);
        }

        new Thread(() -> {
            try {
                String endpoint = null;
                for (String[] c : CATEGORIES) {
                    if (c[0].equals(categoryKey)) {
                        endpoint = c[2];
                    }
                }
                if (endpoint == null) {
                    return;
                }

                String url = apiBase + endpoint + "?page=0&size=200";
                Request.Builder reqb = new Request.Builder().url(url);
                if (!TextUtils.isEmpty(token)) {
                    reqb.header("Authorization", "Bearer " + token);
                }

                try (Response res = client.newCall(reqb.build()).execute()) {
                    if (!res.isSuccessful()) {
                        throw new RuntimeException("Fetch failed: " + res.code());
                    }
                    String text = res.body() != null ? res.body().string() : "";
                    JSONArray arr;
                    if (text.trim().startsWith("{")) {
                        JSONObject top = new JSONObject(text);
                        if (top.has("content") && top.get("content") instanceof JSONArray) {
                            arr = top.getJSONArray("content");
                        } else {
                            arr = new JSONArray();
                        }
                    } else if (text.trim().startsWith("[")) {
                        arr = new JSONArray(text);
                    } else {
                        arr = new JSONArray();
                    }

                    final List<Part> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject r = arr.getJSONObject(i);
                        list.add(partFromJson(r, categoryKey, i));
                    }

                    partsByCategory.put(categoryKey, list);

                    runOnUiThread(() -> refreshCatalogUi());
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    partsContainer.removeAllViews();
                    TextView t = new TextView(BuilderActivity.this);
                    t.setText("Could not load parts: " + e.getMessage());
                    t.setTextColor(ContextCompat.getColor(BuilderActivity.this, R.color.error));
                    partsContainer.addView(t);
                });
            }
        }).start();
    }

    private static String extractDdrFromText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.toUpperCase(Locale.US).replaceAll("[\\s_-]+", "");
        if (normalized.contains("DDR5") || normalized.contains("D5")) {
            return "DDR5";
        }
        if (normalized.contains("DDR4") || normalized.contains("D4")) {
            return "DDR4";
        }
        if (normalized.contains("DDR3") || normalized.contains("D3")) {
            return "DDR3";
        }
        if (normalized.contains("DDR2") || normalized.contains("D2")) {
            return "DDR2";
        }
        return null;
    }

    private static String inferSocketFromName(String name) {
        if (name == null) {
            return null;
        }
        String compact = name.toUpperCase(Locale.US).replaceAll("\\s+", "");
        if (compact.contains("AM5")) {
            return "AM5";
        }
        if (compact.contains("AM4")) {
            return "AM4";
        }
        if (compact.contains("LGA1851")) {
            return "LGA1851";
        }
        if (compact.contains("LGA1700")) {
            return "LGA1700";
        }
        if (compact.contains("LGA1200")) {
            return "LGA1200";
        }
        if (compact.contains("LGA1151")) {
            return "LGA1151";
        }
        return null;
    }

    private static String inferFormFactorFromName(String name) {
        if (name == null) {
            return null;
        }
        String upper = name.toUpperCase(Locale.US);
        if (upper.contains("MINI-ITX") || upper.contains("MINI ITX") || upper.contains("ITX")) {
            return "ITX";
        }
        if (upper.contains("M-ATX") || upper.contains("MATX") || upper.contains("MICRO-ATX") || upper.contains("MICRO ATX")) {
            return "mATX";
        }
        if (upper.contains("ATX")) {
            return "ATX";
        }
        return null;
    }

    private static Double optWattage(JSONObject r) {
        String[] keys = {"wattage", "tdp", "tdpWatts", "powerHint", "power_hint"};
        for (String k : keys) {
            if (r.has(k) && !r.isNull(k)) {
                try {
                    return r.getDouble(k);
                } catch (Exception ignored) {
                    try {
                        return Double.parseDouble(r.optString(k, ""));
                    } catch (Exception ignored2) {
                    }
                }
            }
        }
        return null;
    }

    /** Same display as web selected panel: {@code 105W}. */
    private static String formatWattTdpLabel(Double watts) {
        if (watts == null || watts <= 0) {
            return null;
        }
        return String.format(Locale.US, "%.0fW", watts);
    }

    private static String firstString(JSONObject r, String... keys) {
        for (String k : keys) {
            if (r.has(k) && !r.isNull(k)) {
                String s = r.optString(k, null);
                if (!TextUtils.isEmpty(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Spring/Jackson exposes {@code priceKzt}; SQL column is {@code price_kzt}. Web client checks both.
     */
    private static double readPriceKzt(JSONObject r) {
        String[] keys = {"priceKzt", "price_kzt", "priceKZT", "price"};
        for (String k : keys) {
            if (!r.has(k) || r.isNull(k)) {
                continue;
            }
            Object o = r.opt(k);
            if (o instanceof Number) {
                return ((Number) o).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(o));
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private static Part partFromJson(JSONObject r, String categoryKey, int index) {
        Part p = new Part();
        p.raw = r;
        p.id = !TextUtils.isEmpty(r.optString("id", "")) ? r.optString("id", "") : (categoryKey + "-" + index);
        p.name = r.optString("name", r.optString("title", "Unknown"));
        p.price = readPriceKzt(r);
        p.category = categoryKey;
        p.retailer = r.optString("retailer", "");

        String socket = firstString(r, "socket", "cpuSocket", "socket_type");
        String memoryType = firstString(r, "memoryType", "memoryRamType", "memory_type", "memory_ram_type", "ddr", "ddrType");
        p.wattage = optWattage(r);

        String inferredRam = extractDdrFromText(p.name);
        String inferredSocket = inferSocketFromName(p.name);
        String inferredFf = inferFormFactorFromName(p.name);

        boolean inferSocketForCategory = "cpu".equals(categoryKey) || "motherboard".equals(categoryKey) || "cooling".equals(categoryKey);
        p.socket = !TextUtils.isEmpty(socket) ? socket : (inferSocketForCategory ? inferredSocket : null);

        if ("ram".equals(categoryKey)) {
            if (inferredRam != null) {
                p.ramType = inferredRam;
            } else if (!TextUtils.isEmpty(memoryType)) {
                String mt = memoryType.toUpperCase(Locale.US);
                p.ramType = mt.startsWith("DDR") ? mt : ("DDR" + memoryType);
            }
        } else if (!TextUtils.isEmpty(memoryType)) {
            String mt = memoryType.toUpperCase(Locale.US);
            p.ramType = mt.startsWith("DDR") ? mt : ("DDR" + memoryType);
        }

        String ff = firstString(r, "formFactor", "form_factor");
        p.formFactor = !TextUtils.isEmpty(ff) ? ff : inferredFf;

        String compact = p.name.toUpperCase(Locale.US).replaceAll("\\s+", "");
        String[] candidates = {"AM4", "AM5", "LGA1200", "LGA1700", "LGA1851", "LGA1151"};
        for (String c : candidates) {
            if (compact.contains(c)) {
                p.supportedSockets.add(c);
            }
        }

        return p;
    }

    private static int getPowerBudget(Map<String, Part> selected) {
        Part cpu = selected.get("cpu");
        Part gpu = selected.get("gpu");
        double cpuPower = cpu != null && cpu.wattage != null && cpu.wattage > 0 ? cpu.wattage : 65;
        double gpuPower = gpu != null && gpu.wattage != null && gpu.wattage > 0 ? gpu.wattage : 200;
        return (int) Math.round(cpuPower + gpuPower + 150);
    }

    private static boolean socketsCompatible(String a, String b) {
        if (TextUtils.isEmpty(a) || TextUtils.isEmpty(b)) {
            return true;
        }
        return formatSocketLabel(a).equals(formatSocketLabel(b));
    }

    private static String formatSocketLabel(String socket) {
        return socket.replaceAll("\\s+", "").toUpperCase(Locale.US);
    }

    private static boolean isPartCompatible(Part part, Map<String, Part> selected) {
        Part cpu = selected.get("cpu");
        Part motherboard = selected.get("motherboard");
        Part ram = selected.get("ram");
        Part casePart = selected.get("case");
        int powerBudget = getPowerBudget(selected);

        if ("cpu".equals(part.category)) {
            return motherboard == null || TextUtils.isEmpty(motherboard.socket) || TextUtils.isEmpty(part.socket)
                    || socketsCompatible(motherboard.socket, part.socket);
        }

        if ("motherboard".equals(part.category)) {
            if (cpu != null && !TextUtils.isEmpty(cpu.socket) && !TextUtils.isEmpty(part.socket)
                    && !socketsCompatible(cpu.socket, part.socket)) {
                return false;
            }
            if (ram != null && !TextUtils.isEmpty(ram.ramType) && !TextUtils.isEmpty(part.ramType)
                    && !Objects.equals(ram.ramType, part.ramType)) {
                return false;
            }
            return true;
        }

        if ("ram".equals(part.category)) {
            return motherboard == null || TextUtils.isEmpty(motherboard.ramType) || TextUtils.isEmpty(part.ramType)
                    || Objects.equals(motherboard.ramType, part.ramType);
        }

        if ("psu".equals(part.category)) {
            return part.wattage == null || part.wattage <= 0 || part.wattage >= powerBudget;
        }

        if ("case".equals(part.category)) {
            if (motherboard == null || TextUtils.isEmpty(motherboard.formFactor) || TextUtils.isEmpty(part.formFactor)) {
                return true;
            }
            String mf = motherboard.formFactor;
            String cf = part.formFactor;
            if ("ATX".equals(cf)) {
                return "ATX".equals(mf) || "mATX".equals(mf) || "ITX".equals(mf);
            }
            if ("mATX".equals(cf)) {
                return "mATX".equals(mf) || "ITX".equals(mf);
            }
            if ("ITX".equals(cf)) {
                return "ITX".equals(mf);
            }
            return true;
        }

        if ("cooling".equals(part.category)) {
            if (cpu == null || TextUtils.isEmpty(cpu.socket)) {
                return true;
            }
            final String cpuSock = formatSocketLabel(cpu.socket);
            if (part.supportedSockets != null && !part.supportedSockets.isEmpty()) {
                for (String s : part.supportedSockets) {
                    if (formatSocketLabel(s).equals(cpuSock)) {
                        return true;
                    }
                }
                return false;
            }
            return TextUtils.isEmpty(part.socket) || socketsCompatible(part.socket, cpu.socket)
                    || part.name.toUpperCase(Locale.US).contains(cpuSock);
        }

        if ("gpu".equals(part.category)) {
            double w = part.wattage != null ? part.wattage : 0;
            if (casePart != null && "ITX".equals(casePart.formFactor) && w >= 320) {
                return false;
            }
        }

        return true;
    }

    private boolean isPickedForActiveCategory(Part part) {
        Part picked = selectedParts.get(activeCategoryKey);
        return picked != null && Objects.equals(picked.id, part.id);
    }

    private int countHiddenByCompat(List<Part> list) {
        if (list == null) {
            return 0;
        }
        int n = 0;
        for (Part part : list) {
            if (part.price <= 0) {
                continue;
            }
            if (isPickedForActiveCategory(part)) {
                continue;
            }
            if (!isPartCompatible(part, selectedParts)) {
                n++;
            }
        }
        return n;
    }

    private List<Part> buildFilteredCatalog() {
        List<Part> source = partsByCategory.get(activeCategoryKey);
        if (source == null) {
            return new ArrayList<>();
        }
        String q = "";
        if (searchInput != null && searchInput.getText() != null) {
            q = searchInput.getText().toString().trim().toLowerCase(Locale.US);
        }

        List<Part> out = new ArrayList<>();
        for (Part part : source) {
            if (part.price <= 0) {
                continue;
            }
            if (!isPickedForActiveCategory(part) && !isPartCompatible(part, selectedParts)) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean nameMatch = part.name.toLowerCase(Locale.US).contains(q);
                boolean retailerMatch = part.retailer != null && part.retailer.toLowerCase(Locale.US).contains(q);
                if (!nameMatch && !retailerMatch) {
                    continue;
                }
            }
            out.add(part);
        }

        Collections.sort(out, new Comparator<Part>() {
            @Override
            public int compare(Part a, Part b) {
                int cmp = Double.compare(a.price, b.price);
                if (cmp != 0) {
                    return priceAscending ? cmp : -cmp;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });

        return out;
    }

    /**
     * Catalog row: glass-style card, ~70% name / ~30% price (weights 7 : 3).
     */
    private View createPartCatalogRow(final Part p) {
        boolean picked = isPickedForActiveCategory(p);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setStrokeWidth(dp(picked ? 2 : 1));
        card.setStrokeColor(ContextCompat.getColor(this, picked ? R.color.primary_fixed_dim : R.color.outline_variant));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_low));
        card.setCardElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(14);
        int padV = dp(12);
        row.setPadding(padH, padV, padH, padV);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 7f);
        leftLp.setMarginEnd(dp(10));
        leftCol.setLayoutParams(leftLp);

        TextView nameTv = new TextView(this);
        nameTv.setText(p.name);
        nameTv.setMaxLines(3);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);
        nameTv.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        nameTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameTv.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics()), 1f);
        leftCol.addView(nameTv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (!TextUtils.isEmpty(p.retailer)) {
            TextView retailerTv = new TextView(this);
            retailerTv.setText(p.retailer);
            retailerTv.setMaxLines(1);
            retailerTv.setEllipsize(TextUtils.TruncateAt.END);
            retailerTv.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            retailerTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(4);
            leftCol.addView(retailerTv, rlp);
        }

        String wattLabel = formatWattTdpLabel(p.wattage);
        if (!TextUtils.isEmpty(wattLabel)) {
            TextView tdpTv = new TextView(this);
            tdpTv.setText(wattLabel);
            tdpTv.setTextColor(ContextCompat.getColor(this, R.color.primary_fixed_dim));
            tdpTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            tdpTv.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(4);
            leftCol.addView(tdpTv, tlp);
        }

        TextView priceTv = new TextView(this);
        String priceText = p.price > 0 ? String.format(Locale.US, "%.0f KZT", p.price) : getString(R.string.builder_part_no_price);
        priceTv.setText(priceText);
        priceTv.setMaxLines(3);
        priceTv.setEllipsize(TextUtils.TruncateAt.END);
        priceTv.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        priceTv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        priceTv.setTextColor(ContextCompat.getColor(this, R.color.primary_fixed));
        priceTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        priceTv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams priceLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f);
        priceTv.setLayoutParams(priceLp);

        row.addView(leftCol);
        row.addView(priceTv);
        card.addView(row);

        card.setOnClickListener(v -> {
            selectedParts.put(activeCategoryKey, p);
            refreshSelectedList();
            refreshCatalogUi();
        });

        return card;
    }

    private void refreshCatalogUi() {
        if (activeCategoryKey == null) {
            return;
        }

        List<Part> source = partsByCategory.get(activeCategoryKey);
        int hiddenCompat = countHiddenByCompat(source);
        if (compatHint != null) {
            if (hiddenCompat > 0) {
                compatHint.setVisibility(View.VISIBLE);
                compatHint.setText(getString(R.string.builder_compat_hidden, hiddenCompat));
            } else {
                compatHint.setVisibility(View.GONE);
            }
        }

        List<Part> filtered = buildFilteredCatalog();
        int total = filtered.size();
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) CATALOG_PAGE_SIZE));
        if (catalogPage >= pageCount) {
            catalogPage = pageCount - 1;
        }
        if (catalogPage < 0) {
            catalogPage = 0;
        }

        int start = catalogPage * CATALOG_PAGE_SIZE;
        int end = Math.min(total, start + CATALOG_PAGE_SIZE);
        List<Part> page = start < total ? filtered.subList(start, end) : Collections.emptyList();

        partsContainer.removeAllViews();

        if (source == null || source.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("No parts");
            t.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            partsContainer.addView(t);
            if (partsPagination != null) {
                partsPagination.setVisibility(View.GONE);
            }
            return;
        }

        if (total == 0) {
            TextView t = new TextView(this);
            t.setText("No parts match your search or filters.");
            t.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            partsContainer.addView(t);
            if (partsPagination != null) {
                partsPagination.setVisibility(View.GONE);
            }
            return;
        }

        for (final Part p : page) {
            partsContainer.addView(createPartCatalogRow(p));
        }

        if (partsPagination != null && pagePrev != null && pageNext != null && pageMeta != null) {
            if (total > CATALOG_PAGE_SIZE) {
                partsPagination.setVisibility(View.VISIBLE);
                pagePrev.setEnabled(catalogPage > 0);
                pageNext.setEnabled(catalogPage < pageCount - 1);
                int from = start + 1;
                int to = end;
                pageMeta.setText(getString(R.string.builder_page_meta, catalogPage + 1, pageCount, from, to, total));
            } else {
                partsPagination.setVisibility(View.GONE);
            }
        }
    }

    private boolean isEverySlotFilled() {
        for (String[] slot : CATEGORIES) {
            if (!selectedParts.containsKey(slot[0]) || selectedParts.get(slot[0]) == null) {
                return false;
            }
        }
        return true;
    }

    private void updateSaveButtonState() {
        String title = buildName.getText() != null ? buildName.getText().toString().trim() : "";
        boolean ready = isEverySlotFilled() && !TextUtils.isEmpty(title);
        saveBtn.setEnabled(ready);
        saveBtn.setAlpha(ready ? 1f : 0.45f);
    }

    private void refreshSelectedList() {
        selectedList.removeAllViews();
        double total = 0;
        int count = 0;
        for (String[] slot : CATEGORIES) {
            String key = slot[0];
            Part p = selectedParts.get(key);

            LinearLayout slotBlock = new LinearLayout(this);
            slotBlock.setOrientation(LinearLayout.VERTICAL);
            slotBlock.setPadding(0, dp(6), 0, dp(6));

            TextView row = new TextView(this);
            row.setText(slot[1].toUpperCase(Locale.US) + " · " + (p != null ? p.name : "—"));
            row.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            row.setTypeface(Typeface.MONOSPACE);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            slotBlock.addView(row);

            if (p != null) {
                String wl = formatWattTdpLabel(p.wattage);
                if (!TextUtils.isEmpty(wl)) {
                    TextView wRow = new TextView(this);
                    wRow.setText(wl);
                    wRow.setTextColor(ContextCompat.getColor(this, R.color.primary_fixed_dim));
                    wRow.setTypeface(Typeface.MONOSPACE);
                    wRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                    LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    wlp.topMargin = dp(2);
                    wRow.setLayoutParams(wlp);
                    slotBlock.addView(wRow);
                }
            }

            selectedList.addView(slotBlock);
            if (p != null) {
                count++;
                total += p.price;
            }
        }

        int totalSlots = CATEGORIES.length;
        int pct = totalSlots == 0 ? 0 : (int) Math.round(100.0 * count / totalSlots);
        if (totalEstimate != null) {
            totalEstimate.setText(count > 0 && total > 0 ? String.format(Locale.US, "%.0f KZT", total) : "—");
        }
        if (selectionProgressBar != null) {
            selectionProgressBar.setProgress(pct);
        }
        if (selectionProgressLabel != null) {
            selectionProgressLabel.setText(String.format(Locale.US, "%d / %d components", count, totalSlots));
        }
        if (selectionPercentLabel != null) {
            selectionPercentLabel.setText(String.format(Locale.US, "%d%%", pct));
        }

        updateSaveButtonState();
    }

    private void saveBuild() {
        final String title = buildName.getText() != null ? buildName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Enter a build name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEverySlotFilled()) {
            Toast.makeText(this, "Select all components first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveBtn.setEnabled(false);

        new Thread(() -> {
            try {
                String api = apiBase + "/api/recommendation/manual-builds/drafts";
                JSONObject payload = new JSONObject();
                String userId;
                try {
                    AuthManager mgr = new AuthManager(BuilderActivity.this, apiBase);
                    userId = UserIdHelper.canonicalUserId(mgr, token);
                } catch (Exception ex) {
                    userId = UserIdHelper.decodeJwtSub(token);
                }
                if (userId != null && !userId.isEmpty()) {
                    payload.put("userId", userId);
                }
                payload.put("title", title);

                Request.Builder rb = new Request.Builder().url(api).post(RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8")));
                if (!TextUtils.isEmpty(token)) {
                    rb.header("Authorization", "Bearer " + token);
                }

                String draftId;
                try (Response r = client.newCall(rb.build()).execute()) {
                    if (!r.isSuccessful()) {
                        throw new RuntimeException("Create draft failed: " + r.code());
                    }
                    String text = r.body() != null ? r.body().string() : "";
                    JSONObject obj = new JSONObject(text);
                    draftId = String.valueOf(obj.get("id"));
                }

                for (Map.Entry<String, Part> e : selectedParts.entrySet()) {
                    String cat = e.getKey();
                    Part p = e.getValue();
                    String patchUrl = apiBase + "/api/recommendation/manual-builds/drafts/" + draftId + "/parts";
                    JSONObject patch = new JSONObject();
                    if (userId != null && !userId.isEmpty()) {
                        patch.put("userId", userId);
                    }
                    patch.put("category", cat);
                    JSONObject partJson = new JSONObject();
                    partJson.put("id", p.id);
                    partJson.put("name", p.name);
                    partJson.put("price", p.price);
                    patch.put("part", partJson);
                    patch.put("estimatedPower", 300);

                    Request.Builder prb = new Request.Builder().url(patchUrl).patch(RequestBody.create(patch.toString(), MediaType.get("application/json; charset=utf-8")));
                    if (!TextUtils.isEmpty(token)) {
                        prb.header("Authorization", "Bearer " + token);
                    }

                    try (Response rr = client.newCall(prb.build()).execute()) {
                        if (!rr.isSuccessful()) {
                            throw new RuntimeException("Patch part failed: " + rr.code());
                        }
                    }
                }

                String finalizeUrl = apiBase + "/api/recommendation/manual-builds/drafts/" + draftId + "/finalize";
                JSONObject fin = new JSONObject();
                if (userId != null && !userId.isEmpty()) {
                    fin.put("userId", userId);
                }
                fin.put("title", title);
                fin.put("description", "Saved from native builder");
                fin.put("publicBuild", false);

                Request.Builder frb = new Request.Builder().url(finalizeUrl).post(RequestBody.create(fin.toString(), MediaType.get("application/json; charset=utf-8")));
                if (!TextUtils.isEmpty(token)) {
                    frb.header("Authorization", "Bearer " + token);
                }

                try (Response rr = client.newCall(frb.build()).execute()) {
                    if (!rr.isSuccessful()) {
                        throw new RuntimeException("Finalize failed: " + rr.code());
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(BuilderActivity.this, "Build saved", Toast.LENGTH_LONG).show();
                    updateSaveButtonState();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    Toast.makeText(BuilderActivity.this, "Save error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    updateSaveButtonState();
                });
            }
        }).start();
    }
}
