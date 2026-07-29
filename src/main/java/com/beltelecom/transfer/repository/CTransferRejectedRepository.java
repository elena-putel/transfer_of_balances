package com.beltelecom.transfer.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Выборка отклонённых записей из {@code sprav:c_transfer} для сводного протокола.
 */
public interface CTransferRejectedRepository {

    /**
     * Записи текущего месяца по {@code bill_date}, без сегодняшнего дня,
     * со статусом не из (4, 9, 10). Текст ошибки — из {@code ratsg:s01}.
     */
    List<CTransferRejectedRow> findMonthlyRejectedExcludingToday(LocalDate today);
}
