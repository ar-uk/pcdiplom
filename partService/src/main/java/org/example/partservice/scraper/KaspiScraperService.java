package org.example.partservice.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.example.partservice.model.ParsedCpu;
import org.example.partservice.model.ParsedCpuCooler;
import org.example.partservice.model.ParsedInternalHardDrive;
import org.example.partservice.model.ParsedMemory;
import org.example.partservice.model.ParsedMotherboard;
import org.example.partservice.model.ParsedPcCase;
import org.example.partservice.model.ParsedPowerSupply;
import org.example.partservice.model.ParsedVideoCard;
import org.example.partservice.repository.ParsedCpuRepository;
import org.example.partservice.repository.ParsedCpuCoolerRepository;
import org.example.partservice.repository.ParsedInternalHardDriveRepository;
import org.example.partservice.repository.ParsedMemoryRepository;
import org.example.partservice.repository.ParsedMotherboardRepository;
import org.example.partservice.repository.ParsedPcCaseRepository;
import org.example.partservice.repository.ParsedPowerSupplyRepository;
import org.example.partservice.repository.ParsedVideoCardRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class KaspiScraperService {

    private static final String BASE_URL = "https://kaspi.kz";
    private static final String SEARCH_ENGINE_URL = "https://www.google.com/search";
    private static final String KASPI_SITE_FILTER = "site:kaspi.kz";
    private static final String KASPI_PREFIX = "::kaspi.kz";
    private static final String PRODUCT_RESULTS_API = BASE_URL + "/yml/product-view/pl/results";
    private static final String OFFERS_URL_TEMPLATE = BASE_URL + "/yml/offer-view/offers/%s";
    private static final String CITY_ID = "353220100";
    private static final Pattern SKU_PATTERN = Pattern.compile("-(\\d+)(?:/)?$");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ParsedCpuRepository parsedCpuRepository;
    private final ParsedCpuCoolerRepository parsedCpuCoolerRepository;
    private final ParsedVideoCardRepository parsedVideoCardRepository;
    private final ParsedPowerSupplyRepository parsedPowerSupplyRepository;
    private final ParsedPcCaseRepository parsedPcCaseRepository;
    private final ParsedMemoryRepository parsedMemoryRepository;
    private final ParsedInternalHardDriveRepository parsedInternalHardDriveRepository;
    private final ParsedMotherboardRepository parsedMotherboardRepository;
    private final PartNormalizationService partNormalizationService;

    public void scrapeAllowlistFromKaspi(Integer limit, String onlyPartType) {
        try {
            List<AllowlistPart> allowlist = AllowlistLoader.loadAllowlist();

            if (allowlist.isEmpty()) {
                System.out.println("[KASPI SCRAPER] Allowlist is empty!");
                return;
            }

            if (onlyPartType != null && !onlyPartType.isBlank()) {
                String normalizedType = normalizePartType(onlyPartType);
                allowlist = allowlist.stream()
                    .filter(part -> normalizedType.equals(normalizePartType(part.getPartType())))
                    .toList();
            }

            if (limit != null && limit > 0 && allowlist.size() > limit) {
                allowlist = allowlist.subList(0, limit);
            }

            System.out.println(String.format("[KASPI SCRAPER] Loaded %d CPU allowlist items", allowlist.size()));

            int successCount = 0;
            for (AllowlistPart part : allowlist) {
                try {
                    scrapePart(part);
                    successCount++;
                } catch (Exception e) {
                    System.err.println(String.format("[KASPI SCRAPER] Error scraping %s: %s", part.getPartName(), e.getMessage()));
                }
            }

            System.out.println(String.format("[KASPI SCRAPER] Completed! Successfully processed %d/%d parts", successCount, allowlist.size()));
        } catch (Exception e) {
            System.err.println("[KASPI SCRAPER] Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void scrapeUsingSearchStrategies() {
        try {
            List<SearchStrategy> strategies = SearchStrategyLoader.loadSearchStrategies();
            List<SearchStrategy> kaspiStrategies = strategies.stream()
                .filter(strategy -> "kaspi.kz".equalsIgnoreCase(strategy.getRetailer()))
                .collect(Collectors.toList());

            if (kaspiStrategies.isEmpty()) {
                System.out.println("[KASPI SCRAPER] No Kaspi search strategies loaded");
                return;
            }

            int totalResults = 0;
            for (SearchStrategy strategy : kaspiStrategies) {
                System.out.println("\n[KASPI SCRAPER] Processing strategy: " + strategy);
                if (strategy.getQueries() == null || strategy.getQueries().isEmpty()) {
                    System.out.println("[KASPI SCRAPER] No queries in strategy, skipping");
                    continue;
                }

                for (String query : strategy.getQueries()) {
                    totalResults += scrapeQuery(strategy, query);
                }
            }

            System.out.println(String.format("\n[KASPI SCRAPER] Completed search strategy scraping. Total results saved: %d", totalResults));
        } catch (Exception e) {
            System.err.println("[KASPI SCRAPER] Fatal error during strategy-based scrape: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scrapePart(AllowlistPart part) {
        String normalizedPartType = normalizePartType(part.getPartType());
        scrapeQuery(normalizedPartType, part.getPartName(), normalizedPartType);
    }

    private int scrapeQuery(SearchStrategy strategy, String query) {
        return scrapeQuery(normalizePartType(strategy.getPartType()), query, normalizePartType(strategy.getPartType()), strategy.getMinPrice(), strategy.getMaxPrice());
    }

    private int scrapeQuery(String partName, String query, String partType) {
        return scrapeQuery(partType, query, partName, null, null);
    }

    private int scrapeQuery(String partType, String query, String productDisplayName, BigDecimal minPrice, BigDecimal maxPrice) {
        sleepBeforeRequest();

        System.out.println(String.format("[KASPI SCRAPER] Searching normal engine for: %s (%s)", query, partType));

        List<KaspiSearchProduct> products = searchProducts(query);
        if (products.isEmpty()) {
            System.out.println(String.format("[KASPI SCRAPER] No search results for %s", query));
            return 0;
        }

        List<KaspiSearchProduct> filteredProducts = products;

        int savedCount = 0;
        for (KaspiSearchProduct product : filteredProducts) {
            if (product.sku() == null || product.sku().isBlank()) {
                continue;
            }

            String title = cleanText(product.title());
            if (title.isBlank()) {
                continue;
            }

            ShopProduct normalizedSource = new ShopProduct(
                title,
                null,
                product.url(),
                "in_stock",
                product.sku()
            );

            NormalizedPartResult normalizedPart = partNormalizationService.normalize("kaspi.kz", partType, normalizedSource);
            normalizedPart = new NormalizedPartResult(
                normalizedPart.normalizedName(),
                normalizedPart.normalizedSpecsJson(),
                buildSourcePayloadJsonFromSearch(product),
                normalizedPart.confidenceScore()
            );

            if (saveIntoParsedTable(partType, title, null, product.url(), normalizedPart)) {
                savedCount++;
            }
        }

        System.out.println(String.format("[KASPI SCRAPER] Saved %d listings for %s", savedCount, productDisplayName == null ? query : productDisplayName));
        return savedCount;
    }

    private List<KaspiSearchProduct> searchProducts(String query) {
        List<KaspiSearchProduct> products = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        try {
            String discoveryQuery = buildDiscoveryQuery(query);
            String searchUrl = buildSearchEngineUrl(discoveryQuery);
            System.out.println(String.format("[KASPI SCRAPER] Search engine query: %s", discoveryQuery));
            System.out.println(String.format("[KASPI SCRAPER] Search engine URL: %s", searchUrl));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .GET()
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://www.google.com/")
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println(String.format("[KASPI SCRAPER] Search engine response status=%d", response.statusCode()));
            
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println(String.format("[KASPI SCRAPER] Search engine fetch failed for '%s': HTTP %d", query, response.statusCode()));
                return products;
            }

            Document doc = Jsoup.parse(response.body(), searchUrl);
            Elements productLinks = new Elements();
            productLinks.addAll(doc.select("div.yuRUbf > a[href]"));
            productLinks.addAll(doc.select("a[jsname][href]"));
            productLinks.addAll(doc.select("a[href*='/url?q='][href]"));
            productLinks.addAll(doc.select("li.b_algo h2 a[href]"));

            if (productLinks.isEmpty()) {
                System.out.println(String.format("[KASPI SCRAPER] No result anchors found in search engine page for '%s'", query));
                return products;
            }

            System.out.println(String.format("[KASPI SCRAPER] Found %d result anchors in search engine page for '%s'", productLinks.size(), query));

            for (Element link : productLinks) {
                try {
                    String href = normalizeResultHref(link.attr("href"));
                    if (href.isBlank() || !href.contains("kaspi.kz")) {
                        continue;
                    }

                    String sku = extractSku(href);
                    if (sku.isBlank()) {
                        continue;
                    }

                    String title = cleanText(link.text());
                    if (title.isBlank()) {
                        title = cleanText(link.attr("aria-label"));
                    }

                    if (title.isBlank()) {
                        continue;
                    }

                    String productUrl = href;

                    if (!seen.add(productUrl)) {
                        continue;
                    }

                    products.add(new KaspiSearchProduct(sku, title, productUrl, null, "", null));
                } catch (Exception e) {
                    System.err.println(String.format("[KASPI SCRAPER] Error parsing search result link: %s", e.getMessage()));
                }
            }

            System.out.println(String.format("[KASPI SCRAPER] Extracted %d unique Kaspi results from search engine for '%s'", products.size(), query));

        } catch (Exception e) {
            System.err.println(String.format("[KASPI SCRAPER] Search engine parse failed for '%s': %s", query, e.getMessage()));
            e.printStackTrace();
        }

        return products;
    }

    private String buildSearchEngineUrl(String discoveryQuery) {
        return SEARCH_ENGINE_URL + "?q=" + encodeQuery(discoveryQuery) + "&hl=ru&gl=kz";
    }

    private KaspiOfferDetails fetchOfferDetails(String sku) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("cityId", CITY_ID);
            body.put("id", sku);
            body.put("merchantUID", List.of());
            body.put("limit", 5);
            body.put("page", 0);
            
            // Use LinkedHashMap for product to allow null values
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("brand", "");
            product.put("categoryCodes", List.of());
            product.put("baseProductCodes", List.of());
            product.put("groups", null);
            product.put("productSeries", List.of());
            body.put("product", product);
            
            body.put("sortOption", "PRICE");
            body.put("highRating", null);
            body.put("searchText", null);
            body.put("isExcellentMerchant", false);
            body.put("zoneId", List.of(CITY_ID));
            body.put("installationId", "-1");

            String offerUrl = String.format(OFFERS_URL_TEMPLATE, sku);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(offerUrl))
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                .header("Accept", "application/json, text/*")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("x-ks-city", CITY_ID)
                .build();

            System.out.println(String.format("[KASPI SCRAPER] Fetching offers for sku %s from %s", sku, offerUrl));
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println(String.format("[KASPI SCRAPER] Offer response status=%d for sku %s", response.statusCode(), sku));
            
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println(String.format("[KASPI SCRAPER] Offer fetch failed for sku %s: HTTP %d", sku, response.statusCode()));
                if (response.body() != null && response.body().length() > 0) {
                    String snippet = response.body().length() > 500 ? response.body().substring(0, 500) : response.body();
                    System.err.println(String.format("[KASPI SCRAPER] Offer response snippet: %s", snippet));
                }
                return null;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            System.out.println(String.format("[KASPI SCRAPER] Parsed JSON response for sku %s. Root keys: %s", sku, root.fieldNames().hasNext() ? String.join(", ", (Iterable<String>) () -> root.fieldNames()) : "none"));
            
            JsonNode offersNode = root.path("offers");
            System.out.println(String.format("[KASPI SCRAPER] Offers node is array: %b, size: %d for sku %s", offersNode.isArray(), offersNode.size(), sku));
            
            if (!offersNode.isArray() || offersNode.isEmpty()) {
                System.err.println(String.format("[KASPI SCRAPER] No offers array or empty for sku %s", sku));
                return null;
            }

            JsonNode bestOffer = offersNode.get(0);
            BigDecimal bestPrice = decimalValue(root.path("productCardInfo").path("bestPrice"));
            if (bestPrice == null || bestPrice.compareTo(BigDecimal.ZERO) <= 0) {
                bestPrice = decimalValue(bestOffer.path("price"));
            }

            System.out.println(String.format("[KASPI SCRAPER] Best price for sku %s: %s", sku, bestPrice));

            return new KaspiOfferDetails(
                bestPrice == null ? BigDecimal.ZERO : bestPrice,
                bestOffer.path("merchantName").asText(""),
                bestOffer.path("merchantId").asText(""),
                bestOffer.path("title").asText(""),
                root
            );
        } catch (Exception e) {
            System.err.println(String.format("[KASPI SCRAPER] Offer parse failed for sku %s: %s", sku, e.getClass().getSimpleName() + ": " + e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }

    private List<KaspiSearchProduct> applyPriceFilter(List<KaspiSearchProduct> products, BigDecimal minPrice, BigDecimal maxPrice) {
        return products.stream()
            .filter(product -> {
                if (product.priceKzt() == null) {
                    return true;
                }
                if (minPrice != null && product.priceKzt().compareTo(minPrice) < 0) {
                    return false;
                }
                if (maxPrice != null && product.priceKzt().compareTo(maxPrice) > 0) {
                    return false;
                }
                return true;
            })
            .toList();
    }

    private boolean saveIntoParsedTable(String partType, String name, BigDecimal priceKzt, String url, NormalizedPartResult normalizedPart) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedPartType = normalizePartType(partType);

        switch (normalizedPartType) {
            case "cpu" -> {
                ParsedCpu cpu = parsedCpuRepository.findFirstByUrl(url).orElseGet(ParsedCpu::new);
                populateListing(cpu, name, priceKzt, url, normalizedPart, now);
                parsedCpuRepository.save(cpu);
                return true;
            }
            case "cpu_cooler" -> {
                ParsedCpuCooler cooler = parsedCpuCoolerRepository.findFirstByUrl(url).orElseGet(ParsedCpuCooler::new);
                populateListing(cooler, name, priceKzt, url, normalizedPart, now);
                parsedCpuCoolerRepository.save(cooler);
                return true;
            }
            case "gpu" -> {
                ParsedVideoCard gpu = parsedVideoCardRepository.findFirstByUrl(url).orElseGet(ParsedVideoCard::new);
                populateListing(gpu, name, priceKzt, url, normalizedPart, now);
                parsedVideoCardRepository.save(gpu);
                return true;
            }
            case "power_supply" -> {
                ParsedPowerSupply psu = parsedPowerSupplyRepository.findFirstByUrl(url).orElseGet(ParsedPowerSupply::new);
                populateListing(psu, name, priceKzt, url, normalizedPart, now);
                parsedPowerSupplyRepository.save(psu);
                return true;
            }
            case "pc_case" -> {
                ParsedPcCase pcCase = parsedPcCaseRepository.findFirstByUrl(url).orElseGet(ParsedPcCase::new);
                populateListing(pcCase, name, priceKzt, url, normalizedPart, now);
                parsedPcCaseRepository.save(pcCase);
                return true;
            }
            case "memory", "ram" -> {
                ParsedMemory memory = parsedMemoryRepository.findFirstByUrl(url).orElseGet(ParsedMemory::new);
                populateListing(memory, name, priceKzt, url, normalizedPart, now);
                parsedMemoryRepository.save(memory);
                return true;
            }
            case "internal_memory", "internal_hard_drive" -> {
                ParsedInternalHardDrive drive = parsedInternalHardDriveRepository.findFirstByUrl(url).orElseGet(ParsedInternalHardDrive::new);
                populateListing(drive, name, priceKzt, url, normalizedPart, now);
                parsedInternalHardDriveRepository.save(drive);
                return true;
            }
            case "motherboard" -> {
                ParsedMotherboard motherboard = parsedMotherboardRepository.findFirstByUrl(url).orElseGet(ParsedMotherboard::new);
                populateListing(motherboard, name, priceKzt, url, normalizedPart, now);
                parsedMotherboardRepository.save(motherboard);
                return true;
            }
            default -> {
                System.out.println("[KASPI SCRAPER] Unsupported part type for parsed table save: " + partType);
                return false;
            }
        }
    }

    private String buildSourcePayloadJson(KaspiSearchProduct product, KaspiOfferDetails offerDetails) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("retailer", "kaspi.kz");
        payload.put("sku", product.sku());
        payload.put("sourceTitle", product.title());
        payload.put("url", product.url());
        payload.put("merchantName", offerDetails.merchantName());
        payload.put("merchantId", offerDetails.merchantId());
        payload.put("priceKzt", offerDetails.bestPrice());
        payload.put("offers", offerDetails.raw().path("offers").size());
        payload.put("cityId", CITY_ID);
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildSourcePayloadJsonFromSearch(KaspiSearchProduct product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("retailer", "kaspi.kz");
        payload.put("sku", product.sku());
        payload.put("sourceTitle", product.title());
        payload.put("description", product.description());
        payload.put("url", product.url());
        payload.put("cityId", CITY_ID);
        payload.put("source", "search_api");
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void populateListing(org.example.partservice.model.AbstractParsedListing listing, String productName, BigDecimal priceKzt, String url, NormalizedPartResult normalizedPart, LocalDateTime now) {
        listing.setName(productName);
        listing.setPriceKzt(priceKzt);
        listing.setRetailer("kaspi.kz");
        listing.setCurrency("KZT");
        listing.setUrl(url);
        listing.setLastScraped(now);
        if (normalizedPart != null) {
            listing.setNormalizedName(normalizedPart.normalizedName());
            listing.setNormalizedSpecsJson(normalizedPart.normalizedSpecsJson());
            listing.setSourcePayloadJson(normalizedPart.sourcePayloadJson());
            listing.setConfidenceScore(normalizedPart.confidenceScore());
        }
    }

    private String normalizePartType(String partType) {
        if (partType == null || partType.isBlank()) {
            return "unknown";
        }

        String normalized = partType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "psu", "power-supply", "power supply" -> "power_supply";
            case "ram" -> "memory";
            case "storage", "internal-hard-drive", "internal_hard_drive" -> "internal_memory";
            case "case", "pc-case", "pc case" -> "pc_case";
            case "cooling", "cooler", "cpu-cooler", "cpu cooler" -> "cpu_cooler";
            default -> normalized;
        };
    }

    private void sleepBeforeRequest() {
        try {
            long delayMs = 4000 + (int) (Math.random() * 4000);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String bestTitle(KaspiSearchProduct product, KaspiOfferDetails offerDetails) {
        String title = cleanText(offerDetails.title());
        return title.isBlank() ? product.title() : title;
    }

    private String extractSku(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        Matcher matcher = SKU_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        int questionIndex = url.indexOf('?');
        String stripped = questionIndex >= 0 ? url.substring(0, questionIndex) : url;
        matcher = Pattern.compile("-(\\d+)/?").matcher(stripped);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String encodeQuery(String query) {
        return URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8);
    }

    private String buildDiscoveryQuery(String query) {
        String cleaned = cleanText(query);
        if (cleaned.toLowerCase(Locale.ROOT).startsWith(KASPI_PREFIX)) {
            cleaned = cleaned.substring(KASPI_PREFIX.length()).trim();
        }

        if (cleaned.isBlank()) {
            return KASPI_SITE_FILTER;
        }

        return KASPI_SITE_FILTER + " " + cleaned;
    }

    private String normalizeResultHref(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }

        String value = href.trim();
        if (value.startsWith("//")) {
            value = "https:" + value;
        }

        if (value.startsWith("/url?")) {
            value = "https://www.google.com" + value;
        }

        if (value.contains("google.com/url?")) {
            try {
                int index = value.indexOf("q=");
                if (index >= 0) {
                    String encoded = value.substring(index + 2);
                    int ampIndex = encoded.indexOf('&');
                    if (ampIndex >= 0) {
                        encoded = encoded.substring(0, ampIndex);
                    }
                    String decoded = java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                    if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                        return decoded;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return value;
    }

    private String toAbsoluteKaspiUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }

        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }

        return href.startsWith("/") ? BASE_URL + href : BASE_URL + "/" + href;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            return new BigDecimal(node.asText("0"));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private record KaspiSearchProduct(String sku, String title, String url, BigDecimal priceKzt, String description, JsonNode rawItem) {
    }

    private record KaspiOfferDetails(BigDecimal bestPrice, String merchantName, String merchantId, String title, JsonNode raw) {
    }
}