#!/usr/bin/env bash
# Deep ecosystem validation suite.
# Uses 10 seasons x 4 sessions/season for meaningful cohort depth.
# Runs the same 5 scenarios as the constrained suite.
# Keeps encounter density, telemetry sampling, and player count stable.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BASE_ROOT="analytics/validation-suite-rerun"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RUN_ID="deep-ten-season-${TIMESTAMP}"
SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
REQUIRED_COMPLETION_SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
POINTER_PATH="analytics/validation-suite/latest-run.properties"
POINTER_EXPECTED_SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
COMPLETION_MARKER_NAME=".dataset-complete.properties"

# Deep-run parameters
DEEP_SEASONS=10
DEEP_SESSIONS_PER_SEASON=4
DEEP_PLAYERS=18
DEEP_ARTIFACTS_PER_PLAYER=3
DEEP_ENCOUNTER_DENSITY=5
DEEP_TELEMETRY_SAMPLING=0.25

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id) RUN_ID="$2"; shift 2 ;;
    --scenario) SCENARIOS=("$2"); shift 2 ;;
    --scenarios) IFS=',' read -r -a SCENARIOS <<< "$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

RUN_ROOT="$BASE_ROOT/$RUN_ID"
LOG_ROOT="$RUN_ROOT/logs"
OUTPUT_ROOT="$RUN_ROOT"
mkdir -p "$LOG_ROOT" "$OUTPUT_ROOT"

# ---------------------------------------------------------------------------
# Build (offline using pre-downloaded deps, or via local proxy)
# ---------------------------------------------------------------------------
if [[ -d target/classes/obtuseloot/simulation/worldlab ]]; then
  echo "[deep-run] Using existing compiled classes."
else
  echo "[deep-run] Compiling project..."
  if JAVA_TOOL_OPTIONS="" mvn -q -DskipTests --offline compile 2>/dev/null; then
    echo "[deep-run] Offline compile succeeded."
  else
    echo "[deep-run] Offline compile failed; trying with local proxy on :18081..." >&2
    JAVA_TOOL_OPTIONS="" mvn -q -DskipTests -s /tmp/maven-local-proxy.xml compile
    echo "[deep-run] Compile via local proxy succeeded."
  fi
fi

required_artifacts=(
  "telemetry/ecosystem-events.log"
  "telemetry/rollup-snapshot.properties"
  "rollup-snapshots.json"
  "scenario-metadata.properties"
)

MANIFEST_PATH="$RUN_ROOT/run-manifest.json"
CREATED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

stage_matrix_execution="PENDING"
stage_dataset_contract="PENDING"
stage_completion_marker="PENDING"
stage_latest_run_pointer="PENDING"
stage_analytics_ingestion="PENDING"
stage_ecosystem_evaluation="PENDING"

ts_matrix_execution=""
ts_dataset_contract=""
ts_completion_marker=""
ts_latest_run_pointer=""
ts_analytics_ingestion=""
ts_ecosystem_evaluation=""

declare -A scenario_artifact_status
declare -A scenario_complete

for _s in "${SCENARIOS[@]}"; do
  scenario_complete["$_s"]="false"
  for _r in "${required_artifacts[@]}"; do
    scenario_artifact_status["${_s}/${_r}"]="false"
  done
done

update_scenario_artifacts() {
  local scenario="$1"
  local scenario_path="$OUTPUT_ROOT/$scenario"
  local all_present=true
  for rel in "${required_artifacts[@]}"; do
    local akey="${scenario}/${rel}"
    if [[ -s "$scenario_path/$rel" ]]; then
      scenario_artifact_status["$akey"]="true"
    else
      scenario_artifact_status["$akey"]="false"
      all_present=false
    fi
  done
  if $all_present; then
    scenario_complete["$scenario"]="true"
  else
    scenario_complete["$scenario"]="false"
  fi
}

write_manifest() {
  local run_status="$1"
  local ts
  ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  local dataset_root_val
  if [[ -d "$OUTPUT_ROOT" ]]; then
    dataset_root_val="$(realpath "$OUTPUT_ROOT")"
  else
    dataset_root_val="$OUTPUT_ROOT"
  fi
  local manifest_tmp="${MANIFEST_PATH}.tmp"
  {
    printf '{\n'
    printf '  "run_id": "%s",\n' "$RUN_ID"
    printf '  "dataset_root": "%s",\n' "$dataset_root_val"
    printf '  "created_at": "%s",\n' "$CREATED_AT"
    printf '  "updated_at": "%s",\n' "$ts"
    printf '  "status": "%s",\n' "$run_status"
    printf '  "deep_run_config": {"seasons": %d, "sessionsPerSeason": %d, "players": %d, "artifactsPerPlayer": %d, "encounterDensity": %d, "telemetrySamplingRate": %.2f},\n' \
           "$DEEP_SEASONS" "$DEEP_SESSIONS_PER_SEASON" "$DEEP_PLAYERS" "$DEEP_ARTIFACTS_PER_PLAYER" "$DEEP_ENCOUNTER_DENSITY" "$DEEP_TELEMETRY_SAMPLING"
    printf '  "stages": {\n'
    printf '    "matrix_execution":   {"status": "%s", "updated_at": "%s"},\n' "$stage_matrix_execution" "$ts_matrix_execution"
    printf '    "dataset_contract":   {"status": "%s", "updated_at": "%s"},\n' "$stage_dataset_contract" "$ts_dataset_contract"
    printf '    "completion_marker":  {"status": "%s", "updated_at": "%s"},\n' "$stage_completion_marker" "$ts_completion_marker"
    printf '    "latest_run_pointer": {"status": "%s", "updated_at": "%s"},\n' "$stage_latest_run_pointer" "$ts_latest_run_pointer"
    printf '    "analytics_ingestion":  {"status": "%s", "updated_at": "%s"},\n' "$stage_analytics_ingestion" "$ts_analytics_ingestion"
    printf '    "ecosystem_evaluation": {"status": "%s", "updated_at": "%s"}\n' "$stage_ecosystem_evaluation" "$ts_ecosystem_evaluation"
    printf '  },\n'
    printf '  "scenarios": {\n'
    local first_s=1
    for scenario in "${SCENARIOS[@]}"; do
      [[ $first_s -eq 0 ]] && printf ',\n'
      first_s=0
      local scenario_path_abs
      local scenario_path="$OUTPUT_ROOT/$scenario"
      if [[ -d "$scenario_path" ]]; then
        scenario_path_abs="$(realpath "$scenario_path")"
      else
        scenario_path_abs="${dataset_root_val}/${scenario}"
      fi
      local complete_val="${scenario_complete[$scenario]:-false}"
      printf '    "%s": {\n' "$scenario"
      printf '      "path": "%s",\n' "$scenario_path_abs"
      printf '      "artifacts": {\n'
      local first_a=1
      for rel in "${required_artifacts[@]}"; do
        [[ $first_a -eq 0 ]] && printf ',\n'
        first_a=0
        local akey="${scenario}/${rel}"
        local aval="${scenario_artifact_status[$akey]:-false}"
        printf '        "%s": %s' "$rel" "$aval"
      done
      printf '\n      },\n'
      printf '      "complete": %s\n' "$complete_val"
      printf '    }'
    done
    printf '\n  }\n'
    printf '}\n'
  } > "$manifest_tmp"
  mv "$manifest_tmp" "$MANIFEST_PATH"
}

resolve_config_path() {
  local scenario="$1"
  local candidates=(
    "analytics/validation-suite/configs/${scenario}-run.properties"
    "analytics/validation-suite/configs/${scenario}.properties"
    "analytics/validation-suite-rerun/configs/${scenario}.properties"
  )
  local p
  for p in "${candidates[@]}"; do
    if [[ -f "$p" ]]; then
      printf '%s' "$p"
      return 0
    fi
  done
  return 1
}

is_true_harness_dataset_root() {
  local dataset_root="$1"
  shift
  local scenarios=("$@")
  if [[ -z "$dataset_root" ]] || [[ ! -d "$dataset_root" ]]; then return 1; fi
  local scenario rel
  for scenario in "${scenarios[@]}"; do
    if [[ ! -d "$dataset_root/$scenario" ]]; then return 1; fi
    for rel in "${required_artifacts[@]}"; do
      if [[ ! -s "$dataset_root/$scenario/$rel" ]]; then return 1; fi
    done
  done
  return 0
}

can_write_completion_marker() {
  local run_root="$1"
  local dataset_root="$2"
  shift 2
  local scenarios=("$@")
  if [[ -z "$run_root" ]] || [[ ! -d "$run_root" ]]; then return 1; fi
  if [[ -z "$dataset_root" ]] || [[ ! -d "$dataset_root" ]]; then return 1; fi
  local scenario rel
  for scenario in "${scenarios[@]}"; do
    if [[ ! -d "$dataset_root/$scenario" ]]; then return 1; fi
    for rel in "${required_artifacts[@]}"; do
      if [[ ! -s "$dataset_root/$scenario/$rel" ]]; then return 1; fi
    done
  done
  return 0
}

write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 1: matrix_execution
# ---------------------------------------------------------------------------
stage_matrix_execution="IN_PROGRESS"
ts_matrix_execution="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

status_lines=()
dataset_lines=()
completed_scenarios=()
run_failed=0

for scenario in "${SCENARIOS[@]}"; do
  scenario_root="$OUTPUT_ROOT/$scenario"
  scenario_log="$LOG_ROOT/$scenario.log"
  rm -rf "$scenario_root"
  mkdir -p "$scenario_root"
  scenario_started_epoch="$(date +%s)"

  if ! config_path="$(resolve_config_path "$scenario")"; then
    echo "FAIL: missing config for '$scenario'" >&2
    exit 1
  fi

  echo "[deep-run] Running scenario: $scenario (seasons=$DEEP_SEASONS sessionsPerSeason=$DEEP_SESSIONS_PER_SEASON)"

  set +e
  JAVA_TOOL_OPTIONS="" mvn -q -DskipTests \
    -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner \
    -Dexec.classpathScope=compile \
    -Dworld.outputDirectory="$scenario_root" \
    -Dworld.validationProfile=true \
    -Dworld.telemetrySamplingRate=${DEEP_TELEMETRY_SAMPLING} \
    -Dworld.players=${DEEP_PLAYERS} \
    -Dworld.artifactsPerPlayer=${DEEP_ARTIFACTS_PER_PLAYER} \
    -Dworld.sessionsPerSeason=${DEEP_SESSIONS_PER_SEASON} \
    -Dworld.seasonCount=${DEEP_SEASONS} \
    -Dworld.encounterDensity=${DEEP_ENCOUNTER_DENSITY} \
    -Dworld.scenarioConfigPath="$config_path" \
    org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
    >"$scenario_log" 2>&1
  exit_code=$?
  set -e

  if [[ $exit_code -ne 0 ]]; then
    echo "FAIL: scenario '$scenario' harness exited $exit_code; log=$scenario_log" >&2
    cat "$scenario_log" | tail -20 >&2
    exit 1
  fi

  expected_log_marker="World simulation outputs written to ${scenario_root}"
  if ! grep -Fq "$expected_log_marker" "$scenario_log"; then
    echo "FAIL: completion marker not found in log for '$scenario'" >&2
    exit 1
  fi

  missing=()
  stale=()
  for rel in "${required_artifacts[@]}"; do
    target="$scenario_root/$rel"
    if [[ ! -s "$target" ]]; then
      missing+=("$rel")
      continue
    fi
    artifact_epoch="$(stat -c %Y "$target")"
    if [[ "$artifact_epoch" -lt "$scenario_started_epoch" ]]; then
      stale+=("$rel")
    fi
  done

  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "FAIL: artifacts missing for '$scenario': ${missing[*]}" >&2
    exit 1
  fi
  if [[ ${#stale[@]} -gt 0 ]]; then
    echo "FAIL: stale artifacts for '$scenario': ${stale[*]}" >&2
    exit 1
  fi

  update_scenario_artifacts "$scenario"
  status_lines+=("- ${scenario}: SUCCESS")
  dataset_lines+=("- ${scenario}: VERIFIED")
  completed_scenarios+=("$scenario")
  write_manifest "IN_PROGRESS"
  echo "[deep-run] $scenario DONE"
done

stage_matrix_execution="PASSED"
ts_matrix_execution="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 2: dataset_contract
# ---------------------------------------------------------------------------
stage_dataset_contract="IN_PROGRESS"
ts_dataset_contract="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

for scenario in "${SCENARIOS[@]}"; do
  update_scenario_artifacts "$scenario"
done

stage_dataset_contract="PASSED"
ts_dataset_contract="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 3: completion_marker
# ---------------------------------------------------------------------------
stage_completion_marker="IN_PROGRESS"
ts_completion_marker="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

completion_marker_path="$RUN_ROOT/$COMPLETION_MARKER_NAME"
if can_write_completion_marker "$RUN_ROOT" "$OUTPUT_ROOT" "${SCENARIOS[@]}"; then
  {
    echo "run_id=${RUN_ID}"
    echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "scenario_count=${#SCENARIOS[@]}"
    echo "contract_version=1"
    echo "deep_seasons=${DEEP_SEASONS}"
    echo "deep_sessions_per_season=${DEEP_SESSIONS_PER_SEASON}"
  } > "$completion_marker_path"
  stage_completion_marker="PASSED"
else
  stage_completion_marker="FAILED"
  echo "FAIL: could not write completion marker" >&2
  exit 1
fi
ts_completion_marker="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 4: latest_run_pointer
# ---------------------------------------------------------------------------
stage_latest_run_pointer="IN_PROGRESS"
ts_latest_run_pointer="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

if is_true_harness_dataset_root "$OUTPUT_ROOT" "${SCENARIOS[@]}"; then
  dataset_root_abs="$(realpath "$OUTPUT_ROOT")"
  {
    echo "run_id=${RUN_ID}"
    echo "dataset_root=${dataset_root_abs}"
    echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  } > "$POINTER_PATH"
  stage_latest_run_pointer="PASSED"
else
  stage_latest_run_pointer="FAILED"
  echo "WARN: dataset root did not pass strict harness contract; pointer not updated" >&2
fi
ts_latest_run_pointer="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "READY_FOR_ANALYSIS"

# ---------------------------------------------------------------------------
# Execution report
# ---------------------------------------------------------------------------
{
  echo "SECTION 1: EXECUTION PATH USED"
  echo "- Harness: obtuseloot.simulation.worldlab.WorldSimulationRunner (Maven exec)"
  echo "- Run root: ${RUN_ROOT}"
  echo ""
  echo "SECTION 2: RUNTIME SETTINGS (DEEP RUN)"
  echo "- validationProfile=true"
  echo "- world.players=${DEEP_PLAYERS}"
  echo "- world.artifactsPerPlayer=${DEEP_ARTIFACTS_PER_PLAYER}"
  echo "- world.sessionsPerSeason=${DEEP_SESSIONS_PER_SEASON}"
  echo "- world.seasonCount=${DEEP_SEASONS}"
  echo "- world.encounterDensity=${DEEP_ENCOUNTER_DENSITY}"
  echo "- world.telemetrySamplingRate=${DEEP_TELEMETRY_SAMPLING}"
  echo ""
  echo "SECTION 3: SCENARIOS"
  for s in "${SCENARIOS[@]}"; do echo "- $s"; done
  echo ""
  echo "SECTION 4: COMPLETION STATUS"
  printf '%s\n' "${status_lines[@]}"
  echo ""
  echo "SECTION 5: VERDICT"
  echo "SUCCESS"
} > "$RUN_ROOT/execution-report.md"

echo ""
echo "==================================================="
echo "Deep run complete: ${RUN_ROOT}"
echo "Manifest: ${MANIFEST_PATH}"
echo "==================================================="
