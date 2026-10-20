# ShopPal implementation plan

## Architecture

- `backend/`: Spring Boot REST API, JPA/H2 persistence, deterministic domain services, and Streamable HTTP MCP tools. REST and MCP use the same services.
- `frontend/`: React + TypeScript + Vite dashboard and conversational interface, calling the REST API.
- `docs/`: progress, architecture, and demo/submission material.

## Phase sequence

0. Inspect and prepare (current): confirm toolchain, choose compatible frameworks, establish docs and folder layout.
1. Minimal working Spring Boot backend and health check.
2. Inventory model, repositories, services, and reproducible synthetic data.
3. CSV preview, validation, sample, and import.
4. Deterministic forecasts, ordering, risk, returns, expiry, and durability analysis.
5. REST endpoints and integration checks.
6. Streamable HTTP MCP tools over shared services.
7. React dashboard and CSV workflow.
8. Data-grounded conversational UI.
9. End-to-end and quality verification.
10. README, architecture, API/MCP docs, CSV template, demo script, and submission checklist.

## Phase 0 decisions

- Workspace is empty apart from `outputs/` and `work/`; no existing application files will be overwritten.
- Java 21, Node.js, npm, and Git are present. Maven is not installed or on PATH.
- Official Spring documentation currently lists Spring Boot 4.1.1 as supporting Java 17–26 and Maven 3.6.3+, and Spring AI 2.0.1 documents the WebMVC Streamable HTTP starter. We will use the supported Spring AI starter rather than wiring a second MCP implementation directly.
- Final exact dependency pairing will be committed in the Maven build and resolved by an actual build before proceeding past the backend checkpoint.

## Planned layout

```text
backend/
  pom.xml
  src/main/java/com/shoppal/...
  src/main/resources/application.yml
  src/test/java/com/shoppal/...
frontend/
  package.json
  src/
docs/
  PROGRESS.md
  IMPLEMENTATION_PLAN.md
```
