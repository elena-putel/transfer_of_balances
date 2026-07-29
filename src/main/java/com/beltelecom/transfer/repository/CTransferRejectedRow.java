package com.beltelecom.transfer.repository;

import java.time.LocalDate;

/**
 * Строка сводного протокола по {@code sprav:c_transfer}.
 */
public record CTransferRejectedRow(
        Integer status,
        String errorName,
        LocalDate billDate,
        String ndogBillingA,
        String fioBillingA,
        String ndogBillingB
) {
}
