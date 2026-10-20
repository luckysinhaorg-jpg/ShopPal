package com.shoppal;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class HealthEndpointMvcTest {
    @Test void servesJsonHealthRoute() throws Exception {
        MockMvc mvc=standaloneSetup(new HealthController()).build();
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"status\":\"ok\",\"message\":\"ShopPal API is running\"}"));
    }
}
