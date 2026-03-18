SECTION 1: EXECUTION PATH USED
- Harness: obtuseloot.simulation.worldlab.WorldSimulationRunner (Maven exec)
- Run root: analytics/validation-suite-rerun/deep-lineage-divergence-test

SECTION 2: RUNTIME SETTINGS (DEEP RUN)
- validationProfile=true
- world.players=18
- world.artifactsPerPlayer=3
- world.sessionsPerSeason=4
- world.seasonCount=10
- world.encounterDensity=5
- world.telemetrySamplingRate=0.25

SECTION 3: SCENARIOS
- explorer-heavy
- ritualist-heavy
- gatherer-heavy
- mixed
- random-baseline

SECTION 4: COMPLETION STATUS
- explorer-heavy: SUCCESS
- ritualist-heavy: SUCCESS
- gatherer-heavy: SUCCESS
- mixed: SUCCESS
- random-baseline: SUCCESS

SECTION 5: VERDICT
SUCCESS
