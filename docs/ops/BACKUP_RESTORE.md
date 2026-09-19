# Backup and restore drill

Prove that Postgres backups restore before calling launch complete.

## Local / staging drill

From the repo root (Docker Compose Postgres running):

```powershell
.\backend\scripts\backup-restore-drill.ps1
```

What it does:

1. `pg_dump` to `.run/backups/aileapro-YYYYMMDD-HHMMSS.sql`
2. Creates a temporary database `aileapro_restore_drill`
3. Restores the dump into it
4. Checks `feature_flags` (or `users`) has rows
5. Drops the temporary database

Record the dump path and operator initials in [LAUNCH_CHECKLIST.md](../LAUNCH_CHECKLIST.md).

## Production expectations

- Nightly automated dumps to object storage (region-matched).
- Retention ≥ 14 days.
- Secrets (DB password, AI keys) live in the host secret store — never in the dump filename or git.
- After restore on staging, run Flyway info and smoke `GET /v1/health` + one authenticated `/v1/me`.
