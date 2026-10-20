package com.shoppal.forecasting;

import com.shoppal.sales.SalesRecord;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DemandForecastServiceTest {
    private final DemandForecastService service = new DemandForecastService();
    @Test void usesDocumentedAverageTrendSeasonAndHorizonOnce() {
        LocalDate end = LocalDate.of(2026, 10, 19);
        var rows = java.util.stream.IntStream.range(0, 7).mapToObj(i ->
                new SalesRecord(null, end.minusDays(i), 2, BigDecimal.ONE)).toList();
        var forecast = service.forecast(rows, 7, 1.5, false);
        assertEquals(2.0, forecast.averageDailyDemand(), 0.01);
        assertEquals(1.0, forecast.trendFactor(), 0.01);
        assertEquals(21.0, forecast.forecastUnits(), 0.01);
    }
    @Test void emptyHistoryDoesNotInventDemand() {
        var forecast = service.forecast(List.of(), 7, 1.0, false);
        assertEquals(0, forecast.forecastUnits());
        assertTrue(forecast.assumptions().contains("unknown"));
    }
    @Test void rejectsInvalidHorizon() {
        assertThrows(IllegalArgumentException.class, () -> service.forecast(List.of(), 0, 1.0, false));
    }
}
