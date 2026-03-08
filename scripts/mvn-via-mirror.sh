#!/usr/bin/env bash
set -euo pipefail

# Maven runner for restricted environments where Maven Central is blocked.
# Requires an internal repository manager/proxy (Nexus/Artifactory/etc).

if [[ -z "${MAVEN_MIRROR_URL:-}" ]]; then
  echo "ERROR: MAVEN_MIRROR_URL is not set."
  echo "Set it to your internal Maven proxy URL, for example:"
  echo "  export MAVEN_MIRROR_URL='https://maven-proxy.example.internal/repository/maven-all/'"
  exit 2
fi

SETTINGS_FILE="$(mktemp)"
cleanup() { rm -f "$SETTINGS_FILE"; }
trap cleanup EXIT

cat > "$SETTINGS_FILE" <<XML
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>internal-proxy</id>
      <name>Internal Maven Proxy</name>
      <url>${MAVEN_MIRROR_URL}</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>internal-proxy</id>
      <username>${MAVEN_PROXY_USER:-}</username>
      <password>${MAVEN_PROXY_PASS:-}</password>
    </server>
  </servers>
</settings>
XML

if [[ "$#" -eq 0 ]]; then
  set -- clean package
fi

exec mvn -B -ntp -s "$SETTINGS_FILE" "$@"
