package com.shoppal.recommendations;

import com.shoppal.forecasting.DemandForecastService;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.sales.SalesRecord;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {
    private final RecommendationService service = new RecommendationService(new DemandForecastService());
    private Product product(int stock, int pack, int lead) {
        return new Product("SKU", "Rice", BusinessProfile.KIRANA, BigDecimal.TEN, BigDecimal.ONE,
                stock, lead, 1, pack, null, null, null);
    }
    @Test void addsLeadTimeSafetyStockAndRoundsUpToPack() {
        LocalDate end=LocalDate.of(2026,10,19);
        var history=java.util.stream.IntStream.range(0,7).mapToObj(i ->
                new SalesRecord(null,end.minusDays(i),2,BigDecimal.TEN,true)).toList();
        var result=service.recommend(product(5,4,3),history,2,1.0,true,null,null,false);
        assertEquals(5,result.coverageDays());
        assertEquals(8,result.proposedOrderQuantity());
        assertEquals("DETERMINISTIC_FALLBACK",result.method());
        assertEquals("STOCKOUT_RISK",result.riskLevel());
        assertTrue(result.syntheticData());
    }
    @Test void suppressesOrderWhenNearExpiryIsFlagged() {
        var result=service.recommend(product(0,1,2),List.of(),7,1.0,false,null,null,true);
        assertEquals(0,result.proposedOrderQuantity());
        assertEquals("EXPIRY_REVIEW",result.riskLevel());
        assertTrue(result.reason().contains("Near-expiry"));
    }
    @Test void identifiesOverstockAgainstForecastAndSafetyThreshold() {
        LocalDate end=LocalDate.of(2026,10,19);
        var history=java.util.stream.IntStream.range(0,14).mapToObj(i ->
                new SalesRecord(null,end.minusDays(i),2,BigDecimal.TEN,false)).toList();
        var result=service.recommend(product(30,1,2),history,3,1.0,false,null,null,false);
        assertEquals("OVERSTOCK_RISK",result.riskLevel());
        assertEquals(0,result.proposedOrderQuantity());
    }
    @Test void usesNewsvendorApproximationOnlyWithLongEnoughHistoryAndCosts() {
        LocalDate end=LocalDate.of(2026,10,19);
        var history=java.util.stream.IntStream.range(0,20).mapToObj(i ->
                new SalesRecord(null,end.minusDays(i),i%3,BigDecimal.TEN,false)).toList();
        var result=service.recommend(product(1,1,2),history,7,1.0,false,9.0,1.0,false);
        assertEquals("NEWSVENDOR_NORMAL_APPROXIMATION",result.method());
        assertTrue(result.targetDemand() >= result.forecastDemand());
    }
}
