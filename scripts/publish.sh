#!/usr/bin/env bash
set -euo pipefail

# Publish helper for GitHub Packages.
# Resolves repository slug from GITHUB_REPOSITORY or the current git remote,
# then deploys using Maven server id "github".
#
# Notes:
# - Uses a temporary Maven settings file so credentials are not persisted.
# - Preserves restricted-network compatibility by supporting MAVEN_MIRROR_URL and
#   common proxy env vars (HTTPS_PROXY/HTTP_PROXY/NO_PROXY).

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "ERROR: GITHUB_TOKEN is not set."
  echo "Set a token with package:write (and repo access for private repos)."
  exit 2
fi

resolve_repo_slug() {
  if [[ -n "${GITHUB_REPOSITORY:-}" ]]; then
    printf '%s\n' "$GITHUB_REPOSITORY"
    return 0
  fi

  local remote_url
  remote_url="$(git config --get remote.origin.url || true)"
  if [[ -z "$remote_url" ]]; then
    return 1
  fi

  # Supports:
  # - git@github.com:owner/repo.git
  # - https://github.com/owner/repo.git
  # - https://x-access-token:<token>@github.com/owner/repo.git
  remote_url="${remote_url#git@github.com:}"
  remote_url="${remote_url#https://github.com/}"
  remote_url="${remote_url#http://github.com/}"
  remote_url="${remote_url#https://x-access-token:${GITHUB_TOKEN}@github.com/}"
  remote_url="${remote_url%.git}"

  if [[ "$remote_url" =~ ^[^/]+/[^/]+$ ]]; then
    printf '%s\n' "$remote_url"
    return 0
  fi

  return 1
}

build_proxy_xml() {
  local proxy_source="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
  if [[ -z "$proxy_source" ]]; then
    return 0
  fi

  PROXY_URL="$proxy_source" NO_PROXY_LIST="${NO_PROXY:-${no_proxy:-}}" python - <<'PY'
import os
from urllib.parse import urlparse

proxy_url = os.environ["PROXY_URL"]
no_proxy = os.environ.get("NO_PROXY_LIST", "")
parsed = urlparse(proxy_url)
if not parsed.scheme or not parsed.hostname:
    raise SystemExit(0)

host = parsed.hostname
port = parsed.port
if port is None:
    port = 443 if parsed.scheme == "https" else 80

username = parsed.username or ""
password = parsed.password or ""
non_proxy_hosts = "|".join(x.strip() for x in no_proxy.split(",") if x.strip())

print("  <proxies>")
print("    <proxy>")
print("      <id>env-proxy</id>")
print("      <active>true</active>")
print(f"      <protocol>{parsed.scheme}</protocol>")
print(f"      <host>{host}</host>")
print(f"      <port>{port}</port>")
if username:
    print(f"      <username>{username}</username>")
if password:
    print(f"      <password>{password}</password>")
if non_proxy_hosts:
    print(f"      <nonProxyHosts>{non_proxy_hosts}</nonProxyHosts>")
print("    </proxy>")
print("  </proxies>")
PY
}

REPO_SLUG="$(resolve_repo_slug || true)"
if [[ -z "$REPO_SLUG" ]]; then
  echo "ERROR: Could not determine GitHub repository slug."
  echo "Set GITHUB_REPOSITORY=<owner>/<repo> and rerun."
  exit 2
fi

GITHUB_ACTOR_VALUE="${GITHUB_ACTOR:-${USER:-codex}}"
SETTINGS_FILE="$(mktemp)"
cleanup() { rm -f "$SETTINGS_FILE"; }
trap cleanup EXIT

MIRROR_BLOCK=""
if [[ -n "${MAVEN_MIRROR_URL:-}" ]]; then
  MIRROR_BLOCK=$(cat <<XML
  <mirrors>
    <mirror>
      <id>internal-proxy</id>
      <name>Internal Maven Proxy</name>
      <url>${MAVEN_MIRROR_URL}</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
XML
)
fi

PROXY_BLOCK="$(build_proxy_xml || true)"

cat > "$SETTINGS_FILE" <<XML
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
${MIRROR_BLOCK}
${PROXY_BLOCK}
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_ACTOR_VALUE}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
XML

echo "Publishing com.falcoignis:obtuseloot to GitHub Packages: ${REPO_SLUG}"
exec mvn -B -ntp -s "$SETTINGS_FILE" clean deploy \
  -DaltDeploymentRepository=github::default::https://maven.pkg.github.com/${REPO_SLUG} "$@"
