package com.beltelecom.transfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_load_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferLoadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_region", nullable = false)
    private Integer idRegion;

    @Column(name = "fl_file", length = 22)
    private String flFile;

    @Column(name = "report_file", length = 22)
    private String reportFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoadStatus status;

    @Column(name = "records_total", nullable = false)
    private Integer recordsTotal;

    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed;

    @Column(name = "records_failed", nullable = false)
    private Integer recordsFailed;

    @Column(name = "checksum_expected", precision = 20, scale = 4)
    private BigDecimal checksumExpected;

    @Column(name = "checksum_calculated", precision = 20, scale = 4)
    private BigDecimal checksumCalculated;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public enum LoadStatus {
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        CHECKSUM_ERROR,
        VALIDATION_ERROR
    }
}
