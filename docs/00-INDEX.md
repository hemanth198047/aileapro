# AILEAPRO — Planning Pack Index

Product name: **AILEAPRO**  
Code / repository name: **aileapro**  
Status: **Documentation-only.** No application code until this pack is accepted.

This folder is the source of truth for architecture, product, delivery, and operations. Read documents in the numbered order unless you are jumping to a specific concern.

## Reading order

1. [00-INDEX.md](00-INDEX.md) — this file
2. **[AILEAPRO_MASTER_BUILD_PLAN.md](AILEAPRO_MASTER_BUILD_PLAN.md) — phase-by-phase build script (start Phase 1 here)**
3. [01-VISION.md](01-VISION.md) — product thesis, non-goals, differentiators
4. [02-PRODUCT_SPEC.md](02-PRODUCT_SPEC.md) — complete feature specification
4. [03-PERSONAS_AND_JOURNEYS.md](03-PERSONAS_AND_JOURNEYS.md)
5. [04-INFORMATION_ARCHITECTURE.md](04-INFORMATION_ARCHITECTURE.md)
6. [05-UX_SPEC.md](05-UX_SPEC.md)
7. [06-CURRICULUM_MODEL.md](06-CURRICULUM_MODEL.md)
8. [07-MASTERY_AND_ADAPTIVE_LEARNING.md](07-MASTERY_AND_ADAPTIVE_LEARNING.md)
9. [08-KNOWLEDGE_GRAPH.md](08-KNOWLEDGE_GRAPH.md)
10. [09-AI_ARCHITECTURE.md](09-AI_ARCHITECTURE.md)
11. [10-CONTINUOUS_INTELLIGENCE.md](10-CONTINUOUS_INTELLIGENCE.md)
12. [11-ARCHITECTURE.md](11-ARCHITECTURE.md) — system, domain, data, deployment, scale
13. [12-DATA_MODEL.md](12-DATA_MODEL.md)
14. [13-API_DESIGN.md](13-API_DESIGN.md)
15. [14-TECH_STACK.md](14-TECH_STACK.md)
16. [15-SECURITY.md](15-SECURITY.md)
17. [16-PRIVACY_COMPLIANCE.md](16-PRIVACY_COMPLIANCE.md)
18. [17-OBSERVABILITY.md](17-OBSERVABILITY.md)
19. [18-TESTING_STRATEGY.md](18-TESTING_STRATEGY.md)
20. [19-DEVOPS.md](19-DEVOPS.md)
21. [20-ROADMAP.md](20-ROADMAP.md) — phases, stages, gates, staffing
22. [21-IMPLEMENTATION_PLAYBOOK.md](21-IMPLEMENTATION_PLAYBOOK.md)
23. [22-RISK_REGISTER.md](22-RISK_REGISTER.md)
24. [23-COST_MODEL.md](23-COST_MODEL.md)
25. [24-SEED_CONTENT_PLAN.md](24-SEED_CONTENT_PLAN.md)
26. [25-ACCEPTANCE_CRITERIA.md](25-ACCEPTANCE_CRITERIA.md)
27. [26-GLOSSARY.md](26-GLOSSARY.md)
28. [27-OPERATING_MODEL.md](27-OPERATING_MODEL.md) — legal, team, metrics, Windows/WSL
29. [28-PHASE0_ASSESSMENT.md](28-PHASE0_ASSESSMENT.md) — repository inspection (none yet)
30. [adr/](adr/) — architecture decision records (see **0008 Java backend**)

## Decision freeze (defaults, change only with an ADR)

These defaults exist so implementation does not stall on unstated choices:

| Area | Decision |
|---|---|
| Repo | One git repo: Gradle multi-project (`backend/`) + pnpm (`apps/web`) |
| Web | Next.js (App Router) + TypeScript (UI only) |
| API / worker / executor | **Java 21 + Spring Boot** (`backend/api`, `backend/worker`, `backend/executor`) |
| DB | PostgreSQL 16 + Flyway + Spring Data JPA (+ JDBC for graph/pgvector) |
| Graph | Relational knowledge graph in Postgres (adjacency + closure table); Neo4j optional later |
| Vectors | pgvector, swappable via embedding store interface |
| Cache / jobs | Redis + Spring (streams/lists, scheduling, Spring Batch) |
| Search | OpenSearch (keyword + hybrid) later; Postgres FTS in P0 |
| Auth | Spring Security + Spring Session, RBAC |
| Objects | S3-compatible (MinIO local, AWS S3 prod) |
| Code exec | Isolated workers (E2B or Firecracker/gVisor), never on API JVM |
| AI | Spring AI adapters behind ports (OpenAI, Anthropic, Google, Bedrock, Azure, local) |
| Observability | OpenTelemetry Java agent + Micrometer + Grafana + Sentry |
| Tests | JUnit/Testcontainers/ArchUnit + Playwright + prompt regression |
| Deploy | Docker Compose (dev) → Kubernetes (prod) |
| i18n | Architecture from day one (`en` first locale) |
| Multi-tenant | Single-tenant SaaS first; tenant isolation hooks in schema |

## What is explicitly out of this documentation cycle

- Application source code
- Generated fake curriculum at 50,000-concept scale
- Real production credentials
- Claiming the product is built
