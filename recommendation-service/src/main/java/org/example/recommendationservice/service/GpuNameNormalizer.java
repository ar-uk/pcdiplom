package org.example.recommendationservice.service;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public final class GpuNameNormalizer {

    private static final Map<String, String> CSV_CANONICALS = loadCanonicalMap();

    private GpuNameNormalizer() {
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = normalizeGpuName(value);
        String directKey = compactKey(value);
        String normalizedKey = compactKey(normalized);

        String mapped = CSV_CANONICALS.get(directKey);
        if (mapped != null) {
            return mapped;
        }

        mapped = CSV_CANONICALS.get(normalizedKey);
        if (mapped != null) {
            return mapped;
        }

        return normalized;
    }

    private static Map<String, String> loadCanonicalMap() {
        Map<String, String> canonicalByKey = new LinkedHashMap<>();

        ClassPathResource resource = new ClassPathResource("gpu_reference_scores.csv");
        if (!resource.exists()) {
            return Map.of();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length == 0) {
                    continue;
                }

                String sourceName = columns[0].trim();
                if (sourceName.isBlank()) {
                    continue;
                }

                String canonical = normalizeGpuName(sourceName);
                registerAliases(canonicalByKey, sourceName, canonical);
            }
        } catch (Exception ignored) {
            return Map.of();
        }

        return Map.copyOf(canonicalByKey);
    }

    private static void registerAliases(Map<String, String> target, String sourceName, String canonical) {
        addAlias(target, sourceName, canonical);
        addAlias(target, canonical, canonical);

        String compactCanonical = compactKey(canonical);
        addAlias(target, compactCanonical, canonical);

        if (canonical.startsWith("rtx ")) {
            addAlias(target, canonical.substring(4), canonical);
        }
        if (canonical.startsWith("gtx ")) {
            addAlias(target, canonical.substring(4), canonical);
        }
        if (canonical.startsWith("rx ")) {
            addAlias(target, canonical.substring(3), canonical);
        }
        if (canonical.startsWith("intel arc ")) {
            addAlias(target, canonical.substring(10), canonical);
            addAlias(target, canonical.replace("intel arc ", "arc "), canonical);
        }

        if (canonical.contains(" ti super")) {
            addAlias(target, canonical.replace(" ti super", "tisuper"), canonical);
        }
        if (canonical.contains(" super") && !canonical.contains(" ti super")) {
            addAlias(target, canonical.replace(" super", "s"), canonical);
        }
        if (canonical.contains(" ti") && !canonical.contains(" ti super")) {
            addAlias(target, canonical.replace(" ti", "ti"), canonical);
        }
        if (canonical.contains(" xtx")) {
            addAlias(target, canonical.replace(" xtx", "xtx"), canonical);
        }
        if (canonical.contains(" xt")) {
            addAlias(target, canonical.replace(" xt", "xt"), canonical);
        }
        if (canonical.contains(" gre")) {
            addAlias(target, canonical.replace(" gre", "gre"), canonical);
        }
    }

    private static void addAlias(Map<String, String> target, String alias, String canonical) {
        String key = compactKey(alias);
        if (!key.isBlank()) {
            target.putIfAbsent(key, canonical);
        }
    }

    private static String normalizeGpuName(String value) {
        String normalized = transliterateToLatin(value)
                .toLowerCase(Locale.ROOT);

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");

        normalized = normalized
                .replaceAll("[\\[\\]\\(\\)\\{\\},;:!@#$%^&*+=?\\\\|<>]", " ")
                .replace("nvidia", " ")
                .replace("geforce", " ")
                .replace("amd", " ")
                .replace("radeon", " ")
                .replace("graphics", " ")
                .replace("video card", " ")
                .replace("videocard", " ")
                .replace("series", " ")
                .replace("edition", " ")
                .replace("oc", " ")
                .replace("gpu", " ");

        normalized = normalized
                .replaceAll("\\brtx\\s*([0-9]{4})\\s*s\\b", "rtx $1 super")
                .replaceAll("\\brtx\\s*([0-9]{4})\\s*super\\b", "rtx $1 super")
                .replaceAll("\\brtx\\s*([0-9]{4})\\s*ti\\s*super\\b", "rtx $1 ti super")
                .replaceAll("\\brtx\\s*([0-9]{4})\\s*ti\\b", "rtx $1 ti")
                .replaceAll("\\bgtx\\s*([0-9]{3,4})\\s*ti\\b", "gtx $1 ti")
                .replaceAll("\\brx\\s*([0-9]{4})\\s*(xtx|xt|gre)\\b", "rx $1 $2")
                .replaceAll("\\barc\\s*([ab][0-9]{3})\\s*(16gb|8gb)?\\b", "intel arc $1$2")
                .replaceAll("\\b(rtx|gtx|rx)\\s*([0-9]{3,4})\\b", "$1 $2")
                .replaceAll("\\b([0-9]{4})\\s*s\\b", "$1 super")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return "";
        }

        String compact = compactKey(normalized);
        if (compact.startsWith("rtx")) {
            return normalized.replaceFirst("^(rtx)\\s*(\\d+)(.*)$", "rtx $2$3")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        if (compact.startsWith("gtx")) {
            return normalized.replaceFirst("^(gtx)\\s*(\\d+)(.*)$", "gtx $2$3")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        if (compact.startsWith("rx")) {
            return normalized.replaceFirst("^(rx)\\s*(\\d+)(.*)$", "rx $2$3")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        if (compact.startsWith("arc")) {
            return normalized.replaceFirst("^(arc)\\s*([ab]\\d+)(.*)$", "intel arc $2$3")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return normalized;
    }

    private static String compactKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = transliterateToLatin(value)
                .toLowerCase(Locale.ROOT);

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");

        normalized = normalized
                .replace("nvidia", " ")
                .replace("geforce", " ")
                .replace("amd", " ")
                .replace("radeon", " ")
                .replace("graphics", " ")
                .replace("video card", " ")
                .replace("videocard", " ")
                .replace("series", " ")
                .replace("edition", " ")
                .replace("oc", " ")
                .replace("gpu", " ")
                .replaceAll("[^a-z0-9]", "");

        return normalized;
    }

    private static String transliterateToLatin(String input) {
        StringBuilder builder = new StringBuilder();
        for (char ch : input.toCharArray()) {
            char lower = Character.toLowerCase(ch);
            String replacement = switch (lower) {
                case 'а' -> "a";
                case 'б' -> "b";
                case 'в' -> "v";
                case 'г' -> "g";
                case 'д' -> "d";
                case 'е', 'ё' -> "e";
                case 'ж' -> "zh";
                case 'з' -> "z";
                case 'и' -> "i";
                case 'й' -> "y";
                case 'к' -> "k";
                case 'л' -> "l";
                case 'м' -> "m";
                case 'н' -> "n";
                case 'о' -> "o";
                case 'п' -> "p";
                case 'р' -> "r";
                case 'с' -> "s";
                case 'т' -> "t";
                case 'у' -> "u";
                case 'ф' -> "f";
                case 'х' -> "h";
                case 'ц' -> "ts";
                case 'ч' -> "ch";
                case 'ш' -> "sh";
                case 'щ' -> "sch";
                case 'ъ', 'ь' -> "";
                case 'ы' -> "y";
                case 'э' -> "e";
                case 'ю' -> "yu";
                case 'я' -> "ya";
                default -> null;
            };

            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}