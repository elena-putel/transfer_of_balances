package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferBalanceRepository extends JpaRepository<TransferBalance, Long> {
}
