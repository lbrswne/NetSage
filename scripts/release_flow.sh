#!/usr/bin/env bash
set -euo pipefail

echo "== NetSage Release Flow =="

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

step() { echo; echo "[STEP] $1"; }

step "1) preflight"
./scripts/preflight.sh

step "2) backend smoke (optional)"
if [[ -x ./backend/smoke_test.sh ]]; then
  ./backend/smoke_test.sh || true
fi

step "3) gradle bundleRelease (manual in Android Studio/CLI)"
echo "Run manually: cd android-app && ./gradlew bundleRelease"

echo

echo "DONE: Follow docs/release/final-pack/STAGE13-FINAL-EXECUTION.md"
