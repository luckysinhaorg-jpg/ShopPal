package com.shoppal;

import com.shoppal.mcp.ShopPalMcpTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ShopPalApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ShopPalMcpTools mcpTools;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void dashboardEndpointReturns200WithKiranaSummary() throws Exception {
        mvc.perform(get("/api/dashboard?profile=KIRANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("KIRANA"))
                .andExpect(jsonPath("$.productCount").value(5))
                .andExpect(jsonPath("$.sellableUnits").isNumber())
                .andExpect(jsonPath("$.expiryRiskBatches").isNumber());
    }

    @Test
    void dashboardEndpointReturns200WithFootwearSummary() throws Exception {
        mvc.perform(get("/api/dashboard?profile=FOOTWEAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("FOOTWEAR"))
                .andExpect(jsonPath("$.productCount").value(2));
    }

    @Test
    void productsEndpointReturnsSeededProducts() throws Exception {
        mvc.perform(get("/api/products?profile=KIRANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mvc.perform(get("/api/products?profile=FOOTWEAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/products?profile=PHARMACY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void recommendationsEndpointReturns200() throws Exception {
        mvc.perform(get("/api/recommendations?profile=KIRANA&horizonDays=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mvc.perform(get("/api/recommendations?profile=PHARMACY&horizonDays=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void expiryEndpointReturns200() throws Exception {
        mvc.perform(get("/api/expiry?days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void returnsEndpointReturns200() throws Exception {
        mvc.perform(get("/api/returns?profile=KIRANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mvc.perform(get("/api/returns?profile=FOOTWEAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/returns?profile=PHARMACY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void durabilityEndpointReturns200() throws Exception {
        mvc.perform(get("/api/durability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].supplier").value("StepWell Footwear Supply"));
    }

    @Test
    void seasonalityEndpointReturns200() throws Exception {
        mvc.perform(get("/api/seasonality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026));
    }

    @Test
    void sampleCsvEndpointReturnsCsv() throws Exception {
        mvc.perform(get("/api/import/sample.csv?profile=KIRANA"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("KIR-001")));

        mvc.perform(get("/api/import/sample.csv?profile=PHARMACY"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MED-001")));
    }

    @Test
    void testAuthenticationAndAccountDataIsolation() throws Exception {
        // 1. Verify demo accounts endpoint
        mvc.perform(get("/api/auth/demo-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 2. Login as Kirana owner
        String kiranaLoginJson = "{\"username\":\"kirana@shoppal.local\",\"password\":\"Kirana@123\"}";
        String kiranaResponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(kiranaLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.shopName").value("Sharma General Store"))
                .andExpect(jsonPath("$.user.businessProfile").value("KIRANA"))
                .andReturn().getResponse().getContentAsString();

        String kiranaToken = com.jayway.jsonpath.JsonPath.read(kiranaResponse, "$.token");

        // 3. Login as Pharmacy owner
        String pharmacyLoginJson = "{\"username\":\"pharmacy@shoppal.local\",\"password\":\"Pharmacy@123\"}";
        String pharmacyResponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pharmacyLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.shopName").value("Verma Medicos & Healthcare"))
                .andExpect(jsonPath("$.user.businessProfile").value("PHARMACY"))
                .andReturn().getResponse().getContentAsString();

        String pharmacyToken = com.jayway.jsonpath.JsonPath.read(pharmacyResponse, "$.token");

        // 4. Verify Kirana account data isolation
        mvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + kiranaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("KIRANA"))
                .andExpect(jsonPath("$.shopName").value("Sharma General Store"))
                .andExpect(jsonPath("$.productCount").value(5));

        mvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + kiranaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].sku").value(org.hamcrest.Matchers.startsWith("KIR-")));

        // 5. Verify Pharmacy account data isolation
        mvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + pharmacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("PHARMACY"))
                .andExpect(jsonPath("$.shopName").value("Verma Medicos & Healthcare"))
                .andExpect(jsonPath("$.productCount").value(5));

        mvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + pharmacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].sku").value(org.hamcrest.Matchers.startsWith("MED-")));

        // 6. Test Signup for a brand new owner
        String signupJson = "{\"username\":\"testowner@test.com\",\"password\":\"Secret123\",\"ownerName\":\"Rohan Shah\",\"shopName\":\"Shah Electronics\",\"businessProfile\":\"ELECTRONICS\"}";
        String signupResponse = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("testowner@test.com"))
                .andExpect(jsonPath("$.user.shopName").value("Shah Electronics"))
                .andExpect(jsonPath("$.user.businessProfile").value("ELECTRONICS"))
                .andReturn().getResponse().getContentAsString();

        String newToken = com.jayway.jsonpath.JsonPath.read(signupResponse, "$.token");

        // New account is completely isolated: 0 products, 0 sellable stock
        mvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("ELECTRONICS"))
                .andExpect(jsonPath("$.shopName").value("Shah Electronics"))
                .andExpect(jsonPath("$.productCount").value(0))
                .andExpect(jsonPath("$.sellableUnits").value(0));

        // 7. Verify /api/auth/me
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.ownerName").value("Rohan Shah"));

        // 8. Verify logout
        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpToolsExecuteSuccessfully() {
        var summary = mcpTools.inventorySummary("KIRANA");
        assertNotNull(summary);
        assertNotNull(summary.profile());

        var pharmaSummary = mcpTools.inventorySummary("PHARMACY");
        assertNotNull(pharmaSummary);

        var recs = mcpTools.recommendations("KIRANA", 7);
        assertNotNull(recs);
        assertFalse(recs.isEmpty());

        var alerts = mcpTools.expiryAlerts(30);
        assertNotNull(alerts);

        var returnsAnalysis = mcpTools.returns("KIRANA");
        assertNotNull(returnsAnalysis);

        var durabilityScores = mcpTools.durability();
        assertNotNull(durabilityScores);
        assertFalse(durabilityScores.isEmpty());
    }
}
