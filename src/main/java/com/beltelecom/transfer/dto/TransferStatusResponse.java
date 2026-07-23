package com.beltelecom.transfer.dto;

import com.beltelecom.transfer.entity.TransferLoadLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Статус последней загрузки")
public class TransferStatusResponse {

    private String fileName;
    private String reportFile;
    private TransferLoadLog.LoadStatus status;
    private int recordsTotal;
    private int recordsProcessed;
    private int recordsFailed;
    private BigDecimal checksumExpected;
    private BigDecimal checksumCalculated;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;

    @Schema(description = "Дополнительное сообщение")
    private String message;
}
