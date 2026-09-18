# Phase 13 gate walk: draft hidden from learners; publish audited; rollback restores draft.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase13-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
$slug = "walk-$([guid]::NewGuid().ToString('N').Substring(0,8))"
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
        # logout is 204 with empty body — RestMethod can treat that as failure
        if ($path -eq "/v1/auth/logout") { return $null }
        throw
    }
}

function Get($path) {
    return Invoke-RestMethod -Uri "$api$path" -WebSession $session
}

function GetStatus($path) {
    try {
        Invoke-WebRequest -Uri "$api$path" -WebSession $session -UseBasicParsing | Out-Null
        return 200
    } catch {
        return [int]$_.Exception.Response.StatusCode
    }
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "learner is forbidden on admin studio"
$code = GetStatus "/v1/admin/units"
if ($code -ne 403) { throw "Expected 403 for learner on /v1/admin/units, got $code" }
Write-Host "   forbidden=$code"

Step "grant admin role and re-login"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$compose = Join-Path $repoRoot "infra\compose\docker-compose.yml"
$grantSql = "INSERT INTO user_roles (user_id, role_code) SELECT id, 'admin' FROM users WHERE email = '$email' ON CONFLICT DO NOTHING;"
& docker compose -f $compose exec -T postgres psql -U aileapro -d aileapro -v ON_ERROR_STOP=1 -c $grantSql | Out-Null
Post "/v1/auth/logout" @{} | Out-Null
Post "/v1/auth/login" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "create publishable draft $slug"
$created = Post "/v1/admin/units" @{
    slug = $slug
    title = "Phase 13 Walk Unit"
    difficulty = 2
    objectives = @("State one fact from the walk")
    blocks = @(
        @{ type = "why"; markdown = "Why the walk matters." }
        @{ type = "beginner"; markdown = "A first explanation." }
    )
}
if ($created.status -ne "draft") { throw "Expected draft status" }
if (-not $created.critic.publishable) { throw "Expected critic to allow publish" }
Write-Host ("   status={0} publishable={1}" -f $created.status, $created.critic.publishable)

Step "learner still 404 on draft"
$learnerEmail = "phase13-learn-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
$learner = New-Object Microsoft.PowerShell.Commands.WebRequestSession
function LearnerCsrf {
    Invoke-RestMethod -Uri "$api/v1/auth/csrf" -WebSession $learner | Out-Null
    $token = ($learner.Cookies.GetCookies($api) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    return @{ "X-XSRF-TOKEN" = [System.Net.WebUtility]::UrlDecode($token); "Content-Type" = "application/json" }
}
$reg = @{ email = $learnerEmail; password = "Correct-Horse-9!" } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri "$api/v1/auth/register" -WebSession $learner -Headers (LearnerCsrf) -Body $reg | Out-Null
try {
    Invoke-WebRequest -Uri "$api/v1/units/$slug" -WebSession $learner -UseBasicParsing | Out-Null
    throw "Draft must 404 for learners"
} catch {
    $sc = [int]$_.Exception.Response.StatusCode
    if ($sc -ne 404) { throw "Expected learner 404 on draft, got $sc" }
    Write-Host "   learner_draft_status=$sc"
}

Step "publish with audit snapshot"
$published = Post "/v1/admin/units/$slug/publish" @{ reason = "phase13-walk" }
if ($published.status -ne "published") { throw "Publish did not set published" }
if (-not $published.auditId) { throw "Missing auditId" }
if (-not $published.changeId) { throw "Missing changeId" }
Write-Host ("   version={0} audit={1} change={2}" -f $published.toVersion, $published.auditId, $published.changeId)

$learnerUnit = Invoke-RestMethod -Uri "$api/v1/units/$slug" -WebSession $learner
if ($learnerUnit.title -ne "Phase 13 Walk Unit") { throw "Learner should see published unit" }
Write-Host "   learner_can_read_published=true"

Step "rollback restores draft and learner 404"
$rolled = Post "/v1/admin/units/$slug/rollback" @{}
if ($rolled.status -ne "draft") { throw "Rollback must return draft" }
try {
    Invoke-WebRequest -Uri "$api/v1/units/$slug" -WebSession $learner -UseBasicParsing | Out-Null
    throw "Rolled-back unit must 404 for learners"
} catch {
    $sc = [int]$_.Exception.Response.StatusCode
    if ($sc -ne 404) { throw "Expected learner 404 after rollback, got $sc" }
    Write-Host "   learner_after_rollback=$sc"
}

Write-Host ""
Write-Host "Phase 13 walk complete." -ForegroundColor Green
