package com.shoppal.sales;

import com.shoppal.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_records")
public class SalesRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Product product;
    @NotNull private LocalDate saleDate;
    @NotNull @Min(1) private Integer unitsSold;
    @Min(0) @Column(precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private boolean synthetic;

    protected SalesRecord() {}
    public SalesRecord(Product product, LocalDate saleDate, int unitsSold, BigDecimal unitPrice) {
        this(product, saleDate, unitsSold, unitPrice, false);
    }
    public SalesRecord(Product product, LocalDate saleDate, int unitsSold, BigDecimal unitPrice, boolean synthetic) {
        this.product = product; this.saleDate = saleDate; this.unitsSold = unitsSold; this.unitPrice = unitPrice; this.synthetic = synthetic;
    }
    public boolean isSynthetic() { return synthetic; }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public LocalDate getSaleDate() { return saleDate; }
    public Integer getUnitsSold() { return unitsSold; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}


