package com.beltelecom.transfer.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Заглушка для тестов: таблицы считаются существующими.
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "false")
public class InMemoryInformixTableExistenceChecker implements InformixTableExistenceChecker {

    @Override
    public List<String> findMissingRequiredTables() {
        return List.of();
    }
}
