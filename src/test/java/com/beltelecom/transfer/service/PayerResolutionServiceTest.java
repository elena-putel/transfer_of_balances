package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.InMemoryA2SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayerResolutionServiceTest {

    private InMemoryA2SubscriberRepository a2Repository;
    private PayerResolutionService service;

    @BeforeEach
    void setUp() {
        a2Repository = new InMemoryA2SubscriberRepository();
        TransferProperties properties = new TransferProperties();
        properties.setDefaultCustCode(1);
        service = new PayerResolutionService(a2Repository, properties);
    }

    @Test
    void shouldSetStatus4AndCustCodeWhenUniqueMatch() {
        a2Repository.put(new BigDecimal("17070104879"), "Куренкова Светлана Ивановна", List.of(100500));

        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("17070104879", "Куренкова Светлана Ивановна"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.TO_PROCESS);
        assertThat(entity.getCustCode()).isEqualTo(100500);
    }

    @Test
    void shouldSetStatus18WhenNotFound() {
        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("999", "Неизвестный"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
        assertThat(entity.getCustCode()).isEqualTo(1);
    }

    @Test
    void shouldSetStatus17WhenMultipleFound() {
        a2Repository.put(new BigDecimal("17070104879"), "Куренкова Светлана Ивановна", List.of(1, 2));

        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("17070104879", "Куренкова Светлана Ивановна"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_MULTIPLE_SUBSCRIBERS);
        assertThat(entity.getCustCode()).isEqualTo(1);
    }

    private static TransferRecordDto sampleDto(String ndogB, String fio) {
        return TransferRecordDto.builder()
                .lineNumber(1)
                .ndogBillingA("1707010487904")
                .accountA("74813106")
                .ndogBillingB(ndogB)
                .fioBillingA(fio)
                .summa(new BigDecimal("-11.2378"))
                .billDate(LocalDate.of(2026, 7, 1))
                .build();
    }
}
