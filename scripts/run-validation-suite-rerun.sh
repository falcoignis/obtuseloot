#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BASE_ROOT="analytics/validation-suite-rerun"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RUN_ID="archive-fix-rerun-${TIMESTAMP}"
SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
REQUIRED_COMPLETION_SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
POINTER_PATH="analytics/validation-suite/latest-run.properties"
POINTER_EXPECTED_SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
COMPLETION_MARKER_NAME=".dataset-complete.properties"

TEST_MODE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --scenario)
      SCENARIOS=("$2")
      shift 2
      ;;
    --scenarios)
      IFS=',' read -r -a SCENARIOS <<< "$2"
      shift 2
      ;;
    --test-mode)
      # Test mode: skip mvn compile and WorldSimulationRunner; inject stub
      # artifacts so manifest stage logic can be exercised without a network.
      TEST_MODE=1
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

RUN_ROOT="$BASE_ROOT/$RUN_ID"
LOG_ROOT="$RUN_ROOT/logs"
# Scenario outputs written directly under the run root so paths resolve as
# analytics/validation-suite-rerun/<run-id>/<scenario>.
OUTPUT_ROOT="$RUN_ROOT"
mkdir -p "$LOG_ROOT" "$OUTPUT_ROOT"

if [[ $TEST_MODE -eq 0 ]]; then
  mvn -q -DskipTests compile
fi

required_artifacts=(
  "telemetry/ecosystem-events.log"
  "telemetry/rollup-snapshot.properties"
  "rollup-snapshots.json"
  "scenario-metadata.properties"
)

# ---------------------------------------------------------------------------
# Manifest tracking variables
# ---------------------------------------------------------------------------
MANIFEST_PATH="$RUN_ROOT/run-manifest.json"
CREATED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Stage statuses: PENDING | IN_PROGRESS | PASSED | FAILED | SKIPPED
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

# Per-scenario artifact completeness: keys are "<scenario>/<rel>" -> "true"|"false"
declare -A scenario_artifact_status
declare -A scenario_complete

# Initialize all scenarios/artifacts to false
for _s in "${SCENARIOS[@]}"; do
  scenario_complete["$_s"]="false"
  for _r in "${required_artifacts[@]}"; do
    scenario_artifact_status["${_s}/${_r}"]="false"
  done
done

# Snapshot artifact completeness for a scenario from current filesystem state
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

# Write the canonical run-manifest.json (atomic: write .tmp then mv)
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
    printf '  "stages": {\n'
    printf '    "matrix_execution":   {"status": "%s", "updated_at": "%s"},\n' \
           "$stage_matrix_execution" "$ts_matrix_execution"
    printf '    "dataset_contract":   {"status": "%s", "updated_at": "%s"},\n' \
           "$stage_dataset_contract" "$ts_dataset_contract"
    printf '    "completion_marker":  {"status": "%s", "updated_at": "%s"},\n' \
           "$stage_completion_marker" "$ts_completion_marker"
    printf '    "latest_run_pointer": {"status": "%s", "updated_at": "%s"},\n' \
           "$stage_latest_run_pointer" "$ts_latest_run_pointer"
    printf '    "analytics_ingestion":  {"status": "%s", "updated_at": "%s"},\n' \
           "$stage_analytics_ingestion" "$ts_analytics_ingestion"
    printf '    "ecosystem_evaluation": {"status": "%s", "updated_at": "%s"}\n' \
           "$stage_ecosystem_evaluation" "$ts_ecosystem_evaluation"
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

# ---------------------------------------------------------------------------

is_analysis_only_root() {
  local dataset_root="$1"
  [[ "$dataset_root" == *"/analysis" ]] || [[ "$dataset_root" == *"/analysis/"* ]]
}

is_cli_log_only_root() {
  local dataset_root="$1"
  shift
  local scenarios=("$@")
  local scenario
  for scenario in "${scenarios[@]}"; do
    local scenario_dir="$dataset_root/$scenario"
    if [[ -d "$scenario_dir" ]] && [[ -s "$scenario_dir/cli.log" ]]; then
      local has_required=0
      local rel
      for rel in "${required_artifacts[@]}"; do
        if [[ -s "$scenario_dir/$rel" ]]; then
          has_required=1
          break
        fi
      done
      if [[ $has_required -eq 0 ]]; then
        return 0
      fi
    fi
  done
  return 1
}

is_report_only_root() {
  local dataset_root="$1"
  shift
  local scenarios=("$@")
  local scenario
  for scenario in "${scenarios[@]}"; do
    local scenario_dir="$dataset_root/$scenario"
    if [[ -d "$scenario_dir" ]]; then
      shopt -s nullglob
      local report_files=(
        "$scenario_dir"/*analysis-report.txt
        "$scenario_dir"/*output-manifest.properties
        "$scenario_dir"/*job-record.properties
        "$scenario_dir"/*run-metadata.properties
      )
      shopt -u nullglob
      if [[ ${#report_files[@]} -gt 0 ]]; then
        local has_required=0
        local rel
        for rel in "${required_artifacts[@]}"; do
          if [[ -s "$scenario_dir/$rel" ]]; then
            has_required=1
            break
          fi
        done
        if [[ $has_required -eq 0 ]]; then
          return 0
        fi
      fi
    fi
  done
  return 1
}

is_true_harness_dataset_root() {
  local dataset_root="$1"
  shift
  local scenarios=("$@")

  if [[ -z "$dataset_root" ]] || [[ ! -d "$dataset_root" ]]; then
    return 1
  fi

  # Never point latest-run.properties at analysis-only directories.
  if is_analysis_only_root "$dataset_root"; then
    return 1
  fi

  # Explicitly reject roots that only contain CLI logs or analytics reports.
  if is_cli_log_only_root "$dataset_root" "${scenarios[@]}"; then
    return 1
  fi
  if is_report_only_root "$dataset_root" "${scenarios[@]}"; then
    return 1
  fi

  local scenario rel
  for scenario in "${scenarios[@]}"; do
    if [[ ! -d "$dataset_root/$scenario" ]]; then
      return 1
    fi

    for rel in "${required_artifacts[@]}"; do
      if [[ ! -s "$dataset_root/$scenario/$rel" ]]; then
        return 1
      fi
    done
  done

  return 0
}

can_write_completion_marker() {
  local run_root="$1"
  local dataset_root="$2"
  shift 2
  local scenarios=("$@")

  if [[ -z "$run_root" ]] || [[ ! -d "$run_root" ]]; then
    return 1
  fi

  if [[ -z "$dataset_root" ]] || [[ ! -d "$dataset_root" ]]; then
    return 1
  fi

  local scenario rel
  for scenario in "${scenarios[@]}"; do
    if [[ ! -d "$dataset_root/$scenario" ]]; then
      return 1
    fi
    for rel in "${required_artifacts[@]}"; do
      if [[ ! -s "$dataset_root/$scenario/$rel" ]]; then
        return 1
      fi
    done
  done

  return 0
}

# ---------------------------------------------------------------------------
# Helpers for fail-fast manifest-gated exits
# ---------------------------------------------------------------------------
fail_fast_matrix() {
  local reason="$1"
  stage_matrix_execution="FAILED"
  ts_matrix_execution="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  write_manifest "FAILED"
  echo "FAIL-FAST: matrix_execution stage failed — ${reason}" >&2
}

fail_fast_contract() {
  local reason="$1"
  stage_dataset_contract="FAILED"
  ts_dataset_contract="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  write_manifest "FAILED"
  echo "FAIL-FAST: dataset_contract stage failed — ${reason}" >&2
}

# ---------------------------------------------------------------------------

status_lines=()
dataset_lines=()
completed_scenarios=()
run_failed=0

if [[ ${#SCENARIOS[@]} -eq 0 ]] || [[ -z "${SCENARIOS[*]// }" ]]; then
  echo "No scenarios were provided; refusing to run with an empty scenario list." >&2
  exit 2
fi

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

# ---------------------------------------------------------------------------
# Initialize manifest: IN_PROGRESS, all stages PENDING
# ---------------------------------------------------------------------------
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 1: matrix_execution — run WorldSimulationRunner for each scenario
# ---------------------------------------------------------------------------
stage_matrix_execution="IN_PROGRESS"
ts_matrix_execution="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

for scenario in "${SCENARIOS[@]}"; do
  scenario_root="$OUTPUT_ROOT/$scenario"
  scenario_log="$LOG_ROOT/$scenario.log"
  rm -rf "$scenario_root"
  mkdir -p "$scenario_root"
  scenario_started_epoch="$(date +%s)"

  if ! config_path="$(resolve_config_path "$scenario")"; then
    status_lines+=("- ${scenario}: FAILED (missing scenario config file)")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (config not found)")
    run_failed=1
    # Update artifacts snapshot (all false) and write manifest before failing fast
    update_scenario_artifacts "$scenario"
    fail_fast_matrix "scenario '${scenario}' config not found"
    # Emit failure report and exit
    failure_report="$RUN_ROOT/failure-report.md"
    {
      echo "SECTION 1: RUN FAILURE SUMMARY"
      echo "- Stage failed: matrix_execution"
      echo "- Reason: missing scenario config for '${scenario}'"
      echo "- Run root: ${RUN_ROOT}"
      echo "- Execution report intentionally not written because matrix_execution failed."
      echo
      echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
      printf '%s\n' "${status_lines[@]}"
      echo
      echo "SECTION 3: DATASET CONTRACT VERIFICATION"
      printf '%s\n' "${dataset_lines[@]}"
      echo
      echo "SECTION 4: EXECUTION VERDICT"
      echo "FAILED"
    } > "$failure_report"
    echo "Run failed: ${RUN_ROOT}"
    echo "Failure report: ${failure_report}"
    exit 1
  fi

  if [[ $TEST_MODE -eq 1 ]]; then
    # --test-mode: inject minimal stub artifacts that satisfy the harness
    # contract so the manifest stage logic can be exercised end-to-end.
    mkdir -p "$scenario_root/telemetry"
    printf 'event=TEST_STUB\n' > "$scenario_root/telemetry/ecosystem-events.log"
    printf 'snapshot=stub\n'   > "$scenario_root/telemetry/rollup-snapshot.properties"
    printf '[]\n'               > "$scenario_root/rollup-snapshots.json"
    printf 'scenario=%s\nprofile=test\n' "$scenario" > "$scenario_root/scenario-metadata.properties"
    # Simulate the log marker that the harness normally emits
    echo "World simulation outputs written to ${scenario_root}" > "$scenario_log"
    exit_code=0
  else
    set +e
    mvn -q -DskipTests \
      -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner \
      -Dexec.classpathScope=compile \
      -Dworld.outputDirectory="$scenario_root" \
      -Dworld.validationProfile=true \
      -Dworld.telemetrySamplingRate=0.25 \
      -Dworld.players=18 \
      -Dworld.artifactsPerPlayer=3 \
      -Dworld.sessionsPerSeason=2 \
      -Dworld.seasonCount=3 \
      -Dworld.encounterDensity=5 \
      -Dworld.scenarioConfigPath="$config_path" \
      org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
      >"$scenario_log" 2>&1
    exit_code=$?
    set -e
  fi

  if [[ $exit_code -ne 0 ]]; then
    status_lines+=("- ${scenario}: FAILED (exit code ${exit_code}; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (harness command failed)")
    run_failed=1
    update_scenario_artifacts "$scenario"
    fail_fast_matrix "scenario '${scenario}' harness exited ${exit_code}"
    failure_report="$RUN_ROOT/failure-report.md"
    {
      echo "SECTION 1: RUN FAILURE SUMMARY"
      echo "- Stage failed: matrix_execution"
      echo "- Reason: WorldSimulationRunner exited ${exit_code} for scenario '${scenario}'"
      echo "- Run root: ${RUN_ROOT}"
      echo "- Log: ${scenario_log}"
      echo "- Execution report intentionally not written because matrix_execution failed."
      echo
      echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
      printf '%s\n' "${status_lines[@]}"
      echo
      echo "SECTION 3: DATASET CONTRACT VERIFICATION"
      printf '%s\n' "${dataset_lines[@]}"
      echo
      echo "SECTION 4: EXECUTION VERDICT"
      echo "FAILED"
    } > "$failure_report"
    echo "Run failed: ${RUN_ROOT}"
    echo "Failure report: ${failure_report}"
    exit 1
  fi

  expected_log_marker="World simulation outputs written to ${scenario_root}"
  if ! grep -Fq "$expected_log_marker" "$scenario_log"; then
    status_lines+=("- ${scenario}: FAILED (harness exited 0 but completion marker not found in log; expected '${expected_log_marker}'; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (missing harness completion marker)")
    run_failed=1
    update_scenario_artifacts "$scenario"
    fail_fast_matrix "scenario '${scenario}' harness completion marker not found in log"
    failure_report="$RUN_ROOT/failure-report.md"
    {
      echo "SECTION 1: RUN FAILURE SUMMARY"
      echo "- Stage failed: matrix_execution"
      echo "- Reason: harness completion marker not found in log for '${scenario}'"
      echo "- Expected: ${expected_log_marker}"
      echo "- Log: ${scenario_log}"
      echo "- Execution report intentionally not written because matrix_execution failed."
      echo
      echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
      printf '%s\n' "${status_lines[@]}"
      echo
      echo "SECTION 3: DATASET CONTRACT VERIFICATION"
      printf '%s\n' "${dataset_lines[@]}"
      echo
      echo "SECTION 4: EXECUTION VERDICT"
      echo "FAILED"
    } > "$failure_report"
    echo "Run failed: ${RUN_ROOT}"
    echo "Failure report: ${failure_report}"
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
    status_lines+=("- ${scenario}: FAILED (harness exited 0 but required artifacts missing: ${missing[*]}; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (missing ${missing[*]})")
    run_failed=1
    update_scenario_artifacts "$scenario"
    fail_fast_matrix "scenario '${scenario}' required artifacts missing: ${missing[*]}"
    failure_report="$RUN_ROOT/failure-report.md"
    {
      echo "SECTION 1: RUN FAILURE SUMMARY"
      echo "- Stage failed: matrix_execution"
      echo "- Reason: required artifacts missing for '${scenario}': ${missing[*]}"
      echo "- Run root: ${RUN_ROOT}"
      echo "- Execution report intentionally not written because matrix_execution failed."
      echo
      echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
      printf '%s\n' "${status_lines[@]}"
      echo
      echo "SECTION 3: DATASET CONTRACT VERIFICATION"
      printf '%s\n' "${dataset_lines[@]}"
      echo
      echo "SECTION 4: EXECUTION VERDICT"
      echo "FAILED"
    } > "$failure_report"
    echo "Run failed: ${RUN_ROOT}"
    echo "Failure report: ${failure_report}"
    exit 1
  fi

  if [[ ${#stale[@]} -gt 0 ]]; then
    status_lines+=("- ${scenario}: FAILED (harness exited 0 but required artifacts were stale from an earlier run: ${stale[*]}; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (stale artifacts ${stale[*]})")
    run_failed=1
    update_scenario_artifacts "$scenario"
    fail_fast_matrix "scenario '${scenario}' stale artifacts: ${stale[*]}"
    failure_report="$RUN_ROOT/failure-report.md"
    {
      echo "SECTION 1: RUN FAILURE SUMMARY"
      echo "- Stage failed: matrix_execution"
      echo "- Reason: stale artifacts for '${scenario}': ${stale[*]}"
      echo "- Run root: ${RUN_ROOT}"
      echo "- Execution report intentionally not written because matrix_execution failed."
      echo
      echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
      printf '%s\n' "${status_lines[@]}"
      echo
      echo "SECTION 3: DATASET CONTRACT VERIFICATION"
      printf '%s\n' "${dataset_lines[@]}"
      echo
      echo "SECTION 4: EXECUTION VERDICT"
      echo "FAILED"
    } > "$failure_report"
    echo "Run failed: ${RUN_ROOT}"
    echo "Failure report: ${failure_report}"
    exit 1
  fi

  # Scenario passed — snapshot artifact status into manifest
  update_scenario_artifacts "$scenario"
  status_lines+=("- ${scenario}: SUCCESS (exit code 0; log=${scenario_log})")
  dataset_lines+=("- ${scenario}: VERIFIED (${required_artifacts[*]})")
  completed_scenarios+=("$scenario")
  # Write manifest after each successful scenario so progress is recorded
  write_manifest "IN_PROGRESS"
done

# All scenarios in the matrix completed successfully
stage_matrix_execution="PASSED"
ts_matrix_execution="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 2: dataset_contract — re-verify all scenario roots before continuing
# ---------------------------------------------------------------------------
stage_dataset_contract="IN_PROGRESS"
ts_dataset_contract="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

dataset_contract_ok=1

if [[ ! -d "$OUTPUT_ROOT" ]]; then
  status_lines+=("- RUN ROOT CHECK: FAILED (output root missing at report time: ${OUTPUT_ROOT})")
  dataset_lines+=("- RUN ROOT CHECK: FAILED dataset verification (output root missing)")
  dataset_contract_ok=0
fi

for scenario in "${SCENARIOS[@]}"; do
  scenario_root="$OUTPUT_ROOT/$scenario"
  if [[ ! -d "$scenario_root" ]]; then
    status_lines+=("- ${scenario}: FAILED post-run verification (scenario root missing at report time: ${scenario_root})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (scenario root missing)")
    dataset_contract_ok=0
    continue
  fi

  if [[ ! " ${completed_scenarios[*]} " =~ " ${scenario} " ]]; then
    status_lines+=("- ${scenario}: FAILED post-run verification (scenario did not complete successfully)")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (scenario not completed)")
    dataset_contract_ok=0
    continue
  fi

  missing=()
  for rel in "${required_artifacts[@]}"; do
    target="$scenario_root/$rel"
    if [[ ! -s "$target" ]]; then
      missing+=("$rel")
    fi
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    status_lines+=("- ${scenario}: FAILED post-run verification (required artifacts missing at report time: ${missing[*]})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (post-run missing ${missing[*]})")
    dataset_contract_ok=0
  fi
done

# Re-snapshot all scenarios after post-run verification
for scenario in "${SCENARIOS[@]}"; do
  update_scenario_artifacts "$scenario"
done

if [[ $dataset_contract_ok -eq 0 ]]; then
  run_failed=1
  fail_fast_contract "post-run artifact re-verification failed"
  failure_report="$RUN_ROOT/failure-report.md"
  {
    echo "SECTION 1: RUN FAILURE SUMMARY"
    echo "- Stage failed: dataset_contract"
    echo "- Reason: post-run artifact re-verification failed"
    echo "- Run root: ${RUN_ROOT}"
    echo "- Execution report intentionally not written because dataset contract failed."
    echo
    echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
    printf '%s\n' "${status_lines[@]}"
    echo
    echo "SECTION 3: DATASET CONTRACT VERIFICATION"
    printf '%s\n' "${dataset_lines[@]}"
    echo
    echo "SECTION 4: EXECUTION VERDICT"
    echo "FAILED"
  } > "$failure_report"
  echo "Run failed: ${RUN_ROOT}"
  echo "Failure report: ${failure_report}"
  exit 1
fi

stage_dataset_contract="PASSED"
ts_dataset_contract="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 3: completion_marker — write .dataset-complete.properties
# ---------------------------------------------------------------------------
stage_completion_marker="IN_PROGRESS"
ts_completion_marker="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

completion_marker_path="$RUN_ROOT/$COMPLETION_MARKER_NAME"
completion_marker_written=0
if can_write_completion_marker "$RUN_ROOT" "$OUTPUT_ROOT" "${SCENARIOS[@]}"; then
  completion_tmp="${completion_marker_path}.tmp"
  created_at_cm="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  scenario_count="${#SCENARIOS[@]}"
  {
    echo "run_id=${RUN_ID}"
    echo "created_at=${created_at_cm}"
    echo "scenario_count=${scenario_count}"
    echo "contract_version=1"
  } > "$completion_tmp"
  mv "$completion_tmp" "$completion_marker_path"
  completion_marker_written=1
  status_lines+=("- DATASET COMPLETION MARKER: WRITTEN (${completion_marker_path})")
  dataset_lines+=("- DATASET COMPLETION MARKER: VERIFIED (${SCENARIOS[*]})")
else
  status_lines+=("- DATASET COMPLETION MARKER: SKIPPED (required scenario matrix incomplete or artifacts missing)")
  dataset_lines+=("- DATASET COMPLETION MARKER: SKIPPED (${COMPLETION_MARKER_NAME} not written)")
  run_failed=1
fi

if [[ $completion_marker_written -eq 1 ]] && [[ ! -s "$completion_marker_path" ]]; then
  status_lines+=("- DATASET COMPLETION MARKER: FAILED (${completion_marker_path} missing after write)")
  dataset_lines+=("- DATASET COMPLETION MARKER: FAILED (missing completion marker)")
  run_failed=1
  completion_marker_written=0
fi

if [[ $run_failed -ne 0 ]] || [[ $completion_marker_written -eq 0 ]]; then
  stage_completion_marker="FAILED"
  ts_completion_marker="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  write_manifest "FAILED"
  failure_report="$RUN_ROOT/failure-report.md"
  {
    echo "SECTION 1: RUN FAILURE SUMMARY"
    echo "- Stage failed: completion_marker"
    echo "- Reason: .dataset-complete.properties could not be written"
    echo "- Run root: ${RUN_ROOT}"
    echo "- Execution report intentionally not written because dataset verification failed."
    echo
    echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
    printf '%s\n' "${status_lines[@]}"
    echo
    echo "SECTION 3: DATASET CONTRACT VERIFICATION"
    printf '%s\n' "${dataset_lines[@]}"
    echo
    echo "SECTION 4: EXECUTION VERDICT"
    echo "FAILED"
  } > "$failure_report"
  echo "Run failed: ${RUN_ROOT}"
  echo "Failure report: ${failure_report}"
  exit 1
fi

stage_completion_marker="PASSED"
ts_completion_marker="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

# ---------------------------------------------------------------------------
# STAGE 4: latest_run_pointer — write latest-run.properties
# ---------------------------------------------------------------------------
stage_latest_run_pointer="IN_PROGRESS"
ts_latest_run_pointer="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

pointer_contract_ok=1

if [[ ! -s "$completion_marker_path" ]]; then
  pointer_contract_ok=0
  status_lines+=("- LATEST POINTER: SKIPPED (completion marker missing: ${completion_marker_path})")
  dataset_lines+=("- LATEST POINTER: SKIPPED (latest-run.properties not updated)")
fi
if ! is_true_harness_dataset_root "$OUTPUT_ROOT" "${SCENARIOS[@]}"; then
  pointer_contract_ok=0
  status_lines+=("- LATEST POINTER: SKIPPED (selected dataset root failed strict harness contract for executed scenarios: ${OUTPUT_ROOT})")
  dataset_lines+=("- LATEST POINTER: SKIPPED (latest-run.properties not updated)")
fi
if [[ $pointer_contract_ok -eq 1 ]] && ! is_true_harness_dataset_root "$OUTPUT_ROOT" "${POINTER_EXPECTED_SCENARIOS[@]}"; then
  pointer_contract_ok=0
  status_lines+=("- LATEST POINTER: SKIPPED (selected dataset root failed strict harness contract for constrained matrix scenarios: ${OUTPUT_ROOT})")
  dataset_lines+=("- LATEST POINTER: SKIPPED (latest-run.properties not updated)")
fi

if [[ $pointer_contract_ok -eq 1 ]]; then
  pointer_tmp="${POINTER_PATH}.tmp"
  dataset_root_abs="$(realpath "$OUTPUT_ROOT")"
  created_at_ptr="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  {
    echo "run_id=${RUN_ID}"
    echo "dataset_root=${dataset_root_abs}"
    echo "created_at=${created_at_ptr}"
  } > "$pointer_tmp"
  mv "$pointer_tmp" "$POINTER_PATH"
  status_lines+=("- LATEST POINTER: UPDATED (${POINTER_PATH} -> ${dataset_root_abs})")
  dataset_lines+=("- LATEST POINTER: VERIFIED (${POINTER_EXPECTED_SCENARIOS[*]})")
  stage_latest_run_pointer="PASSED"
else
  stage_latest_run_pointer="FAILED"
  run_failed=1
fi
ts_latest_run_pointer="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_manifest "IN_PROGRESS"

if [[ $run_failed -ne 0 ]]; then
  write_manifest "FAILED"
  failure_report="$RUN_ROOT/failure-report.md"
  {
    echo "SECTION 1: RUN FAILURE SUMMARY"
    echo "- Stage failed: latest_run_pointer"
    echo "- Reason: dataset root failed strict harness contract or completion marker absent"
    echo "- Run root: ${RUN_ROOT}"
    echo "- Execution report intentionally not written because dataset verification failed."
    echo
    echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
    printf '%s\n' "${status_lines[@]}"
    echo
    echo "SECTION 3: DATASET CONTRACT VERIFICATION"
    printf '%s\n' "${dataset_lines[@]}"
    echo
    echo "SECTION 4: EXECUTION VERDICT"
    echo "FAILED"
  } > "$failure_report"
  echo "Run failed: ${RUN_ROOT}"
  echo "Failure report: ${failure_report}"
  exit 1
fi

# ---------------------------------------------------------------------------
# All required stages passed — manifest becomes READY_FOR_ANALYSIS
# (stages 5 and 6 are handled by separate analytics scripts)
# ---------------------------------------------------------------------------
write_manifest "READY_FOR_ANALYSIS"

# ---------------------------------------------------------------------------
# Write execution report (gated: only when manifest is READY_FOR_ANALYSIS)
# ---------------------------------------------------------------------------
report_tmp="$RUN_ROOT/execution-report.md"
{
  echo "SECTION 1: EXECUTION PATH USED"
  echo "- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin"
  echo "- Run root: ${RUN_ROOT}"
  echo "- Logs root: ${LOG_ROOT}"
  echo "- Outputs root: ${OUTPUT_ROOT}"
  echo "- Manifest: ${MANIFEST_PATH}"
  echo
  echo "SECTION 2: SCENARIOS EXECUTED"
  for scenario in "${SCENARIOS[@]}"; do
    echo "- ${scenario}"
  done
  echo
  echo "SECTION 3: RUNTIME SETTINGS USED"
  echo "- validationProfile=true"
  echo "- world.players=18"
  echo "- world.artifactsPerPlayer=3"
  echo "- world.sessionsPerSeason=2"
  echo "- world.seasonCount=3"
  echo "- world.encounterDensity=5"
  echo "- world.telemetrySamplingRate=0.25"
  echo
  echo "SECTION 4: PER-SCENARIO COMPLETION STATUS"
  printf '%s\n' "${status_lines[@]}"
  echo
  echo "SECTION 5: DATASET CONTRACT VERIFICATION"
  printf '%s\n' "${dataset_lines[@]}"
  echo
  echo "SECTION 6: MANIFEST STATUS"
  echo "- Manifest path: ${MANIFEST_PATH}"
  echo "- Manifest status: READY_FOR_ANALYSIS"
  echo "- Stages: matrix_execution=${stage_matrix_execution}, dataset_contract=${stage_dataset_contract}, completion_marker=${stage_completion_marker}, latest_run_pointer=${stage_latest_run_pointer}, analytics_ingestion=${stage_analytics_ingestion}, ecosystem_evaluation=${stage_ecosystem_evaluation}"
  echo
  echo "SECTION 7: EXECUTION VERDICT"
  echo "SUCCESS"
} > "$report_tmp"

cp "$report_tmp" "$BASE_ROOT/execution-report-${TIMESTAMP}.md"
echo "Run complete: ${RUN_ROOT}"
echo "Manifest: ${MANIFEST_PATH}"
echo "Execution report: ${BASE_ROOT}/execution-report-${TIMESTAMP}.md"
