package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;

import java.util.List;

public interface CTransferRepository {

    void saveAll(List<TransferBalance> records);

    long count();

    void deleteAll();
}
