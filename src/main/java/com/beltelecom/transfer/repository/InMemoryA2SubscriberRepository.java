package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.util.FioNormalizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@code ratsg:a2} для тестов.
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "false")
public class InMemoryA2SubscriberRepository implements A2SubscriberRepository {

    private final Map<String, List<A2SubscriberMatch>> storage = new ConcurrentHashMap<>();

    public void put(BigDecimal nomDogOb, String fio, List<A2SubscriberMatch> matches) {
        storage.put(key(nomDogOb, fio), new ArrayList<>(matches));
    }

    public void clear() {
        storage.clear();
    }

    @Override
    public List<A2SubscriberMatch> findMatches(BigDecimal nomDogOb, String fioFromFile) {
        List<A2SubscriberMatch> found = storage.get(key(nomDogOb, fioFromFile));
        return found == null ? List.of() : List.copyOf(found);
    }

    private static String key(BigDecimal nomDogOb, String fio) {
        String dog = nomDogOb == null ? "" : nomDogOb.stripTrailingZeros().toPlainString();
        return dog + "|" + FioNormalizer.normalize(fio);
    }
}
