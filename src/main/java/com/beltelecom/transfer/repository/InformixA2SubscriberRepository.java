package com.beltelecom.transfer.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixA2SubscriberRepository implements A2SubscriberRepository {

    /**
     * Informix-синтаксис {@code db:table}. Используем {@code ?}, а не named params —
     * иначе Spring воспринимает {@code :a2} как параметр.
     */
    private static final String FIND_AB_CODES_SQL = """
            SELECT ab_code
              FROM ratsg:a2
             WHERE nom_dog_ob = ?
               AND LOWER(TRIM(post_recipient)) = ?
            """;

    private final NamedParameterJdbcTemplate informixJdbcTemplate;

    public InformixA2SubscriberRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.informixJdbcTemplate = informixJdbcTemplate;
    }

    @Override
    public List<Integer> findAbCodes(BigDecimal nomDogOb, String postRecipient) {
        String fio = postRecipient == null ? "" : postRecipient.trim().toLowerCase();
        return informixJdbcTemplate.getJdbcTemplate().query(
                FIND_AB_CODES_SQL,
                (rs, rowNum) -> rs.getInt("ab_code"),
                nomDogOb,
                fio);
    }
}
