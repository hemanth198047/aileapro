<#
.SYNOPSIS
One command for the whole local stack: containers, API, web.

.DESCRIPTION
Brings up everything the platform needs to run locally, in dependency order, and
waits for each part to actually answer before starting the next one. A part that
failed is reported as failed with the tail of its own log, rather than leaving a
half-started stack that looks fine until the first request.

Logs and pids live under .run/, so a service started in the background can be
found again by a later run of this script.

.PARAMETER Action
  up        start containers, API, and web (default)
  down      stop web, API, and containers
  restart   down, then up
  status    say what is running, and where
  logs      follow the API and web logs
  seed      load the authored curriculum, assessment, and AI seed data

.PARAMETER Clean
Start from nothing: drops the database volume and the Next.js cache, then seeds.
Destroys local learner data, which is the point.

.PARAMETER SkipSeed
Do not check whether seed data exists. Fast, and wrong on an empty database.

.PARAMETER WithSkeletons
Also start the worker and executor. Both are health-only skeletons until
Phase 9, so they are off by default.

.EXAMPLE
  .\infra\scripts\dev.ps1
  .\infra\scripts\dev.ps1 status
  .\infra\scripts\dev.ps1 up -Clean
  .\infra\scripts\dev.ps1 down
  # Or from repo root: start.bat / stop.bat
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('up', 'down', 'restart', 'status', 'logs', 'seed')]
    [string]$Action = 'up',

    [switch]$Clean,
    [switch]$SkipSeed,
    [switch]$WithSkeletons
)

# Native tools write progress to stderr; treating that as terminating would abort
# the script on a healthy `docker compose up`. Exit codes are checked instead.
$ErrorActionPreference = 'Continue'

$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Compose = Join-Path $Root 'infra\compose\docker-compose.yml'
$RunDir = Join-Path $Root '.run'
$LogDir = Join-Path $RunDir 'logs'
$Gradlew = Join-Path $Root 'backend\gradlew.bat'
$Frontend = Join-Path $Root 'frontend'
$WebApp = Join-Path $Frontend 'web'

$ApiPort = if ($env:AILEAPRO_API_PORT) { [int]$env:AILEAPRO_API_PORT } else { 8090 }
$WebPort = if ($env:PORT) { [int]$env:PORT } else { 3000 }
$WorkerPort = if ($env:AILEAPRO_WORKER_PORT) { [int]$env:AILEAPRO_WORKER_PORT } else { 8081 }
$ExecutorPort = if ($env:AILEAPRO_EXECUTOR_PORT) { [int]$env:AILEAPRO_EXECUTOR_PORT } else { 8082 }

$Containers = @(
    @{ Name = 'postgres'; Port = 5432; Purpose = 'curriculum, learners, attempts, AI usage' }
    @{ Name = 'redis'; Port = 6379; Purpose = 'sessions and queues' }
    @{ Name = 'minio'; Port = 9000; Purpose = 'object storage, S3 API' }
)

function Step($text) { Write-Host "`n== $text" -ForegroundColor Cyan }
function Note($text) { Write-Host "   $text" -ForegroundColor DarkGray }
function Good($text) { Write-Host "   $text" -ForegroundColor Green }
function Warn($text) { Write-Host "   $text" -ForegroundColor Yellow }
function Bad($text) { Write-Host "   $text" -ForegroundColor Red }

function Fail($text) {
    Bad $text
    exit 1
}

function Ensure-Dirs {
    foreach ($dir in @($RunDir, $LogDir)) {
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    }
}

function Tool($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Preflight {
    Step 'checking the tools this needs'
    $missing = @()
    if (-not (Tool 'docker')) { $missing += 'docker (Docker Desktop, running)' }
    if (-not (Tool 'pnpm')) { $missing += 'pnpm (npm i -g pnpm@10)' }
    if (-not (Tool 'node')) { $missing += 'node 20 or newer' }
    if (-not (Test-Path $Gradlew)) { $missing += 'backend/gradlew.bat' }

    if ($missing.Count -gt 0) {
        Bad 'missing:'
        $missing | ForEach-Object { Bad "  - $_" }
        Fail 'install the above, then run this again'
    }

    & docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Fail 'Docker is installed but not responding. Start Docker Desktop and run this again.' }

    Good "docker ok, pnpm ok, node $((& node --version).Trim())"
}

function Compose-Cmd {
    param([string[]]$ComposeArgs)
    & docker compose -f $Compose @ComposeArgs 2>&1 | Out-Null
}

function Container-Health($name) {
    $id = (& docker compose -f $Compose ps -q $name 2>$null | Select-Object -First 1)
    if (-not $id) { return 'absent' }
    $state = (& docker inspect -f '{{.State.Status}}' $id 2>$null)
    if ($state -ne 'running') { return "$state" }
    $health = (& docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $id 2>$null)
    if ($health -eq 'none') { return 'running' }
    return "$health"
}

function Wait-Container($name, $timeoutSeconds = 150) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $health = Container-Health $name
        if ($health -in @('healthy', 'running')) { return $true }
        if ($health -in @('exited', 'dead')) { return $false }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Url-Ok($url, $timeoutSeconds = 5) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec $timeoutSeconds
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
    } catch {
        # An HTTP error still proves something is listening and routing there.
        $status = $_.Exception.Response.StatusCode.value__
        return ($null -ne $status) -and ($status -lt 500)
    }
}

function Wait-Url($url, $timeoutSeconds, $label, $logPath) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Url-Ok $url 3) { return $true }
        Start-Sleep -Seconds 2
    }
    Bad "$label never answered on $url within $timeoutSeconds seconds"
    if ($logPath -and (Test-Path $logPath)) {
        Bad "last lines of $logPath"
        Get-Content $logPath -Tail 25 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkRed }
    }
    return $false
}

function Pid-On-Port($port) {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) { return [int]$conn.OwningProcess }
    return $null
}

function Stop-Tree($processId) {
    if (-not $processId) { return }
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $processId" -ErrorAction SilentlyContinue
    foreach ($child in $children) { Stop-Tree ([int]$child.ProcessId) }
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

# Gradle forks the application JVM out of its daemon rather than out of the
# launcher this script started, so the recorded pid and the port owner are two
# different processes and both have to go.
function Stop-Named($label, $port) {
    $stopped = $false

    $pidFile = Join-Path $RunDir "$label.pid"
    if (Test-Path $pidFile) {
        $recorded = (Get-Content $pidFile | Select-Object -First 1)
        if ($recorded) { Stop-Tree ([int]$recorded); $stopped = $true }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }

    $listening = Pid-On-Port $port
    if ($listening) { Stop-Tree $listening; $stopped = $true }

    if ($stopped) { Good "$label stopped" } else { Note "$label was not running" }
    return $stopped
}

function Start-Background {
    param(
        [string]$Label,
        [string]$CommandLine,
        [string]$WorkingDirectory
    )

    $log = Join-Path $LogDir "$Label.log"
    $err = Join-Path $LogDir "$Label.err.log"
    foreach ($path in @($log, $err)) { Remove-Item $path -Force -ErrorAction SilentlyContinue }

    # Launched through the shell so batch wrappers (gradlew.bat, pnpm.cmd) behave
    # the same here as they do when a person types them.
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/c', $CommandLine) `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $log -RedirectStandardError $err `
        -WindowStyle Hidden -PassThru

    Set-Content -Path (Join-Path $RunDir "$Label.pid") -Value $process.Id
    return @{ Process = $process; Log = $log }
}

function Row-Count($table) {
    $id = (& docker compose -f $Compose ps -q postgres 2>$null | Select-Object -First 1)
    if (-not $id) { return -1 }
    $out = (& docker exec $id psql -U aileapro -d aileapro -t -A -c "select count(*) from $table" 2>$null)
    if ($LASTEXITCODE -ne 0 -or -not $out) { return -1 }
    return [int]($out | Select-Object -First 1)
}

function Start-Containers {
    Step 'starting postgres, redis, and minio'

    if ($Clean) {
        Warn 'clean start: removing container volumes, including the database'
        Compose-Cmd @('down', '-v')
    }

    Compose-Cmd @('up', '-d')

    foreach ($container in $Containers) {
        if (Wait-Container $container.Name) {
            Good "$($container.Name) healthy on $($container.Port), $($container.Purpose)"
        } else {
            Bad "$($container.Name) did not become healthy"
            & docker compose -f $Compose logs --tail 20 $container.Name 2>&1 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkRed }
            Fail "fix $($container.Name) before starting the API"
        }
    }
}

function Seed-Data {
    param([switch]$Force)

    $units = Row-Count 'learning_units'
    $chunks = Row-Count 'content_chunks'
    $projects = Row-Count 'projects'
    $papersWithDoi = 0
    try {
        $papersWithDoi = [int](docker compose -f $Compose exec -T postgres psql -U aileapro -d aileapro -tAc "select count(*) from sources where source_type='PAPER' and doi is not null;" 2>$null)
    } catch {
        $papersWithDoi = 0
    }
    if (-not $Force -and $units -gt 0 -and $chunks -gt 0 -and $projects -gt 0 -and $papersWithDoi -ge 3) {
        Note "curriculum already loaded, $units units, $chunks chunks, $projects projects, $papersWithDoi papers; skipping the seed"
        return
    }
    if (-not $Force -and $units -gt 0 -and ($chunks -le 0 -or $projects -le 0 -or $papersWithDoi -lt 3)) {
        Note "curriculum is loaded but search index, projects, or research papers need a refresh; reseeding"
    }

    Step 'loading seed data: curriculum, assessments, AI catalogue, projects, prompts'
    Note 'this applies pending Flyway migrations on the way through'
    $seedLog = Join-Path $LogDir 'seed.log'
    $backend = Join-Path $Root 'backend'
    Push-Location $backend
    try {
        & $Gradlew ':api:seed' '--console=plain' 2>&1 | Out-File -FilePath $seedLog -Encoding utf8
        $code = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($code -ne 0) {
        Bad 'seeding failed, last lines:'
        Get-Content $seedLog -Tail 30 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkRed }
        Fail 'the API would otherwise start against an unseeded database'
    }
    Good "seeded: $(Row-Count 'learning_units') units, $(Row-Count 'question_bank') questions, $(Row-Count 'projects') projects, $(Row-Count 'prompt_versions') prompt versions, $(Row-Count 'content_chunks') chunks"
}

function Start-Api {
    Step "starting the API on $ApiPort"

    if (Url-Ok "http://localhost:$ApiPort/actuator/health") {
        Good 'already running and healthy'
        return
    }

    Stop-Named 'api' $ApiPort | Out-Null
    $started = Start-Background -Label 'api' -CommandLine "`"$Gradlew`" :api:bootRun --console=plain" `
        -WorkingDirectory (Join-Path $Root 'backend')
    Note "pid $($started.Process.Id), log $($started.Log)"

    if (-not (Wait-Url "http://localhost:$ApiPort/actuator/health" 300 'the API' $started.Log)) {
        Fail 'the API did not come up'
    }
    Good "healthy: http://localhost:$ApiPort/actuator/health"
    Note "API docs: http://localhost:$ApiPort/swagger-ui.html"
}

function Start-Skeletons {
    Step 'starting the worker and executor'
    foreach ($service in @(
            @{ Label = 'worker'; Task = ':worker:bootRun'; Port = $WorkerPort },
            @{ Label = 'executor'; Task = ':executor:bootRun'; Port = $ExecutorPort })) {

        if (Url-Ok "http://localhost:$($service.Port)/actuator/health") {
            Good "$($service.Label) already running"
            continue
        }
        Stop-Named $service.Label $service.Port | Out-Null
        $started = Start-Background -Label $service.Label -CommandLine "`"$Gradlew`" $($service.Task) --console=plain" `
            -WorkingDirectory (Join-Path $Root 'backend')
        if (Wait-Url "http://localhost:$($service.Port)/actuator/health" 240 $service.Label $started.Log) {
            Good "$($service.Label) healthy on $($service.Port), health only until Phase 9"
        }
    }
}

function Start-Web {
    Step "starting the web app on $WebPort"

    if (-not (Test-Path (Join-Path $Frontend 'node_modules'))) {
        Note 'installing frontend workspace dependencies'
        Push-Location $Frontend
        try {
            & pnpm install --frozen-lockfile 2>&1 | Out-File -FilePath (Join-Path $LogDir 'pnpm-install.log') -Encoding utf8
            if ($LASTEXITCODE -ne 0) { Fail "pnpm install failed, see $LogDir\pnpm-install.log" }
        } finally {
            Pop-Location
        }
    }

    $next = Join-Path $WebApp '.next'

    # A production build leaves a BUILD_ID in .next and deletes the chunk files the
    # dev server still expects, after which every route answers 500. Clearing it is
    # the difference between "the app is broken" and a restart.
    $clobbered = Test-Path (Join-Path $next 'BUILD_ID')
    if ($Clean -or $clobbered) {
        Note 'clearing .next, a production build had written into the dev cache'
        Remove-Item -Recurse -Force $next -ErrorAction SilentlyContinue
    } elseif (Url-Ok "http://localhost:$WebPort/login") {
        Good 'already running'
        return
    }

    Stop-Named 'web' $WebPort | Out-Null
    $started = Start-Background -Label 'web' -CommandLine 'pnpm dev' -WorkingDirectory $WebApp
    Note "pid $($started.Process.Id), log $($started.Log)"

    if (-not (Wait-Url "http://localhost:$WebPort/login" 240 'the web app' $started.Log)) {
        Fail 'the web app did not come up'
    }
    Good "ready: http://localhost:$WebPort"
}

function Show-Status {
    Step 'status'

    foreach ($container in $Containers) {
        $health = Container-Health $container.Name
        $line = '{0,-9} {1,-9} localhost:{2}' -f $container.Name, $health, $container.Port
        if ($health -in @('healthy', 'running')) { Good $line } else { Bad $line }
    }

    $services = @(
        @{ Label = 'api'; Url = "http://localhost:$ApiPort/actuator/health"; Port = $ApiPort }
        @{ Label = 'web'; Url = "http://localhost:$WebPort/login"; Port = $WebPort }
        @{ Label = 'worker'; Url = "http://localhost:$WorkerPort/actuator/health"; Port = $WorkerPort }
        @{ Label = 'executor'; Url = "http://localhost:$ExecutorPort/actuator/health"; Port = $ExecutorPort }
    )

    foreach ($service in $services) {
        $up = Url-Ok $service.Url
        $owner = Pid-On-Port $service.Port
        $where = if ($owner) { "localhost:$($service.Port), pid $owner" } else { "localhost:$($service.Port)" }
        $line = '{0,-9} {1,-9} {2}' -f $service.Label, $(if ($up) { 'up' } else { 'down' }), $where
        if ($up) { Good $line }
        elseif ($service.Label -in @('worker', 'executor')) { Note $line }
        else { Bad $line }
    }

    $units = Row-Count 'learning_units'
    if ($units -ge 0) { Note "database: $units units, $(Row-Count 'attempts') attempts, $(Row-Count 'ai_usage') AI calls logged" }
    Note "logs: $LogDir"
}

function Stop-Everything {
    Step 'stopping the web app, the API, and the skeletons'
    Stop-Named 'web' $WebPort | Out-Null
    Stop-Named 'api' $ApiPort | Out-Null
    Stop-Named 'worker' $WorkerPort | Out-Null
    Stop-Named 'executor' $ExecutorPort | Out-Null

    Step 'stopping containers'
    if ($Clean) {
        Warn 'clean stop: removing volumes, including the database'
        Compose-Cmd @('down', '-v')
    } else {
        Compose-Cmd @('down')
        Note 'volumes kept, so the database survives'
    }
    Good 'everything stopped'
}

function Bring-Up {
    Preflight
    Start-Containers
    if (-not $SkipSeed) { Seed-Data }
    Start-Api
    if ($WithSkeletons) { Start-Skeletons }
    Start-Web
    Show-Status
    Write-Host "`nAILEAPRO is up. Open http://localhost:$WebPort and register to start." -ForegroundColor Green
    Write-Host "Follow logs with: .\infra\scripts\dev.ps1 logs  (or start.bat logs)" -ForegroundColor DarkGray
    Write-Host "Stop it with:     stop.bat`n" -ForegroundColor DarkGray
}

function Follow-Logs {
    $paths = @('api', 'web') | ForEach-Object { Join-Path $LogDir "$_.log" } | Where-Object { Test-Path $_ }
    if ($paths.Count -eq 0) { Fail 'no logs yet; start the stack first with start.bat or .\infra\scripts\dev.ps1' }
    Note 'following; Ctrl+C to stop'
    Get-Content -Path $paths -Tail 40 -Wait
}

Ensure-Dirs

switch ($Action) {
    'up' { Bring-Up }
    'down' { Stop-Everything }
    'restart' {
        Stop-Everything
        Bring-Up
    }
    'status' { Show-Status }
    'logs' { Follow-Logs }
    'seed' {
        Preflight
        Start-Containers
        Seed-Data -Force
    }
}
