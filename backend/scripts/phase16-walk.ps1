# Phase 16 gate walk: export/anonymize, flags, legal URLs (web optional).
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase16-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

function Put($path, $body) {
    $json = $body | ConvertTo-Json -Depth 8 -Compress
    $headers = Csrf
    return Invoke-RestMethod -Method Put -Uri "$api$path" -WebSession $session -Headers $headers -Body $json
}

function Get($path) {
    return Invoke-RestMethod -Uri "$api$path" -WebSession $session
}

function ApiDelete($path) {
    $headers = Csrf
    return Invoke-RestMethod -Method Delete -Uri "$api$path" -WebSession $session -Headers $headers
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "export contains account email"
$export = Get "/v1/me/export"
if ($export.account.email -ne $email) { throw "Export email mismatch" }
Write-Host ("   exportedAt={0}" -f $export.exportedAt)

Step "public flags present"
$flags = Get "/v1/flags"
if ($null -eq $flags.lab_enabled) { throw "Missing lab_enabled" }
if ($null -eq $flags.tutor_enabled) { throw "Missing tutor_enabled" }
Write-Host ("   lab={0} tutor={1}" -f $flags.lab_enabled, $flags.tutor_enabled)

Step "admin disables then re-enables lab"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$compose = Join-Path $repoRoot "infra\compose\docker-compose.yml"
$grantSql = "INSERT INTO user_roles (user_id, role_code) SELECT id, 'admin' FROM users WHERE email = '$email' ON CONFLICT DO NOTHING;"
& docker compose -f $compose exec -T postgres psql -U aileapro -d aileapro -v ON_ERROR_STOP=1 -c $grantSql | Out-Null
Post "/v1/auth/logout" @{} | Out-Null
Post "/v1/auth/login" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

$off = Put "/v1/admin/flags/lab_enabled" @{ enabled = $false }
if ($off.enabled -ne $false) { throw "lab_enabled did not turn off" }
$lab = Get "/v1/lab/status"
if ($lab.available -ne $false) { throw "Lab should be unavailable when flag off" }
Put "/v1/admin/flags/lab_enabled" @{ enabled = $true } | Out-Null

Step "anonymize account blocks login"
ApiDelete "/v1/me" | Out-Null
Post "/v1/auth/logout" @{} | Out-Null
try {
    Post "/v1/auth/login" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null
    throw "Login should fail after anonymize"
} catch {
    if ($_.Exception.Message -match "should fail") { throw }
    Write-Host "   login refused after delete (expected)"
}

Write-Host ""
Write-Host "Phase 16 walk complete." -ForegroundColor Green
Write-Host "Legal stubs: /privacy /terms on the web app."
Write-Host "Backup drill: backend/scripts/backup-restore-drill.ps1"
Write-Host "Sign docs/LAUNCH_CHECKLIST.md before public launch."
