package com.beltelecom.transfer.repository;

import com.beltelecom.transfer.entity.TransferPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferPathRepository extends JpaRepository<TransferPath, Long> {

    Optional<TransferPath> findFirstByIdRegionOrderByIdAsc(Integer idRegion);
}
