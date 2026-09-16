# 14 — Tech stack (decision freeze)

Change only via ADR. **Backend is Java-first** ([ADR 0008](adr/0008-java-spring-backend.md)).

## Languages and runtimes

| Layer | Language | Role |
|---|---|---|
| API, worker, executor, shared libs | **Java 21 LTS** | All product servers and domain logic |
| Web UI | TypeScript 5.x | Next.js learner/admin studio only |
| OpenAPI client | TypeScript **generated** | No hand-written API DTOs |
| Learner sandbox | Python 3.12 (P0), Java/JS later | Untrusted student code inside executor images |

The platform itself is not a Node backend. NestJS/Drizzle/BullMQ are **not** used.

## Backend (extensive Java)

| App | Stack |
|---|---|
| `backend/api` | Spring Boot 3.4+, WebMVC (SSE for tutor), Validation, Spring Modulith |
| `backend/worker` | Spring Boot, Redis Streams/lists, `@Scheduled`, Spring Batch (intelligence) |
| `backend/executor` | Spring Boot, Docker Java API or E2B HTTP, no DB credentials |
| `backend/libs/domain` | Pure domain + ports |
| `backend/libs/ai` | Spring AI adapters behind ports |
| `backend/libs/security` | Spring Security helpers |
| `backend/libs/persistence` | JPA entities, JDBC graph/vector |

**Build:** Gradle 8.x, multi-project, Gradle Wrapper committed. JUnit 5, AssertJ, Testcontainers, ArchUnit (Modulith). MapStruct optional for DTO mapping. springdoc-openapi. Micrometer + OpenTelemetry Java agent. Logback JSON (Logstash encoder). Sentry Java.

**Auth:** Spring Security + Spring Session (Redis).

**AI:** Spring AI (OpenAI, Anthropic, Bedrock, Ollama, Azure, Vertex as available) behind `ChatPort` / `EmbeddingPort`. Teaching policy stays in our code. No LangChain as control plane ([ADR 0007](adr/0007-no-langchain-control-plane.md)).

## Frontend (narrow TypeScript)

| App | Stack |
|---|---|
| `apps/web` | Next.js App Router, React 19, Tailwind v4, Radix/shadcn in `packages/ui` |

TanStack Query, Zustand ephemeral only, EventSource for SSE. KaTeX, sanitized Mermaid.

**Why not Vaadin/Thymeleaf:** UX spec requires command palette, split-pane learn+lab, knowledge map, command density of a developer studio. React is the UI runtime; Java is the system of record and intelligence.

## Data

- PostgreSQL 16, `pgvector`, `pg_trgm`
- Flyway migrations
- Redis 7 (cache, session, queues)
- OpenSearch 2.x when hybrid search ships (Postgres FTS in P0 — ADR 0006)
- MinIO local / S3 prod

## Testing

| Layer | Tool |
|---|---|
| Java unit/slice | JUnit 5, Mockito, Spring Boot tests |
| Java integration | Testcontainers (Postgres, Redis) |
| Architecture | ArchUnit / Spring Modulith verifier |
| API contract | springdoc + OpenAPI tests; Pact optional |
| Web | Vitest (UI kit), Playwright (journeys) |
| AI | Prompt fixtures; FakeChatClient in tests (no paid keys in PR) |

## Quality

- Java: Checkstyle or Spotless, Error Prone optional, `-parameters`, NullAway or JSpecify as we mature
- TS: ESLint, Prettier, `strict`
- Both in GitHub Actions

## Observability

OTel Java agent + Micrometer. Grafana stack. Sentry Java + Sentry browser. Structured JSON logs.

## Infra

Docker Compose (web, api, worker, executor, postgres, redis, minio). Kubernetes later. Terraform later.

## Why this split

- **Java extensively:** transactions, RBAC, jobs, AI orchestration, sandbox control, CI engine — all JVM.
- **React only where it wins:** interactive learning UX.
- **Python only in the cage:** student experiments, not the SaaS.

## Rejected for P0

- NestJS / Express / Fastify as API
- Drizzle / Prisma
- Better Auth
- Mongo as primary DB
- Hardcoded OpenAI in React
- JupyterHub for all learners
- Kubernetes on day one
- Java UI frameworks as the learner shell
- Mixing JPA + jOOQ without an ADR
