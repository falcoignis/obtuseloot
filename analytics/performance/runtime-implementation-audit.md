# ObtuseLoot Runtime Integration Audit

## 1. Runtime Trigger Budget Enforcement

### Real runtime trigger paths traced

| Source | Runtime event/source | Emitted trigger(s) | Dispatch entrypoint | Live runtime? | Budget enforcement |
|---|---|---|---|---|---|
| `events/NonCombatAbilityListener#onMove` | `PlayerMoveEvent` crossing chunk boundary | `ON_WORLD_SCAN` | `ArtifactProcessor.processAbilityTriggerWithResult` | Yes | Listener-side probe gate (`allowProbe`) before scheduling + central dispatch budget (`EventAbilityDispatcher.executeWithBudget`) |
| `events/NonCombatAbilityListener#scheduleChunkAwareSense` | Coalesced scheduled task | `ON_WORLD_SCAN`, `ON_STRUCTURE_SENSE` | `ArtifactProcessor.processAbilityTriggerWithResult` | Yes | `ON_STRUCTURE_SENSE` also probe-gated in listener; both then centrally budget-gated per ability in dispatcher |
| `events/NonCombatAbilityListener#onInteract` | block/air interaction | `ON_SOCIAL_INTERACT`, `ON_RITUAL_INTERACT`, `ON_BLOCK_INSPECT` | `ArtifactProcessor.processAbilityTriggerWithResult` | Yes | No listener probe, but centrally budget-gated per ability in dispatcher |
| `events/NonCombatAbilityListener#onEntityInspect` | entity interaction | `ON_ENTITY_INSPECT`, `ON_WITNESS_EVENT` | `ArtifactProcessor.processAbilityTriggerWithResult` | Yes | Centrally budget-gated in dispatcher |
| `events/NonCombatAbilityListener#onHarvest` | crop break | `ON_BLOCK_HARVEST` | `ArtifactProcessor.processAbilityTriggerWithResult` | Yes | Centrally budget-gated in dispatcher; harvest replay keyed off dispatch result mechanic/status |
| `combat/CombatCore#onCombatHit` -> `ArtifactProcessor.processCombat` | combat hit | `ON_HIT`, `ON_CHAIN_COMBAT`, plus `ON_MOVEMENT`/`ON_REPOSITION` and `ON_LOW_HEALTH` via context bonuses | `ArtifactProcessor.triggerAbility` | Yes | No probe gate; centrally budget-gated in dispatcher |
| `events/EventCore#onEntityDeath` -> `ArtifactProcessor.processKill` | kill event | `ON_KILL`, `ON_DRIFT_MUTATION`, `ON_AWAKENING`, `ON_FUSION`, and `ON_MOVEMENT`/`ON_REPOSITION` via kill-context bonus | `ArtifactProcessor.triggerAbility` | Yes | Centrally budget-gated in dispatcher |
| `ArtifactProcessor.recordMemoryEvent` during kill/boss/multikill/awakening/fusion | memory-event engine emission | `ON_MEMORY_EVENT` | `ArtifactProcessor.triggerAbility` | Yes | Memory emission throttled by `ArtifactMemoryEngine.shouldEmitMemoryTrigger`; then centrally budget-gated in dispatcher |
| `ArtifactProcessor.processSimulated*` debug/sim paths | simulation commands | same trigger families (`ON_HIT`, `ON_KILL`, `ON_MULTI_KILL`, `ON_BOSS_KILL`, etc.) | `ArtifactProcessor.triggerAbility` | Runtime path for debug tooling | Centrally budget-gated in dispatcher |

### Budget integration verdict

- **Budget is on the real dispatch path**: every trigger that reaches `ItemAbilityManager.resolveDispatch` is processed through `EventAbilityDispatcher.executeWithBudget`, which does both `preCheck` and `consumeActivation` before execution.
- **Expensive-listener pre-gating is partial/ad hoc**: only movement/structure probing uses listener-level `allowProbe` short-circuiting; interactive and combat/kill listeners do not pre-gate at listener layer.
- **No obvious central bypass in traced paths**: all traced trigger emission methods call `ArtifactProcessor.triggerAbility`/`processAbilityTriggerWithResult`, which route into `resolveDispatch` and dispatcher budget checks.

## 2. Suppression Analytics Identity Integrity

### What is stable and correct

- `AbilityExecutionResult` carries **stable identifiers**: `abilityId`, enum `mechanic`, enum `trigger`, artifact key, holder UUID.
- Manager execution analytics keys are built from enum identities (`mechanic.name()`, `trigger`) rather than display strings.
- Gentle-harvest follow-up checks use enum `AbilityMechanic.HARVEST_RELAY`, not text.

### Remaining integrity defects

- Suppression analytics are still coarse and partially textified:
  - `ItemAbilityManager.suppressionReasonCounts` stores only suppression string values (e.g., `trigger-budget-...`) without mechanic/ability dimensions.
  - `TriggerBudgetManager.suppressionCounts` is keyed only by `TriggerSuppressionReason.name()`.
  - `EventAbilityDispatcher.suppressed(...)` converts enum reason into prefixed string in `suppressionReason`.
- Result: suppression is not natively attributed by **stable mechanic ID + trigger + reason** in a single structured suppression metric.

## 3. Outcome Semantics Integrity

### Distinctions implemented

- System has explicit statuses: `TRIGGER_SEEN`, `ATTEMPTED`, `SUPPRESSED`, `NO_OP`, `SUCCESS`, `FAILED`.
- Dispatch flow records `TRIGGER_SEEN` at event ingress, logs `ATTEMPTED` before budget checks, then records actual result status from execution/suppression.
- `AbilityExecutionResult` also carries `outcomeType` and `meaningfulOutcome` boolean.

### Collapsing/semantic weakness found

- `AbilityExecutor` determines `SUCCESS` vs `NO_OP` from **mechanic class mapping only**, not actual world/context outcome checks.
- For mechanics mapped to non-flavor outcomes (e.g., `HARVEST_RELAY`, `SENSE_PING`, `INSIGHT_REVEAL`), execution returns `SUCCESS`/meaningful by default, even when runtime conditions may produce no practical effect.
- Therefore, no-op vs meaningful separation exists structurally but is only **partially grounded in runtime reality**.

## 4. Memory Trigger Reality Check

- `ON_MEMORY_EVENT` is genuinely emitted in runtime:
  - `ArtifactProcessor.recordMemoryEvent(...)` calls memory engine record/profile and conditionally emits `ON_MEMORY_EVENT`.
  - This method is invoked from real kill/awakening/fusion/multikill/boss-kill flows.
- Emission is throttled/coalesced at memory engine level by `shouldEmitMemoryTrigger` using last event/time/pressure snapshot.
- Once emitted, memory triggers go through normal dispatch and budget checks (`resolveDispatch` -> `executeWithBudget`).
- Execution analytics use the same structured result path as all other triggers.
- Caveat: meaningful/no-op semantics for memory abilities still depend on mechanic mapping, not deep runtime target validation.

## 5. Mechanic Gating Integrity

- Gentle Harvest gating is currently mechanic-based in hot path:
  - harvest listener checks `dispatchResult.hasSuccessfulMechanic(AbilityMechanic.HARVEST_RELAY)` before replay/replant.
- No `effect.contains(...)`/display-name/lore-string mechanic gate was found in the traced runtime harvest path.
- String presentation text exists for UI/lore output, but not as the primary branch condition for harvest replay gating.

## 6. Highest-Risk Defects

1. **Suppression attribution is not dimensioned by stable mechanic/ability identity.**
   - Distorts tuning because operators cannot directly answer “which mechanics are budget-starved most by trigger + reason.”
2. **Meaningful outcome inference is mostly static (mechanic-class based), not runtime-validated.**
   - Inflates “success/meaningful” for contextually empty executions and undermines policy optimization.
3. **Listener-side pre-budgeting is inconsistent across trigger sources.**
   - Movement/structure has probe gating, but other high-frequency/intentional paths rely only on per-ability dispatcher checks, reducing upstream load-shedding consistency.

## 7. Precision Fix Recommendations

1. Add a structured suppression metric keyed by:
   - `abilityId`, `mechanic`, `trigger`, `TriggerSuppressionReason`, optional `policy`, optional `runtimeContext.intentional`.
   - Keep human-readable strings as derived views only.
2. Introduce executor-level runtime outcome contracts per mechanic:
   - Return typed “applied/no-target/no-change” outcomes from mechanic handlers.
   - Map to `SUCCESS` vs `NO_OP` based on real effect, not only mechanic class.
3. Normalize pre-dispatch load-shedding policy:
   - either consistently apply lightweight probe gating for selected noisy sources,
   - or document/centralize why only certain listeners probe-precheck.

## Confidence Summary

- **Trigger budget applied to real runtime triggers: YES**

  The traced runtime emission paths (combat, kill, non-combat interactions, chunk-aware sensing, and memory emissions) all route into `ItemAbilityManager.resolveDispatch` and then `EventAbilityDispatcher.executeWithBudget`, which performs both pre-check and activation budget gating before execution. Some sources also add listener-level probe checks, but central gating is consistently present on the dispatch path.

- **Suppression analytics keyed by stable mechanic IDs: PARTIAL**

  Execution records carry stable IDs (ability ID + enum mechanic/trigger), and many metrics use those keys. However, suppression counters are largely reason-string keyed and not consistently joined with stable mechanic/ability identity in suppression-specific aggregates, which limits causal tuning fidelity.

- **Meaningful outcomes separated from no-ops: PARTIAL**

  The type system and counters distinguish trigger seen / attempted / suppressed / no-op / success and include a meaningful flag. But current executor logic infers meaningfulness from mechanic category rather than runtime world effect checks, so semantically meaningful vs no-op is only partially truthful in practice.
