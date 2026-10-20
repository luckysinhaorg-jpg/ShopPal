package com.shoppal.recommendations;

import com.shoppal.forecasting.DemandForecastService;
import com.shoppal.product.Product;
import com.shoppal.sales.SalesRecord;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecommendationService {
    private final DemandForecastService forecasts;
    public RecommendationService(DemandForecastService forecasts) { this.forecasts = forecasts; }

    public Recommendation recommend(Product product, List<SalesRecord> history, int horizonDays,
                                    double seasonalMultiplier, boolean syntheticData,
                                    Double underageCost, Double overageCost, boolean nearExpiry) {
        int coverageDays = horizonDays + product.getLeadTimeDays();
        var forecast = forecasts.forecast(history, coverageDays, seasonalMultiplier, syntheticData);
        String method = "DETERMINISTIC_FALLBACK";
        double targetDemand = forecast.forecastUnits();
        if (underageCost != null && overageCost != null && underageCost > 0 && overageCost > 0
                && forecast.historyDays() >= 14 && forecast.averageDailyDemand() > 0) {
            double ratio = underageCost / (underageCost + overageCost);
            double standardDeviation = dailyStdDev(history, forecast.historyDays());
            if (standardDeviation > 0) {
                double z = inverseNormalCdf(ratio);
                targetDemand = Math.max(0, forecast.forecastUnits() + z * standardDeviation * Math.sqrt(coverageDays));
                method = "NEWSVENDOR_NORMAL_APPROXIMATION";
            }
        }
        double needed = Math.max(0, targetDemand + product.getSafetyStock() - product.getSellableStock());
        int quantity = (int) Math.ceil(needed);
        int pack = Math.max(1, product.getPackSize());
        if (quantity > 0) quantity = ((quantity + pack - 1) / pack) * pack;
        boolean economical = product.getPurchaseCost() == null || product.getSellingPrice().compareTo(product.getPurchaseCost()) >= 0;
        if (!economical || nearExpiry) quantity = 0;
        String riskLevel;
        String riskExplanation;
        if (nearExpiry) {
            riskLevel = "EXPIRY_REVIEW";
            riskExplanation = "A batch is due within 30 days; review usable stock before replenishing.";
        } else if (forecast.historyDays() == 0) {
            riskLevel = "UNKNOWN";
            riskExplanation = "Insufficient sales history to label stock risk.";
        } else if (product.getSellableStock() < forecast.forecastUnits()) {
            riskLevel = "STOCKOUT_RISK";
            riskExplanation = "Sellable stock is below forecast demand over lead time plus planning horizon.";
        } else if (product.getSellableStock() > targetDemand + Math.max(1, product.getSafetyStock() * 2.0)) {
            riskLevel = "OVERSTOCK_RISK";
            riskExplanation = "Sellable stock is more than forecast target plus twice the configured safety stock.";
        } else {
            riskLevel = "BALANCED";
            riskExplanation = "Sellable stock covers forecast demand without exceeding the overstock review threshold.";
        }
        String reason = nearExpiry ? "Near-expiry stock was flagged; review existing batches before ordering." :
                !economical ? "Estimated purchase cost is at or above selling price; order suppressed." :
                "Forecast demand + safety stock − current sellable stock, rounded up to pack size.";
        return new Recommendation(product.getId(), product.getSku(), product.getName(), quantity,
                method, targetDemand, product.getSellableStock(), coverageDays, forecast.forecastUnits(), riskLevel,
                riskExplanation, reason,
                forecast.synthetic(), forecast.assumptions());
    }

    private static double dailyStdDev(List<SalesRecord> history, int historyDays) {
        if (history == null || history.isEmpty()) return 0;
        var first = history.stream().map(SalesRecord::getSaleDate).min(java.time.LocalDate::compareTo).orElseThrow();
        var last = history.stream().map(SalesRecord::getSaleDate).max(java.time.LocalDate::compareTo).orElseThrow();
        var daily = new java.util.HashMap<java.time.LocalDate, Integer>();
        history.forEach(r -> daily.merge(r.getSaleDate(), r.getUnitsSold(), Integer::sum));
        double mean = history.stream().mapToInt(SalesRecord::getUnitsSold).sum() / (double) historyDays;
        double sum = 0;
        for (var date = first; !date.isAfter(last); date = date.plusDays(1)) sum += Math.pow(daily.getOrDefault(date, 0) - mean, 2);
        return Math.sqrt(sum / Math.max(1, historyDays - 1));
    }

    // Acklam's approximation to the inverse standard normal CDF.
    static double inverseNormalCdf(double p) {
        if (p <= 0 || p >= 1) throw new IllegalArgumentException("p must be between zero and one");
        double[] a={-39.6968302866538,220.946098424521,-275.928510446969,138.357751867269,-30.6647980661472,2.50662827745924};
        double[] b={-54.4760987982241,161.585836858041,-155.698979859887,66.8013118877197,-13.2806815528857};
        double[] c={-0.00778489400243029,-0.322396458041136,-2.40075827716184,-2.54973253934373,4.37466414146497,2.93816398269878};
        double[] d={0.00778469570904146,0.32246712907004,2.445134137143,3.75440866190742};
        double q,r;
        if(p<0.02425){q=Math.sqrt(-2*Math.log(p));return (((((c[0]*q+c[1])*q+c[2])*q+c[3])*q+c[4])*q+c[5])/((((d[0]*q+d[1])*q+d[2])*q+d[3])*q+1);}
        if(p>0.97575){q=Math.sqrt(-2*Math.log(1-p));return -(((((c[0]*q+c[1])*q+c[2])*q+c[3])*q+c[4])*q+c[5])/((((d[0]*q+d[1])*q+d[2])*q+d[3])*q+1);}
        q=p-0.5;r=q*q;return (((((a[0]*r+a[1])*r+a[2])*r+a[3])*r+a[4])*r+a[5])*q/(((((b[0]*r+b[1])*r+b[2])*r+b[3])*r+b[4])*r+1);
    }

    public record Recommendation(Long productId, String sku, String productName, int proposedOrderQuantity,
                                 String method, double targetDemand, int currentSellableStock, int coverageDays,
                                 double forecastDemand, String riskLevel, String riskExplanation, String reason,
                                 boolean syntheticData, String assumptions) {}
}





