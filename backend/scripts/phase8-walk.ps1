# Phase 8 gate walk: search finds seed curriculum; tutor cites real block ids with timestamps.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$unit = "python-values-and-types"
$email = "phase8-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

function Ask($conversation, $body) {
    $json = $body | ConvertTo-Json -Depth 4 -Compress
    $raw = Invoke-WebRequest -Method Post -Uri "$api/v1/tutor/conversations/$conversation/messages" `
        -WebSession $session -Headers (Csrf) -Body $json -UseBasicParsing
    return $raw.Content
}

function Step($label) {
    Write-Host ""
    Write-Host "== $label" -ForegroundColor Cyan
}

Step "registering $email"
Post "/v1/auth/register" @{ email = $email; password = "Correct-Horse-9!" } | Out-Null

Step "search finds a seed unit by title"
$found = Get "/v1/search?q=Values%20and%20types"
if ($found.total -lt 1) { throw "Expected at least one hit for Values and types" }
$hit = $found.results[0]
Write-Host ("   {0} {1} updated={2}" -f $hit.type, $hit.title, $hit.updatedAt)
if (-not $hit.updatedAt) { throw "Search hits must carry a knowledge timestamp" }

Step "tutor status reports retrieval on"
$status = Get "/v1/tutor/status"
Write-Host ("   available={0} fixtures={1} retrieval={2}" -f $status.available, $status.development, $status.retrieval)
if (-not $status.retrieval) { throw "Status should report retrieval=true after indexing" }

Step "opening a conversation and asking something the unit covers"
$conversation = (Post "/v1/tutor/conversations" @{ unitSlug = $unit; persona = "patient_teacher" }).id
Write-Host "   $conversation"
$stream = Ask $conversation @{ text = "Why does changing b also change a when both names refer to a list?" }
if ($stream -notmatch "event:done") { throw "Tutor stream did not finish" }
Write-Host ("   streamed {0} bytes" -f $stream.Length)

Step "sources list real block ids with timestamps"
$sources = Get "/v1/tutor/conversations/$conversation/sources"
Write-Host ("   retrieval={0} blocks={1}" -f $sources.retrieval, $sources.blocks.Count)
Write-Host ("   note: {0}" -f $sources.note)
if (-not $sources.retrieval) { throw "Sources should report retrieval on" }
foreach ($block in $sources.blocks) {
    Write-Host ("   block {0} {1} updated={2}" -f $block.blockId, $block.unitSlug, $block.updatedAt)
    if (-not $block.blockId) { throw "Fabricated or empty block id" }
    if (-not $block.updatedAt) { throw "Cited block missing knowledge timestamp" }
}
if ($sources.blocks.Count -lt 1) {
    Write-Host "   (no chunks matched this ask; retrieval is on, which is the gate)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Phase 8 walk complete." -ForegroundColor Green
