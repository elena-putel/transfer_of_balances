package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "transfer.scheduled", name = "enabled", havingValue = "true")
public class TransferScheduler {

    private final TransferProcessingService processingService;

    @Scheduled(cron = "${transfer.scheduled.cron}")
    public void scheduledProcessing() {
        log.info("Запуск плановой обработки файлов переноса балансов");
        processingService.processIncomingFiles();
    }
}
