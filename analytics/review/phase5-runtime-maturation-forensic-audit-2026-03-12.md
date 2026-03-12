# Phase 5 Runtime Maturation Forensic Audit (2026-03-12)

## Scope and method
- Traced runtime call paths from event listeners and combat processors through ability dispatch, telemetry emission, telemetry buffering, rollup scheduling, and archive persistence.
- Verified behavior from executable code paths, not comments.
- Confirmed with targeted tests: telemetry pipeline, non-combat ability integration, performance regression checks, and parameter registry tests.

## 1) Telemetry schema emission in real runtime
### Runtime path verified
1. Runtime triggers are fired from listener and processor paths (e.g., movement/inspect/ritual/harvest and combat).
2. `ArtifactProcessor.processAbilityTriggerWithResult(...)` routes events to `ItemAbilityManager.resolveDispatch(...)`.
3. `ItemAbilityManager.recordUtilityOutcomes(...)` calls `ArtifactUsageTracker.trackAbilityExecution(...)` for each execution.
4. `ArtifactUsageTracker.trackAbilityExecution(...)` emits `ABILITY_EXECUTION` via `EcosystemTelemetryEmitter.emit(...)`.
5. `EcosystemTelemetryEmitter.emit(...)` records through `TelemetryAggregationService.record(...)`.
6. `TelemetryEventFactory` + `TelemetryFieldContract.normalize(...)` enforce schema keys and default `na` values.

### Schema fields populated by runtime emitters
- Always populated by factory: `timestamp`, `artifact_seed`, `artifact_id`, `lineage_id` (or `na`), `niche` (or `na`).
- Ability execution populates: `trigger`, `mechanic`, `ability_id`, `execution_status`, `utility_score`, `utility_density`, `budget_cost`, `chunk`, `player_id`, `context_tags`.
- Lineage mutation path populates: `drift_window_remaining`, `utility_density`, `ecology_pressure`, `mutation_influence`.
- Competition allocation path populates: `reinforcement_multiplier`, `lineage_momentum`, `ecology_pressure`, `opportunity_share`, `specialization_pressure`.
- Niche reclassification path populates: `subniche`, `specialization_pressure`.

### Schema fields defined but currently not populated by runtime emitters
- `generation`
- `world`
- `dimension`
- `branch_divergence` (read by rollups but never emitted)
- `ROLLUP_GENERATED` event type exists but is never emitted

## 2) Telemetry persistence behavior
- Persistence exists as an append-only log (`analytics/telemetry/ecosystem-events.log`) through `EcosystemHistoryArchive.append(...)`.
- Tick/runtime path writes are buffered first (`TelemetryAggregationBuffer` queue) and persisted in batches (`archiveBatchSize`) on flush.
- Flush scheduling is asynchronous via `TelemetryFlushScheduler` and a Bukkit async repeating task.
- Shutdown path calls `flushAll()` ensuring queued telemetry is persisted before disable.

### Durability caveats
- Telemetry log survives restart/plugin reload because file is append-only on disk.
- In-memory aggregates/rollups are **not rehydrated** from archive at startup (no startup `readAll()` replay), so post-restart rollup state starts cold.
- Debug reload command reloads config/runtime settings but does not reload the parameter registry.

## 3) Rollup generation implementation
- Rollups are derived from `TelemetryAggregationBuffer` snapshots (`nichePopulationSnapshot`, `lineagePopulationSnapshot`, utility/momentum distributions, etc.), not by rescanning archive files.
- `ScheduledEcosystemRollups.maybeRun(nowMs)` enforces interval batching (`telemetryRollupIntervalMs`) and runs after a flush tick.
- Snapshot includes required metrics: niche population, utility density, lineage population, branch counts, carrying-capacity utilization, diversity, turnover.

### Caveat
- Rollups are currently held in memory via `AtomicReference`; there is no persisted rollup store.

## 4) Performance optimization verification
### Bounded/optimized runtime behavior observed
- Trigger dispatch uses `TriggerSubscriptionIndex` to avoid full ability scans on each trigger.
- Non-combat trigger work is coalesced (`TriggerWorkCoalescer`) and movement probes are thresholded and chunk-key-gated.
- Structure sensing applies throttle/backoff logic and cached chunk signal checks.
- Utility signal recomputation is cached (`ArtifactRuntimeCache`) with TTL and max entries.
- Telemetry writes are lightweight enqueue operations with batched archive flushes.
- Rollups are scheduled outside the immediate trigger path and computed from maintained buffer aggregates.

### Remaining risk
- `NichePopulationTracker.rollups()` iterates across active artifacts and all their signal maps when requested; it is bounded to active runtime set, but can still grow with active population size.

## 5) Ability expansion verification
- New non-combat triggers are wired to real events (`ON_WORLD_SCAN`, `ON_STRUCTURE_SENSE`, `ON_BLOCK_HARVEST`, `ON_ENTITY_INSPECT`, `ON_BLOCK_INSPECT`, `ON_RITUAL_INTERACT`, `ON_SOCIAL_INTERACT`, `ON_WITNESS_EVENT`).
- Ability templates include expanded mechanics (`SENSE_PING`, `NAVIGATION_ANCHOR`, `HARVEST_RELAY`, `INSIGHT_REVEAL`, `RITUAL_CHANNEL`, `SOCIAL_ATTUNEMENT`).
- Procedural generation integrates ecology/lineage pressures and utility history into selection scores and mutation paths.
- Execution outcomes are not purely cosmetic for several mechanics: crop replant/navigation/information/structure/memory/social signal outcome types are represented and fed into utility telemetry.

### Caveat
- Runtime `AbilityExecutor` currently maps mechanics to abstract `AbilityOutcomeType` and text, with limited direct world-state side effects in executor itself (notably harvest replant is handled externally by listener logic).

## 6) Parameter registry behavior
- Central registry exists (`EvolutionParameterRegistry`) and loads coefficients from config (`ecosystem.parameters.*`).
- Runtime systems read registry values for ecology modifier sensitivity, lineage momentum influence, mutation amplitude bounds, telemetry cadence, batch size, drift window, and reinforcement curve.
- No automatic self-tuning loops were found that mutate registry coefficients during runtime simulation.

### Caveat
- Debug reload currently does not call `EvolutionParameterRegistry.reload(...)`, so runtime registry changes from config are not applied by `/obtuseloot debug reload`.

## 7) Test coverage status
### Present
- Telemetry emission + schema + periodic flush + rollup metrics + bounded aggregation checks.
- Ability expansion integration checks for trigger set and metadata.
- Performance architecture regression checks around non-combat listener and structure-sense guardrails.
- Parameter registry load/reload unit test.

### Missing / weak
- No end-to-end restart test that replays persisted telemetry archive into runtime rollup state (and code currently does not rehydrate).
- No plugin reload test asserting telemetry continuity and parameter-registry reload wiring.
- No runtime test asserting `branch_divergence` emission (currently absent).
- No test validating consumer use of `TelemetryAggregationAnalytics` in dashboard/runtime outputs.

## Final verdict
**PHASE 5 STATUS: PARTIAL**

Reasoning:
- Core runtime telemetry, batching, persistence, and rollups are genuinely wired into live execution paths.
- Performance controls (indexing/coalescing/buffering) are materially present and exercised.
- Ability expansion is integrated into runtime triggers and utility telemetry.
- However, observability is incomplete for parts of the declared schema (`world`, `dimension`, `generation`, `branch_divergence`) and rollups are not rehydrated from persisted telemetry across restart, limiting continuity.
