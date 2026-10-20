package com.shoppal.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolsRegistrationTest {
    @Test void exposesExpectedToolAnnotations() {
        Set<String> names=java.util.Arrays.stream(ShopPalMcpTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(java.util.Objects::nonNull).map(McpTool::name).collect(Collectors.toSet());
        assertEquals(Set.of("get_inventory_summary","get_recommendations","get_expiry_alerts","analyze_returns","get_footwear_durability"),names);
    }
}
