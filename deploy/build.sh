#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/web"
pnpm install --frozen-lockfile
pnpm build
rm -rf "$ROOT/server/src/main/resources/static"
mkdir -p "$ROOT/server/src/main/resources/static"
cp -R dist/. "$ROOT/server/src/main/resources/static/"
cd "$ROOT/server"
mvn clean package

