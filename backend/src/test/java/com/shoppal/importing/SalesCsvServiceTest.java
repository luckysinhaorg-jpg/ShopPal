package com.shoppal.importing;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SalesCsvServiceTest {
    private final SalesCsvService service = new SalesCsvService(null, null, null);

    @Test
    void previewsValidCsvRows() {
        var result = service.preview((SalesCsvService.HEADER + "\nKIR-001,2026-10-18,3,145.00\n").getBytes(StandardCharsets.UTF_8));
        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.rows().size());
        assertEquals(3, result.rows().getFirst().unitsSold());
    }

    @Test
    void reportsMalformedDatesQuantitiesAndUnclosedQuotes() {
        var result = service.preview((SalesCsvService.HEADER + "\nKIR-001,not-a-date,0,145\n\"KIR-002,2026-10-18,1,220\n").getBytes(StandardCharsets.UTF_8));
        assertEquals(2, result.errors().size());
        assertTrue(result.rows().isEmpty());
    }

    @Test
    void rejectsUnexpectedHeaderAndOversizedFile() {
        assertFalse(service.preview("sku,name".getBytes(StandardCharsets.UTF_8)).errors().isEmpty());
        assertTrue(service.preview(new byte[1_048_577]).errors().getFirst().contains("1 MB"));
    }
}
