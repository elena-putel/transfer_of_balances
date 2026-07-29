package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory реализация для тестов (Informix отключён).
 */
@Repository
@ConditionalOnProperty(prefix = "informix.datasource", name = "enabled", havingValue = "false")
public class InMemoryCTransferRepository implements CTransferRepository {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final List<TransferBalance> storage = new CopyOnWriteArrayList<>();

    @Override
    public void saveAll(List<TransferBalance> records) {
        LocalDateTime now = LocalDateTime.now();
        for (TransferBalance record : records) {
            if (isDuplicate(record)) {
                throw new DataIntegrityViolationException(
                        "Дубликат записи c_transfer_dev: fl_file=" + record.getFlFile()
                                + ", ndog_billing_a=" + record.getNdogBillingA());
            }
            if (record.getDateInput() == null) {
                record.setDateInput(now);
            }
            if (record.getDateMod() == null) {
                record.setDateMod(now);
            }
            if (record.getStatus() == null) {
                record.setStatus(1);
            }
            if (record.getTypeEnter() == null) {
                record.setTypeEnter((short) 0);
            }
            record.setTnId(idSequence.getAndIncrement());
            storage.add(record);
        }
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        idSequence.set(1);
    }

    @Override
    public boolean existsByFlFile(String flFile) {
        if (flFile == null) {
            return false;
        }
        return storage.stream().anyMatch(r -> flFile.equals(r.getFlFile()));
    }

    @Override
    public Set<String> findNdogBillingAWithBillDateInRange(Collection<String> ndogBillingA,
                                                           LocalDate fromInclusive,
                                                           LocalDate toExclusive) {
        if (ndogBillingA == null || ndogBillingA.isEmpty()) {
            return Set.of();
        }
        Set<String> lookup = ndogBillingA.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
        if (lookup.isEmpty()) {
            return Set.of();
        }
        Set<String> found = new HashSet<>();
        for (TransferBalance record : storage) {
            String ndog = record.getNdogBillingA() == null ? null : record.getNdogBillingA().trim();
            LocalDate billDate = record.getBillDate();
            if (ndog != null && lookup.contains(ndog)
                    && billDate != null
                    && !billDate.isBefore(fromInclusive)
                    && billDate.isBefore(toExclusive)) {
                found.add(ndog);
            }
        }
        return found;
    }

    private boolean isDuplicate(TransferBalance candidate) {
        return storage.stream().anyMatch(existing ->
                equalsNullable(existing.getFlFile(), candidate.getFlFile())
                        && equalsNullable(existing.getNdogBillingA(), candidate.getNdogBillingA())
                        && equalsNullable(existing.getAccountA(), candidate.getAccountA())
                        && equalsNullable(existing.getNdogBillingB(), candidate.getNdogBillingB())
                        && equalsNullable(existing.getBillDate(), candidate.getBillDate())
                        && equalsNullable(existing.getSumma(), candidate.getSumma()));
    }

    private static boolean equalsNullable(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
