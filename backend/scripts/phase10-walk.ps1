# Phase 10 gate walk: finish beginner project, portfolio artifact, Apply solid.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase10-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

function Patch($path, $body) {
    $json = $body | ConvertTo-Json -Depth 8 -Compress
    return Invoke-RestMethod -Method Patch -Uri "$api$path" -WebSession $session -Headers (Csrf) -Body $json
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

$mse = @"
def mse(y_true, y_pred):
    n = len(y_true)
    return sum((a - b) ** 2 for a, b in zip(y_true, y_pred)) / n
"@

$fit = @"
def fit(xs, ys):
    n = len(xs)
    mean_x = sum(xs) / n
    mean_y = sum(ys) / n
    num = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
    den = sum((x - mean_x) ** 2 for x in xs)
    slope = num / den
    intercept = mean_y - slope * mean_x
    return slope, intercept
"@

$predict = @"
def fit(xs, ys):
    n = len(xs)
    mean_x = sum(xs) / n
    mean_y = sum(ys) / n
    num = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
    den = sum((x - mean_x) ** 2 for x in xs)
    slope = num / den
    intercept = mean_y - slope * mean_x
    return slope, intercept

def predict(x, slope, intercept):
    return slope * x + intercept
"@

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "projects list includes beginner + outline RAG"
$projects = Get "/v1/projects"
$lr = $projects | Where-Object { $_.slug -eq "linear-regression-beginner" }
$rag = $projects | Where-Object { $_.slug -eq "production-rag" }
Write-Host ("   lr accepts={0} rag status={1}" -f $lr.acceptsSubmissions, $rag.status)
if (-not $lr.acceptsSubmissions) { throw "Beginner project should accept submissions" }
if ($rag.status -ne "outline") { throw "Production RAG should be outline" }

Step "complete three milestones"
Post "/v1/projects/linear-regression-beginner/milestones/mean-squared-error/submit" @{ source = $mse } | Out-Null
Post "/v1/projects/linear-regression-beginner/milestones/fit-line/submit" @{ source = $fit } | Out-Null
$final = Post "/v1/projects/linear-regression-beginner/milestones/predict-and-report/submit" @{ source = $predict }
Write-Host ("   passed={0} completed={1} portfolio={2}" -f $final.passed, $final.projectCompleted, $final.portfolioItemId)
if (-not $final.passed) { throw "Final milestone did not pass" }
if (-not $final.projectCompleted) { throw "Project did not complete" }
if (-not $final.portfolioItemId) { throw "No portfolio item" }

Step "portfolio shows private artifact"
$folio = Get "/v1/portfolio"
Write-Host ("   items={0} visibility={1}" -f $folio.items.Count, $folio.items[0].visibility)
if ($folio.items.Count -lt 1) { throw "Portfolio empty" }
if ($folio.items[0].visibility -ne "private") { throw "Expected private default" }

Step "Apply chip solid on python-functions"
$mastery = Get "/v1/units/python-functions/mastery"
Write-Host ("   apply={0}" -f $mastery.evidence.apply.state)
if ($mastery.evidence.apply.state -ne "solid") { throw "Apply chip not solid" }

Write-Host ""
Write-Host "Phase 10 walk complete." -ForegroundColor Green
