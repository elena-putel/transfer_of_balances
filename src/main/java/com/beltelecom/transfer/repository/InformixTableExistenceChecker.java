package com.beltelecom.transfer.repository;

import java.util.List;

/**
 * Проверка обязательных таблиц Informix перед загрузкой.
 */
public interface InformixTableExistenceChecker {

    /**
     * @return полные имена отсутствующих таблиц ({@code db:table}), пустой список если всё на месте
     */
    List<String> findMissingRequiredTables();
}
