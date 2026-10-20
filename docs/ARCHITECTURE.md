# Architecture

```mermaid
flowchart LR
  UI[React + TypeScript dashboard] -->|REST JSON / multipart CSV| API[Spring Boot API]
  MCP[MCP client] -->|Streamable HTTP /mcp| TOOLS[Spring AI MCP tools]
  API --> SERVICES[Inventory advisor services]
  TOOLS --> SERVICES
  SERVICES --> DOMAIN[Forecast · Orders · Expiry · Returns · Durability]
  DOMAIN --> DATA[(Spring Data JPA · H2)]
```

The deterministic Java services are the single source for REST and MCP calculations. The UI does not compute order quantities. H2 is in-memory for the demo. Seeded records are explicitly marked synthetic; CSV-imported records are marked as imported.

## Package responsibilities

- `product`, `supplier`, `sales`, `inventory`: entities and repositories.
- `forecasting`, `recommendations`, `expiry`, `returns`, `durability`, `seasonality`: deterministic domain calculations.
- `importing`: bounded CSV preview and all-or-nothing import.
- `mcp`: annotated tools wrapping shared advisor services.
- `config`: H2, seed data, and local CORS settings.
