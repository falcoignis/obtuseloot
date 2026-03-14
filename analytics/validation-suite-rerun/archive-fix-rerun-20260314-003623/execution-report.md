SECTION 1: EXECUTION PATH USED
- Repository root: /workspace/ObtuseLoot
- Harness entrypoint: obtuseloot.simulation.worldlab.WorldSimulationRunner via Maven exec plugin
- Run root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623
- Logs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623/logs
- Outputs root: analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623/runs

SECTION 2: SCENARIOS EXECUTED
1. explorer-heavy
2. ritualist-heavy
3. gatherer-heavy
4. mixed
5. random-baseline

SECTION 3: RUNTIME SETTINGS USED
- world.validationProfile=true
- world.players=18
- world.artifactsPerPlayer=3
- world.sessionsPerSeason=2
- world.seasonCount=3
- world.encounterDensity=5
- world.telemetrySamplingRate=0.25
- world.scenarioConfigPath=analytics/validation-suite/configs/<scenario>-run.properties

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS (log: logs/explorer-heavy.log)
- ritualist-heavy: SUCCESS (log: logs/ritualist-heavy.log)
- gatherer-heavy: SUCCESS (log: logs/gatherer-heavy.log)
- mixed: SUCCESS (log: logs/mixed.log)
- random-baseline: SUCCESS (log: logs/random-baseline.log)

SECTION 5: SCENARIO-LOCAL OUTPUT ROOTS POPULATED
- explorer-heavy: runs/explorer-heavy populated with scenario-metadata.properties, world-sim-data.json, rollup-snapshots.json, telemetry/ecosystem-events.log, telemetry/rollup-snapshot.properties
- ritualist-heavy: runs/ritualist-heavy populated with scenario-metadata.properties, world-sim-data.json, rollup-snapshots.json, telemetry/ecosystem-events.log, telemetry/rollup-snapshot.properties
- gatherer-heavy: runs/gatherer-heavy populated with scenario-metadata.properties, world-sim-data.json, rollup-snapshots.json, telemetry/ecosystem-events.log, telemetry/rollup-snapshot.properties
- mixed: runs/mixed populated with scenario-metadata.properties, world-sim-data.json, rollup-snapshots.json, telemetry/ecosystem-events.log, telemetry/rollup-snapshot.properties
- random-baseline: runs/random-baseline populated with scenario-metadata.properties, world-sim-data.json, rollup-snapshots.json, telemetry/ecosystem-events.log, telemetry/rollup-snapshot.properties

SECTION 6: RUNTIME STABILITY
- No run aborted.
- No OutOfMemoryError observed in scenario logs.
- Telemetry buffer remained stable in all scenarios (world-sim-data telemetry.buffer_dropped=0; telemetry.buffer_max=512).
- Heap monitor lines present across runs with bounded usage and completion.

SECTION 7: EXECUTION VERDICT
SUCCESS
