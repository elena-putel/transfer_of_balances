package com.beltelecom.transfer.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * In-memory заглушка для тестов (Informix отключён).
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "false")
public class InMemoryCTransferRejectedRepository implements CTransferRejectedRepository {

    @Override
    public List<CTransferRejectedRow> findMonthlyRejectedExcludingToday(LocalDate today) {
        return List.of();
    }
}
