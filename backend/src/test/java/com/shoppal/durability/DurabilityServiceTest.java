package com.shoppal.durability;

import com.shoppal.product.BusinessProfile;
import com.shoppal.product.Product;
import com.shoppal.sales.ReturnRecord;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DurabilityServiceTest {
    @Test void returnsEvidenceStrengthAndSampleSize() {
        var p=new Product("S","Sandal",BusinessProfile.FOOTWEAR,BigDecimal.TEN,BigDecimal.ONE,1,2,0,1,null,"leather","A");
        var score=new DurabilityService().assess(p,List.of(new ReturnRecord(p,LocalDate.now(),1,"sole separation")));
        assertEquals(1,score.observations());
        assertEquals("LOW",score.evidenceStrength());
        assertEquals(1,score.durabilityRelatedReturns());
    }
}
