package com.beltelecom.transfer.service;

import com.beltelecom.transfer.dto.TransferRecordDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorProtocolAttentionCollectorTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 7);

    @Test
    void shouldDetectZeroAndThresholdAndWrongMonthAndExistingNdog() {
        TransferRecordDto record = TransferRecordDto.builder()
                .ndogBillingA("111")
                .summa(BigDecimal.ZERO)
                .billDate(LocalDate.of(2026, 6, 15))
                .build();

        List<String> reasons = ErrorProtocolAttentionCollector.collect(
                record, Set.of("111"), MONTH);

        assertThat(reasons).containsExactly(
                ErrorProtocolAttentionCollector.SUMMA_ZERO,
                ErrorProtocolAttentionCollector.BILL_DATE_NOT_CURRENT_MONTH,
                ErrorProtocolAttentionCollector.NDOG_ALREADY_IN_CURRENT_MONTH);
    }

    @Test
    void shouldDetectLargeAbsoluteSum() {
        TransferRecordDto positive = TransferRecordDto.builder()
                .ndogBillingA("222")
                .summa(new BigDecimal("300.0"))
                .billDate(LocalDate.of(2026, 7, 1))
                .build();
        TransferRecordDto negative = TransferRecordDto.builder()
                .ndogBillingA("333")
                .summa(new BigDecimal("-300.00"))
                .billDate(LocalDate.of(2026, 7, 20))
                .build();

        assertThat(ErrorProtocolAttentionCollector.collect(positive, Set.of(), MONTH))
                .containsExactly(ErrorProtocolAttentionCollector.SUMMA_THRESHOLD);
        assertThat(ErrorProtocolAttentionCollector.collect(negative, Set.of(), MONTH))
                .containsExactly(ErrorProtocolAttentionCollector.SUMMA_THRESHOLD);
    }

    @Test
    void shouldReturnEmptyWhenNoAttention() {
        TransferRecordDto record = TransferRecordDto.builder()
                .ndogBillingA("444")
                .summa(new BigDecimal("10.50"))
                .billDate(LocalDate.of(2026, 7, 29))
                .build();

        assertThat(ErrorProtocolAttentionCollector.collect(record, Set.of("999"), MONTH)).isEmpty();
    }
}
