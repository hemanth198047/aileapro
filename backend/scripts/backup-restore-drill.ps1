# Backup / restore drill for local Postgres (Compose).
# Requires: docker compose postgres healthy.
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$compose = Join-Path $repoRoot "infra\compose\docker-compose.yml"
$outDir = Join-Path $repoRoot ".run\backups"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dump = Join-Path $outDir "aileapro-$stamp.sql"
$drillDb = "aileapro_restore_drill"

Write-Host "== dump -> $dump" -ForegroundColor Cyan
& docker compose -f $compose exec -T postgres pg_dump -U aileapro -d aileapro --clean --if-exists | Set-Content -Encoding utf8 $dump
if (-not (Test-Path $dump) -or (Get-Item $dump).Length -lt 100) {
    throw "Dump missing or too small: $dump"
}

Write-Host "== create temp db $drillDb" -ForegroundColor Cyan
& docker compose -f $compose exec -T postgres psql -U aileapro -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $drillDb;"
& docker compose -f $compose exec -T postgres psql -U aileapro -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $drillDb OWNER aileapro;"

Write-Host "== restore into $drillDb" -ForegroundColor Cyan
Get-Content -Raw $dump | & docker compose -f $compose exec -T postgres psql -U aileapro -d $drillDb -v ON_ERROR_STOP=1 | Out-Null

Write-Host "== verify rows" -ForegroundColor Cyan
$check = & docker compose -f $compose exec -T postgres psql -U aileapro -d $drillDb -tAc "SELECT count(*) FROM feature_flags;"
$check = ($check | Out-String).Trim()
if (-not $check -or [int]$check -lt 1) {
    throw "feature_flags empty after restore (got '$check')"
}
Write-Host "   feature_flags=$check"

Write-Host "== drop temp db" -ForegroundColor Cyan
& docker compose -f $compose exec -T postgres psql -U aileapro -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $drillDb;"

Write-Host ""
Write-Host "Backup restore drill OK." -ForegroundColor Green
Write-Host "Dump retained at: $dump"
Write-Host "Record path + initials in docs/LAUNCH_CHECKLIST.md"
