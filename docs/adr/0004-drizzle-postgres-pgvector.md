# ADR 0004 — Flyway + JPA + PostgreSQL 16 + pgvector

## Status

Accepted (replaces Drizzle). See [0008](0008-java-spring-backend.md).

## Decision

- Flyway SQL migrations as source of truth.
- Spring Data JPA for aggregate roots (User, LearningUnit, Attempt, …).
- JDBC/`JdbcClient` for closure-table walks, bulk mastery events, `pgvector` similarity.
- One persistence stack. Do not add jOOQ or MyBatis unless an ADR proves JPA is blocking.

## Consequences

Team must review N+1 queries. Graph queries are explicit SQL.
