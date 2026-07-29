package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CTransferRepository {

    void saveAll(List<TransferBalance> records);

    long count();

    void deleteAll();

    boolean existsByFlFile(String flFile);

    /**
     * Номера приложений ({@code ndog_billing_a}), по которым уже есть запись
     * с {@code bill_date} в диапазоне {@code [fromInclusive, toExclusive)}.
     */
    Set<String> findNdogBillingAWithBillDateInRange(Collection<String> ndogBillingA,
                                                    LocalDate fromInclusive,
                                                    LocalDate toExclusive);
}
