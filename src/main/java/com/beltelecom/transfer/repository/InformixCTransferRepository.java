package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixCTransferRepository implements CTransferRepository {

    private static final String INSERT_SQL = """
            INSERT INTO c_transfer_dev (
                in_out, code_adm, cust_code, fio_askr, ndog_billing_a, account_a, ndog_billing_b,
                fio_billing_a, bill_date, type_serv_a, operation, summa, status, date_input, date_mod,
                cod_oper, comment, bill_type_a, type_enter, fl_file
            ) VALUES (
                :inOut, :codeAdm, :custCode, :fioAskr, :ndogBillingA, :accountA, :ndogBillingB,
                :fioBillingA, :billDate, :typeServA, :operation, :summa, :status, :dateInput, :dateMod,
                :codOper, :comment, :billTypeA, :typeEnter, :flFile
            )
            """;

    private final NamedParameterJdbcTemplate informixJdbcTemplate;

    public InformixCTransferRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.informixJdbcTemplate = informixJdbcTemplate;
    }

    @Override
    @Transactional(transactionManager = "informixTransactionManager")
    public void saveAll(List<TransferBalance> records) {
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = records.stream()
                .map(record -> toParams(record, now))
                .toArray(SqlParameterSource[]::new);
        informixJdbcTemplate.batchUpdate(INSERT_SQL, batch);
    }

    @Override
    public long count() {
        Long count = informixJdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT COUNT(*) FROM c_transfer_dev", Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void deleteAll() {
        informixJdbcTemplate.getJdbcTemplate().update("DELETE FROM c_transfer_dev");
    }

    private MapSqlParameterSource toParams(TransferBalance record, LocalDateTime now) {
        LocalDateTime dateInput = record.getDateInput() != null ? record.getDateInput() : now;
        LocalDateTime dateMod = record.getDateMod() != null ? record.getDateMod() : now;
        Integer status = record.getStatus() != null ? record.getStatus() : 1;
        Short typeEnter = record.getTypeEnter() != null ? record.getTypeEnter() : 0;

        return new MapSqlParameterSource()
                .addValue("inOut", record.getInOut())
                .addValue("codeAdm", record.getCodeAdm())
                .addValue("custCode", record.getCustCode())
                .addValue("fioAskr", record.getFioAskr())
                .addValue("ndogBillingA", record.getNdogBillingA())
                .addValue("accountA", record.getAccountA())
                .addValue("ndogBillingB", record.getNdogBillingB())
                .addValue("fioBillingA", record.getFioBillingA())
                .addValue("billDate", record.getBillDate() != null ? Date.valueOf(record.getBillDate()) : null)
                .addValue("typeServA", record.getTypeServA())
                .addValue("operation", record.getOperation())
                .addValue("summa", record.getSumma())
                .addValue("status", status)
                .addValue("dateInput", Timestamp.valueOf(dateInput))
                .addValue("dateMod", Timestamp.valueOf(dateMod))
                .addValue("codOper", record.getCodOper())
                .addValue("comment", record.getComment())
                .addValue("billTypeA", record.getBillTypeA())
                .addValue("typeEnter", typeEnter)
                .addValue("flFile", record.getFlFile());
    }
}
