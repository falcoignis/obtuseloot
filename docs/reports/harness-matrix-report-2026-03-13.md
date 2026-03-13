# Constrained Five-Scenario Harness Matrix Report

SECTION 1: HARNESS EXECUTION PATH USED
- Runner: `obtuseloot.simulation.worldlab.WorldSimulationRunner` via Maven Exec plugin.
- Base command path used for each scenario:
  - `mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner -Dexec.classpathScope=compile ... org.codehaus.mojo:exec-maven-plugin:3.5.0:java`
- Scenario outputs root: `analytics/validation-suite-fresh-harness/runs/<scenario>/`
- Scenario logs root: `analytics/validation-suite-fresh-harness/logs/<scenario>.log`

SECTION 2: CONSTRAINED SCENARIO MATRIX USED
- Config source (existing constrained files):
  - `analytics/validation-suite-rerun/configs/explorer-heavy.properties`
  - `analytics/validation-suite-rerun/configs/ritualist-heavy.properties`
  - `analytics/validation-suite-rerun/configs/gatherer-heavy.properties`
  - `analytics/validation-suite-rerun/configs/mixed.properties`
  - `analytics/validation-suite-rerun/configs/random-baseline.properties`
- Explicit seeds used:
  - explorer-heavy: `311001`
  - ritualist-heavy: `311002`
  - gatherer-heavy: `311003`
  - mixed: `311004`
  - random-baseline: `311005`

SECTION 3: RESOURCE / HORIZON CONSTRAINTS RESPECTED
- Validation/minimal mode:
  - `-Dworld.validationProfile=true`
  - `-Dworld.minimalReports=true`
- Safe constrained horizon settings:
  - `-Dworld.players=8`
  - `-Dworld.artifactsPerPlayer=1`
  - `-Dworld.sessionsPerSeason=3`
  - `-Dworld.seasonCount=2`
  - `-Dworld.encounterDensity=2`
- Memory guardrails:
  - `MAVEN_OPTS='-Xmx768m'`
  - `-Dworld.maxRollupHistoryInMemory=16`
  - `-Dworld.maxSeasonSnapshotsInMemory=8`
  - `-Dworld.maxTelemetryBufferEvents=128`
  - `-Dworld.telemetryArchiveBatchSize=32`
- Multiple rollup snapshots requirement:
  - produced `2` rollup snapshots per scenario.

SECTION 4: PER-SCENARIO COMPLETION STATUS
- explorer-heavy
  - command path: Maven exec + `world.scenarioConfigPath=analytics/validation-suite-rerun/configs/explorer-heavy.properties`
  - status: **COMPLETED** (`World simulation outputs written ...` in log)
  - OOM/heap instability: **none detected**
- ritualist-heavy
  - command path: Maven exec + `world.scenarioConfigPath=analytics/validation-suite-rerun/configs/ritualist-heavy.properties`
  - status: **COMPLETED**
  - OOM/heap instability: **none detected**
- gatherer-heavy
  - command path: Maven exec + `world.scenarioConfigPath=analytics/validation-suite-rerun/configs/gatherer-heavy.properties`
  - status: **COMPLETED**
  - OOM/heap instability: **none detected**
- mixed
  - command path: Maven exec + `world.scenarioConfigPath=analytics/validation-suite-rerun/configs/mixed.properties`
  - status: **COMPLETED**
  - OOM/heap instability: **none detected**
- random-baseline
  - command path: Maven exec + `world.scenarioConfigPath=analytics/validation-suite-rerun/configs/random-baseline.properties`
  - status: **COMPLETED**
  - OOM/heap instability: **none detected**

SECTION 5: OUTPUT ARTIFACT COMPLETENESS
- Required artifacts present for all 5 scenarios:
  - `world-sim-data.json`
  - `rollup-snapshots.json`
  - `telemetry/ecosystem-events.log`
  - `telemetry/rollup-snapshot.properties`
  - `scenario-metadata.properties`
- Validation/minimal outputs produced:
  - `world-sim-report.md`
  - `world-sim-meta-shifts.md` (validation placeholder)
  - `world-sim-balance-findings.md` (validation placeholder)
- Rollup snapshot count per scenario:
  - explorer-heavy: `2`
  - ritualist-heavy: `2`
  - gatherer-heavy: `2`
  - mixed: `2`
  - random-baseline: `2`

SECTION 6: OPERATIONAL FAILURES OR INSTABILITY
- One preliminary high-load attempt (before final constrained matrix) with larger settings (`players=40, artifactsPerPlayer=2, sessionsPerSeason=8, seasonCount=3, encounterDensity=5`) was interrupted after oversized telemetry growth and non-completion in practical runtime.
- No `OutOfMemoryError` signatures were observed in final matrix logs.
- Final constrained matrix runs were stable and completed end-to-end.

SECTION 7: TINY FIXES MADE (IF ANY)
- No source-code changes were required to run the constrained matrix.
- Tiny run-enabling adjustment: reduced runtime knobs (player/session/encounter/memory limits + lower heap cap) to ensure deterministic completion and artifact generation.

SECTION 8: HARNESS READINESS VERDICT
- **STABLE**
- Verdict basis: all 5 constrained scenarios completed and produced required harness artifacts without OOM/heap failures.
