package com.beltelecom.transfer.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка наличия таблиц через {@code systables} соответствующих БД Informix.
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixJdbcTableExistenceChecker implements InformixTableExistenceChecker {

    private static final List<RequiredTable> REQUIRED = List.of(
            new RequiredTable("sprav", "c_transfer_dev"),
            new RequiredTable("exterior", "e_adsl_nsi"),
            new RequiredTable("ratsg", "a2")
    );

    private final JdbcTemplate jdbcTemplate;

    public InformixJdbcTableExistenceChecker(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.jdbcTemplate = informixJdbcTemplate.getJdbcTemplate();
    }

    @Override
    public List<String> findMissingRequiredTables() {
        List<String> missing = new ArrayList<>();
        for (RequiredTable table : REQUIRED) {
            if (!exists(table)) {
                missing.add(table.fullName());
            }
        }
        return missing;
    }

    private boolean exists(RequiredTable table) {
        // db:systables — иначе Spring/JDBC неверно разберёт named-params в имени БД
        String sql = "SELECT COUNT(*) FROM " + table.database() + ":systables WHERE tabname = ? AND tabtype = 'T'";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, table.tableName());
            return count != null && count > 0;
        } catch (Exception ex) {
            log.warn("Не удалось проверить таблицу {}: {}", table.fullName(), ex.getMessage());
            return false;
        }
    }

    private record RequiredTable(String database, String tableName) {
        String fullName() {
            return database + ":" + tableName;
        }
    }
}
