#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${MAVEN_MIRROR_URL:-}" ]]; then
  echo "ERROR: MAVEN_MIRROR_URL must be set for mirror builds." >&2
  exit 2
fi

SETTINGS_FILE="$(mktemp)"
trap 'rm -f "$SETTINGS_FILE"' EXIT

cat > "$SETTINGS_FILE" <<SETTINGS
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>custom-mirror</id>
      <name>Configured Maven Mirror</name>
      <url>${MAVEN_MIRROR_URL}</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>custom-mirror</id>
      <username>${MAVEN_PROXY_USER:-}</username>
      <password>${MAVEN_PROXY_PASS:-}</password>
    </server>
  </servers>
</settings>
SETTINGS

echo "Running Maven via configured mirror: ${MAVEN_MIRROR_URL}"
exec mvn -B -ntp -s "$SETTINGS_FILE" "$@"
