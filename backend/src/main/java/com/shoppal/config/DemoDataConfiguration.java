package com.shoppal.config;

import com.shoppal.account.PasswordSecurity;
import com.shoppal.account.ShopAccount;
import com.shoppal.account.ShopAccountRepository;
import com.shoppal.inventory.InventoryBatch;
import com.shoppal.inventory.InventoryBatchRepository;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.product.ProductRepository;
import com.shoppal.sales.ReturnRecord;
import com.shoppal.sales.ReturnRecordRepository;
import com.shoppal.sales.SalesRecord;
import com.shoppal.sales.SalesRecordRepository;
import com.shoppal.supplier.Supplier;
import com.shoppal.supplier.SupplierRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class DemoDataConfiguration {
    @Bean
    ApplicationRunner seedDemoData(ShopAccountRepository accounts,
                                   ProductRepository products, SupplierRepository suppliers,
                                   SalesRecordRepository sales, InventoryBatchRepository batches,
                                   ReturnRecordRepository returns) {
        return args -> {
            if (accounts.count() > 0) return;

            // 1. Seed Accounts
            ShopAccount kiranaAccount = accounts.save(new ShopAccount(
                "kirana@shoppal.local",
                PasswordSecurity.hash("Kirana@123"),
                "Amit Sharma",
                "Sharma General Store",
                BusinessProfile.KIRANA
            ));

            ShopAccount pharmacyAccount = accounts.save(new ShopAccount(
                "pharmacy@shoppal.local",
                PasswordSecurity.hash("Pharmacy@123"),
                "Dr. Ramesh Verma",
                "Verma Medicos & Healthcare",
                BusinessProfile.PHARMACY
            ));

            ShopAccount footwearAccount = accounts.save(new ShopAccount(
                "footwear@shoppal.local",
                PasswordSecurity.hash("Footwear@123"),
                "Suresh Mehra",
                "Mehra Footwear Collection",
                BusinessProfile.FOOTWEAR
            ));

            // 2. Seed Suppliers
            Supplier local = suppliers.save(new Supplier("Shree Local Distributors", "+91-90000-00001"));
            Supplier pharmaLogistics = suppliers.save(new Supplier("Apollo Healthcare Logistics", "+91-90000-00003"));
            Supplier footwear = suppliers.save(new Supplier("StepWell Footwear Supply", "+91-90000-00002"));

            LocalDate today = LocalDate.now();

            // 3. Seed Kirana Catalog & Data
            List<Product> kiranaProducts = products.saveAll(List.of(
                new Product(kiranaAccount, "KIR-001", "Toor Dal 1 kg", BusinessProfile.KIRANA, bd("145"), bd("112"), 18, 3, 5, 1, local, null, null),
                new Product(kiranaAccount, "KIR-002", "Tea 500 g", BusinessProfile.KIRANA, bd("220"), bd("174"), 9, 4, 4, 1, local, null, null),
                new Product(kiranaAccount, "KIR-003", "Cooking Oil 1 L", BusinessProfile.KIRANA, bd("158"), bd("132"), 12, 2, 6, 1, local, null, null),
                new Product(kiranaAccount, "KIR-004", "Biscuits Family Pack", BusinessProfile.KIRANA, bd("40"), bd("27"), 36, 2, 10, 10, local, null, null),
                new Product(kiranaAccount, "KIR-005", "Detergent 1 kg", BusinessProfile.KIRANA, bd("115"), bd("84"), 7, 5, 3, 1, local, null, null)
            ));
            for (int p = 0; p < kiranaProducts.size(); p++) {
                Product product = kiranaProducts.get(p);
                int dailyBase = new int[]{3, 2, 4, 5, 1}[p];
                for (int day = 29; day >= 0; day--) {
                    int quantity = Math.max(0, dailyBase + (((day + p) % 5 == 0) ? 1 : 0) - (((day + 2 * p) % 11 == 0) ? 1 : 0));
                    if (quantity > 0) sales.save(new SalesRecord(product, today.minusDays(day), quantity, product.getSellingPrice(), true));
                }
            }
            batches.saveAll(List.of(
                new InventoryBatch(kiranaProducts.get(0), "DAL-LOT-21", 8, today.minusDays(300), today.plusDays(20), bd("112")),
                new InventoryBatch(kiranaProducts.get(1), "TEA-LOT-09", 5, today.minusDays(120), today.plusDays(120), bd("174")),
                new InventoryBatch(kiranaProducts.get(2), "OIL-LOT-15", 10, today.minusDays(150), today.minusDays(2), bd("132")),
                new InventoryBatch(kiranaProducts.get(3), "BISC-LOT-04", 12, null, null, bd("27"))
            ));

            // 4. Seed Pharmacy Catalog & Data
            List<Product> pharmacyProducts = products.saveAll(List.of(
                new Product(pharmacyAccount, "MED-001", "Paracetamol 650 mg Strip", BusinessProfile.PHARMACY, bd("45"), bd("28"), 40, 2, 10, 5, pharmaLogistics, null, null),
                new Product(pharmacyAccount, "MED-002", "Amoxicillin 500 mg Strip", BusinessProfile.PHARMACY, bd("120"), bd("82"), 15, 3, 8, 1, pharmaLogistics, null, null),
                new Product(pharmacyAccount, "MED-003", "Cough Relief Syrup 100 ml", BusinessProfile.PHARMACY, bd("95"), bd("60"), 8, 4, 5, 1, pharmaLogistics, null, null),
                new Product(pharmacyAccount, "MED-004", "Antacid Suspension 200 ml", BusinessProfile.PHARMACY, bd("130"), bd("88"), 22, 2, 6, 2, pharmaLogistics, null, null),
                new Product(pharmacyAccount, "MED-005", "Vitamin C & Zinc (30 tabs)", BusinessProfile.PHARMACY, bd("110"), bd("70"), 25, 2, 5, 1, pharmaLogistics, null, null)
            ));
            for (int p = 0; p < pharmacyProducts.size(); p++) {
                Product product = pharmacyProducts.get(p);
                int dailyBase = new int[]{6, 3, 2, 4, 3}[p];
                for (int day = 29; day >= 0; day--) {
                    int quantity = Math.max(0, dailyBase + (((day + p) % 4 == 0) ? 1 : 0) - (((day + p) % 7 == 0) ? 1 : 0));
                    if (quantity > 0) sales.save(new SalesRecord(product, today.minusDays(day), quantity, product.getSellingPrice(), true));
                }
            }
            batches.saveAll(List.of(
                new InventoryBatch(pharmacyProducts.get(0), "PCM-LOT-01", 25, today.minusDays(180), today.plusDays(400), bd("28")),
                new InventoryBatch(pharmacyProducts.get(1), "AMX-LOT-04", 10, today.minusDays(340), today.plusDays(14), bd("82")),
                new InventoryBatch(pharmacyProducts.get(2), "CSY-LOT-09", 5, today.minusDays(360), today.minusDays(3), bd("60")),
                new InventoryBatch(pharmacyProducts.get(3), "ANT-LOT-11", 15, today.minusDays(200), today.plusDays(25), bd("88")),
                new InventoryBatch(pharmacyProducts.get(4), "VIT-LOT-02", 20, null, null, bd("70"))
            ));
            returns.save(new ReturnRecord(pharmacyProducts.get(2), today.minusDays(5), 1, "damaged seal on bottle"));

            // 5. Seed Footwear Catalog & Data
            List<Product> footwearProducts = products.saveAll(List.of(
                new Product(footwearAccount, "FTW-001", "Everyday Walking Shoe", BusinessProfile.FOOTWEAR, bd("1499"), bd("810"), 6, 10, 2, 1, footwear, "synthetic mesh", null),
                new Product(footwearAccount, "FTW-002", "Classic Leather Sandal", BusinessProfile.FOOTWEAR, bd("2199"), bd("1280"), 4, 12, 1, 1, footwear, "leather", "full grain")
            ));
            for (int p = 0; p < footwearProducts.size(); p++) {
                Product product = footwearProducts.get(p);
                int dailyBase = p == 0 ? 1 : 0;
                for (int day = 29; day >= 0; day--) {
                    int quantity = Math.max(0, dailyBase + (((day + p) % 5 == 0) ? 1 : 0) - (((day + 2 * p) % 11 == 0) ? 1 : 0));
                    if (quantity > 0) sales.save(new SalesRecord(product, today.minusDays(day), quantity, product.getSellingPrice(), true));
                }
            }
            returns.save(new ReturnRecord(footwearProducts.get(0), today.minusDays(8), 1, "sole separation"));
            returns.save(new ReturnRecord(footwearProducts.get(1), today.minusDays(15), 1, "size exchange"));
        };
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
