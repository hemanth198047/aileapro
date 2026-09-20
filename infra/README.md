# Infrastructure

Local and deployable ops assets. Application source lives in `frontend/` and `backend/`.

| Path | Role |
|---|---|
| `compose/` | Docker Compose: Postgres 16 + pgvector, Redis 7, MinIO |
| `scripts/dev.ps1` | Windows stack orchestration |
| `scripts/dev.sh` | macOS / Linux stack orchestration |
| `grafana/dashboards/` | Optional AI cost dashboard JSON |

From the **repo root**:

```bash
# preferred
./start.sh          # or start.bat on Windows
./stop.sh           # or stop.bat

# or call the orchestrator directly
./infra/scripts/dev.sh up --with-skeletons
./infra/scripts/dev.sh status
./infra/scripts/dev.sh down
```

```bash
docker compose -f infra/compose/docker-compose.yml up -d
docker compose -f infra/compose/docker-compose.yml down
```
