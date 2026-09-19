# Phase 15 gate walk: Evolve timestamps; admin rejects proposal; honest currentness.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase15-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Csrf {
    Invoke-RestMethod -Uri "$api/v1/auth/csrf" -WebSession $session | Out-Null
    $token = ($session.Cookies.GetCookies($api) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    return @{ "X-XSRF-TOKEN" = [System.Net.WebUtility]::UrlDecode($token); "Content-Type" = "application/json" }
}

function Post($path, $body) {
    $json = if ($null -eq $body) { "{}" } else { $body | ConvertTo-Json -Depth 8 -Compress }
    $headers = Csrf
    try {
        return Invoke-RestMethod -Method Post -Uri "$api$path" -WebSession $session -Headers $headers -Body $json
    } catch {
        if ($path -eq "/v1/auth/logout") { return $null }
        throw
    }
}

function Get($path) {
    return Invoke-RestMethod -Uri "$api$path" -WebSession $session
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "Evolve feed is timestamped and honest"
$feed = Get "/v1/evolve/feed"
Write-Host ("   items={0}" -f $feed.items.Count)
if ($feed.items.Count -lt 1) { throw "Expected published Evolve items (run seed)" }
if ($feed.honesty -notmatch "fully up to date") { throw "Missing honesty copy" }
$first = $feed.items[0]
if (-not $first.occurredAt) { throw "Missing occurredAt" }
if ($first.currentnessNote -notmatch "Verified against") { throw "Missing verification note" }
if ($first.currentnessNote -notmatch "not a claim") { throw "Currentness note must deny fully-up-to-date claims" }
Write-Host ("   note={0}" -f $first.currentnessNote)

Step "grant admin and reject a pending proposal"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$compose = Join-Path $repoRoot "infra\compose\docker-compose.yml"
$grantSql = "INSERT INTO user_roles (user_id, role_code) SELECT id, 'admin' FROM users WHERE email = '$email' ON CONFLICT DO NOTHING;"
& docker compose -f $compose exec -T postgres psql -U aileapro -d aileapro -v ON_ERROR_STOP=1 -c $grantSql | Out-Null
Post "/v1/auth/logout" @{} | Out-Null
Post "/v1/auth/login" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

$pending = Get "/v1/admin/proposals?status=pending"
if ($pending.Count -lt 1) {
    # Ensure one exists
    Post "/v1/admin/proposals" @{
        updateSlug = "legacy-bag-of-words-deprecation"
        title = "Walk reject target"
        unitSlug = "python-values-and-types"
        obsoleteTechnique = $true
        changeExisting = $true
    } | Out-Null
    $pending = Get "/v1/admin/proposals?status=pending"
}
$id = $pending[0].id
$rejected = Post "/v1/admin/proposals/$id/reject" @{ reason = "phase15-walk" }
if ($rejected.status -ne "rejected") { throw "Reject did not stick" }
Write-Host ("   rejected={0}" -f $rejected.id)

Write-Host ""
Write-Host "Phase 15 walk complete." -ForegroundColor Green
Write-Host "Lab metadata IP blocked by Docker network none (DockerNetworkIsolationTest)."
Write-Host "Docs watcher is manual: see docs/ops/DOCS_WATCHER.md"
