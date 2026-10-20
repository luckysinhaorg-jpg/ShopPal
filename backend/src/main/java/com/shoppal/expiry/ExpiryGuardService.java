package com.shoppal.expiry;

import com.shoppal.inventory.InventoryBatch;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ExpiryGuardService {
    public List<ExpiryAlert> assess(List<InventoryBatch> batches, LocalDate today) {
        return batches.stream().map(batch -> {
            if (batch.getExpiryDate() == null) return new ExpiryAlert(batch.getProduct().getId(), batch.getProduct().getName(),
                    batch.getBatchCode(), batch.getQuantity(), null, null, "UNKNOWN_EXPIRY", "Check the package date and record it.");
            long days = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
            String level; String action;
            if (days < 0) { level="EXPIRED"; action="Quarantine and follow applicable disposal rules; never sell expired stock."; }
            else if (days <= 7) { level="DUE_7_DAYS"; action="Rotate first-expiry-first-out and review safe, permitted action."; }
            else if (days <= 15) { level="DUE_15_DAYS"; action="Prioritize first-expiry-first-out stock rotation."; }
            else if (days <= 30) { level="DUE_30_DAYS"; action="Monitor the batch and reduce future replenishment if appropriate."; }
            else { level="OK"; action="No expiry action needed today."; }
            return new ExpiryAlert(batch.getProduct().getId(), batch.getProduct().getName(), batch.getBatchCode(),
                    batch.getQuantity(), batch.getExpiryDate(), days, level, action);
        }).toList();
    }
    public record ExpiryAlert(Long productId, String productName, String batchCode, int quantity,
                              LocalDate expiryDate, Long daysRemaining, String level, String suggestedAction) {}
}
