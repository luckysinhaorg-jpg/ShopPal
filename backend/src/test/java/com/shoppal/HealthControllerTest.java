package com.shoppal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {
    private final HealthController controller = new HealthController();

    @Test
    void reportsHealthyApi() {
        HealthController.HealthResponse response = controller.health();
        assertEquals("ok", response.status());
        assertEquals("ShopPal API is running", response.message());
    }
}
