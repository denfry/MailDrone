package ru.maildrone.core.parcel;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Генератор трек-номеров в формате Почты России: {@code ПР} + 9 цифр + {@code RU},
 * например {@code ПР123456789RU}.
 */
public final class TrackingNumbers {

    private static final String PREFIX = "ПР";
    private static final String SUFFIX = "RU";

    private TrackingNumbers() {
    }

    /** Генерирует случайный трек-номер. Уникальность проверяет вызывающий код. */
    public static String generate() {
        StringBuilder sb = new StringBuilder(13);
        sb.append(PREFIX);
        for (int i = 0; i < 9; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        sb.append(SUFFIX);
        return sb.toString();
    }

    /** Базовая проверка формата трек-номера (без учёта регистра суффикса). */
    public static boolean isValid(String tracking) {
        if (tracking == null) {
            return false;
        }
        String t = tracking.trim().toUpperCase(java.util.Locale.ROOT);
        // ПР в верхнем регистре кириллицей + 9 цифр + RU
        return t.matches("ПР\\d{9}RU");
    }

    /** Приводит ввод пользователя к каноничному виду. */
    public static String normalize(String tracking) {
        if (tracking == null) {
            return "";
        }
        return tracking.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
