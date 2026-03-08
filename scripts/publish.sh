#!/usr/bin/env bash
set -euo pipefail

# Publish helper for GitHub Packages.
# Resolves repository slug from GITHUB_REPOSITORY or the current git remote,
# then deploys using Maven server id "github".

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

cat > "$SETTINGS_FILE" <<XML
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
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
