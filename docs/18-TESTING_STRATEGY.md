# 18 — Testing strategy

A feature is not done without tests. Layered, not only E2E.

## Unit (Java first)

- Mastery score, SRS interval, impact score, graph cycle detect, RBAC, markdown sanitize, router profile pick live in `backend/libs`.
- JUnit 5 + AssertJ. No paid AI in unit tests.

## Integration (Java)

- Testcontainers Postgres + Flyway: unit publish, mastery event → state.
- Redis stream enqueue.
- `@SpringBootTest` slices (`@WebMvcTest`, `@DataJpaTest`) where cheaper.

## Architecture tests

- Spring Modulith / ArchUnit: web cannot import persistence internals of another module; executor lib cannot depend on JPA.

## API

- MockMvc / WebTestClient + session cookies.
- IDOR cases.
- Rate limit smoke.

## Frontend

- Vitest for `packages/ui`.
- Playwright P0 journeys against API (or WireMock for isolated UI). Tutor/lab may use FakeChatClient / fake executor.

## Security tests

- CSRF, XSS in markdown, IDOR, lab cannot hit metadata IP (executor suite on JVM).

## AI evaluation tests

- Prompt regression on JVM fixtures.
- `FakeChatClient` in CI (no paid keys in PR).
- RAG eval set: 20 questions with expected source IDs (P1).

## Load (P1)

k6 on read paths and lab enqueue.

## Gates

PR: `./gradlew check` + web lint/typecheck + unit.  
Nightly: Playwright, prompt eval, full Testcontainers.  
Release: Playwright P0 list in 25-ACCEPTANCE.

## Coverage

JaCoCo on `mastery`, `graph`, `security`, `executor` client. Do not chase 100% of UI.
