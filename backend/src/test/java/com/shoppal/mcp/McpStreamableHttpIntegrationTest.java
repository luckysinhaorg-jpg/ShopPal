package com.shoppal.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class McpStreamableHttpIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void testMcpStreamableHttpFullLifecycle() throws Exception {
        // 1. Initialize with MCP spec 2025-11-25
        String initRequest = """
            {
              "jsonrpc": "2.0",
              "id": "1",
              "method": "initialize",
              "params": {
                "protocolVersion": "2025-11-25",
                "capabilities": {},
                "clientInfo": {
                  "name": "alexa-simulated-client",
                  "version": "1.0.0"
                }
              }
            }
            """;

        MvcResult initResult = mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(initRequest))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId = initResult.getResponse().getHeader("Mcp-Session-Id");
        assertNotNull(sessionId, "Session ID should be returned in Mcp-Session-Id header");
        String initBody = initResult.getResponse().getContentAsString();
        assertTrue(initBody.contains("\"protocolVersion\":\"2025-11-25\""));
        assertTrue(initBody.contains("\"name\":\"shoppal\""));

        // 2. Notification: initialized
        String notifRequest = """
            {
              "jsonrpc": "2.0",
              "method": "notifications/initialized"
            }
            """;
        mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content(notifRequest));

        // 3. tools/list
        String listToolsRequest = """
            {
              "jsonrpc": "2.0",
              "id": "2",
              "method": "tools/list",
              "params": {}
            }
            """;
        MvcResult listResult = mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content(listToolsRequest))
                .andExpect(status().isOk())
                .andReturn();

        String listBody = listResult.getResponse().getContentAsString();
        System.out.println("List Tools Response: " + listBody);
        assertTrue(listBody.contains("get_inventory_summary"));
        assertTrue(listBody.contains("get_recommendations"));
        assertTrue(listBody.contains("get_expiry_alerts"));
        assertTrue(listBody.contains("analyze_returns"));
        assertTrue(listBody.contains("get_footwear_durability"));

        // 4. tools/call: get_inventory_summary for KIRANA
        String callSummaryRequest = """
            {
              "jsonrpc": "2.0",
              "id": "3",
              "method": "tools/call",
              "params": {
                "name": "get_inventory_summary",
                "arguments": {
                  "profile": "KIRANA"
                }
              }
            }
            """;
        MvcResult callResult = mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content(callSummaryRequest))
                .andExpect(status().isOk())
                .andReturn();

        String callBody = callResult.getResponse().getContentAsString();
        System.out.println("Call get_inventory_summary Response: " + callBody);
        assertTrue(callBody.contains("KIRANA"));
        assertTrue(callBody.contains("productCount"));

        // 5. tools/call: get_expiry_alerts
        String callExpiryRequest = """
            {
              "jsonrpc": "2.0",
              "id": "4",
              "method": "tools/call",
              "params": {
                "name": "get_expiry_alerts",
                "arguments": {
                  "days": 30
                }
              }
            }
            """;
        MvcResult callExpiryResult = mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content(callExpiryRequest))
                .andExpect(status().isOk())
                .andReturn();

        String callExpiryBody = callExpiryResult.getResponse().getContentAsString();
        System.out.println("Call get_expiry_alerts Response: " + callExpiryBody);
        assertTrue(callExpiryBody.contains("EXPIRED") || callExpiryBody.contains("DUE_") || callExpiryBody.contains("OK"));
    }
}
