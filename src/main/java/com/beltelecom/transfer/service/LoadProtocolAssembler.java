package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.InformixDataSourceProperties;
import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.LoadProtocolData;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LoadProtocolAssembler {

    private static final Pattern INFORMIX_SERVER_PATTERN =
            Pattern.compile("INFORMIXSERVER=([^;]+)", Pattern.CASE_INSENSITIVE);

    private final TransferProperties transferProperties;
    private final InformixDataSourceProperties informixProperties;

    public LoadProtocolData assemble(String fileName,
                                     List<TransferRecordDto> sourceRecords,
                                     List<TransferBalance> savedEntities,
                                     LocalDateTime startedAt,
                                     LocalDateTime finishedAt,
                                     String filesMovedToPath) {
        return assemble(fileName, sourceRecords, savedEntities, startedAt, finishedAt,
                filesMovedToPath, Set.of(), YearMonth.now());
    }

    public LoadProtocolData assemble(String fileName,
                                     List<TransferRecordDto> sourceRecords,
                                     List<TransferBalance> savedEntities,
                                     LocalDateTime startedAt,
                                     LocalDateTime finishedAt,
                                     String filesMovedToPath,
                                     Set<String> ndogsAlreadyInCurrentMonth,
                                     YearMonth currentMonth) {
        return assembleInternal(fileName, sourceRecords, savedEntities, startedAt, finishedAt,
                null, filesMovedToPath, ndogsAlreadyInCurrentMonth, currentMonth);
    }

    /**
     * Протокол при Fail Fast / ошибке: в БД ничего не загружено, все записи — «не загружено».
     */
    public LoadProtocolData assembleFailed(String fileName,
                                           List<TransferRecordDto> sourceRecords,
                                           LocalDateTime startedAt,
                                           LocalDateTime finishedAt,
                                           String failureReason,
                                           String filesMovedToPath) {
        List<TransferRecordDto> records = sourceRecords == null ? List.of() : sourceRecords;
        List<LoadProtocolData.NotLoadedRow> notLoadedRows = new ArrayList<>(records.size());
        BigDecimal notLoadedSum = BigDecimal.ZERO;

        for (TransferRecordDto source : records) {
            BigDecimal summa = nullToZero(source.getSumma());
            notLoadedSum = notLoadedSum.add(summa);
            notLoadedRows.add(LoadProtocolData.NotLoadedRow.builder()
                    .error(failureReason)
                    .transferDate(source.getBillDate())
                    .applicationNumber(source.getNdogBillingA())
                    .fio(source.getFioBillingA())
                    .contractNumber(source.getNdogBillingB())
                    .build());
        }
        notLoadedRows.sort(Comparator.comparing(
                LoadProtocolData.NotLoadedRow::getTransferDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        InputStats input = calcInputStats(records);

        return baseBuilder(fileName, startedAt, finishedAt, filesMovedToPath)
                .failureReason(failureReason)
                .inputTotalCount(input.totalCount)
                .inputTotalSum(input.totalSum)
                .positiveCount(input.positiveCount)
                .positiveSum(input.positiveSum)
                .negativeCount(input.negativeCount)
                .negativeSum(input.negativeSum)
                .loadedCount(0)
                .loadedSum(BigDecimal.ZERO)
                .statusBreakdown(List.of())
                .notLoadedCount(records.size())
                .notLoadedSum(notLoadedSum)
                .notLoadedRows(notLoadedRows)
                .errorProtocolRows(List.copyOf(notLoadedRows))
                .build();
    }

    private LoadProtocolData assembleInternal(String fileName,
                                              List<TransferRecordDto> sourceRecords,
                                              List<TransferBalance> savedEntities,
                                              LocalDateTime startedAt,
                                              LocalDateTime finishedAt,
                                              String failureReason,
                                              String filesMovedToPath,
                                              Set<String> ndogsAlreadyInCurrentMonth,
                                              YearMonth currentMonth) {
        InputStats input = calcInputStats(sourceRecords);

        Map<Integer, StatusAccumulator> byStatus = new LinkedHashMap<>();
        BigDecimal loadedSum = BigDecimal.ZERO;
        int notLoadedCount = 0;
        BigDecimal notLoadedSum = BigDecimal.ZERO;
        List<LoadProtocolData.NotLoadedRow> notLoadedRows = new ArrayList<>();
        List<LoadProtocolData.NotLoadedRow> errorProtocolRows = new ArrayList<>();

        Set<String> existingNdogs = ndogsAlreadyInCurrentMonth == null
                ? Set.of()
                : ndogsAlreadyInCurrentMonth;
        YearMonth month = currentMonth == null ? YearMonth.now() : currentMonth;

        int size = Math.min(sourceRecords.size(), savedEntities.size());
        for (int i = 0; i < size; i++) {
            TransferRecordDto source = sourceRecords.get(i);
            TransferBalance saved = savedEntities.get(i);
            BigDecimal summa = nullToZero(source.getSumma());
            int status = saved.getStatus() == null ? 0 : saved.getStatus();

            loadedSum = loadedSum.add(summa);
            byStatus.computeIfAbsent(status, StatusAccumulator::new).add(summa);

            List<String> errorParts = new ArrayList<>();
            if (AskrTransferStatus.isRejected(status)) {
                notLoadedCount++;
                notLoadedSum = notLoadedSum.add(summa);
                String statusError = AskrTransferStatus.formatWithCode(status);
                notLoadedRows.add(LoadProtocolData.NotLoadedRow.builder()
                        .error(statusError)
                        .transferDate(source.getBillDate())
                        .applicationNumber(source.getNdogBillingA())
                        .fio(source.getFioBillingA())
                        .contractNumber(source.getNdogBillingB())
                        .build());
                errorParts.add(statusError);
            }

            List<String> attention = ErrorProtocolAttentionCollector.collect(source, existingNdogs, month);
            errorParts.addAll(attention);

            if (!errorParts.isEmpty()) {
                errorProtocolRows.add(LoadProtocolData.NotLoadedRow.builder()
                        .error(ErrorProtocolAttentionCollector.join(errorParts))
                        .transferDate(source.getBillDate())
                        .applicationNumber(source.getNdogBillingA())
                        .fio(source.getFioBillingA())
                        .contractNumber(source.getNdogBillingB())
                        .build());
            }
        }
        notLoadedRows.sort(Comparator.comparing(
                LoadProtocolData.NotLoadedRow::getTransferDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        errorProtocolRows.sort(Comparator.comparing(
                LoadProtocolData.NotLoadedRow::getTransferDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<LoadProtocolData.StatusBreakdown> breakdown = byStatus.values().stream()
                .sorted(Comparator.comparingInt(acc -> acc.status))
                .map(acc -> LoadProtocolData.StatusBreakdown.builder()
                        .statusCode(acc.status)
                        .statusName(AskrTransferStatus.formatWithCode(acc.status))
                        .count(acc.count)
                        .sum(acc.sum)
                        .build())
                .toList();

        return baseBuilder(fileName, startedAt, finishedAt, filesMovedToPath)
                .failureReason(failureReason)
                .inputTotalCount(sourceRecords.size())
                .inputTotalSum(input.totalSum)
                .positiveCount(input.positiveCount)
                .positiveSum(input.positiveSum)
                .negativeCount(input.negativeCount)
                .negativeSum(input.negativeSum)
                .loadedCount(savedEntities.size())
                .loadedSum(loadedSum)
                .statusBreakdown(breakdown)
                .notLoadedCount(notLoadedCount)
                .notLoadedSum(notLoadedSum)
                .notLoadedRows(notLoadedRows)
                .errorProtocolRows(errorProtocolRows)
                .build();
    }

    private LoadProtocolData.LoadProtocolDataBuilder baseBuilder(String fileName,
                                                                   LocalDateTime startedAt,
                                                                   LocalDateTime finishedAt,
                                                                   String filesMovedToPath) {
        return LoadProtocolData.builder()
                .softwareVersion(transferProperties.getSoftwareVersion())
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .userName(informixProperties.getUsername() == null ? "" : informixProperties.getUsername())
                .serverName(extractInformixServer(informixProperties.getUrl()))
                .fileName(fileName)
                .filesMovedToPath(filesMovedToPath == null ? "" : filesMovedToPath);
    }

    private static InputStats calcInputStats(List<TransferRecordDto> sourceRecords) {
        BigDecimal totalSum = BigDecimal.ZERO;
        int positiveCount = 0;
        BigDecimal positiveSum = BigDecimal.ZERO;
        int negativeCount = 0;
        BigDecimal negativeSum = BigDecimal.ZERO;

        for (TransferRecordDto record : sourceRecords) {
            BigDecimal summa = nullToZero(record.getSumma());
            totalSum = totalSum.add(summa);
            if (summa.signum() > 0) {
                positiveCount++;
                positiveSum = positiveSum.add(summa);
            } else if (summa.signum() < 0) {
                negativeCount++;
                negativeSum = negativeSum.add(summa);
            }
        }
        return new InputStats(sourceRecords.size(), totalSum, positiveCount, positiveSum, negativeCount, negativeSum);
    }

    static String extractInformixServer(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "";
        }
        Matcher matcher = INFORMIX_SERVER_PATTERN.matcher(jdbcUrl);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record InputStats(int totalCount, BigDecimal totalSum,
                              int positiveCount, BigDecimal positiveSum,
                              int negativeCount, BigDecimal negativeSum) {
    }

    private static final class StatusAccumulator {
        private final int status;
        private int count;
        private BigDecimal sum = BigDecimal.ZERO;

        private StatusAccumulator(int status) {
            this.status = status;
        }

        private void add(BigDecimal summa) {
            count++;
            sum = sum.add(summa);
        }
    }
}
