#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <spec.properties> [extra cli args...]"
  exit 1
fi

SPEC="$1"
shift || true

mvn -q -DskipTests compile
java -cp target/classes obtuseloot.analytics.ecosystem.AnalyticsCliMain run-spec --spec "$SPEC" "$@"
