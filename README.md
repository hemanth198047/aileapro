# AILEAPRO

The AI university, lab, and personal tutor. `aileapro` is the repository name.

- Planning: [`docs/00-INDEX.md`](docs/00-INDEX.md)
- Build script: [`docs/AILEAPRO_MASTER_BUILD_PLAN.md`](docs/AILEAPRO_MASTER_BUILD_PLAN.md)

## Current stack (through Phase 2)

| Piece | Path |
|---|---|
| API | `backend/api` Java 21 Spring Boot |
| Worker stub | `backend/worker` (health only) |
| Executor stub | `backend/executor` (health only — no learner code) |
| Web | `apps/web` Next.js |
| Design system | `packages/ui` (`@aileapro/ui`) tokens and primitives |
| Data | Postgres 16, Redis 7, MinIO |

## Spaces

`/app` redirects to `/app/today`. Every space below is routed, and each one states what it is
waiting on instead of showing invented progress. `apps/web/lib/spaces.tsx` is the registry that
drives the navigation, command palette (`Ctrl K`), breadcrumb, and empty states.

Today · Learn · Map · Assess · Lab · Studio · Notes · Research · Evolve · Career · Search ·
Settings · Admin (`/app/admin` renders the API's 403 unless you hold the admin role)

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

API: http://localhost:8090/v1/me (after login cookie)  
OpenAPI: http://localhost:8090/swagger-ui.html  
Health: http://localhost:8090/actuator/health

The API listens on 8090 because 8080 is commonly taken (XAMPP/Apache, Jenkins). Override with
`AILEAPRO_API_PORT`, and point the web proxy at it with `AILEAPRO_API_ORIGIN`.

Dev email verification tokens are printed in the API log (not sent by mail yet).

## Tests

```powershell
cd backend
gradle check
pnpm --filter @aileapro/ui typecheck
pnpm --filter web typecheck
pnpm --filter web build
```

If `./gradlew` times out downloading a distribution, use the system `gradle` command (9.x is fine).
