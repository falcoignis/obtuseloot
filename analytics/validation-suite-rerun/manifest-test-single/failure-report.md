SECTION 1: RUN FAILURE SUMMARY
- Stage failed: latest_run_pointer
- Reason: dataset root failed strict harness contract or completion marker absent
- Run root: analytics/validation-suite-rerun/manifest-test-single
- Execution report intentionally not written because dataset verification failed.

SECTION 2: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS (exit code 0; log=analytics/validation-suite-rerun/manifest-test-single/logs/explorer-heavy.log)
- DATASET COMPLETION MARKER: WRITTEN (analytics/validation-suite-rerun/manifest-test-single/.dataset-complete.properties)
- LATEST POINTER: SKIPPED (selected dataset root failed strict harness contract for constrained matrix scenarios: analytics/validation-suite-rerun/manifest-test-single)

SECTION 3: DATASET CONTRACT VERIFICATION
- explorer-heavy: VERIFIED (telemetry/ecosystem-events.log telemetry/rollup-snapshot.properties rollup-snapshots.json scenario-metadata.properties)
- DATASET COMPLETION MARKER: VERIFIED (explorer-heavy)
- LATEST POINTER: SKIPPED (latest-run.properties not updated)

SECTION 4: EXECUTION VERDICT
FAILED
