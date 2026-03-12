# Phase 1.5 Outcome-Weighted Fitness Forensic Audit

## Scope and method
This audit traced concrete runtime call paths from trigger dispatch through execution, utility classification/scoring, telemetry rollup, persistence, and evolution/mutation consumers. Conclusions are based on executable code paths, not comments.

## 1) Summary of Phase-1.5 integration status
- Phase 1.5 utility model is **real and runtime-wired** for scoring, telemetry, fitness calculation, and trait adaptation.
- It is also wired into generation/mutation bias via persisted utility history.
- However, there are still integration gaps that prevent this from being fully utility-driven in all lifecycle states.

## 2) Utility model implementation and wiring
### Implemented model objects
- `UtilityOutcomeRecord`: created in `ArtifactUsageTracker.trackAbilityExecution(...)` from live `AbilityExecutionResult` + context/budget data; consumed by `ArtifactUsageProfile.recordUtilityOutcome(...)`.  
- `ValidatedOutcomeClassifier` + `UtilityScoreContext`: used inside `OutcomeUtilityProfile.ingest(...)` to classify and score each execution with no-op/suppressed/failed handling, contextual relevance, novelty, redundancy, and spam effects.  
- `OutcomeUtilityProfile` / `MechanicUtilitySignal`: accumulates attempts, meaningful outcomes, validated utility, no-op rate, spam/redundancy penalties, and density snapshots per `mechanic@trigger`.  
- `UtilityHistoryRollup`: aggregates per-artifact utility telemetry and exposes utility-based template/mechanic/trigger preference methods consumed by generation and mutation.

## 3) Does validated utility affect evolution decisions?
### Yes, in three concrete places
1. **Fitness evaluation**: `ArtifactFitnessEvaluator.evaluate(...)` explicitly weights validated utility, utility density, budget efficiency, meaningful outcome rate, contextual relevance, and penalizes no-op/spam/redundancy.  
2. **Experience-based genome adaptation**: `ExperienceEvolutionEngine.applyExperienceFeedback(...)` computes fitness from usage profile and applies normalized feedback to genome trait multipliers.  
3. **Generation/mutation bias**: `ProceduralAbilityGenerator` and `AbilityMutationEngine` parse `artifact.getLastUtilityHistory()` and bias family/template/trigger/mechanic selection via `UtilityHistoryRollup` quality/preference methods.

## 4) Are activity proxies still dominating fitness?
- Activity proxies remain present (`usageFrequency`, lifetime, kill participation), but in `ArtifactFitnessEvaluator` they are lower-weight support terms relative to utility terms and explicit penalty terms.
- Net: activity is **not the primary intended driver** in current formula, but can still influence tie-breaking and confidence behavior.

## 5) Meaningful vs no-op scoring distinction
- Distinction is explicit and runtime-enforced:
  - `AbilityExecutor` maps mechanics to `AbilityOutcomeType`; `FLAVOR_ONLY` becomes `NO_OP`, others become `SUCCESS` + meaningful.
  - `ValidatedOutcomeClassifier.score(...)` assigns direct negative scoring for no-op/suppressed/failed and positive weighted scoring for successful meaningful outcomes.

## 6) Spam/redundancy control status
- Present and active in scoring path:
  - Novelty factor decays with repeated outcome streak.
  - Redundancy penalty derives from novelty decay.
  - Spam penalty increases with recent no-op window / repeated outcomes.
  - These penalties are accumulated in `OutcomeUtilityProfile` and fed into both fitness and rollup quality scoring.

## 7) Utility density and efficiency
- Implemented metrics include:
  - `meaningfulOutcomeRate` (meaningful outcomes / attempts)
  - `utilityDensity` (validated utility / budget)
  - `utilityBudgetEfficiency` (validated utility / budget)
- These metrics are used directly in fitness and indirectly in utility-history quality scoring for mutation/generation preferences.

## 8) Analytics/telemetry integrity
### What telemetry can answer now
- High-volume low-value mechanics: `ArtifactUsageTracker.highVolumeLowValueSignals()`.
- Meaningful/no-op distributions by mechanic@trigger: `ItemAbilityManager` counters.
- Utility signal rollups per mechanic@trigger with penalties and density: `ArtifactUsageTracker.utilitySignalRollup()`.

### Durability assessment
- Durable utility telemetry exists at artifact level via encoded `lastUtilityHistory` persisted through YAML/JDBC stores.
- But many analytics counters in `ItemAbilityManager` are in-memory runtime counters, not durable historical stores.

## 9) Test coverage observed
- `ArtifactUtilityFitnessModelTest`: meaningful vs no-op, spam penalties, redundancy/diminishing returns, density/efficiency, activity-vs-utility assertions.
- `ArtifactUsageTrackerUtilityTelemetryTest`: rollup and high-volume low-value detection.
- `UtilityHistoryRollupTest`: encode/parse/hydration behavior and mutation preference fallbacks.
- `ExperienceEvolutionEngineTest`: fitness evaluation sanity and utility-first hierarchy string checks.

## 10) Top remaining defects
1. **Hydration integration gap**: `ArtifactUsageTracker.hydrateFromArtifact(...)` exists but no runtime caller was found in load path, so tracker state may cold-start after load unless rebuilt by new executions.
2. **Selection/retention coupling is indirect**: utility strongly influences genome adaptation and ability selection, but no explicit artifact-level retention/culling mechanism tied to utility was found.
3. **Analytics durability mixed**: several analytics views still rely on in-memory counters, limiting long-horizon forensic confidence unless utility history rollups are the primary source.
4. **Meaningful classification remains mechanic-mapped**: outcome meaningfulness is currently tied to static mechanic→outcome mapping in executor, not external ground-truth validation.

## Final verdict
**UTILITY-DRIVEN EVOLUTION: PARTIAL**

Reasoning: utility scoring is genuinely wired into runtime execution, telemetry, fitness, and mutation/generation bias; penalties and density are active. But end-to-end lifecycle integration remains incomplete due to hydration/load-path gap, mixed telemetry durability, and lack of explicit utility-driven retention/culling layer.
