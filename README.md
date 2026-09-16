# ailepro

**AI Mastery** learning platform.

- Planning: [`docs/00-INDEX.md`](docs/00-INDEX.md)
- Build script: [`docs/AILEPRO_MASTER_BUILD_PLAN.md`](docs/AILEPRO_MASTER_BUILD_PLAN.md)

## Phase 1 stack

| Piece | Path |
|---|---|
| API | `backend/api` Java 21 Spring Boot |
| Worker stub | `backend/worker` (health only) |
| Executor stub | `backend/executor` (health only — no learner code) |
| Web | `apps/web` Next.js |
| Data | Postgres 16, Redis 7, MinIO |

## Run locally

```powershell
docker compose -f infra/compose/docker-compose.yml up -d
cd backend
gradle :api:bootRun
```

In another terminal:

```powershell
corepack enable
pnpm install
pnpm --filter web dev
```

Open http://localhost:3000 — register, then you should land in the studio shell.

API: http://localhost:8080/v1/me (after login cookie)  
OpenAPI: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/actuator/health

Dev email verification tokens are printed in the API log (not sent by mail yet).

## Tests

```powershell
cd backend
gradle check
```

If `./gradlew` times out downloading a distribution, use the system `gradle` command (9.x is fine).
