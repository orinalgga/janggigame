#!/usr/bin/env bash
# 새 코드를 받아 서버를 다시 띄운다: bash deploy/update.sh
set -euo pipefail
cd "$(dirname "$0")/.."
git pull --ff-only
docker compose up -d --build
docker image prune -f >/dev/null
docker compose ps
