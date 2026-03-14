SECTION 1: EXECUTION PATH USED
- Preparation checks: `scripts/run-validation-suite-rerun.sh` completion-marker + post-run root re-verification logic and `WorldSimulationRunner` output marker were verified before run.
- Harness entrypoint: `obtuseloot.simulation.worldlab.WorldSimulationRunner` via Maven exec plugin.
- Logs root: `analytics/validation-suite-constrained/logs`
- Scenario outputs root: `analytics/validation-suite-constrained/runs`

SECTION 2: SCENARIOS EXECUTED
- explorer-heavy
- ritualist-heavy
- gatherer-heavy
- mixed
- random-baseline

SECTION 3: RUNTIME SETTINGS USED
- world.validationProfile=true
- world.players=18
- world.artifactsPerPlayer=3
- world.sessionsPerSeason=2
- world.seasonCount=3
- world.encounterDensity=5
- world.telemetrySamplingRate=0.25

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy: SUCCESS
- ritualist-heavy: SUCCESS
- gatherer-heavy: SUCCESS
- mixed: SUCCESS
- random-baseline: SUCCESS

SECTION 5: SCENARIO OUTPUT ROOTS CREATED
- explorer-heavy: analytics/validation-suite-constrained/runs/explorer-heavy
- ritualist-heavy: analytics/validation-suite-constrained/runs/ritualist-heavy
- gatherer-heavy: analytics/validation-suite-constrained/runs/gatherer-heavy
- mixed: analytics/validation-suite-constrained/runs/mixed
- random-baseline: analytics/validation-suite-constrained/runs/random-baseline

SECTION 6: RUNTIME STABILITY
- explorer-heavy: telemetry.buffer_dropped=0, telemetry.buffer_max=512, telemetry.sampling_rate=0.25, ecosystem-events.log.bytes=50643204 => stable
- ritualist-heavy: telemetry.buffer_dropped=0, telemetry.buffer_max=512, telemetry.sampling_rate=0.25, ecosystem-events.log.bytes=50708696 => stable
- gatherer-heavy: telemetry.buffer_dropped=0, telemetry.buffer_max=512, telemetry.sampling_rate=0.25, ecosystem-events.log.bytes=51227555 => stable
- mixed: telemetry.buffer_dropped=0, telemetry.buffer_max=512, telemetry.sampling_rate=0.25, ecosystem-events.log.bytes=56102811 => stable
- random-baseline: telemetry.buffer_dropped=0, telemetry.buffer_max=512, telemetry.sampling_rate=0.25, ecosystem-events.log.bytes=51434730 => stable

SECTION 7: EXECUTION VERDICT
SUCCESS
