package com.beltelecom.transfer.parser;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransferFileParserTest {

    private TransferFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new TransferFileParser();
    }

    @Test
    void shouldParseValidDataFile() {
        Path file = Path.of("src/test/resources/testdata/epb20260701090706.045");
        List<TransferRecordDto> records = parser.parseDataFile(file);

        assertThat(records).hasSize(8);
        assertThat(records.get(0).getNdogBillingA()).isEqualTo("1707010487904");
        assertThat(records.get(0).getAccountA()).isEqualTo("74813106");
        assertThat(records.get(0).getNdogBillingB()).isEqualTo("17070104879");
        assertThat(records.get(0).getFioBillingA()).isEqualTo("Куренкова Светлана Ивановна");
        assertThat(records.get(0).getSumma()).isEqualByComparingTo(new BigDecimal("-11.2378"));
        assertThat(records.get(0).getBillDate().toString()).isEqualTo("2026-07-01");
    }

    @Test
    void shouldParseValidReportFile() {
        Path file = Path.of("src/test/resources/testdata/epbr20260701090706.045");
        TransferReportDto report = parser.parseReportFile(file);

        assertThat(report.getFileName()).isEqualTo("epb20260701090706.045");
        assertThat(report.getRecordCount()).isEqualTo(8);
        assertThat(report.getChecksumSum()).isEqualByComparingTo(new BigDecimal("47.6346"));
    }
}
