# ADR 0001 — Monorepo: Next.js web + Java backends

## Status

Accepted (amended by [0008](0008-java-spring-backend.md)). NestJS variant **superseded**.

## Context

Many domains; need extractable deploy units; one git repo.

## Decision

- One repo `aileapro`.
- `frontend/web`: Next.js App Router UI. Talks only to `/v1` (optional thin BFF for cookies).
- `frontend/packages/ui`, `frontend/packages/api-client` (generated): frontend only.
- `backend/api`: Spring Boot HTTP API.
- `backend/worker`: Spring Boot job processor (same modules, no public HTTP or internal-only actuator).
- `backend/executor`: Spring Boot execution controller (separate network).
- `backend/libs/*`: shared Java (domain, security, ai ports).
- `infra/`: Compose, local scripts, future k8s.
## Consequences

+ Blast radius: executor ≠ DB.
+ Shared Java modules.
− JVM + Node toolchain on developer machines (WSL2 recommended on Windows).
