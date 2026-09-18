# Phase 11 gate walk: seeded papers only; explainer refuses inventing refs.
$ErrorActionPreference = "Stop"
$api = if ($env:AILEAPRO_API_ORIGIN) { $env:AILEAPRO_API_ORIGIN } else { "http://localhost:8090" }
$email = "phase11-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

Step "reading list is seeded papers only"
$list = Get "/v1/research/papers"
Write-Host ("   papers={0}" -f $list.papers.Count)
if ($list.papers.Count -lt 3) { throw "Expected at least 3 seeded papers" }
$slugs = @($list.papers | ForEach-Object { $_.slug })
if ($slugs -notcontains "attention-2017") { throw "Missing attention-2017" }
if ($slugs -notcontains "rag-2020") { throw "Missing rag-2020" }

Step "explainer cites only the attached seeded paper"
$explained = Post "/v1/research/explain" @{
    question = "What is retrieval-augmented generation?"
    paperSlug = "rag-2020"
}
Write-Host ("   refused={0} citations={1}" -f $explained.refused, $explained.citations.Count)
if ($explained.refused) { throw "Explainer refused a seeded paper" }
if ($explained.citations[0].slug -ne "rag-2020") { throw "Citation was not rag-2020" }
if ($explained.answer -match "Deep Learning Will Solve Everything") { throw "Invented title in answer" }

Step "unmatched question refuses instead of inventing"
$refused = Post "/v1/research/explain" @{ question = "zzzz unrelated underwater basket weaving" }
Write-Host ("   refused={0} code={1}" -f $refused.refused, $refused.refusalCode)
if (-not $refused.refused) { throw "Expected refusal with no sources" }
if ($refused.refusalCode -ne "no_sources") { throw "Expected no_sources" }
if ($refused.citations.Count -ne 0) { throw "Refusal must carry no citations" }

Step "literature map lists paper nodes"
$map = Get "/v1/research/map"
Write-Host ("   nodes={0} edges={1}" -f $map.nodes.Count, $map.edges.Count)
$paperNodes = @($map.nodes | Where-Object { $_.kind -eq "paper" })
if ($paperNodes.Count -lt 3) { throw "Map missing paper nodes" }

Write-Host ""
Write-Host "Phase 11 walk complete." -ForegroundColor Green
