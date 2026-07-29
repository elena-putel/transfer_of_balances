package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.InMemorySideANsiRepository;
import com.beltelecom.transfer.repository.SideANsiRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SideAResolutionServiceTest {

    private InMemorySideANsiRepository repository;
    private SideAResolutionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySideANsiRepository();
        TransferProperties properties = new TransferProperties();
        properties.setDefaultCustCode(1);
        service = new SideAResolutionService(repository, properties);
    }

    @Test
    void shouldSetCodeAdmAndTypeServAWhenUnique() {
        repository.put(new BigDecimal("1707010487904"), new BigDecimal("74813106"),
                List.of(new SideANsiRecord((short) 17, 326)));

        TransferBalance entity = new TransferBalance();
        entity.setCodeAdm((short) 1);
        entity.setTypeServA(1);

        boolean ok = service.resolve(entity, sampleDto());

        assertThat(ok).isTrue();
        assertThat(entity.getCodeAdm()).isEqualTo((short) 17);
        assertThat(entity.getTypeServA()).isEqualTo(326);
        assertThat(entity.getStatus()).isNull();
    }

    @Test
    void shouldSetStatus14WhenNotFound() {
        TransferBalance entity = new TransferBalance();

        boolean ok = service.resolve(entity, sampleDto());

        assertThat(ok).isFalse();
        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_SIDE_A_MISSING);
        assertThat(entity.getCustCode()).isEqualTo(1);
    }

    @Test
    void shouldSetStatus15WhenMultiple() {
        repository.put(new BigDecimal("1707010487904"), new BigDecimal("74813106"),
                List.of(new SideANsiRecord((short) 1, 100), new SideANsiRecord((short) 2, 200)));

        TransferBalance entity = new TransferBalance();

        boolean ok = service.resolve(entity, sampleDto());

        assertThat(ok).isFalse();
        assertThat(entity.getStatus()).isEqualTo(AskrTransferStatus.REJECT_SIDE_A_MULTIPLE);
    }

    private static TransferRecordDto sampleDto() {
        return TransferRecordDto.builder()
                .lineNumber(1)
                .ndogBillingA("1707010487904")
                .accountA("74813106")
                .ndogBillingB("17070104879")
                .fioBillingA("Тест")
                .summa(BigDecimal.ONE)
                .billDate(LocalDate.of(2026, 7, 1))
                .build();
    }
}
