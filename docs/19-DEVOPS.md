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

One script owns the whole local stack, so nobody has to remember the order:

- `.\scripts\dev.ps1` — containers, seed if the database is empty, API, web; waits
  for each part to answer before starting the next and prints where everything is
- `.\scripts\dev.ps1 status` — what is up, on which port, with row counts
- `.\scripts\dev.ps1 down` — stop everything, keep the data
- `.\scripts\dev.ps1 up -Clean` — drop the database volume and the Next.js cache, reseed
- `.\scripts\dev.ps1 seed` — force a seed run
- `.\scripts\dev.ps1 logs` — follow the API and web logs
- `-WithSkeletons` also starts worker (8081) and executor (8082), health-only until Phase 9

Ports: web 3000, API 8090, postgres 5432, redis 6379, minio 9000/9001. Logs and pids
under `.run/`.

Underneath, if you prefer the parts:

- `docker compose -f infra/compose/docker-compose.yml up -d`
- `./gradlew :api:seed` — migrations plus authored curriculum, assessments, AI catalogue
- `./gradlew :api:bootRun`
- `pnpm --filter web dev`

Run a production build while a `next dev` server is up without fear: build and start
write to `.next-prod`, and the dev server keeps using `.next`. Sharing one cache was
what turned every route into a 500 whenever someone verified a build.

JDK 21 and Node 22+ on PATH.
