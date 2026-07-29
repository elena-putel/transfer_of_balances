package com.beltelecom.transfer.service;

import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.LoadProtocolData;
import com.beltelecom.transfer.repository.CTransferRejectedRepository;
import com.beltelecom.transfer.repository.CTransferRejectedRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сводный протокол за месяц: {@code yyyyMMdd_all.xlsx} (шаблон как {@code _error.xlsx}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyAllProtocolService {

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CTransferRejectedRepository rejectedRepository;
    private final LoadErrorExcelService errorExcelService;

    public Path writeMonthlyReport() {
        LocalDate today = LocalDate.now();
        List<CTransferRejectedRow> rows = rejectedRepository.findMonthlyRejectedExcludingToday(today);
        List<LoadProtocolData.NotLoadedRow> protocolRows = toProtocolRows(rows);

        String fileName = today.format(FILE_DATE_FMT) + "_all.xlsx";
        Path target = errorExcelService.writeNamed(fileName, protocolRows);
        log.info("Сводный протокол сохранён: {} (записей: {})", target, protocolRows.size());
        return target;
    }

    private static List<LoadProtocolData.NotLoadedRow> toProtocolRows(List<CTransferRejectedRow> rows) {
        List<LoadProtocolData.NotLoadedRow> result = new ArrayList<>(rows.size());
        for (CTransferRejectedRow row : rows) {
            int status = row.status() == null ? 0 : row.status();
            result.add(LoadProtocolData.NotLoadedRow.builder()
                    .error(AskrTransferStatus.formatWithCode(status, row.errorName()))
                    .transferDate(row.billDate())
                    .applicationNumber(row.ndogBillingA())
                    .fio(row.fioBillingA())
                    .contractNumber(row.ndogBillingB())
                    .build());
        }
        result.sort(Comparator.comparing(
                LoadProtocolData.NotLoadedRow::getTransferDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }
}
