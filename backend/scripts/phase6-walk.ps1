# A walk of the Phase 6 gate against a running API.
#
# It follows one learner through the whole loop: read a unit until the Read chip is solid,
# check that reading enrolled its review cards, grade one, sit a quiz choosing a distractor
# that is built from a named misconception, read the report, and confirm the flag now outranks
# everything else Today could suggest. Then it takes the diagnostic and asks for a plan.
#
# Run with: powershell -ExecutionPolicy Bypass -File scripts/phase6-walk.ps1
$ErrorActionPreference = "Stop"
$api = "http://localhost:8090"
# Python values and types, because its aliasing items are the ones authored with coded
# distractors — the misconception half of the loop has nothing to show on a unit without them.
$unit = "python-values-and-types"
$email = "phase6-$([guid]::NewGuid().ToString('N').Substring(0,8))@aileapro.dev"
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

Step "reading $unit until the Read chip is solid"
Post "/v1/units/$unit/events" @{ kind = "unit_opened" } | Out-Null
# Reading an explanation is part of the floor, not just time on the page: a unit left open
# without an explanation ever being chosen stays PARTIAL however long the tab sits there.
$mastery = Post "/v1/units/$unit/events" @{ kind = "explanation_level"; level = "standard" }
# The server clamps each beat, so this is the same number of beats a real reader would send.
$beats = 0
while ($mastery.evidence.read.state -ne "solid") {
    $mastery = Post "/v1/units/$unit/events" @{ kind = "dwell"; seconds = 60 }
    if (++$beats -gt 40) { throw "Read never went solid after $beats beats" }
}
Write-Host ("   state={0} read={1} dwell={2}s of {3}s" -f `
    $mastery.state, $mastery.evidence.read.state, $mastery.dwellSeconds, $mastery.readFloorSeconds)

Step "reading enrolled the unit's review cards"
$queue = Get "/v1/reviews/due"
Write-Host ("   due={0} overdue={1} leeches={2}" -f $queue.dueCount, $queue.overdueCount, $queue.leechCount)
if ($queue.dueCount -eq 0) { throw "Reading went solid but no cards were enrolled" }

Step "grading one card"
$card = $queue.cards[0]
Write-Host ("   front: {0}" -f $card.front)
$graded = Post "/v1/reviews/$($card.scheduleId)" @{ grade = 4 }
Write-Host ("   back: {0}" -f $graded.back)
Write-Host ("   {0}d -> {1}d, next {2}, ease {3}, counted={4}" -f `
    $graded.intervalBefore, $graded.intervalAfter, $graded.nextDue, $graded.ease, $graded.wasDue)

Step "sitting the quiz, choosing distractors built from misconceptions"
$offer = Get "/v1/units/$unit/assessments"
Write-Host ("   practice={0} quiz={1} ceiling={2}" -f $offer.practice.slug, $offer.quiz.slug, $offer.ceiling)
$attempt = Post "/v1/assessments/$($offer.quiz.slug)/attempts"
while ($attempt.nextItem) {
    $item = $attempt.nextItem
    $body = if ($item.choices.Count -gt 0) {
        # The second choice, deliberately. Authors write the tempting wrong answer first among
        # the distractors, so this is the path that raises a flag rather than merely losing a
        # mark — the point of the walk is to see the flag, the report, and Today react to it.
        @{ selectedIds = @($item.choices[1].id); elapsedSeconds = 20 }
    } else {
        @{ selectedIds = @(); text = "no idea"; elapsedSeconds = 20 }
    }
    $marked = Post "/v1/attempts/$($attempt.id)/items/$($item.id)" $body
    Write-Host ("   q{0} correct={1} unverified={2} raised={3}" -f `
        $item.sort, $marked.correct, $marked.unverified, ($marked.raised.code -join ","))
    $attempt = Get "/v1/attempts/$($attempt.id)"
}

Step "the report"
$report = Post "/v1/attempts/$($attempt.id)/submit"
Write-Host ("   {0}" -f $report.summary)
Write-Host ("   readiness={0}: {1}" -f $report.readiness, $report.readinessReason)
Write-Host ("   state={0}: {1}" -f $report.masteryState, $report.masteryNote)
Write-Host ("   sections: strengths={0} weaknesses={1} misconceptions={2} prereqs={3} revision={4} next={5}" -f `
    $report.strengths.Count, $report.weaknesses.Count, $report.misconceptions.Count, `
    $report.missingPrereqs.Count, $report.revision.Count, $report.nextSteps.Count)
$report.nextSteps | ForEach-Object { Write-Host ("   next: {0} - {1}" -f $_.label, $_.detail) }

Step "what is held against the learner"
$open = Get "/v1/misconceptions"
if (-not $open) { throw "The quiz was answered with coded distractors and nothing was flagged" }
$open | ForEach-Object { Write-Host ("   {0}: {1}" -f $_.code, $_.claim) }

Step "what Today recommends now"
Invoke-RestMethod -Method Put -Uri "$api/v1/me/profile" -WebSession $session -Headers (Csrf) -Body (@{
    targetPath = "ai-engineer-abridged"; hoursPerWeek = 6; modalities = @("visual"); experience = "some_programming"
} | ConvertTo-Json -Compress) | Out-Null
$today = Get "/v1/me/today"
Write-Host ("   rule={0} -> {1} {2} ({3})" -f $today.next.rule, $today.next.verb, $today.next.object, $today.next.href)
Write-Host ("   because: {0}" -f $today.next.reason)

Step "the diagnostic"
$diag = Post "/v1/diagnostic"
$served = 0
while ($diag.nextItem) {
    $item = $diag.nextItem
    $body = if ($item.choices.Count -gt 0) {
        @{ selectedIds = @($item.choices[0].id); elapsedSeconds = 15 }
    } else {
        @{ selectedIds = @(); text = "2"; elapsedSeconds = 15 }
    }
    Post "/v1/attempts/$($diag.id)/items/$($item.id)" $body | Out-Null
    $served++
    $diag = Get "/v1/attempts/$($diag.id)"
}
Write-Host ("   it stopped after {0} items" -f $served)
$diagReport = Post "/v1/attempts/$($diag.id)/submit"
Write-Host ("   {0}" -f $diagReport.summary)
Write-Host ("   readiness={0} state={1}" -f $diagReport.readiness, $diagReport.masteryState)

Step "the plan it produced"
$plan = Post "/v1/attempts/$($diag.id)/plan"
Write-Host ("   v{0} ({1}): {2}" -f $plan.version, $plan.source, $plan.reason)
$plan.priors | ForEach-Object {
    Write-Host ("   prior {0}: {1}/{2} estimate={3} confidence={4}" -f `
        $_.domain.slug, $_.itemsCorrect, $_.itemsServed, $_.estimate, $_.confidence)
}
$plan.items | ForEach-Object { Write-Host ("   {0}: {1} - {2}" -f $_.kind, $_.unit.slug, $_.reason) }

Write-Host ""
Write-Host "Walk complete." -ForegroundColor Green
