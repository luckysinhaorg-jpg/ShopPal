package com.shoppal.product;

import com.shoppal.account.ShopAccount;
import com.shoppal.supplier.Supplier;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "sku"}))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private ShopAccount account;

    @NotBlank @Column(nullable = false) private String sku;
    @NotBlank @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "business_profile", nullable = false)
    private BusinessProfile businessProfile;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;
    @DecimalMin("0.00") @Column(precision = 12, scale = 2)
    private BigDecimal purchaseCost;
    @NotNull @Min(0) @Column(nullable = false) private Integer sellableStock;
    @NotNull @Min(0) @Column(nullable = false) private Integer leadTimeDays;
    @NotNull @Min(0) @Column(nullable = false) private Integer safetyStock;
    @Min(1) @Column(nullable = false) private Integer packSize = 1;
    @ManyToOne(fetch = FetchType.LAZY) private Supplier supplier;
    private String material;
    private String leatherGrade;

    protected Product() {}

    public Product(String sku, String name, BusinessProfile profile, BigDecimal sellingPrice,
                   BigDecimal purchaseCost, int sellableStock, int leadTimeDays, int safetyStock,
                   int packSize, Supplier supplier, String material, String leatherGrade) {
        this(null, sku, name, profile, sellingPrice, purchaseCost, sellableStock, leadTimeDays, safetyStock, packSize, supplier, material, leatherGrade);
    }

    public Product(ShopAccount account, String sku, String name, BusinessProfile profile, BigDecimal sellingPrice,
                   BigDecimal purchaseCost, int sellableStock, int leadTimeDays, int safetyStock,
                   int packSize, Supplier supplier, String material, String leatherGrade) {
        this.account = account;
        this.sku = sku; this.name = name; this.businessProfile = profile; this.sellingPrice = sellingPrice;
        this.purchaseCost = purchaseCost; this.sellableStock = sellableStock; this.leadTimeDays = leadTimeDays;
        this.safetyStock = safetyStock; this.packSize = packSize; this.supplier = supplier;
        this.material = material; this.leatherGrade = leatherGrade;
    }

    public Long getId() { return id; }
    public ShopAccount getAccount() { return account; }
    public void setAccount(ShopAccount account) { this.account = account; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BusinessProfile getBusinessProfile() { return businessProfile; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public Integer getSellableStock() { return sellableStock; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public Integer getSafetyStock() { return safetyStock; }
    public Integer getPackSize() { return packSize; }
    public Supplier getSupplier() { return supplier; }
    public String getMaterial() { return material; }
    public String getLeatherGrade() { return leatherGrade; }
}
