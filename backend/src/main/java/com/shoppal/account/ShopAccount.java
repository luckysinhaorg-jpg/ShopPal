package com.shoppal.account;

import com.shoppal.product.BusinessProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "shop_accounts")
public class ShopAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Column(nullable = false)
    private String ownerName;

    @NotBlank
    @Column(nullable = false)
    private String shopName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "business_profile", nullable = false)
    private BusinessProfile businessProfile;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ShopAccount() {}

    public ShopAccount(String username, String passwordHash, String ownerName, String shopName, BusinessProfile businessProfile) {
        this.username = username.trim().toLowerCase(java.util.Locale.ROOT);
        this.passwordHash = passwordHash;
        this.ownerName = ownerName.trim();
        this.shopName = shopName.trim();
        this.businessProfile = businessProfile;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getOwnerName() { return ownerName; }
    public String getShopName() { return shopName; }
    public BusinessProfile getBusinessProfile() { return businessProfile; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setBusinessProfile(BusinessProfile businessProfile) { this.businessProfile = businessProfile; }
}
