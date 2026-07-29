package com.beltelecom.transfer.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixSideANsiRepository implements SideANsiRepository {

    /**
     * Informix {@code db:table} — только {@code ?}, иначе Spring парсит {@code :e_adsl_nsi} как named-param.
     */
    private static final String FIND_SQL = """
            SELECT region_code, bilcategory
              FROM exterior:e_adsl_nsi
             WHERE ndog = ?
               AND ab_id = ?
               AND type_calculation = 2
               AND privat_code = 1
               AND date_ab_id_cancel IS NOT NULL
               AND date_dog_cancel IS NOT NULL
               AND indicator_ab_id_cancel = 1
               AND indicator_date_dog = 1
            """;

    private final NamedParameterJdbcTemplate informixJdbcTemplate;

    public InformixSideANsiRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.informixJdbcTemplate = informixJdbcTemplate;
    }

    @Override
    public List<SideANsiRecord> findByNdogAndAbId(BigDecimal ndog, BigDecimal abId) {
        return informixJdbcTemplate.getJdbcTemplate().query(
                FIND_SQL,
                (rs, rowNum) -> new SideANsiRecord(
                        (short) rs.getInt("region_code"),
                        rs.getObject("bilcategory") == null ? null : rs.getInt("bilcategory")),
                ndog,
                abId);
    }
}
