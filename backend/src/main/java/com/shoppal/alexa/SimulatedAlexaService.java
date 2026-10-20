package com.shoppal.alexa;

import com.shoppal.account.ShopAccount;
import com.shoppal.expiry.ExpiryGuardService;
import com.shoppal.inventory.InventoryAdvisorService;
import com.shoppal.mcp.ShopPalMcpTools;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.recommendations.RecommendationService;
import com.shoppal.returns.ReturnAnalysisService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SimulatedAlexaService {

    private final InventoryAdvisorService advisor;
    private final ShopPalMcpTools mcpTools;

    public SimulatedAlexaService(InventoryAdvisorService advisor, ShopPalMcpTools mcpTools) {
        this.advisor = advisor;
        this.mcpTools = mcpTools;
    }

    public AlexaResponse processQuery(ShopAccount account, String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return unsupportedResponse(account, "I didn't catch that. How can I help with your inventory today?");
        }

        String normalized = userQuery.toLowerCase(Locale.ROOT).trim();

        // 1. Restocking Workflow
        if (matchesRestockIntent(normalized)) {
            return handleRestockIntent(account, userQuery);
        }

        // 2. Expiry / Batch Tracking Workflow
        if (matchesExpiryIntent(normalized)) {
            return handleExpiryIntent(account, userQuery);
        }

        // 3. Inventory Summary Workflow
        if (matchesSummaryIntent(normalized)) {
            return handleSummaryIntent(account, userQuery);
        }

        // 4. Return / Durability Workflow
        if (matchesReturnIntent(normalized)) {
            return handleReturnIntent(account, userQuery);
        }

        // 5. Unsupported Query
        return unsupportedResponse(account, userQuery);
    }

    private boolean matchesRestockIntent(String q) {
        return q.contains("restock") || q.contains("reorder") || q.contains("replenish")
                || (q.contains("stock") && (q.contains("need") || q.contains("low") || q.contains("buy") || q.contains("run out") || q.contains("order")))
                || q.contains("what should i order");
    }

    private boolean matchesExpiryIntent(String q) {
        return q.contains("expir") || q.contains("expire") || q.contains("expiry")
                || q.contains("batch") || q.contains("quarantine") || q.contains("shelf life") || q.contains("freshness");
    }

    private boolean matchesSummaryIntent(String q) {
        return q.contains("summary") || q.contains("overview") || q.contains("how much stock")
                || (q.contains("inventory") && (q.contains("status") || q.contains("total") || q.contains("how is")));
    }

    private boolean matchesReturnIntent(String q) {
        return q.contains("return") || q.contains("durability") || q.contains("defect");
    }

    private AlexaResponse handleRestockIntent(ShopAccount account, String rawQuery) {
        int horizonDays = 7;
        List<RecommendationService.Recommendation> recs = advisor.recommendations(
                account.getId(), account.getBusinessProfile(), horizonDays, null, null, null);

        List<RecommendationService.Recommendation> needRestock = recs.stream()
                .filter(r -> r.proposedOrderQuantity() > 0)
                .toList();

        StringBuilder speech = new StringBuilder();
        StringBuilder details = new StringBuilder();

        speech.append("For ").append(account.getShopName()).append(", ");
        if (needRestock.isEmpty()) {
            speech.append("all current products have sufficient stock to cover your next ").append(horizonDays).append(" days.");
            details.append("All inventory items meet or exceed safety stock thresholds for the 7-day planning horizon.");
        } else {
            speech.append(needRestock.size()).append(" item");
            if (needRestock.size() > 1) speech.append("s");
            speech.append(" need restocking: ");

            List<String> spokenItems = new ArrayList<>();
            for (var r : needRestock) {
                spokenItems.add(r.productName() + " (order " + r.proposedOrderQuantity() + " units)");
                details.append("• **").append(r.productName()).append("**: ")
                        .append(r.currentSellableStock()).append(" units currently in stock. ")
                        .append("We recommend ordering **").append(r.proposedOrderQuantity())
                        .append(" units** to satisfy ").append(horizonDays).append("-day forecast demand (")
                        .append(String.format(Locale.ROOT, "%.1f", r.forecastDemand())).append(" units, coverage target ")
                        .append(String.format(Locale.ROOT, "%.1f", r.targetDemand())).append(" units). ")
                        .append("Status: *").append(r.riskLevel()).append("* - ").append(r.reason()).append("\n");
            }
            speech.append(String.join(", and ", spokenItems)).append(".");
        }

        var accountInfo = Map.of(
                "shopName", account.getShopName(),
                "ownerName", account.getOwnerName(),
                "businessProfile", account.getBusinessProfile().name()
        );

        return new AlexaResponse(
                rawQuery,
                "RESTOCK_RECOMMENDATIONS",
                speech.toString(),
                details.toString(),
                "get_recommendations",
                true,
                accountInfo,
                needRestock,
                Collections.emptyList(),
                null,
                "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Deterministic inventory recommendations via MCP tool get_recommendations.",
                "Sample seed inventory for " + account.getBusinessProfile().name() + " store."
        );
    }

    private AlexaResponse handleExpiryIntent(ShopAccount account, String rawQuery) {
        int alertDays = 30;
        List<ExpiryGuardService.ExpiryAlert> alerts = advisor.expiry(account.getId(), alertDays);

        List<ExpiryGuardService.ExpiryAlert> expired = alerts.stream().filter(a -> "EXPIRED".equals(a.level())).toList();
        List<ExpiryGuardService.ExpiryAlert> dueSoon = alerts.stream().filter(a -> a.level() != null && a.level().startsWith("DUE_")).toList();
        List<ExpiryGuardService.ExpiryAlert> unknown = alerts.stream().filter(a -> "UNKNOWN_EXPIRY".equals(a.level())).toList();

        StringBuilder speech = new StringBuilder();
        StringBuilder details = new StringBuilder();

        speech.append("Here is the expiry guard check for ").append(account.getShopName()).append(". ");

        if (expired.isEmpty() && dueSoon.isEmpty() && unknown.isEmpty()) {
            speech.append("There are no expired or expiring batches within the next ").append(alertDays).append(" days.");
            details.append("All active inventory batches have good shelf life (>30 days remaining).");
        } else {
            if (!expired.isEmpty()) {
                speech.append("Action required: ").append(expired.size()).append(" batch is expired and must be quarantined immediately. ");
                details.append("### ⛔ EXPIRED BATCHES (Quarantine Immediately)\n");
                for (var a : expired) {
                    details.append("• **").append(a.productName()).append("** (Batch `").append(a.batchCode())
                            .append("`, ").append(a.quantity()).append(" units) expired on ")
                            .append(a.expiryDate()).append(" (").append(Math.abs(a.daysRemaining()))
                            .append(" days ago). **Action:** ").append(a.suggestedAction()).append("\n");
                }
                details.append("\n");
            }

            if (!dueSoon.isEmpty()) {
                speech.append(dueSoon.size()).append(" batch");
                if (dueSoon.size() > 1) speech.append("es are"); else speech.append(" is");
                speech.append(" nearing expiry in ").append(alertDays).append(" days. ");
                details.append("### ⚠️ UPCOMING EXPIRY BATCHES (First-Expiry-First-Out)\n");
                for (var a : dueSoon) {
                    details.append("• **").append(a.productName()).append("** (Batch `").append(a.batchCode())
                            .append("`, ").append(a.quantity()).append(" units) expires on ")
                            .append(a.expiryDate()).append(" (in ").append(a.daysRemaining())
                            .append(" days). **Action:** ").append(a.suggestedAction()).append("\n");
                }
                details.append("\n");
            }

            if (!unknown.isEmpty()) {
                speech.append(unknown.size()).append(" batch has an unrecorded expiry date.");
                details.append("### ❓ UNKNOWN EXPIRY DATES\n");
                for (var a : unknown) {
                    details.append("• **").append(a.productName()).append("** (Batch `").append(a.batchCode())
                            .append("`, ").append(a.quantity()).append(" units) has no recorded date. **Action:** ")
                            .append(a.suggestedAction()).append("\n");
                }
            }
        }

        var accountInfo = Map.of(
                "shopName", account.getShopName(),
                "ownerName", account.getOwnerName(),
                "businessProfile", account.getBusinessProfile().name()
        );

        return new AlexaResponse(
                rawQuery,
                "EXPIRY_ALERTS",
                speech.toString(),
                details.toString(),
                "get_expiry_alerts",
                true,
                accountInfo,
                Collections.emptyList(),
                alerts,
                null,
                "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Real-time batch monitoring via MCP tool get_expiry_alerts.",
                "Sample seed batch inventory for " + account.getBusinessProfile().name() + " store."
        );
    }

    private AlexaResponse handleSummaryIntent(ShopAccount account, String rawQuery) {
        List<Product> products = advisor.products(account.getId(), account.getBusinessProfile());
        int units = products.stream().mapToInt(Product::getSellableStock).sum();
        BigDecimal exposure = products.stream().filter(p -> p.getPurchaseCost() != null)
                .map(p -> p.getPurchaseCost().multiply(BigDecimal.valueOf(p.getSellableStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var productIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());
        long expiryRisks = advisor.expiry(account.getId(), 30).stream()
                .filter(a -> productIds.contains(a.productId()) && !a.level().equals("OK")).count();

        String speech = "For " + account.getShopName() + ", you currently carry " + products.size() +
                " distinct products totaling " + units + " sellable units with an estimated stock exposure of ₹" +
                exposure + ". There are " + expiryRisks + " batches with expiry risks flagged.";

        String details = "• **Total Catalog Size:** " + products.size() + " registered products\n" +
                "• **Sellable Inventory:** " + units + " units across shelves\n" +
                "• **Estimated Capital Exposure:** ₹" + exposure + " (based on wholesale unit costs)\n" +
                "• **Expiry Risk Batches:** " + expiryRisks + " batches require monitoring\n";

        var accountInfo = Map.of(
                "shopName", account.getShopName(),
                "ownerName", account.getOwnerName(),
                "businessProfile", account.getBusinessProfile().name()
        );

        var summary = new ShopPalMcpTools.InventorySummary(
                account.getBusinessProfile().name(),
                products.size(),
                units,
                exposure,
                expiryRisks,
                "Isolated to " + account.getShopName()
        );

        return new AlexaResponse(
                rawQuery,
                "INVENTORY_SUMMARY",
                speech,
                details,
                "get_inventory_summary",
                true,
                accountInfo,
                Collections.emptyList(),
                Collections.emptyList(),
                summary,
                "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Summary metrics computed via MCP tool get_inventory_summary.",
                "Sample seed inventory for " + account.getBusinessProfile().name() + " store."
        );
    }

    private AlexaResponse handleReturnIntent(ShopAccount account, String rawQuery) {
        var returns = advisor.returns(account.getId(), account.getBusinessProfile());
        String speech = "For " + account.getShopName() + ", return analysis is tracked across " + returns.size() + " catalog items.";
        StringBuilder details = new StringBuilder("### Return Analysis\n");
        for (var r : returns) {
            details.append("• **").append(r.productName()).append("**: ")
                    .append(r.unitsSold()).append(" sold, ")
                    .append(r.unitsReturned()).append(" returned (Rate: ")
                    .append(String.format("%.1f%%", r.returnRate() * 100)).append(")\n");
        }

        var accountInfo = Map.of(
                "shopName", account.getShopName(),
                "ownerName", account.getOwnerName(),
                "businessProfile", account.getBusinessProfile().name()
        );

        return new AlexaResponse(
                rawQuery,
                "RETURN_ANALYSIS",
                speech,
                details.toString(),
                "analyze_returns",
                true,
                accountInfo,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Return analysis computed via MCP tool analyze_returns.",
                "Sample seed data."
        );
    }

    private AlexaResponse unsupportedResponse(ShopAccount account, String rawQuery) {
        String profileName = account.getBusinessProfile() == BusinessProfile.PHARMACY ? "Pharmacy" : "Kirana";
        String speech = "I'm simulated Alexa+ for ShopPal. I can help you monitor inventory, restock recommendations, and batch expiry alerts for your "
                + profileName + " shop. Try asking: 'What stock needs restocking and why?' or 'Which batches are nearing expiry?'";

        String details = "### ℹ️ Supported Inquiries\n" +
                "The simulated Alexa+ voice assistant supports the following inventory workflows:\n\n" +
                "1. **Restocking Forecasts:** *\"What stock needs restocking and why?\"*\n" +
                "2. **Batch Expiry & Quarantine:** *\"Which medicine batches are nearing expiry?\"*\n" +
                "3. **Inventory Status:** *\"Give me an inventory summary\"*\n" +
                "4. **Returns & Sales Insights:** *\"Show return analysis\"*\n\n" +
                "*(General requests outside inventory operations are politely declined to keep your retail assistant focused.)*";

        var accountInfo = Map.of(
                "shopName", account.getShopName(),
                "ownerName", account.getOwnerName(),
                "businessProfile", account.getBusinessProfile().name()
        );

        return new AlexaResponse(
                rawQuery,
                "UNSUPPORTED",
                speech,
                details,
                "none",
                false,
                accountInfo,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track).",
                "Sample seed data."
        );
    }

    public record AlexaResponse(
            String query,
            String intent,
            String spokenText,
            String detailedAnswer,
            String mcpToolUsed,
            boolean isSupported,
            Map<String, String> accountInfo,
            List<RecommendationService.Recommendation> recommendations,
            List<ExpiryGuardService.ExpiryAlert> expiryAlerts,
            ShopPalMcpTools.InventorySummary inventorySummary,
            String disclaimer,
            String sampleDataNotice
    ) {}
}
