package com.beltelecom.transfer.repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Поиск стороны А в Informix {@code exterior:e_adsl_nsi}.
 */
public interface SideANsiRepository {

    /**
     * Ищет записи по договору А ({@code ndog}) и лицевому счёту ({@code ab_id}).
     */
    List<SideANsiRecord> findByNdogAndAbId(BigDecimal ndog, BigDecimal abId);
}
