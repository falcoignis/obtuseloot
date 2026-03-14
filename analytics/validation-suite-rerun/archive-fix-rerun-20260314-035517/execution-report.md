SECTION 1: EXECUTION PATH USED
- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin
- Run root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-035517
- Logs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-035517/logs
- Outputs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-035517/runs

SECTION 2: SCENARIOS EXECUTED
- explorer-heavy

SECTION 3: RUNTIME SETTINGS USED
- validationProfile=true
- world.players=18
- world.artifactsPerPlayer=3
- world.sessionsPerSeason=2
- world.seasonCount=3
- world.encounterDensity=5
- world.telemetrySamplingRate=0.25

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/archive-fix-rerun-20260314-035517/logs/explorer-heavy.log)
- LATEST POINTER: SKIPPED (missing full dataset root contract: ritualist-heavy gatherer-heavy mixed random-baseline)

SECTION 5: DATASET CONTRACT VERIFICATION
- explorer-heavy: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- LATEST POINTER: SKIPPED (latest-run.properties not updated)

SECTION 6: EXECUTION VERDICT
SUCCESS
