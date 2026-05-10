package org.example.recommendationservice.service;

import java.text.Normalizer;
import java.util.Locale;

public final class CpuNameNormalizer {

    private CpuNameNormalizer() {
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = transliterateToLatin(value)
                .toLowerCase(Locale.ROOT);

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");

        normalized = normalized
                .replaceAll("[\\[\\]\\(\\)\\{\\},;:!@#$%^&*+=?\\\\|<>]", " ")
                .replace("amd", " ")
                .replace("intel", " ")
                .replace("processor", " ")
                .replace("cpu", " ")
                .replace("series", " ")
                .replace("edition", " ")
                .replace("desktop", " ");

        normalized = normalized
                .replaceAll("\\bcore\\s+ultra\\b", "core ultra")
                .replaceAll("\\bcore\\s+i\\b", "core i")
                .replaceAll("\\bryzen\\s+([3579])\\b", "ryzen $1")
                .replaceAll("\\s+(x3d|x|k|kf|f|g|u|h|hx)\\b", "$1")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return "";
        }

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