package com.beltelecom.transfer.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class LoadProtocolData {

    String softwareVersion;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    String userName;
    String serverName;
    String fileName;

    /** Каталог, куда перенесены исходные файлы после обработки. */
    String filesMovedToPath;

    /** Причина Fail Fast / ошибки загрузки (null — успешная загрузка). */
    String failureReason;

    int inputTotalCount;
    BigDecimal inputTotalSum;
    int positiveCount;
    BigDecimal positiveSum;
    int negativeCount;
    BigDecimal negativeSum;

    int loadedCount;
    BigDecimal loadedSum;
    List<StatusBreakdown> statusBreakdown;

    int notLoadedCount;
    BigDecimal notLoadedSum;
    List<NotLoadedRow> notLoadedRows;

    /**
     * Строки протокола {@code _error.xlsx}: отсев по статусу + предупреждения
     * (нулевая/крупная сумма, дата не текущего месяца, повтор приложения за месяц).
     */
    List<NotLoadedRow> errorProtocolRows;

    public boolean isFailed() {
        return failureReason != null && !failureReason.isBlank();
    }

    @Value
    @Builder
    public static class StatusBreakdown {
        int statusCode;
        String statusName;
        int count;
        BigDecimal sum;
    }

    @Value
    @Builder
    public static class NotLoadedRow {
        String error;
        LocalDate transferDate;
        String applicationNumber;
        String fio;
        String contractNumber;

        public String getComment() {
            return error;
        }
    }
}
