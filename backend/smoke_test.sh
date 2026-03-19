#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8000}"

echo "[1/2] health check"
curl -sS "$BASE_URL/health" | sed -n '1,120p'

echo

echo "[2/2] diagnose check"
curl -sS -X POST "$BASE_URL/diagnose" \
  -H 'Content-Type: application/json' \
  -d '{"log_text":"nslookup failed: NXDOMAIN; destination host unreachable"}' | sed -n '1,200p'

echo

echo "OK: backend smoke test passed"
