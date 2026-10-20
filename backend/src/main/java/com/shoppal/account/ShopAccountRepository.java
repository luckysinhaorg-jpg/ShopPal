package com.shoppal.account;

import com.shoppal.product.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShopAccountRepository extends JpaRepository<ShopAccount, Long> {
    Optional<ShopAccount> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<ShopAccount> findByBusinessProfile(BusinessProfile profile);
}
