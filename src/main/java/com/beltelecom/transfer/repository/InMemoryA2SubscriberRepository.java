package com.beltelecom.transfer.repository;

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

    private final Map<String, List<Integer>> storage = new ConcurrentHashMap<>();

    public void put(BigDecimal nomDogOb, String postRecipient, List<Integer> abCodes) {
        storage.put(key(nomDogOb, postRecipient), new ArrayList<>(abCodes));
    }

    public void clear() {
        storage.clear();
    }

    @Override
    public List<Integer> findAbCodes(BigDecimal nomDogOb, String postRecipient) {
        List<Integer> found = storage.get(key(nomDogOb, postRecipient));
        return found == null ? List.of() : List.copyOf(found);
    }

    private static String key(BigDecimal nomDogOb, String postRecipient) {
        String fio = postRecipient == null ? "" : postRecipient.trim().toLowerCase();
        String dog = nomDogOb == null ? "" : nomDogOb.stripTrailingZeros().toPlainString();
        return dog + "|" + fio;
    }
}
