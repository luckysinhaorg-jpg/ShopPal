package com.shoppal.returns;

import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.sales.ReturnRecord;
import com.shoppal.sales.SalesRecord;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReturnAnalysisServiceTest {
    private final ReturnAnalysisService service = new ReturnAnalysisService();
    private Product product = new Product("S", "Item", BusinessProfile.KIRANA, BigDecimal.TEN,
            new BigDecimal("5"), 1, 1, 0, 1, null, null, null);
    @Test void calculatesReturnRateProfitAndProfitPerThousand() {
        var sales=List.of(new SalesRecord(product, LocalDate.now(), 10, BigDecimal.TEN));
        var returns=List.of(new ReturnRecord(product, LocalDate.now(), 1, "damaged"));
        var a=service.analyze(product,sales,returns,null);
        assertEquals(0.1,a.returnRate(),0.0001);
        assertEquals(new BigDecimal("45.00"),a.estimatedProfit());
        assertEquals(new BigDecimal("900.00"),a.profitPerThousandInvested());
        assertTrue(a.formulaAndLimitations().contains("holding cost is subtracted only when supplied"));
    }
    @Test void withholdsProfitWhenCostOrSalesAreMissing() {
        var a=service.analyze(product,List.of(),List.of(),null);
        assertNull(a.estimatedProfit());
        assertNull(a.grossSales());
        assertEquals(0,a.returnRate());
    }
}
