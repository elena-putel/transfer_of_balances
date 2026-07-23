package com.beltelecom.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferReportDto {

    private String fileName;
    private int recordCount;
    private BigDecimal checksumSum;
}
