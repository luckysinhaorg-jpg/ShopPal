# Development progress

## Current checkpoint

Phase 9 — implementation is built and local checks pass. Phase 10 documentation is prepared. The live CSV-to-dashboard path and MCP client round-trip still need verification on a machine where localhost access is available.

## Completed

- **Phase 0:** inspected the empty workspace and confirmed Java 21.0.10, Node 26.12.0, npm 11.20.0, and Git 2.54.0. The workspace is not an initialized Git repository. Bootstrapped Maven 3.9.11 inside ignored `work/`; added project Maven Wrapper scripts. Reviewed official Spring Boot 4.1.1, Spring AI 2.0.1, and Streamable HTTP documentation.
- **Phase 1:** configured Spring Boot 4.1.1 / Spring AI 2.0.1 / Java 21, H2, validation, JPA, health route, and MCP WebMVC Streamable starter. Backend clean test/package passes. The executable JAR starts successfully.
- **Phase 2:** added product, supplier, batch, sale, return, profile, repositories, validation, and reproducible synthetic Kirana/footwear records. Product listing is available by profile.
- **Phase 3:** added sample CSV download, preview, 1 MB cap, validation, quoted-field parsing, all-or-nothing import, unknown-SKU rejection, and exact-file duplicate detection. Parser tests pass.
- **Phase 4:** implemented and unit-tested deterministic demand forecasting, configurable festival multipliers, lead-time and pack-aware order recommendations, explicit stockout/overstock risk signals, an optional newsvendor normal approximation, expiry actions, return analysis, and footwear durability evidence.
- **Phase 5:** added health, dashboard, product, recommendation, expiry, return, durability, seasonality, and CSV import REST routes. CORS allows only local Vite origins.
- **Phase 6:** added five Spring AI `@McpTool` operations. The packaged app starts the Streamable HTTP MCP server and logs `Registered tools: 5`; tool names are checked by a unit test.
- **Phase 7:** built the responsive React/TypeScript dashboard, data views, profile selector, CSV workflow, search and risk filters, loading/error states, and local REST integration.
- **Phase 8:** built a rule-based conversational demo that routes common stock, expiry, returns, and durability questions to the actual backend services and shows the source path. No Bedrock, voice, Alexa+, or Alexa skill claim is made.
- **Phase 10:** added the architecture, API/MCP docs, CSV template, demo walkthrough, submission checklist, known limitations, and careful product-feedback note.

## Tests and actual results

- `mvn -Dmaven.repo.local=work/m2 -DforkCount=0 -f backend/pom.xml clean test package` — passed; 20 tests, zero failures. Maven 3.9.11 was bootstrapped under `work/maven` because Maven was not globally installed. Test forking was disabled because the sandbox blocked Java dynamic-agent attach.
- `java -jar backend/target/shoppal-backend-0.1.0-SNAPSHOT.jar` — passed startup; H2 initialized, Tomcat started on port 8080, and Spring AI logged five registered tools.
- `npm run build --prefix frontend` — passed TypeScript build and Vite production build (Vite 8.3.4).
- `npm audit --prefix frontend --omit=dev` — passed; zero production vulnerabilities reported.
- The health route was checked with standalone MockMvc. Formula, CSV parser, validation, MCP annotations, expiry, returns, and durability tests passed.

## Incomplete verification and known limitations

- This execution sandbox could not reach the application's loopback port from a separate command (`curl` timed out). The REST API routes therefore were not exercised end-to-end from a running client.
- A full Spring application-context test hung after startup in the restricted environment and was removed; the ordinary unit suite remains green.
- No networked MCP client completed `tools/list` and `tools/call`. The app startup confirms five tools register; use an MCP Inspector/client against `http://127.0.0.1:8080/mcp` before submission.
- Browser rendering, browser console, and CSV-to-recommendation interaction were not visually verified here.
- H2 is in-memory. Only Kirana and footwear have seeded data; other selectable profiles are empty. Forecasts and illustrative 2026 festival multipliers require shop-specific tuning.
- Actual Amazon Developer Console, Alexa+, and Bedrock services were not accessed; no external product feedback is claimed.

## Exact local run commands

Backend (PowerShell):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend (second PowerShell):

```powershell
cd frontend
npm ci
npm run dev
```

Tests/build:

```powershell
cd backend
.\mvnw.cmd test
cd ..\frontend
npm ci
npm run build
```

The backend uses `http://127.0.0.1:8080`; Vite uses `http://127.0.0.1:5173`.


