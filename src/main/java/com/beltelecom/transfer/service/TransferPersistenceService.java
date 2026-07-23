package com.beltelecom.transfer.service;

import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.mapper.TransferMapper;
import com.beltelecom.transfer.repository.TransferBalanceRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferPersistenceService {

    private final TransferBalanceRepository balanceRepository;
    private final TransferMapper transferMapper;

    @Retry(name = "database")
    @Transactional
    public int saveRecords(List<TransferRecordDto> records, String fileName) {
        List<TransferBalance> entities = new ArrayList<>(records.size());
        for (TransferRecordDto record : records) {
            entities.add(transferMapper.toEntity(record, fileName));
        }
        try {
            balanceRepository.saveAll(entities);
            return entities.size();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Обнаружен дубликат при сохранении файла {}: {}", fileName, ex.getMessage());
            throw ex;
        }
    }
}
