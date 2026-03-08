#!/usr/bin/env bash
set -euo pipefail

# Convenience helper that builds and then publishes to GitHub Packages.
#
# Behavior:
# 1) Runs scripts/build.sh with provided args (or default clean package).
# 2) Runs scripts/publish.sh with optional publish args after '--'.
#
# Examples:
#   ./scripts/build-and-publish.sh
#   ./scripts/build-and-publish.sh test
#   ./scripts/build-and-publish.sh clean package -- -DskipTests

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_SCRIPT="$ROOT_DIR/scripts/build.sh"
PUBLISH_SCRIPT="$ROOT_DIR/scripts/publish.sh"

build_args=()
publish_args=()
separator_seen=false

for arg in "$@"; do
  if [[ "$arg" == "--" ]]; then
    separator_seen=true
    continue
  fi

  if [[ "$separator_seen" == false ]]; then
    build_args+=("$arg")
  else
    publish_args+=("$arg")
  fi
done

if [[ "${#build_args[@]}" -eq 0 ]]; then
  build_args=(clean package)
fi

echo "== Step 1/2: Build =="
"$BUILD_SCRIPT" "${build_args[@]}"

echo
echo "== Step 2/2: Publish to GitHub Packages =="
"$PUBLISH_SCRIPT" "${publish_args[@]}"
