package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.ProtocolDirectoryResolver;
import com.beltelecom.transfer.dto.LoadProtocolData;
import com.beltelecom.transfer.exception.TransferProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadProtocolExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProtocolDirectoryResolver protocolDirectoryResolver;

    public Path write(LoadProtocolData data) {
        Path dir = protocolDirectoryResolver.resolve();
        ensureDirectory(dir);

        String protocolFileName = data.getFileName() + "_prt.xlsx";
        Path target = dir.resolve(protocolFileName);

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("Протокол");
            Styles styles = Styles.create(workbook);
            int rowIdx = 0;

            rowIdx = writeTitle(sheet, rowIdx, styles);
            rowIdx = writeHeader(sheet, rowIdx, data, styles);
            if (data.isFailed()) {
                rowIdx = writeFailureBanner(sheet, rowIdx, data, styles);
            }
            rowIdx++;
            rowIdx = writeInputSection(sheet, rowIdx, data, styles);
            rowIdx++;
            rowIdx = writeOutputSection(sheet, rowIdx, data, styles);
            rowIdx++;
            writeNotLoadedSection(sheet, rowIdx, data, styles);

            sheet.setColumnWidth(0, 42 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 16 * 256);

            workbook.write(out);
            log.info("Протокол загрузки сохранён: {}", target);
            return target;
        } catch (IOException ex) {
            throw new TransferProcessingException("PROTOCOL_ERROR",
                    "Не удалось сохранить протокол: " + target, ex);
        }
    }

    private int writeTitle(Sheet sheet, int rowIdx, Styles styles) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(24);
        Cell cell = row.createCell(0);
        cell.setCellValue("Протокол загрузки файлов переноса остатков из АССОМИ");
        cell.setCellStyle(styles.title);
        CellRangeAddress merged = new CellRangeAddress(rowIdx, rowIdx, 0, 2);
        sheet.addMergedRegion(merged);
        applyRegionBorder(merged, sheet);
        row.createCell(1).setCellStyle(styles.title);
        row.createCell(2).setCellStyle(styles.title);
        return rowIdx + 2;
    }

    private int writeHeader(Sheet sheet, int rowIdx, LoadProtocolData data, Styles styles) {
        rowIdx = labelValue(sheet, rowIdx, "Версия ПО:", data.getSoftwareVersion(), styles);
        rowIdx = labelValue(sheet, rowIdx, "Дата загрузки:",
                data.getStartedAt() == null ? "" : data.getStartedAt().format(DATE_FMT), styles);
        rowIdx = labelValue(sheet, rowIdx, "Время начала загрузки:",
                data.getStartedAt() == null ? "" : data.getStartedAt().format(TIME_FMT), styles);
        rowIdx = labelValue(sheet, rowIdx, "Время окончания загрузки:",
                data.getFinishedAt() == null ? "" : data.getFinishedAt().format(TIME_FMT), styles);
        rowIdx = labelValue(sheet, rowIdx, "Продолжительность загрузки:", formatDuration(data), styles);
        rowIdx = labelValue(sheet, rowIdx, "Пользователь:", data.getUserName(), styles);
        rowIdx = labelValue(sheet, rowIdx, "Сервер:", data.getServerName(), styles);
        rowIdx = labelValue(sheet, rowIdx, "Файлы перенесены в:", data.getFilesMovedToPath(), styles);
        return rowIdx;
    }

    private int writeFailureBanner(Sheet sheet, int rowIdx, LoadProtocolData data, Styles styles) {
        Row row = sheet.createRow(rowIdx);
        String text = "ОШИБКА ЗАГРУЗКИ: " + data.getFailureReason();
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(styles.failure);
        CellRangeAddress merged = new CellRangeAddress(rowIdx, rowIdx, 0, 2);
        sheet.addMergedRegion(merged);
        applyRegionBorder(merged, sheet);
        row.createCell(1).setCellStyle(styles.failure);
        row.createCell(2).setCellStyle(styles.failure);
        adjustRowHeight(row, text, 80);
        return rowIdx + 1;
    }

    private int writeInputSection(Sheet sheet, int rowIdx, LoadProtocolData data, Styles styles) {
        rowIdx = sectionTitle(sheet, rowIdx, "1. ВХОД (Обрабатываемый файл):", styles);
        rowIdx = labelValue(sheet, rowIdx, "Имя файла для загрузки:", data.getFileName(), styles);
        rowIdx = labelValue(sheet, rowIdx, "Общее количество записей в файле:", data.getInputTotalCount(), styles);
        rowIdx = labelValue(sheet, rowIdx, "На сумму:", formatSum(data.getInputTotalSum()), styles);
        rowIdx = labelValue(sheet, rowIdx, "Из них:", "", styles);
        rowIdx = labelValue(sheet, rowIdx, "Положительные суммы:",
                "кол-во " + data.getPositiveCount() + "   на сумму " + formatSum(data.getPositiveSum()), styles);
        rowIdx = labelValue(sheet, rowIdx, "Отрицательные суммы:",
                "кол-во " + data.getNegativeCount() + "   на сумму " + formatSum(data.getNegativeSum()), styles);
        return rowIdx;
    }

    private int writeOutputSection(Sheet sheet, int rowIdx, LoadProtocolData data, Styles styles) {
        rowIdx = sectionTitle(sheet, rowIdx, "2. ВЫХОД (Загружено в базу данных):", styles);
        rowIdx = labelValue(sheet, rowIdx, "Количество записей:", data.getLoadedCount(), styles);
        rowIdx = labelValue(sheet, rowIdx, "На сумму:", formatSum(data.getLoadedSum()), styles);
        rowIdx = sectionTitle(sheet, rowIdx, "В разрезе статусов:", styles);

        int tableStart = rowIdx;
        Row header = sheet.createRow(rowIdx++);
        createCell(header, 0, "Наименование статуса", styles.tableHeader);
        createCell(header, 1, "Кол-во записей", styles.tableHeaderCenter);
        createCell(header, 2, "Сумма", styles.tableHeaderCenter);

        for (LoadProtocolData.StatusBreakdown item : data.getStatusBreakdown()) {
            Row row = sheet.createRow(rowIdx++);
            String statusName = item.getStatusName() == null ? "" : item.getStatusName();
            createCell(row, 0, statusName, styles.tableCell);
            createNumericCell(row, 1, item.getCount(), styles.tableCellNumber);
            createCell(row, 2, formatSum(item.getSum()), styles.tableCellNumber);
            adjustRowHeight(row, statusName, 40);
        }
        if (data.getStatusBreakdown().isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, "", styles.tableCell);
            createCell(row, 1, "", styles.tableCell);
            createCell(row, 2, "", styles.tableCell);
        }
        applyTableOuterBorder(sheet, tableStart, rowIdx - 1, 0, 2);
        return rowIdx;
    }

    private int writeNotLoadedSection(Sheet sheet, int rowIdx, LoadProtocolData data, Styles styles) {
        rowIdx = sectionTitle(sheet, rowIdx, "Не загружено:", styles);
        rowIdx = labelValue(sheet, rowIdx, "Количество записей:", data.getNotLoadedCount(), styles);
        rowIdx = labelValue(sheet, rowIdx, "На сумму:", formatSum(data.getNotLoadedSum()), styles);

        int tableStart = rowIdx;
        Row header = sheet.createRow(rowIdx++);
        createCell(header, 0, "Номер договора", styles.tableHeader);
        createCell(header, 1, "Комментарий", styles.tableHeader);
        createCell(header, 2, "", styles.tableHeader);
        CellRangeAddress headerCommentRange = new CellRangeAddress(header.getRowNum(), header.getRowNum(), 1, 2);
        sheet.addMergedRegion(headerCommentRange);

        for (LoadProtocolData.NotLoadedRow item : data.getNotLoadedRows()) {
            Row row = sheet.createRow(rowIdx++);
            String contract = item.getContractNumber() == null ? "" : item.getContractNumber();
            String comment = item.getComment() == null ? "" : item.getComment();
            createCell(row, 0, contract, styles.tableCell);
            CellRangeAddress commentRange = new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 2);
            sheet.addMergedRegion(commentRange);
            createCell(row, 1, comment, styles.tableCell);
            createCell(row, 2, "", styles.tableCell);
            adjustRowHeight(row, comment, 42);
        }
        if (data.getNotLoadedRows().isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, "", styles.tableCell);
            createCell(row, 1, "", styles.tableCell);
            createCell(row, 2, "", styles.tableCell);
        }
        applyTableOuterBorder(sheet, tableStart, rowIdx - 1, 0, 2);
        return rowIdx;
    }

    /**
     * Перенос текста внутри ячейки + увеличение высоты строки (ширина колонок не меняется).
     */
    private static void adjustRowHeight(Row row, String text, int charsPerLine) {
        if (text == null || text.isBlank() || charsPerLine <= 0) {
            return;
        }
        int lines = countWrappedLines(text, charsPerLine);
        row.setHeightInPoints(Math.max(18f, lines * 16f));
    }

    private static int countWrappedLines(String text, int charsPerLine) {
        int lines = 1;
        int lineLen = 0;
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() > charsPerLine) {
                if (lineLen > 0) {
                    lines++;
                    lineLen = 0;
                }
                lines += (word.length() - 1) / charsPerLine;
                lineLen = word.length() % charsPerLine;
                continue;
            }
            if (lineLen == 0) {
                lineLen = word.length();
            } else if (lineLen + 1 + word.length() <= charsPerLine) {
                lineLen += 1 + word.length();
            } else {
                lines++;
                lineLen = word.length();
            }
        }
        return Math.max(1, lines);
    }

    private static int sectionTitle(Sheet sheet, int rowIdx, String title, Styles styles) {
        Row row = sheet.createRow(rowIdx);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.section);
        CellRangeAddress merged = new CellRangeAddress(rowIdx, rowIdx, 0, 2);
        sheet.addMergedRegion(merged);
        applyRegionBorder(merged, sheet);
        row.createCell(1).setCellStyle(styles.section);
        row.createCell(2).setCellStyle(styles.section);
        return rowIdx + 1;
    }

    private static int labelValue(Sheet sheet, int rowIdx, String label, Object value, Styles styles) {
        Row row = sheet.createRow(rowIdx);
        createCell(row, 0, label, styles.label);
        CellRangeAddress valueRange = new CellRangeAddress(rowIdx, rowIdx, 1, 2);
        sheet.addMergedRegion(valueRange);
        String text = value == null ? "" : String.valueOf(value);
        createCell(row, 1, text, styles.value);
        createCell(row, 2, "", styles.value);
        applyRegionBorder(new CellRangeAddress(rowIdx, rowIdx, 0, 0), sheet);
        applyRegionBorder(valueRange, sheet);
        adjustRowHeight(row, text, 42);
        return rowIdx + 1;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createNumericCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void applyRegionBorder(CellRangeAddress region, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    private static void applyTableOuterBorder(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet);
    }

    private static String formatSum(BigDecimal sum) {
        BigDecimal value = sum == null ? BigDecimal.ZERO : sum;
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatDuration(LoadProtocolData data) {
        if (data.getStartedAt() == null || data.getFinishedAt() == null) {
            return "";
        }
        Duration duration = Duration.between(data.getStartedAt(), data.getFinishedAt());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new TransferProcessingException("IO_ERROR", "Не удалось создать каталог протокола: " + dir, e);
        }
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle section;
        private final CellStyle label;
        private final CellStyle value;
        private final CellStyle failure;
        private final CellStyle tableHeader;
        private final CellStyle tableHeaderCenter;
        private final CellStyle tableCell;
        private final CellStyle tableCellNumber;

        private Styles(CellStyle title, CellStyle section, CellStyle label, CellStyle value, CellStyle failure,
                       CellStyle tableHeader, CellStyle tableHeaderCenter,
                       CellStyle tableCell, CellStyle tableCellNumber) {
            this.title = title;
            this.section = section;
            this.label = label;
            this.value = value;
            this.failure = failure;
            this.tableHeader = tableHeader;
            this.tableHeaderCenter = tableHeaderCenter;
            this.tableCell = tableCell;
            this.tableCellNumber = tableCellNumber;
        }

        private static Styles create(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);

            Font normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 11);

            Font failureFont = workbook.createFont();
            failureFont.setBold(true);
            failureFont.setFontHeightInPoints((short) 11);
            failureFont.setColor(IndexedColors.DARK_RED.getIndex());

            CellStyle title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorder(title);

            CellStyle section = workbook.createCellStyle();
            section.setFont(boldFont);
            section.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            section.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorder(section);

            CellStyle label = workbook.createCellStyle();
            label.setFont(boldFont);
            label.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            label.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            label.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorder(label);

            CellStyle value = workbook.createCellStyle();
            value.setFont(normalFont);
            value.setVerticalAlignment(VerticalAlignment.TOP);
            value.setWrapText(true);
            setThinBorder(value);

            CellStyle failure = workbook.createCellStyle();
            failure.setFont(failureFont);
            failure.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failure.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            failure.setAlignment(HorizontalAlignment.LEFT);
            failure.setVerticalAlignment(VerticalAlignment.CENTER);
            failure.setWrapText(true);
            setThinBorder(failure);

            CellStyle tableHeader = workbook.createCellStyle();
            tableHeader.setFont(boldFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeader.setAlignment(HorizontalAlignment.LEFT);
            tableHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorder(tableHeader);

            CellStyle tableHeaderCenter = workbook.createCellStyle();
            tableHeaderCenter.cloneStyleFrom(tableHeader);
            tableHeaderCenter.setAlignment(HorizontalAlignment.CENTER);

            CellStyle tableCell = workbook.createCellStyle();
            tableCell.setFont(normalFont);
            tableCell.setVerticalAlignment(VerticalAlignment.TOP);
            tableCell.setWrapText(true);
            setThinBorder(tableCell);

            CellStyle tableCellNumber = workbook.createCellStyle();
            tableCellNumber.cloneStyleFrom(tableCell);
            tableCellNumber.setAlignment(HorizontalAlignment.RIGHT);

            return new Styles(title, section, label, value, failure,
                    tableHeader, tableHeaderCenter, tableCell, tableCellNumber);
        }

        private static void setThinBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        }
    }
}
