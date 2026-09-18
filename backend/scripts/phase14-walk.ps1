# Phase 14 gate walk: dashboard matches mastery dwell; admin cost burn from ai_usage.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase14-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

Step "dashboard starts at zero dwell"
$before = Get "/v1/me/dashboard"
Write-Host ("   timeOnTask={0} velocity={1}" -f $before.timeOnTaskSeconds, $before.practicedOrHigherInWindow)
if ($before.timeOnTaskSeconds -ne 0) { throw "Expected zero dwell for a fresh learner" }
if ($before.note -notmatch "No XP") { throw "Dashboard must refuse vanity XP copy" }

Step "record dwell on a published unit"
Post "/v1/units/python-values-and-types/events" @{ kind = "unit_opened" } | Out-Null
Post "/v1/units/python-values-and-types/events" @{ kind = "dwell"; seconds = 90 } | Out-Null
$after = Get "/v1/me/dashboard"
Write-Host ("   timeOnTask={0}" -f $after.timeOnTaskSeconds)
if ($after.timeOnTaskSeconds -lt 1) { throw "Dashboard dwell must match mastery_records after heartbeats" }

Step "grant admin and read cost burn"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$compose = Join-Path $repoRoot "infra\compose\docker-compose.yml"
$grantSql = "INSERT INTO user_roles (user_id, role_code) SELECT id, 'admin' FROM users WHERE email = '$email' ON CONFLICT DO NOTHING;"
& docker compose -f $compose exec -T postgres psql -U aileapro -d aileapro -v ON_ERROR_STOP=1 -c $grantSql | Out-Null
Post "/v1/auth/logout" @{} | Out-Null
Post "/v1/auth/login" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

$cost = Get "/v1/admin/analytics/cost?days=30"
Write-Host ("   costUsd={0} calls={1}" -f $cost.costUsd, $cost.calls)
if ($null -eq $cost.costUsd) { throw "Missing costUsd" }
if ($cost.note -notmatch "Estimated") { throw "Cost burn must say estimated" }

$funnel = Get "/v1/admin/analytics/funnel"
Write-Host ("   registered={0} onboarded={1} diagnostic={2} practiced={3}" -f `
    $funnel.registered, $funnel.onboarded, $funnel.diagnosticCompleted, $funnel.firstPracticed)
if ($funnel.registered -lt 1) { throw "Funnel registered count too low" }

Write-Host ""
Write-Host "Phase 14 walk complete." -ForegroundColor Green
