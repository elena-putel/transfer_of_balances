package com.beltelecom.transfer.controller;

import com.beltelecom.transfer.dto.ProcessResponse;
import com.beltelecom.transfer.dto.TransferStatusResponse;
import com.beltelecom.transfer.service.TransferProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "Операции загрузки переноса балансов")
public class TransferController {

    private final TransferProcessingService processingService;

    @PostMapping("/process")
    @Operation(summary = "Запуск обработки файлов", description = "Ручной запуск загрузки файлов из входного каталога")
    public ResponseEntity<ProcessResponse> processFiles() {
        ProcessResponse response = processingService.processIncomingFiles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Статус последней загрузки", description = "Возвращает информацию о последней обработке файлов")
    public ResponseEntity<TransferStatusResponse> getStatus() {
        return ResponseEntity.ok(processingService.getLastLoadStatus());
    }
}
