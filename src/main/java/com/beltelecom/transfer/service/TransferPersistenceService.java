package com.beltelecom.transfer.service;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.mapper.TransferMapper;
import com.beltelecom.transfer.repository.CTransferRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferPersistenceService {

    private final CTransferRepository cTransferRepository;
    private final TransferMapper transferMapper;
    private final SideAResolutionService sideAResolutionService;
    private final PayerResolutionService payerResolutionService;

    @Retry(name = "database")
    public List<TransferBalance> saveRecords(List<TransferRecordDto> records, String fileName) {
        List<TransferBalance> entities = new ArrayList<>(records.size());
        for (TransferRecordDto record : records) {
            TransferBalance entity = transferMapper.toEntity(record, fileName);
            if (sideAResolutionService.resolve(entity, record)) {
                payerResolutionService.resolve(entity, record);
            }
            entities.add(entity);
        }
        try {
            cTransferRepository.saveAll(entities);
            return entities;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Обнаружен дубликат при сохранении файла {}: {}", fileName, ex.getMessage());
            throw ex;
        }
    }
}
