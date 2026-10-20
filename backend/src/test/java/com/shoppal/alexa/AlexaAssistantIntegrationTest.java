package com.shoppal.alexa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AlexaAssistantIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String login(String username, String password) throws Exception {
        String body = """
            {"username":"%s","password":"%s"}
            """.formatted(username, password);
        MvcResult res = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        // Extract token
        int tokenIdx = json.indexOf("\"token\":\"");
        int endIdx = json.indexOf("\"", tokenIdx + 9);
        return json.substring(tokenIdx + 9, endIdx);
    }

    @Test
    void unauthenticatedQueryReturns401() throws Exception {
        mvc.perform(post("/api/alexa/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"what stock needs restocking and why\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kiranaRestockWorkflowWorkflowDemonstration() throws Exception {
        String kiranaToken = login("kirana@shoppal.local", "Kirana@123");

        MvcResult result = mvc.perform(post("/api/alexa/query")
                .header("Authorization", "Bearer " + kiranaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"What stock needs restocking and why?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("RESTOCK_RECOMMENDATIONS"))
                .andExpect(jsonPath("$.mcpToolUsed").value("get_recommendations"))
                .andExpect(jsonPath("$.isSupported").value(true))
                .andExpect(jsonPath("$.accountInfo.shopName").value("Sharma General Store"))
                .andExpect(jsonPath("$.accountInfo.businessProfile").value("KIRANA"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("Kirana restock response: " + content);
        // Check that Kirana shop name and profile match
        assertTrue(content.contains("Sharma General Store"));
        // Strict Isolation: Pharmacy items MUST NOT appear
        assertFalse(content.contains("Amoxicillin"));
        assertFalse(content.contains("Paracetamol"));
    }

    @Test
    void pharmacyExpiryWorkflowWorkflowDemonstration() throws Exception {
        String pharmacyToken = login("pharmacy@shoppal.local", "Pharmacy@123");

        MvcResult result = mvc.perform(post("/api/alexa/query")
                .header("Authorization", "Bearer " + pharmacyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"Which medicine batches are nearing expiry?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("EXPIRY_ALERTS"))
                .andExpect(jsonPath("$.mcpToolUsed").value("get_expiry_alerts"))
                .andExpect(jsonPath("$.isSupported").value(true))
                .andExpect(jsonPath("$.accountInfo.shopName").value("Verma Medicos & Healthcare"))
                .andExpect(jsonPath("$.accountInfo.businessProfile").value("PHARMACY"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // Check that Pharmacy medicines appear (e.g. Cough Syrup expired or Amoxicillin nearing)
        assertTrue(content.contains("Cough Relief Syrup") || content.contains("Amoxicillin"));
        // Strict Isolation: Kirana grocery batches MUST NOT appear
        assertFalse(content.contains("Toor Dal"));
        assertFalse(content.contains("Basmati Rice"));
    }

    @Test
    void unsupportedQueryIsGracefullyHandled() throws Exception {
        String kiranaToken = login("kirana@shoppal.local", "Kirana@123");

        mvc.perform(post("/api/alexa/query")
                .header("Authorization", "Bearer " + kiranaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"What is the weather in Delhi today?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.isSupported").value(false))
                .andExpect(jsonPath("$.mcpToolUsed").value("none"))
                .andExpect(jsonPath("$.spokenText").isNotEmpty());
    }

    @Test
    void samplePromptsTailoredToShopProfile() throws Exception {
        String pharmacyToken = login("pharmacy@shoppal.local", "Pharmacy@123");

        mvc.perform(get("/api/alexa/sample-prompts")
                .header("Authorization", "Bearer " + pharmacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("PHARMACY"))
                .andExpect(jsonPath("$.prompts[0].prompt").value("Which medicine batches are nearing expiry?"));
    }
}
