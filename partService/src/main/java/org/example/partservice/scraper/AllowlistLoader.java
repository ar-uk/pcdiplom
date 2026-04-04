package org.example.partservice.scraper;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class AllowlistLoader {
    /**
     * Load allowed parts from CSV files in classpath resources
     */
    public static List<AllowlistPart> loadAllowlist() {
        List<AllowlistPart> parts = new java.util.ArrayList<>();

        try {
            List<Map.Entry<String, String>> csvByType = List.of(
                new AbstractMap.SimpleEntry<>("gpu", "allowed_list/gpu.csv"),
                new AbstractMap.SimpleEntry<>("cpu", "allowed_list/cpu.csv"),
                new AbstractMap.SimpleEntry<>("cpu_cooler", "allowed_list/cpu_cooler.csv"),
                new AbstractMap.SimpleEntry<>("cpu_cooler", "allowed_list/cooler.csv"),
                new AbstractMap.SimpleEntry<>("memory", "allowed_list/memory.csv"),
                new AbstractMap.SimpleEntry<>("internal_memory", "allowed_list/internal_hard_drive.csv"),
                new AbstractMap.SimpleEntry<>("motherboard", "allowed_list/motherboard.csv"),
                new AbstractMap.SimpleEntry<>("power_supply", "allowed_list/power_supply.csv"),
                new AbstractMap.SimpleEntry<>("power_supply", "allowed_list/psu.csv"),
                new AbstractMap.SimpleEntry<>("pc_case", "allowed_list/pc_case.csv")
            );

            for (Map.Entry<String, String> entry : csvByType) {
                try {
                    String csv = readResourceFile(entry.getValue());
                    if (csv != null && !csv.isEmpty()) {
                        parts.addAll(parseCsv(csv, entry.getKey()));
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not load " + entry.getValue() + ": " + e.getMessage());
                }
            }

            parts = dedupeParts(parts);
            System.out.println("Successfully loaded " + parts.size() + " parts from allowlist");
        } catch (Exception e) {
            System.err.println("Error loading allowlist: " + e.getMessage());
            e.printStackTrace();
        }

        return parts;
    }

    private static String readResourceFile(String resourcePath) throws Exception {
        // Try ClassLoader first
        var inputStream = AllowlistLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream != null) {
            return new String(inputStream.readAllBytes());
        }
        
        // Try with leading slash
        inputStream = AllowlistLoader.class.getResourceAsStream("/" + resourcePath);
        if (inputStream != null) {
            return new String(inputStream.readAllBytes());
        }
        
        // Return empty if not found
        System.err.println("Warning: Resource file not found: " + resourcePath);

        // Fallback for local development: read from recommendation-service resources.
        String fallbackPath = "..\\recommendation-service\\src\\main\\resources\\" + resourcePath.replace('/', '\\');
        if (Files.exists(Paths.get(fallbackPath))) {
            return Files.readString(Paths.get(fallbackPath));
        }

        return "";
    }
    
    private static List<AllowlistPart> parseCsv(String csv, String partType) {
        List<AllowlistPart> parts = new java.util.ArrayList<>();
        String[] lines = csv.split("\n");

        if (lines.length == 0) {
            return parts;
        }

        String[] headers = splitCsvLine(lines[0]);
        int nameIndex = findHeaderIndex(headers, "part_name", "name");
        int brandIndex = findHeaderIndex(headers, "brand");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = splitCsvLine(line);
            int resolvedNameIndex = nameIndex >= 0 ? nameIndex : 0;

            if (resolvedNameIndex >= fields.length) {
                continue;
            }

            String partName = fields[resolvedNameIndex].trim();
            if (partName.isBlank()) {
                continue;
            }

            String brand = "";
            if (brandIndex >= 0 && brandIndex < fields.length) {
                brand = fields[brandIndex].trim();
            }

            parts.add(new AllowlistPart(partName, partType, brand, ""));
        }

        return parts;
    }

    private static int findHeaderIndex(String[] headers, String... candidates) {
        for (int i = 0; i < headers.length; i++) {
            String normalized = headers[i].trim().toLowerCase();
            for (String candidate : candidates) {
                if (candidate.equalsIgnoreCase(normalized)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String[] splitCsvLine(String line) {
        return line.split(",");
    }

    private static List<AllowlistPart> dedupeParts(List<AllowlistPart> source) {
        Map<String, AllowlistPart> deduped = new LinkedHashMap<>();

        for (AllowlistPart item : source) {
            if (item == null || item.getPartName() == null || item.getPartType() == null) {
                continue;
            }

            String key = item.getPartType().trim().toLowerCase(Locale.ROOT) + "::"
                + item.getPartName().trim().toLowerCase(Locale.ROOT);

            deduped.putIfAbsent(key, item);
        }

        return new java.util.ArrayList<>(deduped.values());
    }
}
