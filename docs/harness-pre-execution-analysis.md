# Harness Pre-Execution Analysis

## SECTION 1: VALIDATION FLAGS VERIFIED

All required validation flags are accepted as JVM system properties by `WorldSimulationRunner` and passed into `WorldSimulationConfig`:

- `world.validationProfile` → toggles validation profile mode.
- `world.players` → `playerCount`.
- `world.artifactsPerPlayer` → artifacts generated per player.
- `world.sessionsPerSeason` → inner seasonal session loop count.
- `world.seasonCount` → outer simulation season loop count.
- `world.encounterDensity` → encounters generated per simulated session.

Runtime consumption in harness:

- `validationProfile` is used to reduce in-memory telemetry/rollup/snapshot limits and to skip heavy report generation for validation speed.
- `players` and `artifactsPerPlayer` bound simulated population size (`playerCount * artifactsPerPlayer`, with scenario-derived floor).
- `sessionsPerSeason` and `seasonCount` define total round count (`sessionsPerSeason * seasonCount`).
- `encounterDensity` bounds per-session encounter loop work.

Validation configuration evidence also exists in tests (`WorldSimulationHarnessValidationProfileTest`) which builds config with validation profile enabled and asserts required output artifacts.

## SECTION 2: STABILITY CONTROLS VERIFIED

### Required

- `world.telemetrySamplingRate`: **Supported**.
  - Read via `System.getProperty("world.telemetrySamplingRate", "1.0")` and clamped by `clampSamplingRate(...)`.
  - Applied when constructing `EcosystemTelemetryEmitter`, reducing telemetry emission volume proportionally.
  - Runtime effect: lowers event pressure in telemetry buffer/archive, helping stability and bounded IO.

### Optional (if available)

- `world.branchLifecycleCoalescing`: **Not found** (no parsing or usage in runner/harness).
- `world.branchLifecycleEvaluationInterval`: **Not found** (no parsing or usage in runner/harness).

Branch lifecycle analytics are produced (`branch_lifecycle_timeline` and pruning diagnostics), but no runtime flag-level lifecycle coalescing/evaluation interval controls are currently exposed.

## SECTION 3: TARGET VALIDATION SETTINGS

Recommended bounded validation configuration:

- `world.players=18`
- `world.artifactsPerPlayer=3`
- `world.sessionsPerSeason=2`
- `world.seasonCount=3`
- `world.encounterDensity=5`
- `world.telemetrySamplingRate=0.25`
- `world.validationProfile=true`

Why this remains bounded:

- Total seasonal rounds remain finite: `2 * 3 = 6` rounds.
- Approximate encounter operations are finite: `players * artifactsPerPlayer * sessionsPerSeason * seasonCount * encounterDensity`.
- Telemetry sampling at `0.25` probabilistically cuts emitted telemetry events to ~25% of full emission volume.

## SECTION 4: HARNESS RUN READINESS

**READY**

- Required validation flags are implemented and used.
- Required stability control (`world.telemetrySamplingRate`) is implemented and actively wired.
- Validation profile behavior exists and is covered by tests for output/ingestibility.
- Optional branch lifecycle runtime controls are not currently exposed, but this does not block validation-matrix execution with bounded settings.
