package com.beltelecom.transfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_balance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cust_code", nullable = false)
    private Integer custCode;

    @Column(name = "fio_askr", length = 155)
    private String fioAskr;

    @Column(name = "ndog_billing_a", nullable = false, length = 20)
    private String ndogBillingA;

    @Column(name = "account_a", nullable = false, length = 20)
    private String accountA;

    @Column(name = "ndog_billing_b", nullable = false, length = 20)
    private String ndogBillingB;

    @Column(name = "fio_billing_a", nullable = false, length = 155)
    private String fioBillingA;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "type_serv_a", nullable = false)
    private Integer typeServA;

    @Column(name = "operation", nullable = false)
    private Short operation;

    @Column(name = "summa", nullable = false, precision = 15, scale = 0)
    private BigDecimal summa;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "date_input", nullable = false)
    private LocalDateTime dateInput;

    @Column(name = "date_mod", nullable = false)
    private LocalDateTime dateMod;

    @Column(name = "cod_oper", nullable = false)
    private Integer codOper;

    @Column(name = "comment", length = 200)
    private String comment;

    @Column(name = "bill_type_a")
    private Short billTypeA;

    @Column(name = "type_enter", nullable = false)
    private Short typeEnter;

    @Column(name = "fl_file", length = 22)
    private String flFile;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (dateInput == null) {
            dateInput = now;
        }
        dateMod = now;
        if (status == null) {
            status = 1;
        }
        if (typeEnter == null) {
            typeEnter = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateMod = LocalDateTime.now();
    }
}
