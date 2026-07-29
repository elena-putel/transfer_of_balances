package com.beltelecom.transfer.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixCTransferRejectedRepository implements CTransferRejectedRepository {

    /**
     * Informix {@code db:table} — только {@code ?}, иначе Spring парсит named-params из имён таблиц.
     */
    private static final String FIND_MONTHLY_REJECTED_SQL = """
            SELECT t.status,
                   s01.nam AS error_name,
                   t.bill_date,
                   t.ndog_billing_a,
                   t.fio_billing_a,
                   t.ndog_billing_b
              FROM sprav:c_transfer t
              LEFT JOIN ratsg:s01 s01
                ON s01.kodi = 'S327'
               AND s01.kod = t.status
             WHERE t.bill_date >= ?
               AND t.bill_date < ?
               AND t.status NOT IN (4, 9, 10)
             ORDER BY t.bill_date, t.ndog_billing_a
            """;

    private final NamedParameterJdbcTemplate informixJdbcTemplate;

    public InformixCTransferRejectedRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.informixJdbcTemplate = informixJdbcTemplate;
    }

    @Override
    public List<CTransferRejectedRow> findMonthlyRejectedExcludingToday(LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        return informixJdbcTemplate.getJdbcTemplate().query(
                FIND_MONTHLY_REJECTED_SQL,
                (rs, rowNum) -> new CTransferRejectedRow(
                        rs.getObject("status") == null ? null : rs.getInt("status"),
                        rs.getString("error_name"),
                        rs.getDate("bill_date") == null ? null : rs.getDate("bill_date").toLocalDate(),
                        trimToNull(rs.getString("ndog_billing_a")),
                        trimToNull(rs.getString("fio_billing_a")),
                        trimToNull(rs.getString("ndog_billing_b"))),
                Date.valueOf(monthStart),
                Date.valueOf(today));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
