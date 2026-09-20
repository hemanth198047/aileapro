#!/usr/bin/env bash
# AILEAPRO — one-command local start (macOS / Linux)
# Layout: frontend/  backend/  infra/
#
# Usage:
#   ./start.sh              start everything
#   ./start.sh down         stop everything
#   ./start.sh restart      stop then start
#   ./start.sh status       show what is running
#   ./start.sh logs         follow API/web logs
#   ./start.sh seed         force re-seed curriculum
#
# Windows: use start.bat / stop.bat instead.

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not on PATH. Install Docker Desktop (or the Docker daemon), then run again." >&2
  exit 1
fi

ACTION="${1:-up}"
DEV="$ROOT/infra/scripts/dev.sh"
chmod +x "$DEV" "$ROOT/backend/gradlew" 2>/dev/null || true

echo
echo "== AILEAPRO $ACTION"
echo "   repo: $ROOT"
echo

case "$ACTION" in
  up|restart)
    exec "$DEV" "$ACTION" --with-skeletons
    ;;
  down|status|logs|seed)
    exec "$DEV" "$@"
    ;;
  *)
    echo "Unknown action: $ACTION" >&2
    echo "Usage: ./start.sh [up|down|restart|status|logs|seed]" >&2
    exit 1
    ;;
esac
