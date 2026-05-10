package org.example.partservice.util;

import java.util.*;


public class ComponentFilterUtil {

    // Global reject patterns if title contains any of these, reject immediately
    private static final Set<String> GLOBAL_REJECT_PATTERNS = Set.of(
        "системный блок",
        "готовый пк",
        "готовый ПК",
        "игровой компьютер",
        "компьютер",
        "моноблок",
        "ноутбук",
        "ноут",
        "laptop",
        "desktop pc",
        "pc build",
        "сборка",
        "workstation"
    );

    // Required patterns per component type must contain at least one for that type
    private static final Map<String, Set<String>> REQUIRED_PATTERNS = Map.of(
        "GPU", Set.of("видеокарта", "geforce", "radeon", "rtx", "gtx", "rx", "gpu", "arc", "tesla"),
        "CPU", Set.of("процессор", "ryzen", "core i3", "core i5", "core i7", "core i9", "core i3-", "core i5-", "core i7-", "core i9-", "cpu", "xeon"),
        "MOTHERBOARD", Set.of("материнская плата", "motherboard", "b650", "x670", "z790", "b760", "h770", "b560", "z490"),
        "RAM", Set.of("оперативная память", "ddr4", "ddr5", "ram", "память", "кит памяти"),
        "PSU", Set.of("блок питания", "power supply", "psu", "источник питания"),
        "CASE", Set.of("корпус", "case", "chassis"),
        "SSD", Set.of("ssd", "накопитель", "m.2", "nvme", "solid state"),
        "CPU_COOLER", Set.of("кулер", "cooler", "tower cooler", "liquid cooler", "aio"),
        "RAM_COOLER", Set.of("ОЗУ", "RAM cooler", "memory cooler"),
        "HDD", Set.of("жёсткий диск", "hdd", "3.5")
    );

    public static boolean isValidComponent(String title, String componentType) {
        if (title == null || title.isBlank()) {
            return false;
        }

        String titleLower = title.toLowerCase();

        // Hard rejection: if matches any global pattern, reject immediately
        if (GLOBAL_REJECT_PATTERNS.stream().anyMatch(titleLower::contains)) {
            return false;
        }

        // Soft requirement: must contain at least one required pattern for component type
        Set<String> required = REQUIRED_PATTERNS.get(componentType.toUpperCase());
        if (required == null || required.isEmpty()) {
            return false;
        }

        return required.stream().anyMatch(titleLower::contains);
    }

    public static Optional<String> detectComponentType(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        String titleLower = title.toLowerCase();

        // Check if global reject patterns match (full PC, laptop, etc.)
        if (GLOBAL_REJECT_PATTERNS.stream().anyMatch(titleLower::contains)) {
            return Optional.empty();
        }

        // Try to match against required patterns for each type
        for (Map.Entry<String, Set<String>> entry : REQUIRED_PATTERNS.entrySet()) {
            String type = entry.getKey();
            Set<String> patterns = entry.getValue();

            if (patterns.stream().anyMatch(titleLower::contains)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    public static double scoreComponentConfidence(String title, String componentType) {
        if (title == null || title.isBlank()) {
            return 0.0;
        }

        String titleLower = title.toLowerCase();

        // Hard reject if matches global patterns
        if (GLOBAL_REJECT_PATTERNS.stream().anyMatch(titleLower::contains)) {
            return 0.0;
        }

        Set<String> required = REQUIRED_PATTERNS.get(componentType.toUpperCase());
        if (required == null || required.isEmpty()) {
            return 0.0;
        }

        // Count how many required patterns match
        long matchCount = required.stream().filter(titleLower::contains).count();

        if (matchCount == 0) {
            return 0.0;
        }

        // Confidence increases with more pattern matches
        // 1 match = 0.7, 2 matches = 0.85, 3+ = 1.0
        return Math.min(0.7 + (matchCount * 0.15), 1.0);
    }
}
