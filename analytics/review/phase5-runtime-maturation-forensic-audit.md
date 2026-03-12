# Phase 5 Runtime Maturation Forensic Audit

## Scope and Method
This audit traces executable runtime paths from Bukkit/Paper event listeners through ability dispatch, utility tracking, telemetry buffering, archive persistence, and rollup generation. Conclusions are based on code paths and tests, not comments.

## 1) Phase 5 Integration Summary
- Runtime telemetry is integrated into ability execution, niche classification changes, lineage mutations/branching, and competition allocation.
- Telemetry persistence exists as an append-only log (`analytics/telemetry/ecosystem-events.log`) with batched writes.
- Rollups are generated from in-memory aggregation buffer snapshots.
- Performance controls exist (trigger subscription index, trigger budgets, coalescing, chunk-key throttles, cache/backoff structures).
- Ability expansion is integrated into runtime triggers and utility telemetry for non-combat mechanics.
- Parameter registry loads coefficients from config and is consumed by runtime evolution formulas.

Overall: implemented, but with notable schema/coverage/runtime-gaps.

## 2) Where Telemetry Is Emitted (Traced Runtime)
Primary runtime path:
1. `NonCombatAbilityListener` receives runtime events (`onMove`, `onInteract`, `onEntityInspect`, `onHarvest`) and calls `ArtifactProcessor.processAbilityTriggerWithResult(...)`.
2. `ArtifactProcessor.triggerAbility(...)` constructs `AbilityEventContext` and delegates to `ItemAbilityManager.resolveDispatch(...)`.
3. `EventAbilityDispatcher` executes definitions and returns `AbilityExecutionResult` entries.
4. `ItemAbilityManager.recordUtilityOutcomes(...)` forwards each executed result into `ArtifactUsageTracker.trackAbilityExecution(...)`.
5. `ArtifactUsageTracker.trackAbilityExecution(...)` emits `ABILITY_EXECUTION` telemetry via `EcosystemTelemetryEmitter.emit(...)`.
6. `EcosystemTelemetryEmitter.emit(...)` forwards to `TelemetryAggregationService.record(...)`.
7. `TelemetryAggregationService.record(...)` enqueues in `TelemetryAggregationBuffer` and flushes to `EcosystemHistoryArchive` in batches.

Additional emitters:
- `NichePopulationTracker.recordTelemetry(...)` emits `NICHE_CLASSIFICATION_CHANGE` when dominant niche changes.
- `LineageRegistry.assignLineage(...)` emits `LINEAGE_UPDATE`.
- `LineageRegistry.recordDescendantBias(...)` emits `MUTATION_EVENT` and `BRANCH_FORMATION`.
- `AdaptiveSupportAllocator.allocateFor(...)` emits `COMPETITION_ALLOCATION`.

## 3) Telemetry Schema Population Findings
Persisted event envelope fields are:
- `timestampMs`, `type`, `artifactSeed`, `lineageId`, `niche`, `attributes`.

Requested schema compatibility:
- Present by mapping:
  - `artifact_id` -> `artifactSeed`
  - `lineage_id` -> `lineageId`
  - `niche` -> `niche`
  - `trigger`/`mechanic` -> attributes on `ABILITY_EXECUTION`
- Missing or not consistently populated:
  - `execution_status` (code stores `status`, different key name)
  - `utility_score` (not emitted)
  - `ecology_pressure` (only emitted on `MUTATION_EVENT` as `ecologicalPressure`)
  - `lineage_momentum` (not emitted)
  - `mutation_influence` (emitted only on `MUTATION_EVENT`)
  - `artifact_id` naming mismatch (`artifactSeed`)

Conclusion: telemetry is runtime-real, but schema alignment is partial and field naming is inconsistent.

## 4) Telemetry Persistence Behavior
What exists:
- `EcosystemHistoryArchive.append(...)` writes append-only encoded lines.
- `TelemetryAggregationService.flush()` drains bounded batch size and appends.
- Plugin shutdown (`onDisable`) calls `ecosystemTelemetryEmitter.flush()`.

Restart / reload durability:
- Archive file persists on disk and can be read via `readAll()`.
- Runtime aggregation buffer/rollups are reinitialized at startup; no replay of archived data into buffer on enable.

Tick I/O pressure:
- Hot path uses in-memory queue/counters.
- Disk write occurs only on flush batch threshold or explicit flush.
- However, scheduled async task currently calls `rollups().maybeRun(...)` directly, not `scheduledRollupTick(...)`; so periodic flush is not guaranteed unless batch threshold is reached.

## 5) Rollup Generation Verification
- `ScheduledEcosystemRollups.run(...)` derives:
  - `NichePopulationRollup` from `TelemetryAggregationBuffer.nichePopulationSnapshot()`
  - `LineagePopulationRollup` from `lineagePopulationSnapshot()`
  - `EcosystemSnapshot.eventCounts` from `typeCountsSnapshot()`
- This is buffer-derived and avoids raw archive rescans.
- Rollup scheduling is batched via `minIntervalMs` and async scheduler.

Gap:
- Rollup payload lacks requested metrics like utility density, branch counts, and carrying-capacity utilization.

## 6) Performance Optimization Verification
Verified bounded/runtime-safe mechanisms:
- Trigger subscription index avoids full ability scans in normal path.
- Trigger budget manager gates probe/execution volume.
- Work coalescing for move/scan events prevents repeated chunk work.
- Structure sensing uses chunk-local signal checks + TTL cache + locate API failure backoff.
- Telemetry uses lock-free queue and counter snapshots; archive writes are batched.

Risk areas:
- `ArtifactRuntimeCache.cleanup()` scans entire cache on each `getOrCompute` call; bounded by max entries but still O(n) per lookup.
- `LineageCompetitionModel.evaluate(...)` iterates all lineages when building support allocations; not tick-driven everywhere but potentially expensive if invoked frequently under large lineage cardinality.

## 7) Ability Expansion Verification
Integrated abilities:
- Registry now includes 18 non-combat templates across world scan, structure sense, ritual/social/block/entity/memory triggers.
- Runtime listeners fire these triggers in real gameplay interactions.
- Outcomes feed utility scoring (`ArtifactUsageProfile.recordUtilityOutcome`) and telemetry (`ABILITY_EXECUTION`).
- Niche mapping is active through `NichePopulationTracker` + `EcosystemRoleClassifier` from utility signals.

Meaningful outcomes:
- At least one mechanic (`HARVEST_RELAY`) performs concrete world effect (crop replant) via listener post-processing.
- Many mechanics currently produce classified outcome types/presentation text rather than direct world mutations; telemetry records them as meaningful by outcome type mapping.

Vanilla duplication check:
- No direct enchantment/potion application path was found in this non-combat runtime path.

## 8) Parameter Registry Behavior
- `EvolutionParameterRegistry` loads profile coefficients from `config.yml` and exposes atomic active profile.
- `ExperienceEvolutionEngine` reads registry profile at runtime for ecology modifier and mutation amplitude clamping.
- No automatic self-tuning loop was found that mutates profile values during runtime.

Gap:
- `driftWindowDurationTicks` and `competitionReinforcementCurve` are loaded but not consumed in runtime formulas.

## 9) Test Coverage Findings
Present tests:
- Telemetry emission + archive persistence + buffer rollup sourcing (`EcosystemTelemetryPipelineTest`).
- Performance architecture regressions (`PerformanceArchitectureRegressionTest`).
- Registry reload behavior (`EvolutionParameterRegistryTest`).
- Ability pool/trigger integration (`NonCombatAbilityIntegrationTest`).

Missing or weak:
- No plugin lifecycle integration test proving telemetry replay/continuity across full plugin reload with rollups restored.
- No test that verifies full requested telemetry schema keys are emitted with exact names.
- No test asserting rollups include utility density/branch counts/capacity utilization in telemetry rollup artifacts.

## Remaining Defects / Risks
1. Telemetry schema mismatch and partial population for requested forensic fields.
2. `ROLLUP_GENERATED` event type exists but is not emitted by rollup scheduler.
3. Periodic telemetry flush path appears bypassed by scheduler wiring (`maybeRun` called directly).
4. Rollup model limited to population/event counts (no utility density / capacity utilization / branch counts).
5. Two registry parameters are currently inert (`driftWindowDurationTicks`, `competitionReinforcementCurve`).

## Final Verdict
**PHASE 5 STATUS: PARTIAL**

Reasoning:
- Runtime telemetry, persistence, rollups, performance controls, and ability expansion are genuinely wired into live execution paths.
- But schema completeness, rollup richness, scheduled persistence guarantees, and some parameter usage remain incomplete.
