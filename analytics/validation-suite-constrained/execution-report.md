SECTION 1: EXECUTION COMMANDS USED
- Build validation: `mvn -q -DskipTests compile`
- Harness command template: `mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner -Dexec.classpathScope=compile -Dworld.outputDirectory=<scenario-dir> -Dworld.validationProfile=true -Dworld.telemetrySamplingRate=0.25 -Dworld.players=18 -Dworld.artifactsPerPlayer=3 -Dworld.sessionsPerSeason=2 -Dworld.seasonCount=3 -Dworld.encounterDensity=5 -Dworld.scenarioConfigPath=analytics/validation-suite-rerun/configs/<scenario>.properties org.codehaus.mojo:exec-maven-plugin:3.5.0:java`

SECTION 2: SCENARIOS EXECUTED
- explorer-heavy
- ritualist-heavy
- gatherer-heavy
- mixed
- random-baseline

SECTION 3: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS (exit_code=0, log=analytics/validation-suite-constrained/logs/explorer-heavy.log)
- ritualist-heavy: SUCCESS (exit_code=0, log=analytics/validation-suite-constrained/logs/ritualist-heavy.log)
- gatherer-heavy: SUCCESS (exit_code=0, log=analytics/validation-suite-constrained/logs/gatherer-heavy.log)
- mixed: SUCCESS (exit_code=0, log=analytics/validation-suite-constrained/logs/mixed.log)
- random-baseline: SUCCESS (exit_code=0, log=analytics/validation-suite-constrained/logs/random-baseline.log)

SECTION 4: TELEMETRY SAMPLING CONFIRMATION
- explorer-heavy: metadata telemetry_sampling_rate=0.25, world-sim-data telemetry.sampling_rate=0.25, rollup_history_windows=3
- ritualist-heavy: metadata telemetry_sampling_rate=0.25, world-sim-data telemetry.sampling_rate=0.25, rollup_history_windows=3
- gatherer-heavy: metadata telemetry_sampling_rate=0.25, world-sim-data telemetry.sampling_rate=0.25, rollup_history_windows=3
- mixed: metadata telemetry_sampling_rate=0.25, world-sim-data telemetry.sampling_rate=0.25, rollup_history_windows=3
- random-baseline: metadata telemetry_sampling_rate=0.25, world-sim-data telemetry.sampling_rate=0.25, rollup_history_windows=3

SECTION 5: RUNTIME STABILITY
- explorer-heavy: errors=none, telemetry.buffer_dropped=0, telemetry.buffer_max=512 => stable
- ritualist-heavy: errors=none, telemetry.buffer_dropped=0, telemetry.buffer_max=512 => stable
- gatherer-heavy: errors=none, telemetry.buffer_dropped=0, telemetry.buffer_max=512 => stable
- mixed: errors=none, telemetry.buffer_dropped=0, telemetry.buffer_max=512 => stable
- random-baseline: errors=none, telemetry.buffer_dropped=0, telemetry.buffer_max=512 => stable

SECTION 6: EXECUTION VERDICT
SUCCESS
