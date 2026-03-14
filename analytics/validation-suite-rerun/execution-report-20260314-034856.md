SECTION 1: EXECUTION PATH USED
- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin
- Run root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-034856
- Logs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-034856/logs
- Outputs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-034856/runs

SECTION 2: SCENARIOS EXECUTED
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
- random-baseline: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/archive-fix-rerun-20260314-034856/logs/random-baseline.log)
- LATEST POINTER: SKIPPED (missing full dataset root contract: explorer-heavy ritualist-heavy gatherer-heavy mixed)

SECTION 5: DATASET CONTRACT VERIFICATION
- random-baseline: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- LATEST POINTER: SKIPPED (latest-run.properties not updated)

SECTION 6: EXECUTION VERDICT
SUCCESS
