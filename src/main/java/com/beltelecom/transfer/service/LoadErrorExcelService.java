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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadErrorExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String TITLE =
            "Перечень абонентов, по которым не перенесены средства в АСКР-Э/Облтелеком";

    private final ProtocolDirectoryResolver protocolDirectoryResolver;

    public Path write(LoadProtocolData data) {
        List<LoadProtocolData.NotLoadedRow> rows = data.getErrorProtocolRows() != null
                ? data.getErrorProtocolRows()
                : data.getNotLoadedRows();
        return writeNamed(data.getFileName() + "_error.xlsx", rows);
    }

    public Path writeNamed(String outputFileName, List<LoadProtocolData.NotLoadedRow> rows) {
        Path dir = protocolDirectoryResolver.resolve();
        ensureDirectory(dir);

        Path target = dir.resolve(outputFileName);

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("Ошибки");
            Styles styles = Styles.create(workbook);

            int rowIdx = 0;
            rowIdx = writeTitle(sheet, rowIdx, styles);
            rowIdx++;
            writeTable(sheet, rowIdx, rows, styles);

            sheet.setColumnWidth(0, 55 * 256);
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 28 * 256);
            sheet.setColumnWidth(3, 40 * 256);
            sheet.setColumnWidth(4, 22 * 256);

            workbook.write(out);
            log.info("Файл протокола сохранён: {}", target);
            return target;
        } catch (IOException ex) {
            throw new TransferProcessingException("PROTOCOL_ERROR",
                    "Не удалось сохранить файл протокола: " + target, ex);
        }
    }

    private int writeTitle(Sheet sheet, int rowIdx, Styles styles) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(28);
        Cell cell = row.createCell(0);
        cell.setCellValue(TITLE);
        cell.setCellStyle(styles.title);
        CellRangeAddress merged = new CellRangeAddress(rowIdx, rowIdx, 0, 4);
        sheet.addMergedRegion(merged);
        for (int col = 1; col <= 4; col++) {
            row.createCell(col).setCellStyle(styles.title);
        }
        applyRegionBorder(merged, sheet);
        return rowIdx + 1;
    }

    private void writeTable(Sheet sheet, int rowIdx, List<LoadProtocolData.NotLoadedRow> rows, Styles styles) {
        int tableStart = rowIdx;

        Row header = sheet.createRow(rowIdx++);
        createCell(header, 0, "Ошибка", styles.tableHeader);
        createCell(header, 1, "Дата переноса средств", styles.tableHeaderCenter);
        createCell(header, 2, "Номер приложения", styles.tableHeaderCenter);
        createCell(header, 3, "ФИО", styles.tableHeader);
        createCell(header, 4, "Номер договора", styles.tableHeaderCenter);

        for (LoadProtocolData.NotLoadedRow item : rows) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, nullToEmpty(item.getError()), styles.tableCell);
            createCell(row, 1, formatDate(item.getTransferDate()), styles.tableCellCenter);
            createCell(row, 2, nullToEmpty(item.getApplicationNumber()), styles.tableCellCenter);
            createCell(row, 3, nullToEmpty(item.getFio()), styles.tableCell);
            createCell(row, 4, nullToEmpty(item.getContractNumber()), styles.tableCellCenter);
        }

        if (rows.isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            for (int col = 0; col <= 4; col++) {
                createCell(row, col, "", styles.tableCell);
            }
        }

        applyTableOuterBorder(sheet, tableStart, rowIdx - 1, 0, 4);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FMT);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
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

    private void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new TransferProcessingException("IO_ERROR", "Не удалось создать каталог протокола: " + dir, e);
        }
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle tableHeader;
        private final CellStyle tableHeaderCenter;
        private final CellStyle tableCell;
        private final CellStyle tableCellCenter;

        private Styles(CellStyle title, CellStyle tableHeader, CellStyle tableHeaderCenter,
                       CellStyle tableCell, CellStyle tableCellCenter) {
            this.title = title;
            this.tableHeader = tableHeader;
            this.tableHeaderCenter = tableHeaderCenter;
            this.tableCell = tableCell;
            this.tableCellCenter = tableCellCenter;
        }

        private static Styles create(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);

            Font normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 11);

            CellStyle title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            title.setWrapText(true);
            setThinBorder(title);

            CellStyle tableHeader = workbook.createCellStyle();
            tableHeader.setFont(boldFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeader.setAlignment(HorizontalAlignment.CENTER);
            tableHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            tableHeader.setWrapText(true);
            setThinBorder(tableHeader);

            CellStyle tableHeaderCenter = workbook.createCellStyle();
            tableHeaderCenter.cloneStyleFrom(tableHeader);

            CellStyle tableCell = workbook.createCellStyle();
            tableCell.setFont(normalFont);
            tableCell.setVerticalAlignment(VerticalAlignment.CENTER);
            tableCell.setWrapText(true);
            setThinBorder(tableCell);

            CellStyle tableCellCenter = workbook.createCellStyle();
            tableCellCenter.cloneStyleFrom(tableCell);
            tableCellCenter.setAlignment(HorizontalAlignment.CENTER);

            return new Styles(title, tableHeader, tableHeaderCenter, tableCell, tableCellCenter);
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
