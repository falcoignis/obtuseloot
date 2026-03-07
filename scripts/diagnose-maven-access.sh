#!/usr/bin/env bash
set -euo pipefail

URL="https://repo.maven.apache.org/maven2/"

echo "== Maven access diagnosis =="
echo "Target: $URL"
echo

echo "-- Via configured proxy environment --"
proxy_line="$(curl -I -s "$URL" | sed -n '1p' || true)"
if [[ -n "$proxy_line" ]]; then
  echo "$proxy_line"
else
  echo "No HTTP response line (likely blocked before HTTP response)."
fi

echo
echo "-- Direct (proxy env disabled for this probe) --"
direct_line="$(env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY curl -I -s "$URL" | sed -n '1p' || true)"
if [[ -n "$direct_line" ]]; then
  echo "$direct_line"
else
  echo "No HTTP response line (likely network-unreachable without proxy)."
fi

echo
echo "-- Maven plugin resolution probe --"
if mvn -q -DskipTests help:effective-pom >/dev/null 2>&1; then
  echo "Maven can currently resolve plugins/dependencies."
else
  echo "Maven cannot currently resolve plugins/dependencies from configured repositories."
fi
