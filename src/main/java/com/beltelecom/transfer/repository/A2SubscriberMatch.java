package com.beltelecom.transfer.repository;

import java.time.LocalDate;

/**
 * Результат поиска абонента в {@code ratsg:a2}.
 */
public record A2SubscriberMatch(int abCode, String fullName, LocalDate dateMod) {
}
