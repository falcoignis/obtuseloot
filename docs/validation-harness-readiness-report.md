# Validation Harness Readiness Report

## SECTION 1: HARNESS ENTRYPOINTS

- Java CLI entrypoint: `obtuseloot.simulation.worldlab.WorldSimulationRunner#main` builds `WorldSimulationConfig` from system properties and invokes `WorldSimulationHarness#runAndWriteOutputs`.
- Secondary Java CLI entrypoint: `obtuseloot.simulation.worldlab.OpenEndednessTestRunner#main` also constructs `WorldSimulationConfig` and invokes `WorldSimulationHarness#runAndWriteOutputs`.
- Shell harness workflow: `scripts/run-world-lab-validation.sh` compiles and repeatedly invokes `WorldSimulationRunner` via Maven exec.

## SECTION 2: VALIDATION CONFIGURATION PATHS

- Validation mode flag is part of `WorldSimulationConfig` as `validationProfile`.
- `WorldSimulationRunner` maps `-Dworld.validationProfile` into config.
- Harness consumes validation mode directly (`config.validationProfile()`) for reduced in-memory sizes, telemetry buffering, and report behavior.
- Scenario configuration path is also wired (`world.scenarioConfigPath`) and loaded via `EvolutionExperimentConfig.load(...)` before execution.

## SECTION 3: MINIMAL OUTPUT VALIDATION

Validation profile behavior is explicitly enforced in `WorldSimulationHarness#writeReports`:

- Writes reduced JSON payload (`world`, `player`, `artifact`, `ability`, `lineage`, `simulation_scenario`, telemetry/rollups, `validation_profile=true`).
- Intentionally omits broad/heavy sections present in full mode (e.g., `seasonal_snapshots`, `phase6_experiment_outputs`, visualization/report fanout).
- Emits placeholder lightweight markdown for:
  - `world-sim-meta-shifts.md` (disabled heavy narrative)
  - `world-sim-balance-findings.md` (disabled heavy balance findings)
- Returns early, preventing generation of additional heavy/nested artifact trees.

Automated validation test coverage confirms this:

- `WorldSimulationHarnessValidationProfileTest#validationProfileProducesRequiredArtifactsAndRemainsAnalyticsIngestible` asserts output existence and checks `"validation_profile": true` and absence of `phase6_experiment_outputs`.

## SECTION 4: LIFECYCLE TELEMETRY PATHS

Lifecycle telemetry fields are present and emitted:

- Event emission path: `LineageRegistry` converts `BranchLifecycleTransition` into telemetry attributes, including:
  - `lifecycle_state`
  - `survival_score`
  - `maintenance_cost`
  - collapse transition marker (`event=branch-collapsed`) and `collapse_reason`
- Schema availability path: `TelemetryFieldContract` includes canonical lifecycle/collapse fields (`survival_score`, `maintenance_cost`, `lifecycle_state`, `lifecycle_from`, `collapse_reason`, etc.).
- Aggregation path: `TelemetryAggregationBuffer` reads `survival_score`, `maintenance_cost`, and collapse/lifecycle states for rollups.
- Test verification path: `BranchCollapsePressureLifecycleTest#collapseLifecycleSignalsAreEmittedToTelemetry` asserts collapsed events and lifecycle field presence.

## SECTION 5: CONFIGURATION READINESS VERDICT

**READY**

Rationale:

1. Harness can run in validation mode (flag is defined, wired, and validated by dedicated tests).
2. Minimal artifact generation is explicitly enforced (placeholder reports + early return in validation profile branch).
3. Lifecycle telemetry is emitted with required branch lifecycle, survival, maintenance, and collapse transition fields and validated in tests.
