package com.shoppal.inventory;

import com.shoppal.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_batches", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "batch_code"}))
public class InventoryBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Product product;
    @NotNull @Column(name = "batch_code", nullable = false) private String batchCode;
    @NotNull @Min(0) @Column(nullable = false) private Integer quantity;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    @Min(0) @Column(precision = 12, scale = 2) private BigDecimal purchaseCost;

    protected InventoryBatch() {}
    public InventoryBatch(Product product, String batchCode, int quantity, LocalDate manufacturingDate,
                          LocalDate expiryDate, BigDecimal purchaseCost) {
        this.product = product; this.batchCode = batchCode; this.quantity = quantity;
        this.manufacturingDate = manufacturingDate; this.expiryDate = expiryDate; this.purchaseCost = purchaseCost;
    }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getBatchCode() { return batchCode; }
    public Integer getQuantity() { return quantity; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
}
