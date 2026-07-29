package com.beltelecom.transfer.service;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.domain.AskrTransferStatus;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.repository.SideANsiRecord;
import com.beltelecom.transfer.repository.SideANsiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Определение стороны А по {@code exterior:e_adsl_nsi}.
 * <ul>
 *   <li>однозначное совпадение → {@code code_adm=region_code}, {@code type_serv_a=bilcategory}</li>
 *   <li>не найден → status=14</li>
 *   <li>несколько → status=15</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SideAResolutionService {

    private final SideANsiRepository sideANsiRepository;
    private final TransferProperties properties;

    /**
     * @return {@code true}, если сторона А определена однозначно и можно искать сторону Б
     */
    public boolean resolve(TransferBalance entity, TransferRecordDto dto) {
        BigDecimal ndog = parseNumber(dto.getNdogBillingA());
        BigDecimal abId = parseNumber(dto.getAccountA());
        if (ndog == null || abId == null) {
            applyMissing(entity);
            log.debug("Строка {}: ndog_billing_a/account_a не число — status={}",
                    dto.getLineNumber(), AskrTransferStatus.REJECT_SIDE_A_MISSING);
            return false;
        }

        List<SideANsiRecord> found = sideANsiRepository.findByNdogAndAbId(ndog, abId);

        if (found.isEmpty()) {
            applyMissing(entity);
            log.debug("Строка {}: сторона А не найдена (ndog={}, ab_id={}) — status={}",
                    dto.getLineNumber(), ndog, abId, AskrTransferStatus.REJECT_SIDE_A_MISSING);
            return false;
        }

        if (found.size() > 1) {
            applyMultiple(entity);
            log.debug("Строка {}: найдено {} записей стороны А — status={}",
                    dto.getLineNumber(), found.size(), AskrTransferStatus.REJECT_SIDE_A_MULTIPLE);
            return false;
        }

        SideANsiRecord record = found.getFirst();
        if (record.regionCode() != null) {
            entity.setCodeAdm(record.regionCode());
        }
        if (record.bilCategory() != null) {
            entity.setTypeServA(record.bilCategory());
        }
        log.debug("Строка {}: сторона А region_code={}, bilcategory={}",
                dto.getLineNumber(), record.regionCode(), record.bilCategory());
        return true;
    }

    private void applyMissing(TransferBalance entity) {
        entity.setCustCode(properties.getDefaultCustCode());
        entity.setStatus(AskrTransferStatus.REJECT_SIDE_A_MISSING);
    }

    private void applyMultiple(TransferBalance entity) {
        entity.setCustCode(properties.getDefaultCustCode());
        entity.setStatus(AskrTransferStatus.REJECT_SIDE_A_MULTIPLE);
    }

    private static BigDecimal parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
