SECTION 1: EXECUTION PATH USED
- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin
- Run root: analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb
- Logs root: analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs
- Outputs root: analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb

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

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs/explorer-heavy.log)
- ritualist-heavy: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs/ritualist-heavy.log)
- gatherer-heavy: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs/gatherer-heavy.log)
- mixed: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs/mixed.log)
- random-baseline: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/logs/random-baseline.log)
- DATASET COMPLETION MARKER: WRITTEN (analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb/.dataset-complete.properties)
- LATEST POINTER: UPDATED (analytics/validation-suite/latest-run.properties -> /home/user/obtuseloot/analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb)

SECTION 5: DATASET CONTRACT VERIFICATION
- explorer-heavy: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- ritualist-heavy: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- gatherer-heavy: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- mixed: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- random-baseline: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- DATASET COMPLETION MARKER: VERIFIED (explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)
- LATEST POINTER: VERIFIED (explorer-heavy ritualist-heavy gatherer-heavy mixed random-baseline)

SECTION 6: EXECUTION VERDICT
SUCCESS
