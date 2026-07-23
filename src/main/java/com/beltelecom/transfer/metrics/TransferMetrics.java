package com.beltelecom.transfer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TransferMetrics {

    private final Counter processedRecordsCounter;
    private final Counter errorCounter;
    private final Counter filesProcessedCounter;
    private final Timer processingTimer;

    public TransferMetrics(MeterRegistry registry) {
        this.processedRecordsCounter = Counter.builder("transfer.records.processed")
                .description("Количество успешно обработанных записей")
                .register(registry);
        this.errorCounter = Counter.builder("transfer.errors.total")
                .description("Количество ошибок при обработке")
                .register(registry);
        this.filesProcessedCounter = Counter.builder("transfer.files.processed")
                .description("Количество обработанных файлов")
                .register(registry);
        this.processingTimer = Timer.builder("transfer.processing.duration")
                .description("Время выполнения обработки файлов")
                .register(registry);
    }

    public void incrementProcessedRecords(int count) {
        processedRecordsCounter.increment(count);
    }

    public void incrementErrors(int count) {
        errorCounter.increment(count);
    }

    public void incrementFilesProcessed() {
        filesProcessedCounter.increment();
    }

    public void recordDuration(long durationMs) {
        processingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
