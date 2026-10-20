package com.shoppal.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {
    List<SalesRecord> findByProductIdAndSaleDateBetweenOrderBySaleDate(Long productId, LocalDate start, LocalDate end);
    List<SalesRecord> findByProductAccountIdAndSaleDateBetweenOrderBySaleDate(Long accountId, LocalDate start, LocalDate end);
}
