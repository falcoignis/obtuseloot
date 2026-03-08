#!/usr/bin/env bash
set -euo pipefail

# Portable build entrypoint for local dev/CI.
# Behavior:
# 1) Try a standard Maven build if Maven Central appears reachable.
# 2) If blocked and MAVEN_MIRROR_URL is set, route via the mirror helper.
# 3) Otherwise fail with actionable instructions.

MAVEN_CMD=(mvn -B -ntp clean package)
CENTRAL_URL="https://repo.maven.apache.org/maven2/"

is_reachable() {
  local code
  code="$(curl -IsS --max-time 8 -o /dev/null -w '%{http_code}' "$1" || true)"
  [[ "$code" =~ ^2|3 ]]
}

echo "== ObtuseLoot build helper =="
echo

if is_reachable "$CENTRAL_URL"; then
  echo "Maven Central probe: reachable"
  echo "Running: ${MAVEN_CMD[*]}"
  exec "${MAVEN_CMD[@]}" "$@"
fi

echo "Maven Central probe: unreachable or blocked."

if [[ -n "${MAVEN_MIRROR_URL:-}" ]]; then
  echo "MAVEN_MIRROR_URL is set; retrying build via scripts/mvn-via-mirror.sh"
  exec "$(dirname "$0")/mvn-via-mirror.sh" "$@"
fi

cat <<'MSG'
ERROR: Cannot reach Maven Central and no mirror has been configured.

Set your internal repository proxy, then rerun:
  export MAVEN_MIRROR_URL='https://maven-proxy.example.internal/repository/maven-all/'
  ./scripts/build.sh

Optional authentication:
  export MAVEN_PROXY_USER='...'
  export MAVEN_PROXY_PASS='...'
MSG
exit 2
