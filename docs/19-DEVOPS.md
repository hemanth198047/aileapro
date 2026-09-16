# 19 — DevOps and environments

## Environments

| Name | Purpose | Data |
|---|---|---|
| local | compose | fixtures |
| ci | ephemeral | testcontainers |
| staging | prod-like | anonymized/seed |
| prod | users | real |

## CI pipeline (GitHub Actions)

1. Set up Temurin JDK 21 + Gradle cache  
2. `./gradlew check` (unit, architecture, integration)  
3. Flyway migrate against CI Postgres  
4. Set up Node (pnpm) for `apps/web`  
5. lint, typecheck, generate `packages/api-client` from OpenAPI artifact  
6. Playwright (nightly vs staging)  
7. Docker images: `api`, `worker`, `executor`, `web` on main  
8. Staging migrate + deploy

## Config

12-factor. `application.yml` + env overrides. **Refuse prod boot** if `aileapro.ai.provider=fake`.

## Migrations

Flyway SQL in `backend/api`. Expand/contract. Never destructive without backup note.

## Secrets

GitHub Environments, later ASM/GCP SM. Rotate runbook. JVM reads only env/secret manager.

## Releases

Semver tags. Changelog. Feature flags (`flags` table): `lab_enabled`, `tutor_enabled`, `ci_agents`. Independent rollback of web vs api vs executor.

## Backups

Postgres PITR in prod. Daily snapshots staging. MinIO versioning.

## Executor fleet

Pinned sandbox images. Deploy separately. Resilience4j circuit breaker from API.

## Incident runbook (short)

1. Status page / banner  
2. Disable flag (lab/tutor)  
3. Rotate leaked key  
4. Rollback web/api independently  
5. Curriculum rollback via Update Center  

## Local DX

- `make up` — Compose: postgres, redis, minio, api, worker, web  
- `./gradlew :api:bootRun`  
- `pnpm --filter web dev`  
- `./gradlew :api:seed` — seed curriculum  
- JDK 21 and Node 22 on PATH (WSL2 recommended on Windows)
