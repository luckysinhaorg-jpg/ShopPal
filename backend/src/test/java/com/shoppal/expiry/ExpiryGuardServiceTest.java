package com.shoppal.expiry;

import com.shoppal.inventory.InventoryBatch;
import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExpiryGuardServiceTest {
    @Test void distinguishesExpiredUpcomingAndUnknownExpiry() {
        var p=new Product("S","Rice",BusinessProfile.KIRANA,BigDecimal.TEN,BigDecimal.ONE,10,1,0,1,null,null,null);
        var batches=List.of(new InventoryBatch(p,"OLD",1,null,LocalDate.of(2026,10,18),BigDecimal.ONE),
                new InventoryBatch(p,"SOON",2,null,LocalDate.of(2026,10,24),BigDecimal.ONE),
                new InventoryBatch(p,"UNKNOWN",3,null,null,BigDecimal.ONE));
        var alerts=new ExpiryGuardService().assess(batches,LocalDate.of(2026,10,19));
        assertEquals("EXPIRED",alerts.get(0).level());
        assertEquals("DUE_7_DAYS",alerts.get(1).level());
        assertEquals("UNKNOWN_EXPIRY",alerts.get(2).level());
        assertTrue(alerts.get(0).suggestedAction().contains("never sell"));
    }
}
