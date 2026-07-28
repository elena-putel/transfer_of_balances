package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.dto.LoadProtocolData;
import com.beltelecom.transfer.dto.ProcessResponse;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferReportDto;
import com.beltelecom.transfer.dto.TransferStatusResponse;
import com.beltelecom.transfer.dto.ValidationErrorDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.entity.TransferLoadLog;
import com.beltelecom.transfer.exception.TransferProcessingException;
import com.beltelecom.transfer.exception.ValidationFailedException;
import com.beltelecom.transfer.mapper.TransferMapper;
import com.beltelecom.transfer.metrics.TransferMetrics;
import com.beltelecom.transfer.parser.TransferFileParser;
import com.beltelecom.transfer.repository.TransferLoadLogRepository;
import com.beltelecom.transfer.validation.TransferRecordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferProcessingService {

    private final TransferProperties properties;
    private final TransferFileParser fileParser;
    private final TransferRecordValidator validator;
    private final TransferPersistenceService persistenceService;
    private final TransferLoadLogRepository loadLogRepository;
    private final TransferMapper transferMapper;
    private final TransferMetrics metrics;
    private final LoadProtocolAssembler protocolAssembler;
    private final LoadProtocolExcelService protocolExcelService;
    private final LoadErrorExcelService errorExcelService;

    public ProcessResponse processIncomingFiles() {
        long startTime = System.currentTimeMillis();
        Path inputDir = Path.of(properties.getInputDirectory());
        ensureDirectoryExists(inputDir);

        List<Path> dataFiles = findDataFiles(inputDir);
        if (dataFiles.isEmpty()) {
            return ProcessResponse.builder()
                    .message("Файлы для обработки не найдены в " + inputDir)
                    .build();
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<FileProcessResult>> futures = dataFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> processFilePair(file), executor))
                    .toList();

            List<FileProcessResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            int succeeded = (int) results.stream().filter(FileProcessResult::success).count();
            int failed = results.size() - succeeded;
            int totalRecords = results.stream().mapToInt(FileProcessResult::recordsProcessed).sum();

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordDuration(duration);

            return ProcessResponse.builder()
                    .filesProcessed(results.size())
                    .filesSucceeded(succeeded)
                    .filesFailed(failed)
                    .totalRecords(totalRecords)
                    .message(String.format("Обработано файлов: %d, успешно: %d, ошибок: %d, записей: %d, время: %d ms",
                            results.size(), succeeded, failed, totalRecords, duration))
                    .build();
        }
    }

    public TransferStatusResponse getLastLoadStatus() {
        return loadLogRepository.findTopByOrderByStartedAtDesc()
                .map(transferMapper::toStatusResponse)
                .orElse(TransferStatusResponse.builder()
                        .status(null)
                        .message("Загрузки ещё не выполнялись")
                        .build());
    }

    private FileProcessResult processFilePair(Path dataFile) {
        String fileName = dataFile.getFileName().toString();
        Path reportFile = resolveReportFile(dataFile);
        TransferLoadLog loadLog = createLoadLog(fileName, reportFile);
        LocalDateTime startedAt = loadLog.getStartedAt();
        List<TransferRecordDto> records = List.of();

        try {
            if (!Files.exists(reportFile)) {
                String error = "Не найден файл отчёта: " + reportFile.getFileName();
                failLoad(loadLog, TransferLoadLog.LoadStatus.FAILED, error);
                writeFailureReports(fileName, records, startedAt, loadLog.getFinishedAt(), error);
                moveToError(dataFile, reportFile);
                metrics.incrementErrors(1);
                return new FileProcessResult(false, 0);
            }

            records = fileParser.parseDataFile(dataFile);
            TransferReportDto report = fileParser.parseReportFile(reportFile);

            if (!fileName.equals(report.getFileName())) {
                String error = "Имя файла в отчёте (" + report.getFileName() + ") не совпадает с " + fileName;
                failLoad(loadLog, TransferLoadLog.LoadStatus.VALIDATION_ERROR, error);
                writeFailureReports(fileName, records, startedAt, loadLog.getFinishedAt(), error);
                moveToError(dataFile, reportFile);
                metrics.incrementErrors(1);
                return new FileProcessResult(false, 0);
            }

            BigDecimal calculatedChecksum = records.stream()
                    .map(TransferRecordDto::getSumma)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            loadLog.setChecksumExpected(report.getChecksumSum());
            loadLog.setChecksumCalculated(calculatedChecksum);
            loadLog.setRecordsTotal(records.size());

            if (report.getChecksumSum() != null
                    && calculatedChecksum.compareTo(report.getChecksumSum()) != 0) {
                String error = "Fail Fast: контрольная сумма не совпадает. Ожидается "
                        + report.getChecksumSum() + ", рассчитано " + calculatedChecksum;
                failLoad(loadLog, TransferLoadLog.LoadStatus.CHECKSUM_ERROR, error);
                writeFailureReports(fileName, records, startedAt, loadLog.getFinishedAt(), error);
                moveToError(dataFile, reportFile);
                metrics.incrementErrors(records.size());
                return new FileProcessResult(false, 0);
            }

            List<ValidationErrorDto> recordErrors = validator.validateRecords(records);
            List<ValidationErrorDto> reportErrors = validator.validateReport(report, records, calculatedChecksum);
            List<ValidationErrorDto> allErrors = new ArrayList<>(recordErrors);
            allErrors.addAll(reportErrors);

            if (!allErrors.isEmpty()) {
                String errorMsg = allErrors.stream()
                        .map(e -> "строка " + e.getLineNumber() + " [" + e.getField() + "]: " + e.getMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Ошибка валидации");
                failLoad(loadLog, TransferLoadLog.LoadStatus.VALIDATION_ERROR, errorMsg);
                writeFailureReports(fileName, records, startedAt, loadLog.getFinishedAt(), errorMsg);
                moveToError(dataFile, reportFile);
                metrics.incrementErrors(allErrors.size());
                throw new ValidationFailedException(allErrors.stream().map(ValidationErrorDto::getMessage).toList());
            }

            List<TransferBalance> saved = persistenceService.saveRecords(records, fileName);
            completeLoad(loadLog, TransferLoadLog.LoadStatus.SUCCESS, saved.size(), 0, null);
            writeSuccessReports(fileName, records, saved, startedAt, loadLog.getFinishedAt());
            moveToProcessed(dataFile, reportFile);

            metrics.incrementProcessedRecords(saved.size());
            metrics.incrementFilesProcessed();

            log.info("Файл {} успешно обработан: {} записей", fileName, saved.size());
            return new FileProcessResult(true, saved.size());

        } catch (ValidationFailedException ex) {
            return new FileProcessResult(false, 0);
        } catch (Exception ex) {
            log.error("Ошибка обработки файла {}", fileName, ex);
            failLoad(loadLog, TransferLoadLog.LoadStatus.FAILED, ex.getMessage());
            writeFailureReports(fileName, records, startedAt, loadLog.getFinishedAt(),
                    ex.getMessage() == null ? "Неизвестная ошибка" : ex.getMessage());
            moveToError(dataFile, reportFile);
            metrics.incrementErrors(1);
            return new FileProcessResult(false, 0);
        }
    }

    private void writeSuccessReports(String fileName,
                                     List<TransferRecordDto> records,
                                     List<TransferBalance> saved,
                                     LocalDateTime startedAt,
                                     LocalDateTime finishedAt) {
        try {
            LoadProtocolData protocolData = protocolAssembler.assemble(
                    fileName,
                    records,
                    saved,
                    startedAt,
                    finishedAt == null ? LocalDateTime.now() : finishedAt,
                    resolveDirectoryPath(properties.getProcessedDirectory()));
            protocolExcelService.write(protocolData);
            errorExcelService.write(protocolData);
        } catch (Exception ex) {
            log.warn("Не удалось сформировать протокол для {}: {}", fileName, ex.getMessage());
        }
    }

    private void writeFailureReports(String fileName,
                                     List<TransferRecordDto> records,
                                     LocalDateTime startedAt,
                                     LocalDateTime finishedAt,
                                     String failureReason) {
        try {
            LoadProtocolData protocolData = protocolAssembler.assembleFailed(
                    fileName,
                    records,
                    startedAt,
                    finishedAt == null ? LocalDateTime.now() : finishedAt,
                    failureReason,
                    resolveDirectoryPath(properties.getErrorDirectory()));
            protocolExcelService.write(protocolData);
            errorExcelService.write(protocolData);
        } catch (Exception ex) {
            log.warn("Не удалось сформировать протокол ошибки для {}: {}", fileName, ex.getMessage());
        }
    }

    private static String resolveDirectoryPath(String directory) {
        return Path.of(directory).toAbsolutePath().normalize().toString();
    }

    private TransferLoadLog createLoadLog(String fileName, Path reportFile) {
        TransferLoadLog logEntry = TransferLoadLog.builder()
                .flFile(fileName)
                .reportFile(reportFile.getFileName().toString())
                .status(TransferLoadLog.LoadStatus.IN_PROGRESS)
                .recordsTotal(0)
                .recordsProcessed(0)
                .recordsFailed(0)
                .startedAt(LocalDateTime.now())
                .build();
        return loadLogRepository.save(logEntry);
    }

    private void completeLoad(TransferLoadLog logEntry, TransferLoadLog.LoadStatus status,
                              int processed, int failed, String errorMessage) {
        logEntry.setStatus(status);
        logEntry.setRecordsProcessed(processed);
        logEntry.setRecordsFailed(failed);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setFinishedAt(LocalDateTime.now());
        logEntry.setDurationMs(java.time.Duration.between(logEntry.getStartedAt(), logEntry.getFinishedAt()).toMillis());
        loadLogRepository.save(logEntry);
    }

    private void failLoad(TransferLoadLog logEntry, TransferLoadLog.LoadStatus status, String errorMessage) {
        log.warn("Ошибка загрузки {}: {}", logEntry.getFlFile(), errorMessage);
        completeLoad(logEntry, status, 0, logEntry.getRecordsTotal(), errorMessage);
    }

    private List<Path> findDataFiles(Path inputDir) {
        try (Stream<Path> stream = Files.list(inputDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(properties.getDataFilePrefix()))
                    .filter(p -> p.getFileName().toString().endsWith(properties.getFileExtension()))
                    .filter(p -> !p.getFileName().toString().contains(properties.getReportSuffix()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new TransferProcessingException("IO_ERROR", "Не удалось прочитать каталог: " + inputDir, e);
        }
    }

    private Path resolveReportFile(Path dataFile) {
        String name = dataFile.getFileName().toString();
        String reportName = properties.getDataFilePrefix()
                + properties.getReportSuffix()
                + name.substring(properties.getDataFilePrefix().length());
        return dataFile.getParent().resolve(reportName);
    }

    private void moveToProcessed(Path dataFile, Path reportFile) {
        moveFile(dataFile, Path.of(properties.getProcessedDirectory()));
        if (Files.exists(reportFile)) {
            moveFile(reportFile, Path.of(properties.getProcessedDirectory()));
        }
    }

    private void moveToError(Path dataFile, Path reportFile) {
        moveFile(dataFile, Path.of(properties.getErrorDirectory()));
        if (Files.exists(reportFile)) {
            moveFile(reportFile, Path.of(properties.getErrorDirectory()));
        }
    }

    private void moveFile(Path source, Path targetDir) {
        ensureDirectoryExists(targetDir);
        try {
            Files.move(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Не удалось переместить файл {}: {}", source, e.getMessage());
        }
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new TransferProcessingException("IO_ERROR", "Не удалось создать каталог: " + dir, e);
        }
    }

    private record FileProcessResult(boolean success, int recordsProcessed) {
    }
}
