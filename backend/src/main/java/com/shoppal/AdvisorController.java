package com.shoppal;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.account.ShopAccountRepository;
import com.shoppal.durability.DurabilityService;
import com.shoppal.expiry.ExpiryGuardService;
import com.shoppal.inventory.InventoryAdvisorService;
import com.shoppal.product.BusinessProfile;
import com.shoppal.recommendations.RecommendationService;
import com.shoppal.returns.ReturnAnalysisService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AdvisorController {
    private final InventoryAdvisorService advisor;
    private final AuthService authService;
    private final ShopAccountRepository accounts;

    public AdvisorController(InventoryAdvisorService advisor, AuthService authService, ShopAccountRepository accounts) {
        this.advisor = advisor;
        this.authService = authService;
        this.accounts = accounts;
    }

    private Long resolveAccountId(String authHeader, BusinessProfile profile) {
        Optional<ShopAccount> authAccount = authService.resolveAccount(authHeader);
        if (authAccount.isPresent()) {
            return authAccount.get().getId();
        }
        if (profile != null) {
            var demoAccs = accounts.findByBusinessProfile(profile);
            if (!demoAccs.isEmpty()) return demoAccs.get(0).getId();
        }
        return null;
    }

    @GetMapping("/recommendations")
    public List<RecommendationService.Recommendation> recommendations(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "KIRANA") BusinessProfile profile,
            @RequestParam(defaultValue = "7") int horizonDays,
            @RequestParam(required = false) Double seasonalMultiplier,
            @RequestParam(required = false) Double underageCost,
            @RequestParam(required = false) Double overageCost) {
        Long accountId = resolveAccountId(authHeader, profile);
        return advisor.recommendations(accountId, profile, horizonDays, seasonalMultiplier, underageCost, overageCost);
    }

    @GetMapping("/expiry")
    public List<ExpiryGuardService.ExpiryAlert> expiry(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) BusinessProfile profile) {
        Long accountId = resolveAccountId(authHeader, profile);
        return advisor.expiry(accountId, days);
    }

    @GetMapping("/returns")
    public List<ReturnAnalysisService.ReturnAnalysis> returns(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "KIRANA") BusinessProfile profile) {
        Long accountId = resolveAccountId(authHeader, profile);
        return advisor.returns(accountId, profile);
    }

    @GetMapping("/durability")
    public List<DurabilityService.DurabilityScore> durability(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long accountId = resolveAccountId(authHeader, BusinessProfile.FOOTWEAR);
        return advisor.durability(accountId);
    }

    @GetMapping("/seasonality")
    public Map<String, Object> seasonality() { return advisor.seasonality(); }
}
