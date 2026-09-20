# AILEAPRO

The AI university, lab, and personal tutor. `aileapro` is the repository name.

- Planning: [`docs/00-INDEX.md`](docs/00-INDEX.md)
- Build script: [`docs/AILEAPRO_MASTER_BUILD_PLAN.md`](docs/AILEAPRO_MASTER_BUILD_PLAN.md)

## Repository layout

| Tree | What lives here |
|---|---|
| [`frontend/`](frontend/) | Next.js web app (`web/`) and UI package (`packages/ui`) — pnpm workspace |
| [`backend/`](backend/) | Java 21 / Spring Boot API, worker, executor, shared libs, seed content |
| [`infra/`](infra/) | Docker Compose, Grafana dashboards, local start/stop scripts |
| [`docs/`](docs/) | Product and engineering specs |

```text
aileapro/
  frontend/                 # TypeScript only
    web/                    # Next.js App Router
    packages/ui/            # @aileapro/ui design system
    package.json            # pnpm workspace root
  backend/                  # Java only
    api/  worker/  executor/
    libs/  scripts/
  infra/
    compose/                # Postgres, Redis, MinIO
    scripts/dev.ps1|.sh     # stack orchestration
    grafana/
  docs/
  start.bat / start.sh      # one-command local start
  stop.bat  / stop.sh
```

## Run locally

**Windows:** double-click `start.bat` (or `stop.bat` to tear down).

**macOS / Linux:**

```bash
chmod +x start.sh stop.sh
./start.sh
```

That brings up Docker, seeds if needed, API `:8090`, web `:3000`, and the lab executor.

Manual pieces (optional):

```bash
# infra
docker compose -f infra/compose/docker-compose.yml up -d

# backend
cd backend && ./gradlew :api:bootRun

# frontend
cd frontend && pnpm install && pnpm --filter web dev
```

Open http://localhost:3000 — register, then you land in the studio shell.

API health: http://localhost:8090/actuator/health · OpenAPI: http://localhost:8090/swagger-ui.html

The API listens on 8090 because 8080 is often taken. Override with `AILEAPRO_API_PORT`; point the web proxy with `AILEAPRO_API_ORIGIN`.

## Spaces

`/app` redirects to `/app/today`. `frontend/web/lib/spaces.tsx` is the registry behind navigation, the command palette (`Ctrl K`), breadcrumb, and empty states.

## Tests

```bash
cd backend && ./gradlew check
cd frontend && pnpm -r typecheck && pnpm --filter web build
```
