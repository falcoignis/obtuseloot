# Phase 5 Runtime Maturation Forensic Audit (Runtime-Path Verified)

Repository: `ObtuseLoot`
Scope: telemetry emission/persistence, rollups, performance bounds, ability expansion, parameter registry, and tests.
Method: traced executable runtime paths from event listeners through ability dispatch, utility tracking, telemetry buffering, rollup scheduling, and archive/snapshot persistence.

## 1) Phase 5 integration summary

**Status: PARTIAL**

Phase 5 runtime capabilities are integrated and active in production paths:

- Runtime abilities are dispatched from real Bukkit events and routed through `ArtifactProcessor -> ItemAbilityManager -> EventAbilityDispatcher`.
- Ability execution telemetry and ecosystem telemetry events are emitted and normalized into the Phase 5 schema.
- Telemetry is buffered, batch-flushed to append-only archive, and periodically rolled up.
- Rollups are generated from in-memory telemetry aggregation snapshots (not archive re-scans).
- Startup rehydration restores state from snapshot first, otherwise bounded recent archive replay.
- Parameter registry feeds runtime coefficients and telemetry scheduling controls.

However, **important Phase 5 risks remain**:

- Some runtime paths still do linear scans over active artifact maps (bounded by active set, but not O(1)).
- Ability expansion is mostly informational/social signaling, with only narrow direct world-state mutation (crop replant) on the live listener path.
- Runtime config reload command does not currently reload the parameter registry profile.

## 2) Where telemetry is emitted

### Runtime emission path

1. Non-combat runtime events trigger `ArtifactProcessor.processAbilityTriggerWithResult(...)`.
2. `ItemAbilityManager.resolveDispatch(...)` runs indexed/full dispatch and records per-execution outcomes.
3. `ArtifactUsageTracker.trackAbilityExecution(...)` emits `ABILITY_EXECUTION` telemetry with utility + runtime context fields.
4. `EcosystemTelemetryEmitter.emit(...)` forwards to `TelemetryAggregationService.record(...)`.
5. `TelemetryAggregationBuffer.enqueue(...)` updates live counters/snapshots and pending queue.
6. `TelemetryAggregationService.flush()/flushAll()` drains queue to `EcosystemHistoryArchive.append(...)`.

### Additional runtime telemetry emitters

- `LineageRegistry` emits:
  - `LINEAGE_UPDATE` (assignment/update)
  - `MUTATION_EVENT` (descendant bias record)
  - `BRANCH_FORMATION` (new branch)
  with lineage-derived `branch_divergence` and `specialization_trajectory` enrichment.
- `NichePopulationTracker` emits `NICHE_CLASSIFICATION_CHANGE` on dominant niche transition.
- `AdaptiveSupportAllocator` emits `COMPETITION_ALLOCATION` when adaptive support is computed.
- `TelemetryAggregationService.scheduledRollupTick(...)` emits `ROLLUP_GENERATED` after successful rollup.

### Schema population check (requested key fields)

Observed populated on runtime emitters (direct or normalized):

- `artifact_id`, `artifact_seed` (factory)
- `lineage_id`
- `trigger`
- `mechanic`
- `niche`
- `execution_status`
- `utility_score`
- `ecology_pressure`
- `lineage_momentum`
- `mutation_influence`

No Phase 5 schema field was found to be permanently dead in current runtime emitters; previously risky fields (`branch_divergence`, `specialization_trajectory`) are now emitted on lineage/niche/allocation paths.

## 3) Telemetry persistence behavior

Telemetry persistence is implemented as:

- **In-memory queue + aggregates:** `TelemetryAggregationBuffer`
- **Append-only durable log:** `EcosystemHistoryArchive.append(...)`
- **Snapshot persistence:** `TelemetryRollupSnapshotStore.write(...)`

Lifecycle:

- Runtime scheduling calls `TelemetryFlushScheduler.run() -> emitter.scheduledTick(...)` asynchronously.
- Each scheduled tick flushes a batch and maybe runs rollup.
- Plugin shutdown calls `ecosystemTelemetryEmitter.flushAll()` to drain remaining queue.

Restart/reload survival:

- On startup, `TelemetryAggregationService.initializeFromHistory()` uses `RollupStateHydrator`:
  - snapshot restore first (`rehydrated_snapshot`), else
  - replay recent archive window (`rehydrated_replay`).

Risk:

- `EcosystemHistoryArchive.readRecent(...)` currently calls `readAll()` then slices tail, so replay fallback performs full-file read before truncation for large archives.

## 4) Rollup generation implementation

Rollups are generated from telemetry aggregation snapshots in `ScheduledEcosystemRollups.run(...)`:

- niche population
- meaningful outcome counts
- niche utility density
- saturation/opportunity/specialization pressure
- lineage population
- branch count
- lineage utility density
- lineage momentum
- specialization trajectory
- drift window remaining
- branch divergence
- ecosystem-level carrying-capacity utilization proxy / diversity / turnover

Scheduling/batching:

- `maybeRun(minIntervalMs)` gates rollup frequency.
- Rollups execute during scheduled flush ticks, not on every ability execution.
- `ROLLUP_GENERATED` event is appended when a rollup is actually generated.

Conclusion: rollups are telemetry-buffer-derived and interval-batched, not computed by re-scanning historical archive each time.

## 5) Performance optimizations verified

### Verified optimizations in runtime path

- Trigger subscription indexing avoids repeated full profile scans on dispatch (`TriggerSubscriptionIndex` + indexed bindings).
- Trigger work coalescing in `NonCombatAbilityListener` reduces redundant move-trigger work.
- Structure sensing is throttled and cached (`STRUCTURE_THROTTLE_MS`, chunk cache TTLs, locate failure backoff).
- Telemetry writes are queue + batch append (no per-event synchronous disk write in dispatch path).
- Runtime utility signal cache (`ArtifactRuntimeCache`) bounds entry count and idle lifetime.

### Residual bounded-but-linear work

- `NichePopulationTracker.rollups()` iterates active artifacts/signals and is used by pressure/support computations.
- `ScheduledEcosystemRollups.run(...)` traverses snapshot maps linearly with current active dimensions.

These are outside the per-event archive I/O path, but they are still O(active artifacts / map size) operations.

## 6) Ability expansion verification

Ability pool expansion exists and is wired into runtime triggers through listener-driven dispatch (world scan, structure sense, inspect, ritual/social interaction, harvest, witness/memory).

Integration checks:

- Abilities are selected by procedural resolver and dispatched by trigger.
- Utility scoring records outcome relevance/budget for each execution.
- Niche classification consumes utility signals and emits niche-change telemetry.
- Ability execution telemetry includes mechanic/trigger/outcome/utility context.

Meaningful runtime outcomes:

- `HARVEST_RELAY` path yields a direct world effect (replant after harvest) when successful.
- Several other mechanics map to informational/social signal outcome types; these are runtime-visible but less materially mutative.

Vanilla duplication check:

- No direct potion-effect/enchantment application found on the Phase 5 non-combat runtime path.

## 7) Parameter registry behavior

`EvolutionParameterRegistry` is config-backed and loaded during plugin enable. Runtime systems consume profile coefficients for:

- saturation sensitivity
- lineage momentum influence
- mutation amplitude bounds
- drift window duration
- competition reinforcement curve
- telemetry flush/archive/rollup/rehydration intervals

No autonomous self-tuning loop that rewrites coefficients at runtime was found.

Risk:

- Debug reload path reloads config/settings but does not call parameter-registry reload, so updated coefficients may not take effect until full plugin restart.

## 8) Tests coverage snapshot

Existing tests that cover requested Phase 5 concerns:

- telemetry emission + schema normalization + required fields
- rollup generation and expanded rollup metrics
- rehydration from persisted snapshot
- periodic flush persistence behavior
- lineage telemetry field propagation (`branch_divergence`, `specialization_trajectory`)
- non-combat ability pool/trigger integration
- parameter registry load/reload behavior
- basic runtime/perf architecture guards for non-combat listener and structure sensing

No new tests were added in this audit commit.

## 9) Remaining defects / risks

1. `readRecent()` full-read fallback can degrade startup rehydration performance for very large archive files.
2. Active-artifact rollup/pressure computations remain linear in active set size (bounded but not constant time).
3. Parameter registry is not refreshed in the debug reload command path.
4. Ability runtime impact is uneven: one clear direct world-state effect (`HARVEST_RELAY`) versus several largely informational/social outcomes.

---

## Final verdict

**PHASE 5 STATUS: PARTIAL**

Reasoning:

- Telemetry is operational, schema-normalized, and emitted in live execution paths.
- Persistence and rollups are real and integrated, with rollups sourced from live buffer snapshots.
- Performance work added meaningful protections (indexing/coalescing/caching/batching), but not all heavy work is fully decoupled from active-set linear scans.
- Ability expansion is integrated into execution + telemetry + utility/niche systems, but runtime impact is currently skewed toward observability/signaling over broad mechanical world effects.
- Parameter registry is correctly central for coefficients and does not self-tune, but live reload wiring is incomplete.
