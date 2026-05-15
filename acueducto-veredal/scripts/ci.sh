#!/usr/bin/env bash
# Verificaciones locales/portables del SDLC (GitHub Actions, Bitbucket Pipelines, etc.).
# Uso: ./scripts/ci.sh [all|backend|frontend]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-all}"

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

run_backend() {
  command -v mvn >/dev/null 2>&1 || die "Maven (mvn) no está en PATH"
  if ! docker info >/dev/null 2>&1; then
    die "Docker no está en ejecución. Los tests de backend usan Testcontainers (PostgreSQL). Inicia Docker Desktop y vuelve a ejecutar: ./scripts/ci.sh backend"
  fi
  log "Backend: mvn test (requiere Docker)"
  (cd "$ROOT/backend" && mvn -B test)
}

run_frontend() {
  command -v npm >/dev/null 2>&1 || die "npm no está en PATH"
  log "Frontend: npm ci + build"
  (
    cd "$ROOT/frontend-angular"
    if [[ -f package-lock.json ]]; then
      npm ci --no-audit --no-fund
    else
      npm install --no-audit --no-fund
    fi
    npm run build
  )
}

case "$TARGET" in
  all)
    run_backend
    run_frontend
    ;;
  backend|be)
    run_backend
    ;;
  frontend|fe)
    run_frontend
    ;;
  -h|--help|help)
    cat <<'EOF'
Uso: ./scripts/ci.sh [all|backend|frontend]

  all       Backend (mvn test) + frontend (npm ci + build) [default]
  backend   Solo backend
  frontend  Solo frontend

Notas:
  - Los tests de backend pueden requerir Docker (Testcontainers).
  - En CI de GitHub/Bitbucket, invoca este mismo script desde el pipeline.
EOF
    exit 0
    ;;
  *)
    die "Target desconocido: $TARGET (usa all|backend|frontend)"
    ;;
esac

log "CI local OK ($TARGET)"
