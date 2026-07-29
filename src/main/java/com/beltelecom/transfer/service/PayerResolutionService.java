package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.A2SubscriberMatch;
import com.beltelecom.transfer.repository.A2SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Определение плательщика по {@code ratsg:a2} и проставление статуса записи.
 * <ul>
 *   <li>однозначное совпадение → status=4, cust_code=ab_code, fio_askr=ФИО из a2</li>
 *   <li>несколько записей → status=17</li>
 *   <li>не найден → status=18</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayerResolutionService {

    private final A2SubscriberRepository a2SubscriberRepository;
    private final TransferProperties properties;

    public void resolve(TransferBalance entity, TransferRecordDto dto) {
        BigDecimal nomDogOb = parseNomDogOb(dto.getNdogBillingB());
        if (nomDogOb == null) {
            applyNotFound(entity);
            log.debug("Строка {}: ndog_billing_b не число — status={}", dto.getLineNumber(),
                    AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
            return;
        }

        String fio = dto.getFioBillingA() == null ? "" : dto.getFioBillingA().trim();
        List<A2SubscriberMatch> matches = a2SubscriberRepository.findMatches(nomDogOb, fio);

        if (matches.isEmpty()) {
            applyNotFound(entity);
            log.debug("Строка {}: абонент не найден (nom_dog_ob={}, fio={}) — status={}",
                    dto.getLineNumber(), nomDogOb, fio,
                    AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
            return;
        }

        if (matches.size() > 1) {
            applyMultiple(entity);
            log.debug("Строка {}: найдено {} абонентов — status={}",
                    dto.getLineNumber(), matches.size(),
                    AskrTransferStatus.REJECT_MULTIPLE_SUBSCRIBERS);
            return;
        }

        A2SubscriberMatch match = matches.getFirst();
        entity.setCustCode(match.abCode());
        entity.setFioAskr(match.fullName());
        entity.setStatus(AskrTransferStatus.TO_PROCESS);
        log.debug("Строка {}: плательщик ab_code={}, fio_askr={} — status={}",
                dto.getLineNumber(), match.abCode(), match.fullName(), AskrTransferStatus.TO_PROCESS);
    }

    private void applyNotFound(TransferBalance entity) {
        entity.setCustCode(properties.getDefaultCustCode());
        entity.setStatus(AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
    }

    private void applyMultiple(TransferBalance entity) {
        entity.setCustCode(properties.getDefaultCustCode());
        entity.setStatus(AskrTransferStatus.REJECT_MULTIPLE_SUBSCRIBERS);
    }

    private static BigDecimal parseNomDogOb(String ndogBillingB) {
        if (ndogBillingB == null || ndogBillingB.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(ndogBillingB.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
