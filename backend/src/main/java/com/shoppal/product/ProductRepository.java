package com.shoppal.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"supplier"})
    List<Product> findByBusinessProfileOrderByName(BusinessProfile profile);

    @EntityGraph(attributePaths = {"supplier"})
    List<Product> findByAccountIdOrderByName(Long accountId);

    @EntityGraph(attributePaths = {"supplier"})
    List<Product> findByAccountIdAndBusinessProfileOrderByName(Long accountId, BusinessProfile profile);

    Optional<Product> findByAccountIdAndSku(Long accountId, String sku);

    List<Product> findByAccountId(Long accountId);
}
