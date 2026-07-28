package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.A2SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Определение плательщика по {@code ratsg:a2} и проставление статуса записи.
 * <ul>
 *   <li>однозначное совпадение → status=4, cust_code=ab_code</li>
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

        String postRecipient = dto.getFioBillingA() == null ? "" : dto.getFioBillingA().trim();
        List<Integer> abCodes = a2SubscriberRepository.findAbCodes(nomDogOb, postRecipient);

        if (abCodes.isEmpty()) {
            applyNotFound(entity);
            log.debug("Строка {}: абонент не найден (nom_dog_ob={}, post_recipient={}) — status={}",
                    dto.getLineNumber(), nomDogOb, postRecipient,
                    AskrTransferStatus.REJECT_SUBSCRIBER_NOT_FOUND);
            return;
        }

        if (abCodes.size() > 1) {
            applyMultiple(entity);
            log.debug("Строка {}: найдено {} абонентов — status={}",
                    dto.getLineNumber(), abCodes.size(),
                    AskrTransferStatus.REJECT_MULTIPLE_SUBSCRIBERS);
            return;
        }

        entity.setCustCode(abCodes.getFirst());
        entity.setStatus(AskrTransferStatus.TO_PROCESS);
        log.debug("Строка {}: плательщик ab_code={} — status={}",
                dto.getLineNumber(), abCodes.getFirst(), AskrTransferStatus.TO_PROCESS);
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
