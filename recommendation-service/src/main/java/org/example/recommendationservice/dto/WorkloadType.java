package org.example.recommendationservice.dto;

public enum WorkloadType {
    ESPORTS,
    GAMING,
    AAA,
    WORK,
    CREATION,
    MIXED;

    public static WorkloadType fromText(String value) {
        if (value == null) {
            return MIXED;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("esport")) {
            return ESPORTS;
        }
        if (normalized.contains("aaa") || normalized.contains("single player") || normalized.contains("ultra")) {
            return AAA;
        }
        if (normalized.contains("work") || normalized.contains("office") || normalized.contains("coding") || normalized.contains("programming")) {
            return WORK;
        }
        if (normalized.contains("creation") || normalized.contains("editing") || normalized.contains("render") || normalized.contains("stream")) {
            return CREATION;
        }
        if (normalized.contains("game") || normalized.contains("gaming") || normalized.contains("cyberpunk") || normalized.contains("ray tracing") || normalized.contains("path tracing")) {
            return GAMING;
        }
        return MIXED;
    }
}