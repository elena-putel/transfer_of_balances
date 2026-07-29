package com.beltelecom.transfer.domain;

import java.util.Map;

/**
 * Статусы записей переноса (АСКР / загрузка).
 */
public final class AskrTransferStatus {

    /** На обработку — плательщик определён однозначно. */
    public static final int TO_PROCESS = 4;

    /** Отмена переноса со стороны заказчика. */
    public static final int CANCELLED_BY_CUSTOMER = 9;

    /** Обработано в АСКР. */
    public static final int PROCESSED_IN_ASKR = 10;

    /** Отказ АСКР: отсутствует сторона А. */
    public static final int REJECT_SIDE_A_MISSING = 14;

    /** Отказ АСКР: несколько записей стороны А. */
    public static final int REJECT_SIDE_A_MULTIPLE = 15;

    /** Отказ АСКР: не соответствует ФИО. */
    public static final int REJECT_FIO_MISMATCH = 16;

    /** Отказ АСКР: несколько абонентов по договору. */
    public static final int REJECT_MULTIPLE_SUBSCRIBERS = 17;

    /** Отказ АСКР: не найден абонент стороны Б. */
    public static final int REJECT_SUBSCRIBER_NOT_FOUND = 18;

    private static final Map<Integer, String> NAMES = Map.of(
            TO_PROCESS, "на обработку",
            CANCELLED_BY_CUSTOMER, "отмена переноса со стороны заказчика",
            PROCESSED_IN_ASKR, "обработано в АСКР",
            REJECT_SIDE_A_MISSING, "отказ со стороны АСКР: отсутствует сторона А",
            REJECT_SIDE_A_MULTIPLE, "отказ со стороны АСКР: несколько записей стороны А",
            REJECT_FIO_MISMATCH, "отказ со стороны АСКР: не соответствует ФИО",
            REJECT_MULTIPLE_SUBSCRIBERS, "отказ со стороны АСКР: несколько абонентов по договору",
            REJECT_SUBSCRIBER_NOT_FOUND, "отказ со стороны АСКР: не найден абонент стороны Б"
    );

    private AskrTransferStatus() {
    }

    public static String nameOf(int status) {
        return NAMES.getOrDefault(status, "статус " + status);
    }

    /** Код статуса + расшифровка, например {@code 14 отказ со стороны АСКР: отсутствует сторона А}. */
    public static String formatWithCode(int status) {
        return status + " " + nameOf(status);
    }

    public static String formatWithCode(int status, String nameFromDb) {
        String name = (nameFromDb == null || nameFromDb.isBlank()) ? nameOf(status) : nameFromDb.trim();
        return status + " " + name;
    }

    public static boolean isRejected(int status) {
        return status != TO_PROCESS && status != PROCESSED_IN_ASKR;
    }

    public static boolean isSideARejected(Integer status) {
        return status != null
                && (status == REJECT_SIDE_A_MISSING || status == REJECT_SIDE_A_MULTIPLE);
    }
}
