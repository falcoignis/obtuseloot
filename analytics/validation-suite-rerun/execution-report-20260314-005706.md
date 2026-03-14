SECTION 1: EXECUTION PATH USED
- Harness entrypoint: `obtuseloot.simulation.worldlab.WorldSimulationRunner` via Maven exec plugin.
- Run root: `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706`
- Logs root: `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/logs`
- Outputs root: `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs`

SECTION 2: SCENARIOS EXECUTED
- explorer-heavy
- ritualist-heavy
- gatherer-heavy
- mixed
- random-baseline

SECTION 3: RUNTIME SETTINGS USED
- validationProfile=true
- world.players=18
- world.artifactsPerPlayer=3
- world.sessionsPerSeason=2
- world.seasonCount=3
- world.encounterDensity=5
- world.telemetrySamplingRate=0.25
- world.scenarioConfigPath=analytics/validation-suite/configs/<scenario>-run.properties

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: completed (process exit code 0; log includes `World simulation outputs written to .../runs/explorer-heavy`).
- ritualist-heavy: completed (process exit code 0; log includes `World simulation outputs written to .../runs/ritualist-heavy`).
- gatherer-heavy: completed (process exit code 0; log includes `World simulation outputs written to .../runs/gatherer-heavy`).
- mixed: completed (process exit code 0; log includes `World simulation outputs written to .../runs/mixed`).
- random-baseline: completed (process exit code 0; log includes `World simulation outputs written to .../runs/random-baseline`).

SECTION 5: SCENARIO-LOCAL OUTPUT ROOTS POPULATED
- explorer-heavy: `runs/explorer-heavy` populated with `scenario-metadata.properties`, `world-sim-data.json`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`.
- ritualist-heavy: `runs/ritualist-heavy` populated with `scenario-metadata.properties`, `world-sim-data.json`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`.
- gatherer-heavy: `runs/gatherer-heavy` populated with `scenario-metadata.properties`, `world-sim-data.json`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`.
- mixed: `runs/mixed` populated with `scenario-metadata.properties`, `world-sim-data.json`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`.
- random-baseline: `runs/random-baseline` populated with `scenario-metadata.properties`, `world-sim-data.json`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`.
- Shared-root telemetry output was not used as success criteria.

SECTION 6: RUNTIME STABILITY
- No OutOfMemoryError detected in any scenario log.
- No runtime exceptions or Maven build failures detected in scenario logs.
- Memory checkpoints remained bounded across runs (heap_used_mb observed below heap_max_mb=4096 in all scenarios).
- Telemetry buffer remained stable (`telemetry_buffer_size=0` at logged checkpoints).
- Scenario-local telemetry archives emitted and non-empty for all scenarios (`telemetry/ecosystem-events.log` present with >50MB each).

SECTION 7: EXECUTION VERDICT
SUCCESS
