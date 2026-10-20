# ShopPal (Dukaan Dost)

**Stock smarter. Waste less. Keep customers happy.**

ShopPal is a web-based inventory advisor and retail assistant for small Indian businesses (Kirana, Pharmacy, Footwear, etc.). Its decision support uses transparent Java rules for ordering, expiry, return, and durability analysis.

---

## 🎙️ Build, Ship, Shape: Amazon Developer Hackathon (Alexa+ Track)

### Simulated Alexa+ Experience Notice
> **Important Note on Alexa+ Availability:** Amazon's official hackathon FAQ confirms that participants cannot access the gated Alexa+ MCP Toolkit, CLI, or Web Simulator, and that the **simulated Alexa+ conversational experience is an eligible and officially approved alternative**.
> 
> ShopPal implements this simulated Alexa+ experience directly in the web dashboard. It feels like a natural voice-assistant interaction with conversational audio-style spoken outputs and rich visual response cards. Actual voice I/O is not required.
> 
> **ShopPal does NOT claim an actual live connection to Amazon's gated Alexa+ service.** Instead, every user interaction routes deterministically to ShopPal's real backend and local Spring AI MCP server.

### Demonstration Workflows
The simulated Alexa+ experience demonstrates two core retail workflows with strict account-level data isolation:
1. **Kirana Owner Workflow**:
   - Query: *"What stock needs restocking and why?"*
   - Invokes MCP Tool: `get_recommendations`
   - Dynamically analyzes 7-day sales velocity and lead-time coverage to recommend order quantities (e.g., Basmati Rice and Toor Dal) while explaining why other staples have sufficient safety stock.
2. **Pharmacy Owner Workflow**:
   - Query: *"Which medicine batches are nearing expiry?"*
   - Invokes MCP Tool: `get_expiry_alerts`
   - Dynamically analyzes medicine batches to flag expired drugs for immediate quarantine (e.g., Cough Relief Syrup) and near-expiry drugs for First-Expiry-First-Out dispensing (e.g., Amoxicillin), while keeping Kirana grocery batches completely isolated.

---

## Model Context Protocol (MCP) Verification
ShopPal implements **MCP specification 2025-11-25** over **Streamable HTTP** at `http://127.0.0.1:8080/mcp`.
- **Protocol Version**: `2025-11-25`
- **Supported Methods**: `initialize`, `notifications/initialized`, `tools/list`, and `tools/call`.
- **Session Transport**: Managed via standard `Mcp-Session-Id` headers.
- **Account Isolation**: Tools operate strictly on the authenticated shop's catalog and batch records.

---

## Run locally

Requirements: Java 21, internet access on first build to download Maven/dependencies, and Node.js 20.19+ or 22.12+.

1. Start the backend in PowerShell:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   The API and MCP Streamable HTTP endpoint bind to `127.0.0.1:8080`.

2. In a second terminal, start the frontend:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

   Open `http://127.0.0.1:5173`.

3. Sign in using any seeded demo account or register a new shop:
   - **Kirana Store**: `kirana@shoppal.local` / `Kirana@123`
   - **Pharmacy Shop**: `pharmacy@shoppal.local` / `Pharmacy@123`
   - **Footwear Shop**: `footwear@shoppal.local` / `Footwear@123`

---

## What is implemented

- **Unified Authentication & Multi-Business Extensibility**: Secure PBKDF2 authentication supporting Kirana, Pharmacy, Footwear, and future shop types with full account-level data isolation.
- **Simulated Alexa+ Assistant**: Natural voice-like conversational assistant with spoken speech bubbles, underlying MCP tool badges, and rich visual product/batch cards.
- **Spring AI MCP Server**: Spec-compliant Streamable HTTP endpoint (`2025-11-25`) exposing `get_inventory_summary`, `get_recommendations`, `get_expiry_alerts`, `analyze_returns`, and `get_footwear_durability`.
- **Kirana Inventory Recommendations**: Deterministic fallback, lead-time coverage, safety stock, pack rounding, expiry suppression, and optional normal-approximation newsvendor calculation.
- **Pharmacy Expiry & Quarantine Guard**: Medicine batch tracking, quarantine alerts for expired batches, and FEFO suggestions.
- **Illustrative 2026 Seasonality**: Festival multipliers (Vijayadashami, Diwali) in `application.yml`.
- **Return & Quality Signals**: Returned units vs units sold, profit analysis, and evidence-based footwear durability scoring.
- **CSV Import & Validation**: Isolated CSV import with preview, error checking, and duplicate detection.

---

## Limitations

- H2 uses an in-memory database; data resets when the backend restarts.
- **Alexa+ Status**: Simulated Alexa+ experience grounded in local MCP tools. No live connection to Amazon AWS/Bedrock or Alexa+ gated service is claimed.
- Seeded inventory and sales records are labeled demo/sample data for demonstration purposes.



