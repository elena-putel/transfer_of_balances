package com.beltelecom.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRecordDto {

    private int lineNumber;
    private String ndogBillingA;
    private String accountA;
    private String ndogBillingB;
    private String fioBillingA;
    private BigDecimal summa;
    private LocalDate billDate;
}
