#!/usr/bin/env bash
set -euo pipefail

BASE_COMMIT="${1:-c03664f}"
MONO_COMMIT="${2:-8a8ebc8}"

# Ensure we have clean working tree before branch surgery.
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree is not clean. Commit/stash changes first." >&2
  exit 1
fi

current_branch="$(git rev-parse --abbrev-ref HEAD)"

cleanup() {
  git switch "$current_branch" >/dev/null 2>&1 || true
}
trap cleanup EXIT

recreate_branch() {
  local name="$1"
  if git show-ref --verify --quiet "refs/heads/$name"; then
    git branch -D "$name" >/dev/null
  fi
}

recreate_branch split/pr1-foundation
recreate_branch split/pr2-artifacts
recreate_branch split/pr3-engines
recreate_branch split/pr4-core

git switch -c split/pr1-foundation "$BASE_COMMIT" >/dev/null

git checkout "$MONO_COMMIT" -- \
  src/main/java/com/falcoignis/obtuseloot/data/SoulData.java \
  src/main/java/com/falcoignis/obtuseloot/lore/LoreEngine.java

git rm \
  src/main/java/com/falcoignis/obtuseloot/data/PlayerSoulState.java \
  src/main/java/com/falcoignis/obtuseloot/engine/SoulEngine.java \
  src/main/java/com/falcoignis/obtuseloot/lore/Epithets.java \
  src/main/java/com/falcoignis/obtuseloot/lore/Histories.java \
  src/main/java/com/falcoignis/obtuseloot/lore/Observations.java \
  src/main/java/com/falcoignis/obtuseloot/lore/Secrets.java >/dev/null

git add -A
git commit -m "Refactor soul and lore modules into engine structure" >/dev/null

git switch -c split/pr2-artifacts >/dev/null
git checkout "$MONO_COMMIT" -- \
  src/main/java/com/falcoignis/obtuseloot/artifacts \
  src/main/java/com/falcoignis/obtuseloot/reputation \
  src/main/java/com/falcoignis/obtuseloot/debug/ArtifactDebugger.java

git add -A
git commit -m "Add artifact domain, reputation, and debugging support" >/dev/null

git switch -c split/pr3-engines >/dev/null
git checkout "$MONO_COMMIT" -- \
  src/main/java/com/falcoignis/obtuseloot/awakening \
  src/main/java/com/falcoignis/obtuseloot/drift \
  src/main/java/com/falcoignis/obtuseloot/evolution

git add -A
git commit -m "Introduce awakening, drift, and evolution engines" >/dev/null

git switch -c split/pr4-core >/dev/null
git checkout "$MONO_COMMIT" -- \
  src/main/java/com/falcoignis/obtuseloot/obtuseengine \
  src/main/java/com/falcoignis/obtuseloot/ObtuseLoot.java

git add -A
git commit -m "Wire modular cores into main plugin entrypoint" >/dev/null

if ! git diff --quiet "$MONO_COMMIT"..split/pr4-core; then
  echo "ERROR: split/pr4-core does not match monolithic commit $MONO_COMMIT" >&2
  exit 1
fi

echo "Created branch stack:"
echo "  split/pr1-foundation"
echo "  split/pr2-artifacts"
echo "  split/pr3-engines"
echo "  split/pr4-core"
echo "Verified: split/pr4-core == $MONO_COMMIT"
