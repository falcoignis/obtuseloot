SECTION 1: RUN FAILURE SUMMARY
- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin
- Run root: analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708
- Outputs root: analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708
- Execution report intentionally not written because dataset verification failed.

SECTION 2: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: FAILED (exit code 1; log=analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708/logs/explorer-heavy.log)
- ritualist-heavy: FAILED (exit code 1; log=analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708/logs/ritualist-heavy.log)
- gatherer-heavy: FAILED (exit code 1; log=analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708/logs/gatherer-heavy.log)
- mixed: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708/logs/mixed.log)
- random-baseline: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/archive-fix-rerun-20260315-040708/logs/random-baseline.log)
- explorer-heavy: FAILED post-run verification (scenario did not complete successfully)
- ritualist-heavy: FAILED post-run verification (scenario did not complete successfully)
- gatherer-heavy: FAILED post-run verification (scenario did not complete successfully)

SECTION 3: DATASET CONTRACT VERIFICATION
- explorer-heavy: FAILED dataset verification (harness command failed)
- ritualist-heavy: FAILED dataset verification (harness command failed)
- gatherer-heavy: FAILED dataset verification (harness command failed)
- mixed: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- random-baseline: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- explorer-heavy: FAILED dataset verification (scenario not completed)
- ritualist-heavy: FAILED dataset verification (scenario not completed)
- gatherer-heavy: FAILED dataset verification (scenario not completed)

SECTION 4: EXECUTION VERDICT
FAILED
