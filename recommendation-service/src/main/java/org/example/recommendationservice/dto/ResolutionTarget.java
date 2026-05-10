package org.example.recommendationservice.dto;

public enum ResolutionTarget {
    P1080,
    P1440,
    P4K;

    public static ResolutionTarget fromText(String value) {
        if (value == null) {
            return P1080;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("4k")) {
            return P4K;
        }
        if (normalized.contains("1440")) {
            return P1440;
        }
        return P1080;
    }

    public String label() {
        return switch (this) {
            case P1080 -> "1080p";
            case P1440 -> "1440p";
            case P4K -> "4k";
        };
    }
}