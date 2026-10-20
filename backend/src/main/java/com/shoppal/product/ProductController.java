package com.shoppal.product;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.account.ShopAccountRepository;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;
    private final AuthService authService;
    private final ShopAccountRepository accounts;

    public ProductController(ProductRepository products, AuthService authService, ShopAccountRepository accounts) {
        this.products = products;
        this.authService = authService;
        this.accounts = accounts;
    }

    @GetMapping
    public List<ProductSummary> list(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) BusinessProfile profile) {

        Optional<ShopAccount> authAccount = authService.resolveAccount(authHeader);
        List<Product> list;

        if (authAccount.isPresent()) {
            ShopAccount acc = authAccount.get();
            BusinessProfile target = (profile != null) ? profile : acc.getBusinessProfile();
            list = products.findByAccountIdAndBusinessProfileOrderByName(acc.getId(), target);
        } else {
            BusinessProfile target = (profile != null) ? profile : BusinessProfile.KIRANA;
            var demoAccs = accounts.findByBusinessProfile(target);
            if (!demoAccs.isEmpty()) {
                list = products.findByAccountIdAndBusinessProfileOrderByName(demoAccs.get(0).getId(), target);
            } else {
                list = products.findByBusinessProfileOrderByName(target);
            }
        }

        return list.stream()
                .map(p -> new ProductSummary(p.getId(), p.getSku(), p.getName(), p.getBusinessProfile(),
                        p.getSellableStock(), p.getSellingPrice(), p.getPurchaseCost()))
                .toList();
    }

    public record ProductSummary(Long id, String sku, String name, BusinessProfile profile,
                                 Integer stock, BigDecimal sellingPrice, BigDecimal purchaseCost) {}
}
