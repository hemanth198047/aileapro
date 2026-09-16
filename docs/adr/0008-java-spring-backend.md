# ADR 0008 — Java / Spring Boot owns all backend

## Status

Accepted. Supersedes the NestJS/Drizzle/BullMQ/Better Auth portions of ADR 0001, 0004, and 0005.

## Context

The product owner requires **Java to be used as extensively as possible** on the server. The original freeze used TypeScript (NestJS) for API, workers, and executor control. The learning UX still benefits from a modern React web client.

## Decision

1. **All backend processes are Java 21:** `api`, `worker`, `executor` (controller), shared `libs`.
2. **Framework:** Spring Boot 3.4+ with **Spring Modulith** domain modules (not a single anemic package).
3. **Persistence:** PostgreSQL 16 + **Flyway** + **Spring Data JPA** (and native SQL / JDBC templates for graph/closure and pgvector). Hibernate is allowed; do not hide pgvector behind leaky abstractions — keep a `VectorStore` port.
4. **Jobs:** Spring + Redis (Redis Streams or lists) + `@Scheduled` + optional Spring Batch for intelligence pipelines. Not BullMQ.
5. **Auth:** **Spring Security** + server sessions (Spring Session Redis). OAuth2 client in P1. Not Better Auth.
6. **AI:** **Spring AI** behind our ports (`ChatPort`, `EmbeddingPort`, `ModerationPort`) so providers stay swappable. Spring AI is an adapter layer, not the product’s teaching policy.
7. **API docs:** springdoc-openapi; TypeScript client generated for `apps/web`.
8. **Web UI remains Next.js + TypeScript.** A Java-only UI (Thymeleaf/Vaadin/JSP) cannot meet the split-pane studio UX bar without a large rewrite and is **rejected for P0**.
9. **Learner sandbox language** remains Python (and later others) **inside** the executor image — that is curriculum, not the platform language.
10. **Repo:** Gradle multi-project for Java (`/backend`) + pnpm for `apps/web`. One git repo `ailepro`. Make/`just` scripts orchestrate both.

## Consequences

+ Natural fit for RBAC, transactions, long-lived services, strong typing, hiring Java talent.
+ Executor and API can share `libs` without Node.
− Two runtimes locally (Node for UI, JVM for servers) — Compose hides this.
− OpenAPI contract is the type bridge; do not duplicate DTOs by hand in TS.
− Slightly slower UI iteration than a TS BFF — acceptable.

## Non-goals

- Rewriting the frontend in Java.
- Using Java applets or running learner Java *instead of* Python in P0 labs (Java as a **lab language** is P2 via the same executor contract).
