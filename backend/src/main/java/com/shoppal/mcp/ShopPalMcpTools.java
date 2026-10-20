package com.shoppal.mcp;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.account.ShopAccountRepository;
import com.shoppal.durability.DurabilityService;
import com.shoppal.expiry.ExpiryGuardService;
import com.shoppal.inventory.InventoryAdvisorService;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.recommendations.RecommendationService;
import com.shoppal.returns.ReturnAnalysisService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ShopPalMcpTools {
    private final InventoryAdvisorService advisor;
    private final AuthService authService;
    private final ShopAccountRepository accountRepository;

    public ShopPalMcpTools(InventoryAdvisorService advisor, AuthService authService, ShopAccountRepository accountRepository) {
        this.advisor = advisor;
        this.authService = authService;
        this.accountRepository = accountRepository;
    }

    private ShopAccount resolveAccount(BusinessProfile fallbackProfile) {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                String authHeader = sra.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7).trim();
                    var user = authService.resolveAccount(token).orElse(null);
                    if (user != null) return user;
                }
            }
        } catch (Exception ignored) {
        }
        if (fallbackProfile != null) {
            return accountRepository.findByBusinessProfile(fallbackProfile).stream().findFirst().orElse(null);
        }
        return null;
    }

    @McpTool(name = "get_inventory_summary", description = "Summarize products, sellable units, approximate stock exposure, and current expiry risks for one business profile.")
    public InventorySummary inventorySummary(@McpToolParam(description = "Business profile: KIRANA, PHARMACY, or FOOTWEAR", required = true) String profile) {
        BusinessProfile business = BusinessProfile.valueOf(profile.toUpperCase(java.util.Locale.ROOT));
        ShopAccount account = resolveAccount(business);
        Long accountId = account != null ? account.getId() : null;

        List<Product> products = advisor.products(accountId, business);
        int units = products.stream().mapToInt(Product::getSellableStock).sum();
        BigDecimal exposure = products.stream().filter(p -> p.getPurchaseCost() != null)
                .map(p -> p.getPurchaseCost().multiply(BigDecimal.valueOf(p.getSellableStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var productIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());
        long expiryRisks = advisor.expiry(accountId, 30).stream()
                .filter(a -> productIds.contains(a.productId()) && !a.level().equals("OK")).count();

        String shopLabel = account != null ? " for " + account.getShopName() : "";
        return new InventorySummary(business.name(), products.size(), units, exposure, expiryRisks,
                "Exposure uses recorded unit purchase costs and sellable stock" + shopLabel + "; batches with unknown cost are excluded.");
    }

    @McpTool(name = "get_recommendations", description = "Return deterministic order recommendations with their forecast assumptions for the selected retail profile.")
    public List<RecommendationService.Recommendation> recommendations(
            @McpToolParam(description = "Business profile: KIRANA, PHARMACY, or FOOTWEAR", required = true) String profile,
            @McpToolParam(description = "Planning horizon in days, 1 to 365", required = true) int horizonDays) {
        BusinessProfile business = BusinessProfile.valueOf(profile.toUpperCase(java.util.Locale.ROOT));
        ShopAccount account = resolveAccount(business);
        Long accountId = account != null ? account.getId() : null;
        return advisor.recommendations(accountId, business, horizonDays, null, null, null);
    }

    @McpTool(name = "get_expiry_alerts", description = "List expired, unknown-expiry, and upcoming-expiry inventory batches; expired batches must not be sold.")
    public List<ExpiryGuardService.ExpiryAlert> expiryAlerts(
            @McpToolParam(description = "Upcoming alert window in days, 1 to 365", required = true) int days) {
        ShopAccount account = resolveAccount(null);
        if (account == null) {
            account = resolveAccount(BusinessProfile.KIRANA);
        }
        Long accountId = account != null ? account.getId() : null;
        return advisor.expiry(accountId, days);
    }

    @McpTool(name = "analyze_returns", description = "Analyze units sold, returns, return reasons, rate, and available estimated profit for a profile.")
    public List<ReturnAnalysisService.ReturnAnalysis> returns(
            @McpToolParam(description = "Business profile: KIRANA, PHARMACY, or FOOTWEAR", required = true) String profile) {
        BusinessProfile business = BusinessProfile.valueOf(profile.toUpperCase(java.util.Locale.ROOT));
        ShopAccount account = resolveAccount(business);
        Long accountId = account != null ? account.getId() : null;
        return advisor.returns(accountId, business);
    }

    @McpTool(name = "get_footwear_durability", description = "Show evidence-based footwear durability scores, return observation counts, and evidence strength.")
    public List<DurabilityService.DurabilityScore> durability() {
        ShopAccount account = resolveAccount(BusinessProfile.FOOTWEAR);
        Long accountId = account != null ? account.getId() : null;
        return advisor.durability(accountId);
    }

    public record InventorySummary(String profile, int productCount, int sellableUnits, BigDecimal estimatedStockExposure,
                                   long expiryRiskBatches, String limitations) {
    }
}
