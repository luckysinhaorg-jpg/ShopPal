package com.shoppal;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.account.ShopAccountRepository;
import com.shoppal.inventory.InventoryAdvisorService;
import com.shoppal.product.BusinessProfile;
import com.shoppal.sales.SalesRecord;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final InventoryAdvisorService advisor;
    private final AuthService authService;
    private final ShopAccountRepository accounts;

    public DashboardController(InventoryAdvisorService advisor, AuthService authService, ShopAccountRepository accounts) {
        this.advisor = advisor;
        this.authService = authService;
        this.accounts = accounts;
    }

    @GetMapping
    public DashboardSummary summary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) BusinessProfile profile) {

        Optional<ShopAccount> authAccount = authService.resolveAccount(authHeader);
        Long accountId = null;
        BusinessProfile targetProfile = profile;
        String shopName = null;
        String ownerName = null;

        if (authAccount.isPresent()) {
            ShopAccount acc = authAccount.get();
            accountId = acc.getId();
            targetProfile = (profile != null) ? profile : acc.getBusinessProfile();
            shopName = acc.getShopName();
            ownerName = acc.getOwnerName();
        } else {
            if (targetProfile == null) targetProfile = BusinessProfile.KIRANA;
            var demoAccs = accounts.findByBusinessProfile(targetProfile);
            if (!demoAccs.isEmpty()) {
                ShopAccount acc = demoAccs.get(0);
                accountId = acc.getId();
                shopName = acc.getShopName();
                ownerName = acc.getOwnerName();
            }
        }

        var products = advisor.products(accountId, targetProfile);
        int units = products.stream().mapToInt(p -> p.getSellableStock()).sum();
        BigDecimal exposure = products.stream().filter(p -> p.getPurchaseCost() != null)
                .map(p -> p.getPurchaseCost().multiply(BigDecimal.valueOf(p.getSellableStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate since = LocalDate.now().minusDays(29);
        List<SalesRecord> recent = products.stream().flatMap(p -> advisor.history(p).stream())
                .filter(s -> !s.getSaleDate().isBefore(since)).toList();
        int sold = recent.stream().mapToInt(SalesRecord::getUnitsSold).sum();

        var profileIds = products.stream().map(p -> p.getId()).collect(java.util.stream.Collectors.toSet());
        long expiryRisks = advisor.expiry(accountId, 30).stream()
                .filter(a -> profileIds.contains(a.productId()) && !a.level().equals("OK")).count();

        var orderAdvice = advisor.recommendations(accountId, targetProfile, 7, null, null, null);
        long reorderCount = orderAdvice.stream().filter(r -> r.proposedOrderQuantity() > 0).count();
        long stockRiskCount = orderAdvice.stream().filter(r -> r.riskLevel().equals("STOCKOUT_RISK")
                || r.riskLevel().equals("OVERSTOCK_RISK")).count();

        String dataSource = recent.isEmpty() ? "NO_SALES_DATA" : recent.stream().allMatch(SalesRecord::isSynthetic) ? "SYNTHETIC_DEMO" :
                recent.stream().anyMatch(SalesRecord::isSynthetic) ? "MIXED" : "IMPORTED_SALES";

        return new DashboardSummary(targetProfile.name(), products.size(), units, sold, exposure, expiryRisks, stockRiskCount, reorderCount,
                dataSource, "Inventory exposure is recorded purchase cost × sellable stock; products without purchase cost are excluded.",
                shopName, ownerName);
    }

    public record DashboardSummary(String profile, int productCount, int sellableUnits, int unitsSoldLast30Days,
            BigDecimal estimatedStockExposure, long expiryRiskBatches, long stockRiskProducts, long productsToReorder,
            String salesDataSource, String limitations, String shopName, String ownerName) {}
}
