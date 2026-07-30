package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Informix {@code sprav:c_transfer}. Только {@code ?}-плейсхолдеры —
 * иначе Spring NamedParameterJdbcTemplate воспринимает {@code :c_transfer} как параметр.
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixCTransferRepository implements CTransferRepository {

    private static final String INSERT_SQL = """
            INSERT INTO sprav:c_transfer (
                in_out, code_adm, cust_code, fio_askr, ndog_billing_a, account_a, ndog_billing_b,
                fio_billing_a, bill_date, type_serv_a, operation, summa, status, date_input, date_mod,
                cod_oper, comment, bill_type_a, type_enter, fl_file
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public InformixCTransferRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.jdbcTemplate = informixJdbcTemplate.getJdbcTemplate();
    }

    @Override
    @Transactional(transactionManager = "informixTransactionManager")
    public void saveAll(List<TransferBalance> records) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                bindInsert(ps, records.get(i), now);
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sprav:c_transfer", Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM sprav:c_transfer");
    }

    @Override
    public boolean existsByFlFile(String flFile) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sprav:c_transfer WHERE fl_file = ?",
                Long.class,
                flFile);
        return count != null && count > 0;
    }

    @Override
    public Set<String> findNdogBillingAWithBillDateInRange(Collection<String> ndogBillingA,
                                                           LocalDate fromInclusive,
                                                           LocalDate toExclusive) {
        if (ndogBillingA == null || ndogBillingA.isEmpty()) {
            return Set.of();
        }
        List<String> ndogs = ndogBillingA.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (ndogs.isEmpty()) {
            return Set.of();
        }

        String placeholders = IntStream.range(0, ndogs.size())
                .mapToObj(i -> "?")
                .collect(Collectors.joining(", "));
        String sql = """
                SELECT DISTINCT TRIM(ndog_billing_a) AS ndog_billing_a
                  FROM sprav:c_transfer
                 WHERE TRIM(ndog_billing_a) IN (%s)
                   AND bill_date >= ?
                   AND bill_date < ?
                """.formatted(placeholders);

        Object[] args = new Object[ndogs.size() + 2];
        for (int i = 0; i < ndogs.size(); i++) {
            args[i] = ndogs.get(i);
        }
        args[ndogs.size()] = Date.valueOf(fromInclusive);
        args[ndogs.size() + 1] = Date.valueOf(toExclusive);

        List<String> found = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("ndog_billing_a"), args);
        return new HashSet<>(found);
    }

    private static void bindInsert(PreparedStatement ps, TransferBalance record, LocalDateTime now)
            throws SQLException {
        LocalDateTime dateInput = record.getDateInput() != null ? record.getDateInput() : now;
        LocalDateTime dateMod = record.getDateMod() != null ? record.getDateMod() : now;
        Integer status = record.getStatus() != null ? record.getStatus() : 1;
        Short typeEnter = record.getTypeEnter() != null ? record.getTypeEnter() : 0;

        ps.setObject(1, record.getInOut());
        ps.setObject(2, record.getCodeAdm());
        ps.setObject(3, record.getCustCode());
        setNullableString(ps, 4, record.getFioAskr());
        ps.setString(5, record.getNdogBillingA());
        ps.setString(6, record.getAccountA());
        ps.setString(7, record.getNdogBillingB());
        ps.setString(8, record.getFioBillingA());
        if (record.getBillDate() != null) {
            ps.setDate(9, Date.valueOf(record.getBillDate()));
        } else {
            ps.setNull(9, Types.DATE);
        }
        ps.setObject(10, record.getTypeServA());
        ps.setObject(11, record.getOperation());
        ps.setBigDecimal(12, record.getSumma());
        ps.setInt(13, status);
        ps.setTimestamp(14, Timestamp.valueOf(dateInput));
        ps.setTimestamp(15, Timestamp.valueOf(dateMod));
        ps.setObject(16, record.getCodOper());
        setNullableString(ps, 17, record.getComment());
        ps.setObject(18, record.getBillTypeA());
        ps.setObject(19, typeEnter);
        ps.setString(20, record.getFlFile());
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }
}
