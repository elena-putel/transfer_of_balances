package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.A2SubscriberMatch;
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
    void shouldSetStatus4CustCodeAndFioAskrWhenUniqueMatch() {
        a2Repository.put(new BigDecimal("17070104879"), "Куренкова Светлана Ивановна",
                List.of(new A2SubscriberMatch(100500, "Куренкова Светлана Ивановна")));

        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("17070104879", "Куренкова Светлана Ивановна"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.TO_PROCESS);
        assertThat(entity.getCustCode()).isEqualTo(100500);
        assertThat(entity.getFioAskr()).isEqualTo("Куренкова Светлана Ивановна");
    }

    @Test
    void shouldSetStatus18WhenNotFound() {
        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("999", "Неизвестный"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
        assertThat(entity.getCustCode()).isEqualTo(1);
        assertThat(entity.getFioAskr()).isNull();
    }

    @Test
    void shouldSetStatus17WhenMultipleFound() {
        a2Repository.put(new BigDecimal("17070104879"), "Куренкова Светлана Ивановна",
                List.of(
                        new A2SubscriberMatch(1, "Куренкова Светлана Ивановна"),
                        new A2SubscriberMatch(2, "Куренкова Светлана Ивановна")));

        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("17070104879", "Куренкова Светлана Ивановна"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_MULTIPLE_SUBSCRIBERS);
        assertThat(entity.getCustCode()).isEqualTo(1);
    }

    @Test
    void shouldMatchWhenYoAndYeDiffer() {
        a2Repository.put(new BigDecimal("17070104879"), "Королёва Алёна",
                List.of(new A2SubscriberMatch(77, "Королёва Алёна")));

        TransferBalance entity = new TransferBalance();
        service.resolve(entity, sampleDto("17070104879", "Королева Алена"));

        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.TO_PROCESS);
        assertThat(entity.getCustCode()).isEqualTo(77);
        assertThat(entity.getFioAskr()).isEqualTo("Королёва Алёна");
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
