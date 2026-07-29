package com.beltelecom.transfer.repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Поиск абонента (плательщика) в Informix {@code ratsg:a2} (+ ФИО из a5/a5i/a5o).
 */
public interface A2SubscriberRepository {

    /**
     * Ищет абонентов по договору Б ({@code nom_dog_ob}, {@code client_type=1})
     * и ФИО из файла, сопоставляемому со сборным ФИО из справочников.
     */
    List<A2SubscriberMatch> findMatches(BigDecimal nomDogOb, String fioFromFile);
}
