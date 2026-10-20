# Model Context Protocol (MCP) Server

ShopPal provides a local MCP Server powered by Spring AI (`spring-ai-starter-mcp-server-webmvc` 2.0.1) operating over **Streamable HTTP**.

- **Protocol Version**: `2025-11-25` (also backwards compatible with `2024-11-05`, `2025-03-26`, `2025-06-18`)
- **Transport**: Streamable HTTP
- **Endpoint**: `http://127.0.0.1:8080/mcp`
- **Supported Methods**:
  - `initialize`: Establishes protocol version `2025-11-25` and returns the `Mcp-Session-Id` header.
  - `notifications/initialized`: Acknowledges client initialization.
  - `tools/list`: Lists all available inventory tools and schemas.
  - `tools/call`: Executes a tool deterministically and returns structured content.

---

## Registered MCP Tools

1. **`get_inventory_summary(profile)`**
   - Summarizes total product count, sellable units on shelves, estimated wholesale stock exposure, and active expiry risks for the authenticated shop.
   - Operates on the authenticated account's catalog.

2. **`get_recommendations(profile, horizonDays)`**
   - Returns deterministic order quantities based on lead time, safety stock, 7-day sales velocity, and festival multipliers.
   - Factors in near-expiry batches to prevent over-ordering.

3. **`get_expiry_alerts(days)`**
   - Assesses batch shelf life into `EXPIRED` (quarantine required), `DUE_7_DAYS`, `DUE_15_DAYS`, `DUE_30_DAYS`, and `UNKNOWN_EXPIRY`.
   - Strictly isolated to the authenticated shop's inventory.

4. **`analyze_returns(profile)`**
   - Computes return rates, return reasons, and estimated net profit.

5. **`get_footwear_durability()`**
   - Evidence-based footwear durability scoring and observation counts.

---

## Verification & Automated Testing

The MCP server implementation is verified by automated integration tests in [McpStreamableHttpIntegrationTest.java](file:///backend/src/test/java/com/shoppal/mcp/McpStreamableHttpIntegrationTest.java):
1. **Initialize Phase**: Sends `POST /mcp` with protocol `2025-11-25`. Verifies HTTP 200 and receipt of `Mcp-Session-Id`.
2. **Notification Phase**: Sends `notifications/initialized` with the session ID.
3. **Tools Discovery**: Calls `tools/list` and confirms all 5 tools are declared with descriptions and parameter schemas.
4. **Tool Execution**: Calls `tools/call` for `get_inventory_summary` and `get_expiry_alerts` and validates JSON-RPC response format and data isolation.
