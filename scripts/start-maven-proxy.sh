#!/usr/bin/env bash
# start-maven-proxy.sh — Start the local Maven artifact proxy server.
#
# This is required before running any Maven command in this environment.
# See CLAUDE.md ## Maven Dependencies for full context.
#
# Usage:
#   ./scripts/start-maven-proxy.sh          # starts on port 18080
#   ./scripts/start-maven-proxy.sh 18080    # explicit port
#
# The proxy stays running until this shell session ends. Call it once per
# session before any mvn invocation. Repeated calls are safe (noop if port
# is already in use on 127.0.0.1).

set -euo pipefail

PORT="${1:-18080}"

if curl -s --max-time 2 "http://127.0.0.1:${PORT}/central/org/apache/maven/maven/3.9.11/maven-3.9.11.pom" -o /dev/null 2>/dev/null; then
  echo "Maven proxy already running on port ${PORT}."
  exit 0
fi

python3 /usr/local/bin/maven-proxy-server.py "${PORT}" &
PROXY_PID=$!
echo "Started Maven proxy (PID ${PROXY_PID}) on http://127.0.0.1:${PORT}"

# Wait briefly and verify it's up
sleep 1
if ! kill -0 "${PROXY_PID}" 2>/dev/null; then
  echo "ERROR: Proxy failed to start." >&2
  exit 1
fi
echo "Proxy is ready."
