package com.beltelecom.transfer.service;

import com.beltelecom.transfer.dto.TransferRecordDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Причины внесения загруженной записи в протокол {@code _error}
 * (запись при этом всё равно сохраняется в {@code c_transfer}).
 */
final class ErrorProtocolAttentionCollector {

    static final String SUMMA_ZERO = "Сумма равна 0";
    static final String SUMMA_THRESHOLD = "Сумма >= 300.00 или <= -300.00";
    static final String BILL_DATE_NOT_CURRENT_MONTH = "Дата начисления не относится к текущему месяцу";
    static final String NDOG_ALREADY_IN_CURRENT_MONTH =
            "По номеру приложения уже есть запись с датой начисления за текущий месяц";

    private static final BigDecimal ABS_SUMMA_LIMIT = new BigDecimal("300.0");

    private ErrorProtocolAttentionCollector() {
    }

    static List<String> collect(TransferRecordDto source,
                                Set<String> ndogsAlreadyInCurrentMonth,
                                YearMonth currentMonth) {
        List<String> reasons = new ArrayList<>(4);
        BigDecimal summa = source.getSumma() == null ? BigDecimal.ZERO : source.getSumma();

        if (summa.compareTo(BigDecimal.ZERO) == 0) {
            reasons.add(SUMMA_ZERO);
        }
        if (summa.abs().compareTo(ABS_SUMMA_LIMIT) >= 0) {
            reasons.add(SUMMA_THRESHOLD);
        }

        LocalDate billDate = source.getBillDate();
        if (billDate == null || !YearMonth.from(billDate).equals(currentMonth)) {
            reasons.add(BILL_DATE_NOT_CURRENT_MONTH);
        }

        String ndog = source.getNdogBillingA();
        if (ndog != null && !ndog.isBlank()
                && ndogsAlreadyInCurrentMonth.contains(ndog.trim())) {
            reasons.add(NDOG_ALREADY_IN_CURRENT_MONTH);
        }
        return reasons;
    }

    static String join(List<String> parts) {
        return String.join("; ", parts);
    }
}
