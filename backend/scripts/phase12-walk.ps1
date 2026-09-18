# Phase 12 gate walk: gaps from mastery only; self-report does not shrink them.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase12-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Csrf {
    Invoke-RestMethod -Uri "$api/v1/auth/csrf" -WebSession $session | Out-Null
    $token = ($session.Cookies.GetCookies($api) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    return @{ "X-XSRF-TOKEN" = [System.Net.WebUtility]::UrlDecode($token); "Content-Type" = "application/json" }
}

function Post($path, $body) {
    $json = if ($null -eq $body) { "{}" } else { $body | ConvertTo-Json -Depth 8 -Compress }
    return Invoke-RestMethod -Method Post -Uri "$api$path" -WebSession $session -Headers (Csrf) -Body $json
}

function Put($path, $body) {
    $json = $body | ConvertTo-Json -Depth 8 -Compress
    return Invoke-RestMethod -Method Put -Uri "$api$path" -WebSession $session -Headers (Csrf) -Body $json
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

Step "claim expert experience on the AI Engineer path"
Put "/v1/me/profile" @{
    targetPath = "ai-engineer-abridged"
    experience = "working_in_ml"
    hoursPerWeek = 12
} | Out-Null

Step "gaps still show not job-ready"
$gaps = Get "/v1/me/career/gaps"
Write-Host ("   gaps={0} ready={1}/{2} persona={3}" -f $gaps.gapCount, $gaps.readyUnitCount, $gaps.requiredUnitCount, $gaps.interviewPersona)
if ($gaps.readyForRole) { throw "Self-report must not mark the role ready" }
if ($gaps.gapCount -le 0) { throw "Expected gaps for a fresh learner" }
if ($gaps.honesty -notmatch "Not job-ready") { throw "Missing honesty copy" }
if ($gaps.interviewPersona -ne "interview_coach") { throw "Expected interview_coach persona" }
$claimedGaps = $gaps.gapCount

Step "changing experience claim does not change gap count"
Put "/v1/me/profile" @{ experience = "none" } | Out-Null
$again = Get "/v1/me/career/gaps"
Write-Host ("   gaps after claim change={0}" -f $again.gapCount)
if ($again.gapCount -ne $claimedGaps) { throw "Gaps must ignore self-reported experience" }

Step "interview coach conversation starts"
$conv = Post "/v1/tutor/conversations" @{
    unitSlug = "python-values-and-types"
    persona = "interview_coach"
}
Write-Host ("   persona={0}" -f $conv.persona)
if ($conv.persona -ne "interview_coach") { throw "Interview coach persona not stored" }

Write-Host ""
Write-Host "Phase 12 walk complete." -ForegroundColor Green
