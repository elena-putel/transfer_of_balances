package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferLoadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferLoadLogRepository extends JpaRepository<TransferLoadLog, Long> {

    Optional<TransferLoadLog> findTopByOrderByStartedAtDesc();

    Optional<TransferLoadLog> findByFlFile(String flFile);
}
