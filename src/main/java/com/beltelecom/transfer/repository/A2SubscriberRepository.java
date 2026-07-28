package com.beltelecom.transfer.repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Поиск абонента (плательщика) в Informix {@code ratsg:a2}.
 */
public interface A2SubscriberRepository {

    /**
     * Ищет {@code ab_code} по договору ({@code nom_dog_ob}) и ФИО/получателю ({@code post_recipient}).
     */
    List<Integer> findAbCodes(BigDecimal nomDogOb, String postRecipient);
}
