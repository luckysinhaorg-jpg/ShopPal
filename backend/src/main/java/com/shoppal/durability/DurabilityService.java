package com.shoppal.durability;

import com.shoppal.product.Product;
import com.shoppal.sales.ReturnRecord;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DurabilityService {
    public DurabilityScore assess(Product product, List<ReturnRecord> returns) {
        long durabilityReturns = returns.stream().filter(r -> {
            String reason = r.getReason().toLowerCase();
            return reason.contains("sole") || reason.contains("stitch") || reason.contains("wear")
                    || reason.contains("defect") || reason.contains("durab");
        }).mapToLong(ReturnRecord::getUnitsReturned).sum();
        int observations = returns.stream().mapToInt(ReturnRecord::getUnitsReturned).sum();
        int score = observations == 0 ? 75 : (int) Math.round(Math.max(0, 100 - 100.0 * durabilityReturns / observations));
        String evidence = observations < 5 ? "LOW" : observations < 20 ? "MEDIUM" : "HIGH";
        return new DurabilityScore(product.getId(), product.getName(), product.getMaterial(), product.getLeatherGrade(),
                product.getSupplier() == null ? null : product.getSupplier().getName(), score, observations,
                durabilityReturns, evidence, observations == 0 ? "No return evidence; score is a neutral placeholder, not a quality claim." :
                "Based only on recorded return observations; a larger sample is needed for a reliable comparison.");
    }
    public record DurabilityScore(Long productId, String productName, String material, String leatherGrade,
                                  String supplier, int score, int observations, long durabilityRelatedReturns,
                                  String evidenceStrength, String explanation) {}
}
