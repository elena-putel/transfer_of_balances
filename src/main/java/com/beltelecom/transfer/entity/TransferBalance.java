package com.beltelecom.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Строка таблицы Informix {@code c_transfer_dev}. Таблица уже существует — DDL не выполняется.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferBalance {

    private Long tnId;
    private Short inOut;
    private Short codeAdm;
    private Integer custCode;
    private String fioAskr;
    private String ndogBillingA;
    private String accountA;
    private String ndogBillingB;
    private String fioBillingA;
    private LocalDate billDate;
    private Integer typeServA;
    private Short operation;
    private BigDecimal summa;
    private Integer status;
    private LocalDateTime dateInput;
    private LocalDateTime dateMod;
    private Integer codOper;
    private String comment;
    private Short billTypeA;
    private Short typeEnter;
    private String flFile;
}
