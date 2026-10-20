package com.shoppal.inventory;

import com.shoppal.durability.DurabilityService;
import com.shoppal.expiry.ExpiryGuardService;
import com.shoppal.forecasting.DemandForecastService;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.product.ProductRepository;
import com.shoppal.recommendations.RecommendationService;
import com.shoppal.returns.ReturnAnalysisService;
import com.shoppal.sales.ReturnRecordRepository;
import com.shoppal.sales.SalesRecordRepository;
import com.shoppal.seasonality.FestivalCalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InventoryAdvisorService {
    private final ProductRepository products;
    private final SalesRecordRepository sales;
    private final InventoryBatchRepository batches;
    private final ReturnRecordRepository returns;
    private final RecommendationService recommendations;
    private final ExpiryGuardService expiry;
    private final ReturnAnalysisService returnAnalysis;
    private final DurabilityService durability;
    private final FestivalCalendarService festivalCalendar;

    public InventoryAdvisorService(ProductRepository products, SalesRecordRepository sales, InventoryBatchRepository batches,
            ReturnRecordRepository returns, RecommendationService recommendations, ExpiryGuardService expiry,
            ReturnAnalysisService returnAnalysis, DurabilityService durability, FestivalCalendarService festivalCalendar) {
        this.products=products; this.sales=sales; this.batches=batches; this.returns=returns;
        this.recommendations=recommendations; this.expiry=expiry; this.returnAnalysis=returnAnalysis;
        this.durability=durability; this.festivalCalendar=festivalCalendar;
    }

    public List<RecommendationService.Recommendation> recommendations(BusinessProfile profile, int horizonDays,
            Double requestedMultiplier, Double underageCost, Double overageCost) {
        return recommendations(null, profile, horizonDays, requestedMultiplier, underageCost, overageCost);
    }

    public List<RecommendationService.Recommendation> recommendations(Long accountId, BusinessProfile profile, int horizonDays,
            Double requestedMultiplier, Double underageCost, Double overageCost) {
        if (horizonDays < 1 || horizonDays > 365) throw new IllegalArgumentException("horizonDays must be between 1 and 365");
        List<Product> list = products(accountId, profile);
        return list.stream().map(p -> {
            var history = history(p);
            boolean synthetic = !history.isEmpty() && history.stream().allMatch(r -> r.isSynthetic());
            int coverageDays = horizonDays + p.getLeadTimeDays();
            double multiplier = requestedMultiplier == null
                    ? festivalCalendar.multiplier(LocalDate.now().plusDays(1), coverageDays) : requestedMultiplier;
            boolean nearExpiry = batches.findByProductId(p.getId()).stream().anyMatch(b -> b.getExpiryDate() != null
                    && !b.getExpiryDate().isBefore(LocalDate.now()) && !b.getExpiryDate().isAfter(LocalDate.now().plusDays(30)));
            return recommendations.recommend(p, history, horizonDays, multiplier, synthetic,
                    underageCost, overageCost, nearExpiry);
        }).toList();
    }

    public List<ExpiryGuardService.ExpiryAlert> expiry(int days) {
        return expiry(null, days);
    }

    public List<ExpiryGuardService.ExpiryAlert> expiry(Long accountId, int days) {
        if (days < 1 || days > 365) throw new IllegalArgumentException("days must be between 1 and 365");
        LocalDate today = LocalDate.now();
        List<InventoryBatch> batchList = accountId == null ? batches.findAll() : batches.findByProductAccountId(accountId);
        return expiry.assess(batchList, today).stream()
                .filter(a -> a.daysRemaining() == null || a.daysRemaining() < 0 || a.daysRemaining() <= days).toList();
    }

    public List<ReturnAnalysisService.ReturnAnalysis> returns(BusinessProfile profile) {
        return returns(null, profile);
    }

    public List<ReturnAnalysisService.ReturnAnalysis> returns(Long accountId, BusinessProfile profile) {
        List<Product> list = products(accountId, profile);
        return list.stream().map(p ->
                returnAnalysis.analyze(p, history(p), returns.findByProductIdOrderByReturnDateDesc(p.getId()), null)).toList();
    }

    public List<DurabilityService.DurabilityScore> durability() {
        return durability(null);
    }

    public List<DurabilityService.DurabilityScore> durability(Long accountId) {
        List<Product> list = accountId == null
                ? products.findByBusinessProfileOrderByName(BusinessProfile.FOOTWEAR)
                : products.findByAccountIdAndBusinessProfileOrderByName(accountId, BusinessProfile.FOOTWEAR);
        return list.stream().map(p ->
                durability.assess(p, returns.findByProductIdOrderByReturnDateDesc(p.getId()))).toList();
    }

    public Map<String,Object> seasonality() {
        return Map.of("year",festivalCalendar.calendarYear(),
                "note","Festival dates and multipliers are illustrative and should be adapted to the shop's region and history.");
    }

    public List<InventoryBatch> batches() { return batches.findAll(); }
    public List<InventoryBatch> batches(Long accountId) {
        return accountId == null ? batches.findAll() : batches.findByProductAccountId(accountId);
    }

    public List<Product> products(BusinessProfile profile) {
        return products.findByBusinessProfileOrderByName(profile);
    }

    public List<Product> products(Long accountId, BusinessProfile profile) {
        if (accountId == null) {
            return profile == null ? products.findAll() : products.findByBusinessProfileOrderByName(profile);
        }
        return profile == null
                ? products.findByAccountIdOrderByName(accountId)
                : products.findByAccountIdAndBusinessProfileOrderByName(accountId, profile);
    }

    public List<com.shoppal.sales.SalesRecord> history(Product p) {
        return sales.findByProductIdAndSaleDateBetweenOrderBySaleDate(p.getId(), LocalDate.now().minusDays(365), LocalDate.now());
    }
}
