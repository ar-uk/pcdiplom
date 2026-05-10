package org.example.partservice.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PartNormalizationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("(?i)(\\d{1,3})\\s*(tb|gb)");
    private static final Pattern MODULE_PATTERN = Pattern.compile("(?i)(\\d+)\\s*[xх]\\s*(\\d{1,3})\\s*gb");
    private static final Pattern SPEED_PATTERN = Pattern.compile("(?i)(\\d{4,5})\\s*mt/?s|\\b(\\d{4,5})\\b");
    private static final Pattern CAS_PATTERN = Pattern.compile("(?i)cl\\s*(\\d+)");
    private static final Pattern WATT_PATTERN = Pattern.compile("(?i)(\\d{3,4})\\s*w");
    private static final Pattern VRAM_PATTERN = Pattern.compile("(?i)(\\d{1,2})\\s*gb");
    private static final Pattern SOCKET_PATTERN = Pattern.compile("(?i)\\b(am4|am5|lga\\s?1200|lga\\s?1700|lga\\s?1851|tr4|strx4|str5)\\b");
    private static final Pattern CHIPSET_PATTERN = Pattern.compile("(?i)\\b([abhzx]?\\d{2,3})\\b");
    private static final Pattern FORM_FACTOR_PATTERN = Pattern.compile("(?i)\\b(atx|matx|micro[- ]?atx|mini[- ]?itx|itx)\\b");
    private static final Pattern MEMORY_TYPE_PATTERN = Pattern.compile("(?i)\\bddr[45]\\b");
    private static final Pattern CLOCK_PATTERN = Pattern.compile("(?i)(\\d{1,2}(?:\\.\\d+)?)\\s*ghz");
    private static final Pattern CORE_PATTERN = Pattern.compile("(?i)(\\d{1,2})\\s*(?:cores?|ядра|ядер)");
    private static final Pattern THREAD_PATTERN = Pattern.compile("(?i)(\\d{1,2})\\s*(?:threads?|потоков|потока)");

    public NormalizedPartResult normalize(String retailer, String partType, ShopProduct product) {
        String sourceTitle = product == null ? "" : safeText(product.getName());
        String canonicalRetailer = normalizeRetailer(retailer);
        String canonicalPartType = normalizePartType(partType);
        String normalizedSource = normalizeSourceText(sourceTitle);

        Map<String, Object> specs = new LinkedHashMap<>();
        specs.put("retailer", canonicalRetailer);
        specs.put("partType", canonicalPartType);
        specs.put("sourceTitle", sourceTitle);
        specs.put("sourceTokenizedTitle", normalizedSource);

        switch (canonicalPartType) {
            case "cpu" -> enrichCpuSpecs(normalizedSource, specs);
            case "motherboard" -> enrichMotherboardSpecs(normalizedSource, specs);
            case "memory" -> enrichMemorySpecs(normalizedSource, specs);
            case "power_supply" -> enrichPowerSupplySpecs(normalizedSource, specs);
            case "internal_memory" -> enrichStorageSpecs(normalizedSource, specs);
            case "gpu" -> enrichGpuSpecs(normalizedSource, specs);
            case "pc_case" -> enrichCaseSpecs(normalizedSource, specs);
            case "cpu_cooler" -> enrichCoolerSpecs(normalizedSource, specs);
            default -> specs.put("normalizedKind", canonicalPartType);
        }

        String normalizedName = buildNormalizedName(canonicalPartType, normalizedSource);
        BigDecimal confidenceScore = calculateConfidence(specs);

        return new NormalizedPartResult(
            normalizedName,
            toJson(specs),
            buildSourcePayloadJson(canonicalRetailer, canonicalPartType, product, sourceTitle, normalizedName),
            confidenceScore
        );
    }

    private void enrichCpuSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("brand", detectBrand(normalizedSource));
        specs.put("family", detectCpuFamily(normalizedSource));
        specs.put("cores", firstInt(normalizedSource, CORE_PATTERN));
        specs.put("threads", firstInt(normalizedSource, THREAD_PATTERN));
        specs.put("socket", firstToken(normalizedSource, SOCKET_PATTERN));
        specs.put("baseClockGhz", firstDecimal(normalizedSource, CLOCK_PATTERN));
        specs.put("tdpW", firstInt(normalizedSource, WATT_PATTERN));
    }

    private void enrichMotherboardSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("brand", detectBrand(normalizedSource));
        specs.put("socket", firstToken(normalizedSource, SOCKET_PATTERN));
        specs.put("chipset", firstToken(normalizedSource, CHIPSET_PATTERN));
        specs.put("formFactor", canonicalFormFactor(firstToken(normalizedSource, FORM_FACTOR_PATTERN)));
        specs.put("memoryType", firstToken(normalizedSource, MEMORY_TYPE_PATTERN).toUpperCase(Locale.ROOT));
        specs.put("m2Slots", countSlotHints(normalizedSource, "m2", "m.2", "nvme"));
    }

    private void enrichMemorySpecs(String normalizedSource, Map<String, Object> specs) {
        Matcher moduleMatcher = MODULE_PATTERN.matcher(normalizedSource);
        if (moduleMatcher.find()) {
            Integer moduleCount = parseInt(moduleMatcher.group(1));
            Integer moduleSize = parseInt(moduleMatcher.group(2));
            specs.put("moduleCount", moduleCount);
            specs.put("capacityGb", moduleCount == null || moduleSize == null ? null : moduleCount * moduleSize);
        } else {
            specs.put("moduleCount", countSlotHints(normalizedSource, "x", "module", "stick"));
            specs.put("capacityGb", firstCapacityGb(normalizedSource));
        }
        specs.put("memoryType", firstToken(normalizedSource, MEMORY_TYPE_PATTERN).toUpperCase(Locale.ROOT));
        specs.put("speedMtS", firstSpeed(normalizedSource));
        specs.put("casLatency", firstInt(normalizedSource, CAS_PATTERN));
    }

    private void enrichPowerSupplySpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("wattage", firstInt(normalizedSource, WATT_PATTERN));
        specs.put("certification", firstCertification(normalizedSource));
        specs.put("modular", containsAny(normalizedSource, "modular", "fully modular", "semi modular"));
    }

    private void enrichStorageSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("capacityGb", firstCapacityGb(normalizedSource));
        specs.put("storageType", containsAny(normalizedSource, "nvme", "ssd", "hdd") ? firstMatchedToken(normalizedSource, "nvme", "ssd", "hdd") : "");
        specs.put("interface", containsAny(normalizedSource, "m.2", "sata", "pcie") ? firstMatchedToken(normalizedSource, "m.2", "sata", "pcie") : "");
        specs.put("formFactor", containsAny(normalizedSource, "2.5", "3.5", "m.2") ? firstMatchedToken(normalizedSource, "m.2", "2.5", "3.5") : "");
    }

    private void enrichGpuSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("brand", detectBrand(normalizedSource));
        specs.put("family", firstMatchedToken(normalizedSource, "rtx", "gtx", "rx", "arc"));
        specs.put("vramGb", firstVramGb(normalizedSource));
        specs.put("model", normalizedSource);
    }

    private void enrichCaseSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("formFactor", canonicalFormFactor(firstToken(normalizedSource, FORM_FACTOR_PATTERN)));
        specs.put("supportsAtx", containsAny(normalizedSource, "atx"));
        specs.put("supportsMatx", containsAny(normalizedSource, "matx", "micro atx"));
        specs.put("supportsMiniItx", containsAny(normalizedSource, "mini itx", "mini-itx", "itx"));
    }

    private void enrichCoolerSpecs(String normalizedSource, Map<String, Object> specs) {
        specs.put("coolerType", containsAny(normalizedSource, "water", "aio", "liquid") ? "liquid" : containsAny(normalizedSource, "air") ? "air" : "");
        specs.put("radiatorMm", firstRadiatorSize(normalizedSource));
        specs.put("tdpW", firstInt(normalizedSource, WATT_PATTERN));
    }

    private String buildNormalizedName(String partType, String normalizedSource) {
        String cleaned = normalizedSource
            .replaceAll("\\b(shop\\.kz|kz|ru)\\b", "")
            .replaceAll("\\s+", " ")
            .trim();

        if (cleaned.isBlank()) {
            return partType;
        }

        return cleaned;
    }

    private BigDecimal calculateConfidence(Map<String, Object> specs) {
        Set<String> meaningfulKeys = new LinkedHashSet<>(Arrays.asList(
            "brand", "family", "socket", "chipset", "formFactor", "memoryType", "capacityGb", "wattage", "vramGb", "coolerType"
        ));

        int matched = 0;
        for (String key : meaningfulKeys) {
            Object value = specs.get(key);
            if (value != null && !value.toString().isBlank()) {
                matched++;
            }
        }

        BigDecimal confidence = BigDecimal.valueOf(0.45 + (matched * 0.1));
        if (matched >= 4) {
            confidence = confidence.add(BigDecimal.valueOf(0.05));
        }

        return confidence.min(BigDecimal.valueOf(0.95)).setScale(3, RoundingMode.HALF_UP);
    }

    private String buildSourcePayloadJson(String retailer, String partType, ShopProduct product, String sourceTitle, String normalizedName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("retailer", retailer);
        payload.put("partType", partType);
        payload.put("sourceTitle", sourceTitle);
        payload.put("normalizedName", normalizedName);
        payload.put("url", product == null ? "" : safeText(product.getUrl()));
        payload.put("priceKzt", product == null || product.getPriceKzt() == null ? null : product.getPriceKzt());
        payload.put("stock", product == null ? "" : safeText(product.getStock()));
        payload.put("productCode", product == null ? "" : safeText(product.getProductCode()));
        return toJson(payload);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalizeRetailer(String retailer) {
        String value = safeText(retailer).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "shop.kz", "shop", "shopkz" -> "shop.kz";
            default -> value.isBlank() ? "unknown" : value;
        };
    }

    private String normalizePartType(String partType) {
        String value = transliterateToLatin(safeText(partType).toLowerCase(Locale.ROOT));
        return switch (value) {
            case "cpu", "processor", "protsessor", "procesor" -> "cpu";
            case "motherboard", "materinskaya plata", "mainboard" -> "motherboard";
            case "memory", "ram", "ozu", "operativnaya pamyat" -> "memory";
            case "gpu", "video card", "videocard", "videokarta" -> "gpu";
            case "power supply", "psu", "bp", "blok pitaniya" -> "power_supply";
            case "storage", "ssd", "hdd", "internal hard drive", "nakopitel" -> "internal_memory";
            case "case", "pc case", "korpus" -> "pc_case";
            case "cooling", "cooler", "cpu cooler", "kuler" -> "cpu_cooler";
            default -> value.replaceAll("[^a-z0-9_\\- ]", "").replace(' ', '_');
        };
    }

    private String normalizeSourceText(String input) {
        String transliterated = transliterateToLatin(safeText(input).toLowerCase(Locale.ROOT));
        transliterated = transliterated
            .replace("raizen", "ryzen")
            .replace("reizen", "ryzen")
            .replace("ozu", "ram")
            .replace("blok pitaniya", "power supply")
            .replace("materinskaya plata", "motherboard")
            .replace("videokarta", "gpu")
            .replace("korpus", "case")
            .replace("kuler", "cooler")
            .replace("prozessor", "processor");

        transliterated = Normalizer.normalize(transliterated, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        transliterated = transliterated.replaceAll("[^a-z0-9\\s+\\-./]", " ");
        transliterated = transliterated.replaceAll("\\s+", " ").trim();
        return transliterated;
    }

    private String transliterateToLatin(String input) {
        Map<Character, String> map = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"), Map.entry('д', "d"),
            Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "y"), Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""), Map.entry('ы', "y"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya")
        );

        StringBuilder builder = new StringBuilder();
        for (char ch : safeText(input).toCharArray()) {
            char lower = Character.toLowerCase(ch);
            String replacement = map.get(lower);
            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String firstToken(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.isBlank()) {
                    return group.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                }
            }
        }
        return "";
    }

    private String firstMatchedToken(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) {
                return token;
            }
        }
        return "";
    }

    private Integer firstInt(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && group.matches("\\d+")) {
                    return parseInt(group);
                }
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && group.matches("\\d+(?:\\.\\d+)?")) {
                    return new BigDecimal(group).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        return null;
    }

    private Integer firstCapacityGb(String text) {
        Matcher matcher = CAPACITY_PATTERN.matcher(text);
        if (matcher.find()) {
            Integer amount = parseInt(matcher.group(1));
            String unit = safeText(matcher.group(2)).toLowerCase(Locale.ROOT);
            if (amount != null && "tb".equals(unit)) {
                return amount * 1024;
            }
            return amount;
        }
        return null;
    }

    private Integer firstSpeed(String text) {
        Matcher matcher = SPEED_PATTERN.matcher(text);
        if (matcher.find()) {
            String first = matcher.group(1);
            String second = matcher.group(2);
            if (first != null && first.matches("\\d+")) {
                return parseInt(first);
            }
            if (second != null && second.matches("\\d+")) {
                return parseInt(second);
            }
        }
        return null;
    }

    private Integer firstVramGb(String text) {
        Matcher matcher = VRAM_PATTERN.matcher(text);
        Integer result = null;
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && value.matches("\\d+")) {
                int parsed = parseInt(value);
                if (parsed <= 48) {
                    result = parsed;
                }
            }
        }
        return result;
    }

    private String firstCertification(String text) {
        if (text.contains("80+ gold")) {
            return "80+ Gold";
        }
        if (text.contains("80+ bronze")) {
            return "80+ Bronze";
        }
        if (text.contains("80+ platinum")) {
            return "80+ Platinum";
        }
        if (text.contains("80+ titanium")) {
            return "80+ Titanium";
        }
        if (text.contains("80+")) {
            return "80+";
        }
        return "";
    }

    private Integer firstRadiatorSize(String text) {
        Matcher matcher = Pattern.compile("(?i)\\b(120|240|280|360|420)\\s*mm\\b").matcher(text);
        if (matcher.find()) {
            return parseInt(matcher.group(1));
        }
        return null;
    }

    private Integer countSlotHints(String text, String... tokens) {
        int count = 0;
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count == 0 ? null : count;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String detectBrand(String text) {
        if (containsAny(text, "amd", "ryzen", "radeon", "athlon", "epyc")) {
            return "AMD";
        }
        if (containsAny(text, "intel", "core", "xeon", "arc", "pentium", "celeron")) {
            return "Intel";
        }
        if (containsAny(text, "nvidia", "geforce", "rtx", "gtx")) {
            return "NVIDIA";
        }
        String hardwareVendor = firstMatchedToken(text, "asus", "msi", "gigabyte", "asrock", "deepcool", "corsair", "kingston", "adata");
        if (!hardwareVendor.isBlank()) {
            return hardwareVendor.toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private String detectCpuFamily(String text) {
        if (containsAny(text, "ryzen")) {
            return "Ryzen";
        }
        if (containsAny(text, "core ultra")) {
            return "Core Ultra";
        }
        if (containsAny(text, "core i9", "core i7", "core i5", "core i3")) {
            return "Core";
        }
        if (containsAny(text, "xeon")) {
            return "Xeon";
        }
        if (containsAny(text, "athlon")) {
            return "Athlon";
        }
        return "";
    }

    private String canonicalFormFactor(String raw) {
        String value = safeText(raw).toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "");
        return switch (value) {
            case "microatx", "matx" -> "mATX";
            case "miniitx", "itx" -> "Mini-ITX";
            case "atx" -> "ATX";
            default -> safeText(raw).toUpperCase(Locale.ROOT);
        };
    }

    private Integer parseInt(String value) {
        try {
            String sanitized = value == null ? "" : value.replaceAll("[^0-9]", "");
            if (sanitized.isBlank()) {
                return null;
            }
            return Integer.parseInt(sanitized);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}