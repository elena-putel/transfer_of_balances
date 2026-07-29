package com.beltelecom.transfer.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@code exterior:e_adsl_nsi} для тестов.
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "false")
public class InMemorySideANsiRepository implements SideANsiRepository {

    private final Map<String, List<SideANsiRecord>> storage = new ConcurrentHashMap<>();

    public void put(BigDecimal ndog, BigDecimal abId, List<SideANsiRecord> records) {
        storage.put(key(ndog, abId), new ArrayList<>(records));
    }

    public void clear() {
        storage.clear();
    }

    @Override
    public List<SideANsiRecord> findByNdogAndAbId(BigDecimal ndog, BigDecimal abId) {
        List<SideANsiRecord> found = storage.get(key(ndog, abId));
        return found == null ? List.of() : List.copyOf(found);
    }

    private static String key(BigDecimal ndog, BigDecimal abId) {
        String dog = ndog == null ? "" : ndog.stripTrailingZeros().toPlainString();
        String account = abId == null ? "" : abId.stripTrailingZeros().toPlainString();
        return dog + "|" + account;
    }
}
