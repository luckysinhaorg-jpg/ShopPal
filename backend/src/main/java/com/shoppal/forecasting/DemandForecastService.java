package com.shoppal.forecasting;

import com.shoppal.sales.SalesRecord;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DemandForecastService {
    public Forecast forecast(List<SalesRecord> records, int horizonDays, double seasonalMultiplier, boolean synthetic) {
        if (horizonDays < 1 || horizonDays > 730) throw new IllegalArgumentException("horizonDays must be between 1 and 730");
        if (!Double.isFinite(seasonalMultiplier) || seasonalMultiplier < 0.1 || seasonalMultiplier > 5.0)
            throw new IllegalArgumentException("seasonalMultiplier must be between 0.1 and 5.0");
        if (records == null || records.isEmpty()) return new Forecast(0, 0, 1, seasonalMultiplier, horizonDays, 0,
                synthetic, "No sales history; demand is unknown and forecast quantity is zero.");
        LocalDate first = records.stream().map(SalesRecord::getSaleDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate last = records.stream().map(SalesRecord::getSaleDate).max(LocalDate::compareTo).orElseThrow();
        long historyDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(first, last) + 1);
        double units = records.stream().mapToInt(SalesRecord::getUnitsSold).sum();
        double average = units / historyDays;
        if (average == 0) return new Forecast(0, 0, 1, seasonalMultiplier, horizonDays, (int) historyDays,
                synthetic, "Recorded history contains zero units sold.");
        LocalDate recentStart = last.minusDays(6);
        double recentUnits = records.stream().filter(r -> !r.getSaleDate().isBefore(recentStart))
                .mapToInt(SalesRecord::getUnitsSold).sum();
        long recentDays = Math.min(7, historyDays);
        double recentAverage = recentUnits / recentDays;
        double trend = Math.max(0.5, Math.min(2.0, recentAverage / average));
        double demand = average * trend * seasonalMultiplier * horizonDays;
        String assumption = historyDays < 7 ? "Short history: trend is based on available dates and may be noisy." :
                "Rule-based forecast: historical daily average × capped recent trend × one seasonal multiplier × horizon.";
        return new Forecast(round2(average), round2(recentAverage), round2(trend), seasonalMultiplier,
                horizonDays, (int) historyDays, synthetic, assumption, round2(demand));
    }
    private static double round2(double n) { return Math.round(n * 100.0) / 100.0; }
    public record Forecast(double averageDailyDemand, double recentDailyDemand, double trendFactor,
                           double seasonalMultiplier, int horizonDays, int historyDays, boolean synthetic,
                           String assumptions, double forecastUnits) {
        public Forecast(double averageDailyDemand, double recentDailyDemand, double trendFactor,
                        double seasonalMultiplier, int horizonDays, int historyDays, boolean synthetic,
                        String assumptions) {
            this(averageDailyDemand, recentDailyDemand, trendFactor, seasonalMultiplier, horizonDays,
                    historyDays, synthetic, assumptions, 0);
        }
    }
}


