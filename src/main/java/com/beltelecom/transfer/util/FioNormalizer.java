package com.beltelecom.transfer.util;

import java.util.Locale;

/**
 * Нормализация ФИО для сравнения файла с БД.
 */
public final class FioNormalizer {

    private FioNormalizer() {
    }

    /**
     * Trim, схлопывание пробелов, lower-case, {@code ё}/{@code Ё} → {@code е}.
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }
}
