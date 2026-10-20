package com.shoppal.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    List<ReturnRecord> findByProductIdOrderByReturnDateDesc(Long productId);
    List<ReturnRecord> findByProductAccountIdOrderByReturnDateDesc(Long accountId);
}
