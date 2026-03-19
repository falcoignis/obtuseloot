# Deep Long-Horizon Post-Ability-Expansion Fresh Validation

- Requested fresh run id: `deep-long-horizon-post-ability-fresh`.
- Dataset root created: `analytics/validation-suite-rerun/deep-long-horizon-post-ability-fresh`.
- Outcome: the fresh run did **not** complete, so freshness could not be proven for a full 32-window post-expansion ecological assessment.

## SECTION 1: DATASET FRESHNESS CHECK
- New dataset root was created under a distinct run id and new manifest file was written.
- Freshness proof failed because the harness did not reach `READY_FOR_ANALYSIS`; current manifest status is `IN_PROGRESS`.
- Only partial artifacts exist for `explorer-heavy` at the time execution was stopped (`telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`), while required scenario outputs such as `rollup-snapshots.json` were never produced.
- Because the run never completed the first scenario, artifact timestamps and metadata do **not** prove a valid full post-expansion dataset.
- Per the task requirement, analysis stops here.

## SECTION 2: SCENARIO SUMMARY TABLE
| Scenario | Status | Notes |
| --- | --- | --- |
| explorer-heavy | INCOMPLETE | Harness was interrupted before `rollup-snapshots.json` and scenario completion marker were produced. |
| ritualist-heavy | NOT STARTED | Blocked behind incomplete first scenario. |
| gatherer-heavy | NOT STARTED | Blocked behind incomplete first scenario. |
| mixed | NOT STARTED | Blocked behind incomplete first scenario. |
| random-baseline | NOT STARTED | Blocked behind incomplete first scenario. |

## SECTION 3: TIME-SERIES SUMMARY
- No valid 32-window fresh dataset exists yet, so no time-series ecological summary can be reported.

## SECTION 4: PER-SCENARIO TABLES
- Not generated because the required fresh dataset contract was not satisfied.

## SECTION 5: LINEAGE TURNOVER ANALYSIS
- Not generated because `rollup-snapshots.json` was not produced for the fresh run.

## SECTION 6: STRUCTURAL BEHAVIOR
- Not generated because the fresh run never reached an analyzable state.

## SECTION 7: COMPARISON VS BASELINE
- Baseline source remains `docs/deep-long-horizon-validation.md`.
- Comparison is intentionally **not** performed because the post-expansion side is incomplete; using old artifacts here would violate the prompt.

## SECTION 8: SUCCESS CRITERIA EVALUATION
- `>=3 scenarios have zero zero-share windows`: **not evaluated**.
- `>=2 scenarios maintain >3% child share long-term`: **not evaluated**.
- `no scenario permanently collapses after bifurcation`: **not evaluated**.
- `lineage turnover remains bounded`: **not evaluated**.
- `no convergence to a single dominant niche`: **not evaluated**.

### Execution notes
- Compile succeeded before the run attempt.
- Fresh run command executed: `bash scripts/run-deep-validation.sh --run-id deep-long-horizon-post-ability-fresh`.
- The harness remained inside `explorer-heavy` long enough to emit memory checkpoints but did not complete the scenario. Last observed log lines:
  - `Picked up JAVA_TOOL_OPTIONS: `
  - `[world-sim][memory] generation=4 heap_used_mb=250 heap_max_mb=4096 telemetry_buffer_size=0 telemetry_total_recorded=88652 in_memory_rollup_count=1 active_artifact_count=54 lineage_count=35 branch_count=24`
  - `[world-sim][memory] generation=8 heap_used_mb=45 heap_max_mb=4096 telemetry_buffer_size=0 telemetry_total_recorded=264579 in_memory_rollup_count=2 active_artifact_count=54 lineage_count=35 branch_count=27`

LONG_HORIZON_RESULT: FAILED
