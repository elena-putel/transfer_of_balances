package com.beltelecom.transfer.parser;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferReportDto;
import com.beltelecom.transfer.exception.TransferProcessingException;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TransferFileParser {

    private static final char SEPARATOR = ';';
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int DATA_FIELDS = 6;
    private static final int REPORT_FIELDS_MIN = 3;

    public List<TransferRecordDto> parseDataFile(Path filePath) {
        List<TransferRecordDto> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath, Charset.forName("Windows-1251"));
             var csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).build())
                     .build()) {

            String[] line;
            int lineNumber = 0;
            while ((line = csvReader.readNext()) != null) {
                lineNumber++;
                if (isBlankLine(line)) {
                    continue;
                }
                if (line.length < DATA_FIELDS) {
                    throw new TransferProcessingException("PARSE_ERROR",
                            "Строка " + lineNumber + ": ожидается " + DATA_FIELDS + " полей, получено " + line.length);
                }
                records.add(parseRecord(line, lineNumber));
            }
        } catch (IOException | CsvValidationException e) {
            throw new TransferProcessingException("IO_ERROR", "Ошибка чтения файла: " + filePath.getFileName(), e);
        }
        return records;
    }

    public TransferReportDto parseReportFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath, Charset.forName("Windows-1251"));
             var csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).build())
                     .build()) {

            String[] line = csvReader.readNext();
            if (line == null || isBlankLine(line)) {
                throw new TransferProcessingException("PARSE_ERROR", "Файл отчёта пуст: " + filePath.getFileName());
            }
            if (line.length < REPORT_FIELDS_MIN) {
                throw new TransferProcessingException("PARSE_ERROR",
                        "Файл отчёта: ожидается минимум " + REPORT_FIELDS_MIN + " полей");
            }

            return TransferReportDto.builder()
                    .fileName(trim(line[0]))
                    .recordCount(parseInt(line[1], "recordCount"))
                    .checksumSum(parseDecimal(line[2], "checksum"))
                    .build();
        } catch (IOException | CsvValidationException e) {
            throw new TransferProcessingException("IO_ERROR", "Ошибка чтения отчёта: " + filePath.getFileName(), e);
        }
    }

    private TransferRecordDto parseRecord(String[] line, int lineNumber) {
        return TransferRecordDto.builder()
                .lineNumber(lineNumber)
                .ndogBillingA(trim(line[0]))
                .accountA(trim(line[1]))
                .ndogBillingB(trim(line[2]))
                .fioBillingA(trim(line[3]))
                .summa(parseDecimal(line[4], "summa"))
                .billDate(parseDate(line[5], lineNumber))
                .build();
    }

    private LocalDate parseDate(String value, int lineNumber) {
        String trimmed = trim(value);
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new TransferProcessingException("PARSE_ERROR",
                    "Строка " + lineNumber + ": неверный формат даты '" + trimmed + "', ожидается dd.MM.yyyy");
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        String normalized = trim(value).replace(',', '.');
        if (normalized.isEmpty()) {
            throw new TransferProcessingException("PARSE_ERROR", "Поле " + field + " не может быть пустым");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new TransferProcessingException("PARSE_ERROR",
                    "Поле " + field + ": неверный числовой формат '" + value + "'");
        }
    }

    private int parseInt(String value, String field) {
        String trimmed = trim(value);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new TransferProcessingException("PARSE_ERROR",
                    "Поле " + field + ": ожидается целое число, получено '" + value + "'");
        }
    }

    private boolean isBlankLine(String[] line) {
        if (line == null || line.length == 0) {
            return true;
        }
        for (String field : line) {
            if (field != null && !field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
