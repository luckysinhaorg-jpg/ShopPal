package com.shoppal.inventory;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    @Override
    @EntityGraph(attributePaths = {"product"})
    List<InventoryBatch> findAll();

    @EntityGraph(attributePaths = {"product"})
    List<InventoryBatch> findByProductAccountId(Long accountId);

    List<InventoryBatch> findByProductId(Long productId);
}
