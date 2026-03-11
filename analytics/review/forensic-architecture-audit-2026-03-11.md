# ObtuseLoot Forensic Architecture Audit (Two-Pass)

Date: 2026-03-11

Scope: runtime plugin path (`src/main/java`) + tests (`src/test/java`) + telemetry/report wiring.

Method: call-path tracing from Bukkit/Paper event entry points through dispatch, budgeting, execution, telemetry, and persistence.

## Pass 1 (Recent architecture wave)

### Inventory & integration status

- Stable ability IDs + mechanic enums: **Integrated** in registry/definitions/dispatch result schemas.
- Structured execution result model (`AbilityExecutionResult`/`AbilityDispatchResult`): **Integrated but shallow** (no mechanic-side effects beyond text + one listener-side harvest action).
- Trigger budget manager/profiles/policies: **Integrated** into dispatch and pre-probe flow, but telemetry semantics are coarse and partly conflated.
- Non-combat listener + chunk-aware context + coalescer: **Integrated** on movement/interact paths.
- Structure sensing + chunk PDC cache: **Integrated** but with risky structure-type resolution and likely false negatives.
- Memory-trigger emission throttling: **Integrated** for kill/fusion/awakening chain, but not broadly instrumented.
- Trigger subscription index: **Integrated** for join/lazy rebuild and dispatch fast path.
- Text identity/personality/voice system: **Integrated for presentation**, not mechanic-driving.
- Artifact naming/rank/discovery model: **Partially integrated** due dual-name truth and rank string heuristics.
- Paper API wrappers: **Partially integrated**; some wrappers are practical, others add complexity without strong runtime payoff.

### High-risk gaps in recent changes

1. Structured execution exists, but `AbilityExecutor` does not apply real in-world effects for most mechanics; it mostly classifies outcomes and emits strings. Runtime behavior therefore diverges from telemetry labels.
2. Trigger analytics are in-memory and only exported by one report writer at startup/shutdown; there is no periodic flush or durable runtime series.
3. Name identity has split truth (`ArtifactNaming.displayName` vs `generatedName` snapshot), creating stale identifiers in index bindings and persistence representations.
4. Structure lookup uses reflective `locateNearestStructure` with the first enum constant, not a curated set, making “structure sense” semantically unstable.
5. Budget suppression and outcome counters can overrepresent dispatch attempts relative to meaningful effects because ATTEMPTED is always recorded before execution.

## Pass 2 (Full-system)

### Runtime truth summary

- Runtime entry points are centralized in Bukkit listeners (`CombatCore`, `EventCore`, `NonCombatAbilityListener`) and route into `ArtifactProcessor` then `ItemAbilityManager`.
- Dispatch/budget bookkeeping is centralized; effect realization is mostly not.
- Evolution and genome systems are rich, but they optimize over usage/memory proxies more than verified mechanic success in live runtime.
- Dashboard metrics are assembled mainly from static `analytics/*.json|*.md` artifacts, not live dispatch streams.

### Sources of truth and drift risks

- Artifact identity: owner UUID + `artifactStorageKey`; item-PDC stores both, but resolve path only reads storage key.
- Name/text identity: `ArtifactNaming`/text resolver; duplicate `generatedName` cache can drift.
- Ability truth: registry templates + generated `AbilityDefinition`s + trigger index snapshots.
- Budget truth: in-memory `TriggerBudgetManager` pools.
- Analytics truth: in-memory counters + report files; no persistent runtime event log.

### Systemic findings

- Ability system supports stable IDs and modern dispatch semantics, but ability mechanics are not first-class effect handlers yet.
- Evolution pipeline appears sophisticated but uses indirect fitness/pressure signals; opportunity/outcome mismatch remains possible.
- Tests are broad numerically but many “integration” tests are source-string assertions or synthetic unit tests detached from Bukkit runtime.

## Immediate corrective priorities

1. Introduce real per-mechanic effect executors (world/action handlers) and align `AbilityExecutionResult` with actual side effects.
2. Add durable telemetry pipeline (periodic flush + versioned schema) for trigger seen/suppressed/attempted/no-op/success/meaningful, keyed by stable IDs.
3. Eliminate name dual-truth by making one canonical field and deriving all secondary views.
4. Harden structure sensing: explicit structure type set, deterministic API path, measurable cache hit/miss telemetry.
5. Add end-to-end integration tests with mocked Bukkit/Paper adapters for event->dispatch->budget->effect->analytics validation.

