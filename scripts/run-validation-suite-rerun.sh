#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BASE_ROOT="analytics/validation-suite-rerun"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RUN_ID="archive-fix-rerun-${TIMESTAMP}"
SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
POINTER_PATH="analytics/validation-suite/latest-run.properties"
POINTER_EXPECTED_SCENARIOS=(explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)

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
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

RUN_ROOT="$BASE_ROOT/$RUN_ID"
LOG_ROOT="$RUN_ROOT/logs"
OUTPUT_ROOT="$RUN_ROOT/runs"
mkdir -p "$LOG_ROOT" "$OUTPUT_ROOT"

mvn -q -DskipTests compile

required_artifacts=(
  "telemetry/ecosystem-events.log"
  "telemetry/rollup-snapshot.properties"
  "rollup-snapshots.json"
  "scenario-metadata.properties"
)

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
    continue
  fi

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

  if [[ $exit_code -ne 0 ]]; then
    status_lines+=("- ${scenario}: FAILED (exit code ${exit_code}; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (harness command failed)")
    run_failed=1
    continue
  fi

  expected_log_marker="World simulation outputs written to ${scenario_root}"
  if ! grep -Fq "$expected_log_marker" "$scenario_log"; then
    status_lines+=("- ${scenario}: FAILED (harness exited 0 but completion marker not found in log; expected '${expected_log_marker}'; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (missing harness completion marker)")
    run_failed=1
    continue
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
    continue
  fi

  if [[ ${#stale[@]} -gt 0 ]]; then
    status_lines+=("- ${scenario}: FAILED (harness exited 0 but required artifacts were stale from an earlier run: ${stale[*]}; log=${scenario_log})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (stale artifacts ${stale[*]})")
    run_failed=1
    continue
  fi

  status_lines+=("- ${scenario}: SUCCESS (exit code 0; log=${scenario_log})")
  dataset_lines+=("- ${scenario}: VERIFIED (${required_artifacts[*]})")
  completed_scenarios+=("$scenario")
done

# Re-verify all expected scenario roots right before writing a success report so
# the execution report cannot be emitted if required dataset artifacts disappear,
# output roots are deleted, or files were written outside scenario-local roots.
if [[ ! -d "$OUTPUT_ROOT" ]]; then
  status_lines+=("- RUN ROOT CHECK: FAILED (output root missing at report time: ${OUTPUT_ROOT})")
  dataset_lines+=("- RUN ROOT CHECK: FAILED dataset verification (output root missing)")
  run_failed=1
fi

for scenario in "${SCENARIOS[@]}"; do
  scenario_root="$OUTPUT_ROOT/$scenario"
  if [[ ! -d "$scenario_root" ]]; then
    status_lines+=("- ${scenario}: FAILED post-run verification (scenario root missing at report time: ${scenario_root})")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (scenario root missing)")
    run_failed=1
    continue
  fi

  if [[ ! " ${completed_scenarios[*]} " =~ " ${scenario} " ]]; then
    status_lines+=("- ${scenario}: FAILED post-run verification (scenario did not complete successfully)")
    dataset_lines+=("- ${scenario}: FAILED dataset verification (scenario not completed)")
    run_failed=1
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
    run_failed=1
  fi
done

if [[ $run_failed -eq 0 ]]; then
  verdict="SUCCESS"
else
  verdict="FAILED"
fi

if [[ $run_failed -eq 0 ]]; then
  pointer_missing=()
  if [[ ! -d "$OUTPUT_ROOT" ]]; then
    pointer_missing+=("run output root")
  fi

  for scenario in "${POINTER_EXPECTED_SCENARIOS[@]}"; do
    if [[ ! -d "$OUTPUT_ROOT/$scenario" ]]; then
      pointer_missing+=("$scenario")
    fi
  done

  if [[ ${#pointer_missing[@]} -eq 0 ]]; then
    pointer_tmp="${POINTER_PATH}.tmp"
    created_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    {
      echo "run_id=${RUN_ID}"
      echo "dataset_root=${OUTPUT_ROOT}"
      echo "created_at=${created_at}"
    } > "$pointer_tmp"
    mv "$pointer_tmp" "$POINTER_PATH"
  else
    status_lines+=("- LATEST POINTER: SKIPPED (missing full dataset root contract: ${pointer_missing[*]})")
    dataset_lines+=("- LATEST POINTER: SKIPPED (latest-run.properties not updated)")
  fi

  report_tmp="$RUN_ROOT/execution-report.md"
  {
    echo "SECTION 1: EXECUTION PATH USED"
    echo "- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin"
    echo "- Run root: ${RUN_ROOT}"
    echo "- Logs root: ${LOG_ROOT}"
    echo "- Outputs root: ${OUTPUT_ROOT}"
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
    echo "SECTION 6: EXECUTION VERDICT"
    echo "$verdict"
  } > "$report_tmp"

  cp "$report_tmp" "$BASE_ROOT/execution-report-${TIMESTAMP}.md"
  echo "Run complete: ${RUN_ROOT}"
  echo "Execution report: ${BASE_ROOT}/execution-report-${TIMESTAMP}.md"
else
  failure_report="$RUN_ROOT/failure-report.md"
  {
    echo "SECTION 1: RUN FAILURE SUMMARY"
    echo "- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin"
    echo "- Run root: ${RUN_ROOT}"
    echo "- Outputs root: ${OUTPUT_ROOT}"
    echo "- Execution report intentionally not written because dataset verification failed."
    echo
    echo "SECTION 2: PER-SCENARIO COMPLETION STATUS"
    printf '%s\n' "${status_lines[@]}"
    echo
    echo "SECTION 3: DATASET CONTRACT VERIFICATION"
    printf '%s\n' "${dataset_lines[@]}"
    echo
    echo "SECTION 4: EXECUTION VERDICT"
    echo "$verdict"
  } > "$failure_report"
  echo "Run failed: ${RUN_ROOT}"
  echo "Failure report: ${failure_report}"
  exit 1
fi
