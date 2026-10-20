package com.shoppal.returns;

import com.shoppal.product.Product;
import com.shoppal.sales.ReturnRecord;
import com.shoppal.sales.SalesRecord;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReturnAnalysisService {
    public ReturnAnalysis analyze(Product product, List<SalesRecord> sales, List<ReturnRecord> returns,
                                 BigDecimal estimatedHoldingCost) {
        int unitsSold = sales.stream().mapToInt(SalesRecord::getUnitsSold).sum();
        int unitsReturned = returns.stream().mapToInt(ReturnRecord::getUnitsReturned).sum();
        double rate = unitsSold == 0 ? 0 : unitsReturned / (double) unitsSold;
        BigDecimal gross = sales.stream().filter(s -> s.getUnitPrice() != null)
                .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getUnitsSold())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasPrices = !sales.isEmpty() && sales.stream().allMatch(s -> s.getUnitPrice() != null);
        BigDecimal profit = null, returnOnInvestment = null;
        if (hasPrices && product.getPurchaseCost() != null && unitsSold > 0 && unitsReturned <= unitsSold) {
            int keptUnits = Math.max(0, unitsSold - unitsReturned);
            BigDecimal netSales = sales.stream().filter(s -> s.getUnitPrice() != null)
                    .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getUnitsSold())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal refundedValue = returns.stream().map(r -> product.getSellingPrice().multiply(BigDecimal.valueOf(r.getUnitsReturned())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = product.getPurchaseCost().multiply(BigDecimal.valueOf(keptUnits));
            BigDecimal holding = estimatedHoldingCost == null ? BigDecimal.ZERO : estimatedHoldingCost;
            profit = netSales.subtract(refundedValue).subtract(cost).subtract(holding).setScale(2, RoundingMode.HALF_UP);
            BigDecimal invested = product.getPurchaseCost().multiply(BigDecimal.valueOf(unitsSold));
            if (invested.signum() > 0) returnOnInvestment = profit.multiply(BigDecimal.valueOf(1000))
                    .divide(invested, 2, RoundingMode.HALF_UP);
        }
        return new ReturnAnalysis(product.getId(), product.getSku(), product.getName(), unitsSold, unitsReturned,
                rate, returns.stream().map(ReturnRecord::getReason).distinct().toList(),
                hasPrices ? gross.setScale(2, RoundingMode.HALF_UP) : null, hasPrices,
                estimatedHoldingCost, profit, returnOnInvestment, profit == null ? "Financial profit unavailable: valid sales, return counts, sales prices, and unit purchase cost are required." :
                "Profit estimate = recorded sales revenue − estimated refunds at current selling price − purchase cost of non-returned units; holding cost is subtracted only when supplied.");
    }
    public record ReturnAnalysis(Long productId, String sku, String productName, int unitsSold,
                                 int unitsReturned, double returnRate, List<String> reasons,
                                 BigDecimal grossSales, boolean grossSalesComplete, BigDecimal estimatedHoldingCost,
                                 BigDecimal estimatedProfit, BigDecimal profitPerThousandInvested,
                                 String formulaAndLimitations) {}
}

