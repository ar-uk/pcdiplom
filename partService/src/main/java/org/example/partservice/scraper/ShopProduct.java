package org.example.partservice.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopProduct {
    private static final Pattern PRICE_NUMBER_PATTERN = Pattern.compile("(\\d{2,3}(?:\\s\\d{3})+|\\d{5,})");
    private static final String BASE_URL = "https://shop.kz";
    private static final String SEARCH_API_URL = "https://sort.diginetica.net/search";
    private static final String API_KEY = "Z72L941338";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SEARCH_URL_TEMPLATE = BASE_URL + "/search/?q=%s";

    private String name;
    private BigDecimal priceKzt;
    private String url;
    private String stock;
    private String productCode;

    public static List<ShopProduct> searchShop(String query, String partType) {
        List<ShopProduct> products = new ArrayList<>();

        try {
            Set<String> seenUrls = new LinkedHashSet<>();
            searchProductsFromApi(query, products, seenUrls);

            // Keep a fallback HTML parser for cases where API shape changes.
            if (products.isEmpty()) {
                String searchUrl = buildSearchUrl(query, partType);
                Document doc = fetchDocument(searchUrl, query);
                if (doc != null) {
                    parseProductsFromDocument(doc, products, seenUrls);
                }

                String fallbackUrl = BASE_URL + "/search/?q=" + encodeQuery(query);
                Document fallbackDoc = fetchDocument(fallbackUrl, query);
                if (fallbackDoc != null) {
                    parseProductsFromDocument(fallbackDoc, products, seenUrls);
                }
            }

            System.out.println(String.format("shop.kz search for '%s' found %d products", query, products.size()));
        } catch (Exception e) {
            System.err.println("Error scraping shop.kz for query '" + query + "': " + e.getMessage());
        }

        return products;
    }

    private static void searchProductsFromApi(String query, List<ShopProduct> products, Set<String> seenUrls) {
        try {
            String apiUrl = buildApiUrl(query);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println(String.format("shop.kz API fetch failed for '%s': HTTP %d", query, response.statusCode()));
                return;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode productsNode = root.path("products");

            if (!productsNode.isArray()) {
                return;
            }

            for (JsonNode item : productsNode) {
                String productName = readText(item, "name");
                String productUrl = toAbsoluteUrl(readText(item, "link_url"));

                if (productName.isBlank() || !isLikelyProductUrl(productUrl) || !seenUrls.add(productUrl)) {
                    continue;
                }

                BigDecimal price = parsePrice(item.path("price"));
                boolean available = readBoolean(item, "available", true);
                String stock = available ? "in_stock" : "out_of_stock";
                String productCode = readAttribute(item, "xmlid");

                products.add(new ShopProduct(productName.trim(), price, productUrl, stock, productCode));
            }
        } catch (Exception e) {
            System.err.println(String.format("shop.kz API parse failed for '%s': %s", query, e.getMessage()));
        }
    }

    private static String buildApiUrl(String query) {
        StringBuilder sb = new StringBuilder(SEARCH_API_URL).append("?");
        appendParam(sb, "apiKey", API_KEY);
        appendParam(sb, "strategy", "advanced_xname,zero_queries");
        appendParam(sb, "fullData", "true");
        appendParam(sb, "withCorrection", "true");
        appendParam(sb, "withFacets", "true");
        appendParam(sb, "treeFacets", "true");
        appendParam(sb, "useCategoryPrediction", "false");
        appendParam(sb, "withSku", "false");
        appendParam(sb, "useCompletion", "true");
        appendParam(sb, "showUnavailable", "true");
        appendParam(sb, "unavailableMultiplier", "0.2");
        appendParam(sb, "size", "40");
        appendParam(sb, "offset", "0");
        appendParam(sb, "regionId", "global");
        appendParam(sb, "sort", "DEFAULT");
        appendParam(sb, "lang", "ru");
        appendParam(sb, "st", query == null ? "" : query.trim());

        if (sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    private static void appendParam(StringBuilder sb, String key, String value) {
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
            .append('=')
            .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8))
            .append('&');
    }

    private static String readText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return "";
        }

        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return "";
        }

        String value = valueNode.asText("");
        return value == null ? "" : value.trim();
    }

    private static boolean readBoolean(JsonNode node, String field, boolean defaultValue) {
        if (node == null || node.isMissingNode()) {
            return defaultValue;
        }

        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return defaultValue;
        }

        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        }

        String raw = valueNode.asText("").trim().toLowerCase();
        if (raw.isEmpty()) {
            return defaultValue;
        }
        if ("true".equals(raw) || "1".equals(raw) || "yes".equals(raw)) {
            return true;
        }
        if ("false".equals(raw) || "0".equals(raw) || "no".equals(raw)) {
            return false;
        }
        return defaultValue;
    }

    private static String readAttribute(JsonNode productNode, String attributeName) {
        JsonNode attributeNode = productNode.path("attributes").path(attributeName);
        if (attributeNode.isArray() && attributeNode.size() > 0) {
            return attributeNode.get(0).asText("");
        }
        if (!attributeNode.isMissingNode() && !attributeNode.isNull()) {
            return attributeNode.asText("");
        }
        return "";
    }

    private static BigDecimal parsePrice(JsonNode priceNode) {
        if (priceNode == null || priceNode.isMissingNode() || priceNode.isNull()) {
            return BigDecimal.ZERO;
        }

        try {
            if (priceNode.isNumber()) {
                BigDecimal numeric = priceNode.decimalValue();
                return numeric.compareTo(BigDecimal.ZERO) >= 0 ? numeric : BigDecimal.ZERO;
            }
            return extractPriceFromText(priceNode.asText(""));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String buildSearchUrl(String query, String partType) {
        return String.format(SEARCH_URL_TEMPLATE, encodeQuery(query));
    }

    private static String encodeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        return URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
    }

    private static Document fetchDocument(String url, String query) {
        try {
            return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8,kk;q=0.6")
                .header("Cache-Control", "max-age=0")
                .header("DNT", "1")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(20000)
                .followRedirects(true)
                .get();
        } catch (Exception e) {
            System.err.println(String.format("shop.kz fetch failed for '%s': %s", query, e.getMessage()));
            return null;
        }
    }

    private static void parseProductsFromDocument(Document doc, List<ShopProduct> products, Set<String> seenUrls) {
        // Primary parser for shop.kz search cards.
        Elements cards = doc.select("div.bx-product-item-description");
        for (Element card : cards) {
            Element productLink = card.selectFirst(".product-item-title a[href], a[href^='/offer/']");
            if (productLink == null) {
                continue;
            }

            String productUrl = toAbsoluteUrl(productLink.attr("href"));
            if (!isLikelyProductUrl(productUrl) || !seenUrls.add(productUrl)) {
                continue;
            }

            String productName = productLink.text();
            if (productName == null || productName.isBlank()) {
                productName = productLink.attr("title");
            }
            if (productName == null || productName.isBlank()) {
                continue;
            }

            Element priceEl = card.selectFirst(".item-block-price strong, .price-box strong, .item-price strong, .price-container span");
            String priceText = priceEl != null ? priceEl.text() : card.text();
            BigDecimal price = extractPriceFromText(priceText);

            products.add(new ShopProduct(productName.trim(), price, productUrl, "unknown", ""));
        }

        if (!products.isEmpty()) {
            return;
        }

        // Fallback parser: collect direct offer links if card markup changes.
        Elements links = doc.select("a[href^='/offer/'], a[href*='/offer/']");
        for (Element link : links) {
            addProductFromLink(link, products, seenUrls);
        }
    }

    private static void addProductFromLink(Element productLink, List<ShopProduct> products, Set<String> seenUrls) {
        if (productLink == null) {
            return;
        }

        String productUrl = toAbsoluteUrl(productLink.attr("href"));
        if (productUrl.isBlank()) {
            return;
        }

        if (!isLikelyProductUrl(productUrl)) {
            return;
        }

        if (!seenUrls.add(productUrl)) {
            return;
        }

        String productName = productLink.attr("title");
        if (productName == null || productName.isBlank()) {
            productName = productLink.text();
        }

        if (productName == null || productName.isBlank()) {
            return;
        }

        BigDecimal price = extractPriceFromText(productLink.text());
        products.add(new ShopProduct(productName, price, productUrl, "unknown", ""));
    }

    private static String toAbsoluteUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }

        String url = href.trim();
        if (!url.startsWith("http")) {
            url = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
        }

        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex >= 0) {
            url = url.substring(0, fragmentIndex);
        }

        return url;
    }

    private static boolean isLikelyProductUrl(String url) {
        String normalized = url.toLowerCase();

        if (normalized.startsWith("javascript:")) {
            return false;
        }

        if (normalized.contains("/search") || normalized.contains("?q=")) {
            return false;
        }

        if (normalized.endsWith(".jpg") || normalized.endsWith(".png") || normalized.endsWith(".svg")) {
            return false;
        }

        return normalized.contains("/offer/");
    }

    private static BigDecimal extractPriceFromText(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        Matcher matcher = PRICE_NUMBER_PATTERN.matcher(text.replace('\u00A0', ' '));
        BigDecimal best = BigDecimal.ZERO;
        while (matcher.find()) {
            String normalized = matcher.group(1).replace(" ", "");
            if (normalized.length() < 4) {
                continue;
            }
            try {
                BigDecimal candidate = new BigDecimal(normalized);
                if (candidate.compareTo(new BigDecimal("10000")) >= 0 && candidate.compareTo(new BigDecimal("10000000")) <= 0) {
                    if (best.compareTo(BigDecimal.ZERO) == 0 || candidate.compareTo(best) < 0) {
                        best = candidate;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }
}