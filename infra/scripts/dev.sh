#!/usr/bin/env bash
# One command for the whole local stack on macOS / Linux: containers, API, web.
#
# Usage:
#   ./infra/scripts/dev.sh              # up (default)
#   ./infra/scripts/dev.sh up
#   ./infra/scripts/dev.sh down
#   ./infra/scripts/dev.sh restart
#   ./infra/scripts/dev.sh status
#   ./infra/scripts/dev.sh logs
#   ./infra/scripts/dev.sh seed
#   ./infra/scripts/dev.sh up --clean
#   ./infra/scripts/dev.sh up --skip-seed
#   ./infra/scripts/dev.sh up --with-skeletons
# Prefer ./start.sh / ./stop.sh from the repo root.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="$ROOT/infra/compose/docker-compose.yml"
RUN_DIR="$ROOT/.run"
LOG_DIR="$RUN_DIR/logs"
GRADLEW="$ROOT/backend/gradlew"
FRONTEND="$ROOT/frontend"
WEB_APP="$FRONTEND/web"

API_PORT="${AILEAPRO_API_PORT:-8090}"
WEB_PORT="${PORT:-3000}"
WORKER_PORT="${AILEAPRO_WORKER_PORT:-8081}"
EXECUTOR_PORT="${AILEAPRO_EXECUTOR_PORT:-8082}"

ACTION="${1:-up}"
shift || true

CLEAN=0
SKIP_SEED=0
WITH_SKELETONS=0
for arg in "$@"; do
  case "$arg" in
    --clean|-Clean) CLEAN=1 ;;
    --skip-seed|-SkipSeed) SKIP_SEED=1 ;;
    --with-skeletons|-WithSkeletons) WITH_SKELETONS=1 ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

step() { printf '\n== %s\n' "$1"; }
note() { printf '   %s\n' "$1"; }
good() { printf '   %s\n' "$1"; }
warn() { printf '   %s\n' "$1"; }
bad()  { printf '   %s\n' "$1" >&2; }
fail() { bad "$1"; exit 1; }

ensure_dirs() {
  mkdir -p "$RUN_DIR" "$LOG_DIR"
}

preflight() {
  step "checking the tools this needs"
  local missing=()
  command -v docker >/dev/null || missing+=("docker")
  command -v pnpm >/dev/null || missing+=("pnpm (npm i -g pnpm@10)")
  command -v node >/dev/null || missing+=("node 20 or newer")
  [[ -x "$GRADLEW" || -f "$GRADLEW" ]] || missing+=("backend/gradlew")
  if ((${#missing[@]})); then
    bad "missing:"
    for m in "${missing[@]}"; do bad "  - $m"; done
    fail "install the above, then run this again"
  fi
  chmod +x "$GRADLEW" 2>/dev/null || true
  if ! docker info >/dev/null 2>&1; then
    fail "Docker is installed but not responding. Start Docker Desktop / the daemon and run again."
  fi
  good "docker ok, pnpm ok, node $(node --version | tr -d '\r')"
}

compose() {
  docker compose -f "$COMPOSE" "$@"
}

container_health() {
  local name="$1" id state health
  id="$(compose ps -q "$name" 2>/dev/null | head -n1 || true)"
  [[ -n "$id" ]] || { echo absent; return; }
  state="$(docker inspect -f '{{.State.Status}}' "$id" 2>/dev/null || echo missing)"
  [[ "$state" == running ]] || { echo "$state"; return; }
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$id" 2>/dev/null || echo none)"
  if [[ "$health" == none ]]; then echo running; else echo "$health"; fi
}

wait_container() {
  local name="$1" timeout="${2:-150}" deadline
  deadline=$((SECONDS + timeout))
  while (( SECONDS < deadline )); do
    local health
    health="$(container_health "$name")"
    case "$health" in
      healthy|running) return 0 ;;
      exited|dead) return 1 ;;
    esac
    sleep 2
  done
  return 1
}

url_ok() {
  local url="$1"
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 5 "$url" 2>/dev/null || echo 000)"
  [[ "$code" =~ ^[1234][0-9][0-9]$ ]]
}

wait_url() {
  local url="$1" timeout="$2" label="$3" log_path="${4:-}"
  local deadline=$((SECONDS + timeout))
  while (( SECONDS < deadline )); do
    if url_ok "$url"; then return 0; fi
    sleep 2
  done
  bad "$label never answered on $url within ${timeout}s"
  if [[ -n "$log_path" && -f "$log_path" ]]; then
    bad "last lines of $log_path"
    tail -n 25 "$log_path" | sed 's/^/      /' >&2
  fi
  return 1
}

pid_on_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -n1 || true
  elif command -v ss >/dev/null 2>&1; then
    ss -lptn "sport = :$port" 2>/dev/null | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -n1 || true
  else
    true
  fi
}

stop_tree() {
  local pid="$1"
  [[ -n "$pid" ]] || return 0
  local kids
  kids="$(pgrep -P "$pid" 2>/dev/null || true)"
  for kid in $kids; do stop_tree "$kid"; done
  kill -TERM "$pid" 2>/dev/null || true
  sleep 0.5
  kill -KILL "$pid" 2>/dev/null || true
}

stop_named() {
  local label="$1" port="$2"
  local stopped=0 pid_file="$RUN_DIR/$label.pid" recorded listening
  if [[ -f "$pid_file" ]]; then
    recorded="$(head -n1 "$pid_file" || true)"
    if [[ -n "$recorded" ]]; then stop_tree "$recorded"; stopped=1; fi
    rm -f "$pid_file"
  fi
  listening="$(pid_on_port "$port")"
  if [[ -n "$listening" ]]; then stop_tree "$listening"; stopped=1; fi
  if (( stopped )); then good "$label stopped"; else note "$label was not running"; fi
}

start_background() {
  local label="$1" workdir="$2"
  shift 2
  local log="$LOG_DIR/$label.log" err="$LOG_DIR/$label.err.log"
  rm -f "$log" "$err"
  (
    cd "$workdir"
    nohup "$@" >>"$log" 2>>"$err" &
    echo $! >"$RUN_DIR/$label.pid"
  )
  note "pid $(cat "$RUN_DIR/$label.pid"), log $log"
}

row_count() {
  local table="$1" out
  out="$(compose exec -T postgres psql -U aileapro -d aileapro -tAc "select count(*) from $table" 2>/dev/null || true)"
  out="$(echo "$out" | tr -d '[:space:]')"
  if [[ "$out" =~ ^[0-9]+$ ]]; then echo "$out"; else echo -1; fi
}

start_containers() {
  step "starting postgres, redis, and minio"
  if (( CLEAN )); then
    warn "clean start: removing container volumes, including the database"
    compose down -v >/dev/null
  fi
  compose up -d >/dev/null
  local name
  for name in postgres redis minio; do
    if wait_container "$name"; then
      good "$name healthy"
    else
      bad "$name did not become healthy"
      compose logs --tail 20 "$name" | sed 's/^/      /' >&2
      fail "fix $name before starting the API"
    fi
  done
}

seed_data() {
  local force="${1:-0}"
  local units chunks projects papers
  units="$(row_count learning_units)"
  chunks="$(row_count content_chunks)"
  projects="$(row_count projects)"
  papers="$(compose exec -T postgres psql -U aileapro -d aileapro -tAc "select count(*) from sources where source_type='PAPER' and doi is not null;" 2>/dev/null | tr -d '[:space:]' || echo 0)"
  if (( ! force )) && (( units > 0 )) && (( chunks > 0 )) && (( projects > 0 )) && (( papers >= 3 )); then
    note "curriculum already loaded, $units units, $chunks chunks, $projects projects, $papers papers; skipping the seed"
    return 0
  fi
  step "loading seed data: curriculum, assessments, AI catalogue, projects, prompts"
  note "this applies pending Flyway migrations on the way through"
  local seed_log="$LOG_DIR/seed.log"
  (
    cd "$ROOT/backend"
    ./gradlew :api:seed --console=plain
  ) >"$seed_log" 2>&1 || {
    bad "seeding failed, last lines:"
    tail -n 30 "$seed_log" | sed 's/^/      /' >&2
    fail "the API would otherwise start against an unseeded database"
  }
  good "seeded: $(row_count learning_units) units, $(row_count question_bank) questions, $(row_count projects) projects, $(row_count content_chunks) chunks"
}

start_api() {
  step "starting the API on $API_PORT"
  if url_ok "http://localhost:$API_PORT/actuator/health"; then
    good "already running and healthy"
    return 0
  fi
  stop_named api "$API_PORT" >/dev/null || true
  start_background api "$ROOT/backend" ./gradlew :api:bootRun --console=plain
  wait_url "http://localhost:$API_PORT/actuator/health" 300 "the API" "$LOG_DIR/api.log" \
    || fail "the API did not come up"
  good "healthy: http://localhost:$API_PORT/actuator/health"
  note "API docs: http://localhost:$API_PORT/swagger-ui.html"
}

start_skeletons() {
  step "starting the worker and executor"
  local label task port
  for entry in "worker|:worker:bootRun|$WORKER_PORT" "executor|:executor:bootRun|$EXECUTOR_PORT"; do
    IFS='|' read -r label task port <<<"$entry"
    if url_ok "http://localhost:$port/actuator/health"; then
      good "$label already running"
      continue
    fi
    stop_named "$label" "$port" >/dev/null || true
    start_background "$label" "$ROOT/backend" ./gradlew "$task" --console=plain
    if wait_url "http://localhost:$port/actuator/health" 240 "$label" "$LOG_DIR/$label.log"; then
      good "$label healthy on $port"
    fi
  done
}

start_web() {
  step "starting the web app on $WEB_PORT"
  if [[ ! -d "$FRONTEND/node_modules" ]]; then
    note "installing frontend workspace dependencies"
    (cd "$FRONTEND" && pnpm install --frozen-lockfile) >"$LOG_DIR/pnpm-install.log" 2>&1 \
      || fail "pnpm install failed, see $LOG_DIR/pnpm-install.log"
  fi
  local next="$WEB_APP/.next"
  if (( CLEAN )) || [[ -f "$next/BUILD_ID" ]]; then
    note "clearing .next"
    rm -rf "$next"
  elif url_ok "http://localhost:$WEB_PORT/login"; then
    good "already running"
    return 0
  fi
  stop_named web "$WEB_PORT" >/dev/null || true
  start_background web "$WEB_APP" pnpm dev
  wait_url "http://localhost:$WEB_PORT/login" 240 "the web app" "$LOG_DIR/web.log" \
    || fail "the web app did not come up"
  good "ready: http://localhost:$WEB_PORT"
}

show_status() {
  step "status"
  local name health
  for name in postgres redis minio; do
    health="$(container_health "$name")"
    if [[ "$health" == healthy || "$health" == running ]]; then good "$name  $health"; else bad "$name  $health"; fi
  done
  local label url port up owner
  for entry in "api|http://localhost:$API_PORT/actuator/health|$API_PORT" \
               "web|http://localhost:$WEB_PORT/login|$WEB_PORT" \
               "worker|http://localhost:$WORKER_PORT/actuator/health|$WORKER_PORT" \
               "executor|http://localhost:$EXECUTOR_PORT/actuator/health|$EXECUTOR_PORT"; do
    IFS='|' read -r label url port <<<"$entry"
    if url_ok "$url"; then up=up; else up=down; fi
    owner="$(pid_on_port "$port")"
    if [[ "$up" == up ]]; then
      good "$label  up  localhost:$port${owner:+, pid $owner}"
    elif [[ "$label" == worker || "$label" == executor ]]; then
      note "$label  down  localhost:$port"
    else
      bad "$label  down  localhost:$port"
    fi
  done
  local units
  units="$(row_count learning_units)"
  if (( units >= 0 )); then note "database: $units units"; fi
  note "logs: $LOG_DIR"
}

stop_everything() {
  step "stopping the web app, the API, and the skeletons"
  stop_named web "$WEB_PORT" || true
  stop_named api "$API_PORT" || true
  stop_named worker "$WORKER_PORT" || true
  stop_named executor "$EXECUTOR_PORT" || true
  step "stopping containers"
  if (( CLEAN )); then
    warn "clean stop: removing volumes, including the database"
    compose down -v >/dev/null
  else
    compose down >/dev/null
    note "volumes kept, so the database survives"
  fi
  good "everything stopped"
}

bring_up() {
  preflight
  start_containers
  if (( ! SKIP_SEED )); then seed_data 0; fi
  start_api
  if (( WITH_SKELETONS )); then start_skeletons; fi
  start_web
  show_status
  printf '\nAILEAPRO is up. Open http://localhost:%s and register to start.\n' "$WEB_PORT"
  printf 'Follow logs with: ./infra/scripts/dev.sh logs  (or ./start.sh logs)\n'
  printf 'Stop it with:     ./stop.sh\n\n'
}

follow_logs() {
  local paths=()
  [[ -f "$LOG_DIR/api.log" ]] && paths+=("$LOG_DIR/api.log")
  [[ -f "$LOG_DIR/web.log" ]] && paths+=("$LOG_DIR/web.log")
  ((${#paths[@]})) || fail "no logs yet; start the stack first with ./start.sh"
  note "following; Ctrl+C to stop"
  tail -n 40 -F "${paths[@]}"
}

ensure_dirs

case "$ACTION" in
  up) bring_up ;;
  down) stop_everything ;;
  restart) stop_everything; bring_up ;;
  status) show_status ;;
  logs) follow_logs ;;
  seed) preflight; start_containers; seed_data 1 ;;
  *)
    echo "Usage: $0 {up|down|restart|status|logs|seed} [--clean] [--skip-seed] [--with-skeletons]" >&2
    exit 1
    ;;
esac
