#!/usr/bin/env bash
set -euo pipefail

echo "== NetSage Preflight =="

echo "[1] check project files"
required=(
  "android-app/app/build.gradle.kts"
  "android-app/settings.gradle.kts"
  "backend/main.py"
  "docs/release/PRELAUNCH-CHECKLIST.md"
)
for f in "${required[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "missing: $f"; exit 1
  fi
done
echo "ok"

echo "[2] backend smoke (optional)"
if command -v curl >/dev/null 2>&1; then
  echo "curl found"
else
  echo "curl not found, skip"
fi

echo "[3] git status"
git status --short

echo "Done: preflight finished"
