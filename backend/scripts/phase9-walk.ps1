# Phase 9 gate walk: demo snippet runs, timeout kills, API never execs user code.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase9-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

function Get($path) {
    return Invoke-RestMethod -Uri "$api$path" -WebSession $session
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "lab status"
$status = Get "/v1/lab/status"
Write-Host ("   available={0} fixtures={1} kind={2} remaining={3}/{4}" -f `
    $status.available, $status.development, $status.executorKind, $status.remainingToday, $status.dailyCap)
if (-not $status.available) { throw "Lab is not available" }

Step "seed Python snippet runs and shows output"
$run = Post "/v1/lab/runs" @{
    language = "python"
    source = $status.demoSource
}
Write-Host ("   status={0} stdout={1}" -f $run.status, ($run.stdout -replace "`n", " ").Substring(0, [Math]::Min(80, $run.stdout.Length)))
if ($run.status -ne "succeeded") { throw "Demo run did not succeed" }
if ($run.stdout -notmatch "\[1, 2, 3\]") { throw "Demo stdout missing expected list" }

Step "timeout kills the job"
$timed = Post "/v1/lab/runs" @{
    language = "python"
    source = "print(1)`n# __AILEAPRO_FORCE_TIMEOUT__`n"
    timeoutMs = 200
}
Write-Host ("   status={0} timedOut={1} code={2}" -f $timed.status, $timed.timedOut, $timed.errorCode)
if (-not $timed.timedOut) { throw "Timeout marker did not time out" }
if ($timed.errorCode -ne "sandbox_timeout") { throw "Expected sandbox_timeout" }

Step "history lists both runs"
$history = Get "/v1/lab/runs"
Write-Host ("   runs={0} note={1}" -f $history.runs.Count, $history.note)
if ($history.runs.Count -lt 2) { throw "Expected at least two runs in history" }

Write-Host ""
Write-Host "Phase 9 walk complete. API ProcessBuilder proof is ApiDoesNotExecUserCodeTest." -ForegroundColor Green
