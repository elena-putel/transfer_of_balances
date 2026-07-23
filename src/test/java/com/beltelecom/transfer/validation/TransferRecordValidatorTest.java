package com.beltelecom.transfer.validation;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferReportDto;
import com.beltelecom.transfer.dto.ValidationErrorDto;
import com.beltelecom.transfer.parser.TransferFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRecordValidatorTest {

    private TransferRecordValidator validator;
    private TransferFileParser parser;

    @BeforeEach
    void setUp() {
        validator = new TransferRecordValidator();
        parser = new TransferFileParser();
    }

    @Test
    void shouldPassValidRecords() {
        List<TransferRecordDto> records = parser.parseDataFile(
                Path.of("src/test/resources/testdata/epb20260701090706.045"));
        List<ValidationErrorDto> errors = validator.validateRecords(records);
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldDetectEmptyFio() {
        List<TransferRecordDto> records = parser.parseDataFile(
                Path.of("src/test/resources/testdata/epb_invalid_fio.045"));
        List<ValidationErrorDto> errors = validator.validateRecords(records);
        assertThat(errors).anyMatch(e -> "fio_billing_a".equals(e.getField()));
    }

    @Test
    void shouldDetectInvalidAccount() {
        List<TransferRecordDto> records = parser.parseDataFile(
                Path.of("src/test/resources/testdata/epb_invalid_account.045"));
        List<ValidationErrorDto> errors = validator.validateRecords(records);
        assertThat(errors).anyMatch(e -> "account_a".equals(e.getField()));
    }

    @Test
    void shouldDetectChecksumMismatch() {
        List<TransferRecordDto> records = parser.parseDataFile(
                Path.of("src/test/resources/testdata/epb_invalid_checksum.045"));
        TransferReportDto report = parser.parseReportFile(
                Path.of("src/test/resources/testdata/epbr_invalid_checksum.045"));
        BigDecimal calculated = records.stream()
                .map(TransferRecordDto::getSumma)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ValidationErrorDto> errors = validator.validateReport(report, records, calculated);
        assertThat(errors).anyMatch(e -> "checksum".equals(e.getField()));
    }

    @ParameterizedTest
    @CsvSource({
            "1707010487904, true",
            "'', false",
            "ABC, false",
            "123456789012345678901, false"
    })
    void shouldValidateContractNumber(String value, boolean valid) {
        TransferRecordDto record = TransferRecordDto.builder()
                .lineNumber(1)
                .ndogBillingA(value)
                .accountA("74813106")
                .ndogBillingB("17070104879")
                .fioBillingA("Тест")
                .summa(new BigDecimal("1.0"))
                .billDate(java.time.LocalDate.of(2026, 7, 1))
                .build();

        List<ValidationErrorDto> errors = validator.validateRecords(List.of(record));
        if (valid) {
            assertThat(errors.stream().noneMatch(e -> "ndog_billing_a".equals(e.getField()))).isTrue();
        } else {
            assertThat(errors.stream().anyMatch(e -> "ndog_billing_a".equals(e.getField()))).isTrue();
        }
    }
}
