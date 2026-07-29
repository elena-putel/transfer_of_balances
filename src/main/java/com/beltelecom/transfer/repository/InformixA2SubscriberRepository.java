package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.util.FioNormalizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InformixA2SubscriberRepository implements A2SubscriberRepository {

    /**
     * Informix-синтаксис {@code db:table}. Используем {@code ?}, а не named params —
     * иначе Spring воспринимает {@code :a2} как параметр.
     */
    private static final String FIND_CANDIDATES_SQL = """
            SELECT a2.ab_code,
                   CONCAT(
                       CONCAT(NVL(TRIM(a5.sirname), ''), ' '),
                       CONCAT(
                           CONCAT(NVL(TRIM(a5i.name), ''), ' '),
                           NVL(TRIM(a5o.name), '')
                       )
                   ) AS full_name
              FROM ratsg:a2 a2
              LEFT JOIN ratsg:a5 a5 ON a2.name_code = a5.sirname_code
              LEFT JOIN ratsg:a5i a5i ON a2.full_name = a5i.name_code
              LEFT JOIN ratsg:a5o a5o ON a2.full_otchestvo = a5o.name_code
             WHERE a2.client_type = 1
               AND a2.nom_dog_ob = ?
            """;

    private final NamedParameterJdbcTemplate informixJdbcTemplate;

    public InformixA2SubscriberRepository(
            @Qualifier("informixJdbcTemplate") NamedParameterJdbcTemplate informixJdbcTemplate) {
        this.informixJdbcTemplate = informixJdbcTemplate;
    }

    @Override
    public List<A2SubscriberMatch> findMatches(BigDecimal nomDogOb, String fioFromFile) {
        String expectedFio = FioNormalizer.normalize(fioFromFile);
        List<Candidate> candidates = informixJdbcTemplate.getJdbcTemplate().query(
                FIND_CANDIDATES_SQL,
                (rs, rowNum) -> new Candidate(rs.getInt("ab_code"), rs.getString("full_name")),
                nomDogOb);

        List<A2SubscriberMatch> matched = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (expectedFio.equals(FioNormalizer.normalize(candidate.fullName()))) {
                matched.add(new A2SubscriberMatch(candidate.abCode(), trimFullName(candidate.fullName())));
            }
        }
        return matched;
    }

    private static String trimFullName(String fullName) {
        return fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
    }

    private record Candidate(int abCode, String fullName) {
    }
}
