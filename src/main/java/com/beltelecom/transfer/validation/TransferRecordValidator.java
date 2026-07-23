package com.beltelecom.transfer.validation;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferReportDto;
import com.beltelecom.transfer.dto.ValidationErrorDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TransferRecordValidator {

    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final int MAX_NDOG_LENGTH = 20;
    private static final int MAX_ACCOUNT_LENGTH = 20;
    private static final int MAX_FIO_LENGTH = 155;
    private static final BigDecimal MAX_SUMMA = new BigDecimal("999999999999999");

    public List<ValidationErrorDto> validateRecords(List<TransferRecordDto> records) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        for (TransferRecordDto record : records) {
            validateField(errors, record.getLineNumber(), "ndog_billing_a", record.getNdogBillingA(),
                    () -> validateContractNumber(record.getNdogBillingA()));
            validateField(errors, record.getLineNumber(), "account_a", record.getAccountA(),
                    () -> validateAccount(record.getAccountA()));
            validateField(errors, record.getLineNumber(), "ndog_billing_b", record.getNdogBillingB(),
                    () -> validateContractNumber(record.getNdogBillingB()));
            validateField(errors, record.getLineNumber(), "fio_billing_a", record.getFioBillingA(),
                    () -> validateFio(record.getFioBillingA()));
            validateField(errors, record.getLineNumber(), "summa", String.valueOf(record.getSumma()),
                    () -> validateSumma(record.getSumma()));
            validateField(errors, record.getLineNumber(), "bill_date",
                    record.getBillDate() == null ? null : record.getBillDate().toString(),
                    () -> validateBillDate(record.getBillDate()));
        }
        return errors;
    }

    public List<ValidationErrorDto> validateReport(TransferReportDto report, List<TransferRecordDto> records,
                                                    BigDecimal calculatedChecksum) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        if (report.getFileName() == null || report.getFileName().isBlank()) {
            errors.add(error(0, "fl_file", report.getFileName(), "Имя файла в отчёте обязательно"));
        } else if (report.getFileName().length() > 22) {
            errors.add(error(0, "fl_file", report.getFileName(), "Длина имени файла не более 22 символов"));
        }
        if (report.getRecordCount() != records.size()) {
            errors.add(error(0, "record_count", String.valueOf(report.getRecordCount()),
                    "Количество записей в отчёте (" + report.getRecordCount()
                            + ") не совпадает с файлом (" + records.size() + ")"));
        }
        if (report.getChecksumSum() != null && calculatedChecksum != null
                && report.getChecksumSum().compareTo(calculatedChecksum) != 0) {
            errors.add(error(0, "checksum", report.getChecksumSum().toPlainString(),
                    "Контрольная сумма не совпадает: ожидается "
                            + report.getChecksumSum().toPlainString()
                            + ", рассчитано " + calculatedChecksum.toPlainString()));
        }
        return errors;
    }

    private void validateField(List<ValidationErrorDto> errors, int line, String field, String value,
                               Runnable validator) {
        try {
            validator.run();
        } catch (ValidationException e) {
            errors.add(error(line, field, value, e.getMessage()));
        }
    }

    private void validateContractNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Номер договора обязателен");
        }
        if (value.length() > MAX_NDOG_LENGTH) {
            throw new ValidationException("Длина номера договора не более " + MAX_NDOG_LENGTH + " символов");
        }
        if (!DIGITS_ONLY.matcher(value).matches()) {
            throw new ValidationException("Номер договора должен содержать только цифры");
        }
    }

    private void validateAccount(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Номер счёта обязателен");
        }
        if (value.length() > MAX_ACCOUNT_LENGTH) {
            throw new ValidationException("Длина номера счёта не более " + MAX_ACCOUNT_LENGTH + " символов");
        }
        if (!DIGITS_ONLY.matcher(value).matches()) {
            throw new ValidationException("Номер счёта должен содержать только цифры");
        }
    }

    private void validateFio(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("ФИО абонента обязательно");
        }
        if (value.length() > MAX_FIO_LENGTH) {
            throw new ValidationException("Длина ФИО не более " + MAX_FIO_LENGTH + " символов");
        }
    }

    private void validateSumma(BigDecimal value) {
        if (value == null) {
            throw new ValidationException("Сумма обязательна");
        }
        if (value.abs().compareTo(MAX_SUMMA) > 0) {
            throw new ValidationException("Сумма превышает допустимый диапазон");
        }
    }

    private void validateBillDate(java.time.LocalDate value) {
        if (value == null) {
            throw new ValidationException("Дата начисления обязательна");
        }
    }

    private ValidationErrorDto error(int line, String field, String value, String message) {
        return ValidationErrorDto.builder()
                .lineNumber(line)
                .field(field)
                .value(value)
                .message(message)
                .build();
    }

    private static class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
}
