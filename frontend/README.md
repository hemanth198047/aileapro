# Frontend (pnpm)

TypeScript / Next.js only. Does not contain Java or Docker definitions.

| Path | Role |
|---|---|
| `web/` | Next.js App Router learner and admin UI |
| `packages/ui/` | `@aileapro/ui` design tokens and primitives |

```bash
cd frontend
pnpm install
pnpm --filter web dev          # http://localhost:3000
pnpm -r typecheck
pnpm --filter web build
```

API origin defaults to `http://localhost:8090` (see `web/next.config.ts`). Override with `AILEAPRO_API_ORIGIN`.

Prefer starting the whole stack from the repo root with `start.bat` / `./start.sh`.
