#!/usr/bin/env bash
# AILEAPRO — stop the local stack (macOS / Linux)
# Windows: use stop.bat instead.

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

DEV="$ROOT/infra/scripts/dev.sh"
chmod +x "$DEV" 2>/dev/null || true

echo
echo "== AILEAPRO stop"
echo "   repo: $ROOT"
echo

"$DEV" down
echo
echo "AILEAPRO is down. Start again with ./start.sh"
echo
