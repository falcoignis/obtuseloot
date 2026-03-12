# Phase 1 Forensic Audit: Outcome-Weighted Fitness Runtime Integration

## Verdict
**Rating: PARTIAL**

Phase 1 utility modeling is wired into live execution and into genome feedback used during ability profile generation, but it is **not broadly integrated into mutation selection/retention systems outside that path**, and telemetry remains mostly in-memory runtime counters rather than durable persisted utility history.

## Key Runtime Call Path (verified)
1. Trigger dispatch starts in `ItemAbilityManager.resolveDispatch`.
2. Dispatcher executes ability attempts with budget checks (`EventAbilityDispatcher.executeWithBudget`).
3. `AbilityExecutor.execute` emits structured `AbilityExecutionResult` with status + outcome type + meaningful flag.
4. `ItemAbilityManager.recordUtilityOutcomes` forwards each execution to `ArtifactUsageTracker.trackAbilityExecution`.
5. `ArtifactUsageTracker` creates `UtilityOutcomeRecord` and calls `ArtifactUsageProfile.recordUtilityOutcome`.
6. `OutcomeUtilityProfile.ingest` classifies + scores with `ValidatedOutcomeClassifier`, accumulating utility, density, spam, redundancy, no-op rate.
7. `ArtifactFitnessEvaluator.evaluate` consumes those utility metrics (plus some activity terms) and applies penalties.
8. `ExperienceEvolutionEngine.applyExperienceFeedback` uses fitness to alter genome trait multipliers.
9. `ProceduralAbilityGenerator.generate` uses that evolved genome before selecting ability templates.

## Primary Question Findings

### 1) Does validated utility influence evolution/selection/retention?
- **Yes for experience-driven genome feedback**: validated utility contributes directly to fitness; fitness modifies trait multipliers that feed ability generation.
- **No evidence of direct utility-driven retention pools or explicit utility-weighted mutation operators** outside this feedback loop.

### 2) Are activity proxies still dominating?
- Activity proxies remain in fitness (`usageFrequency`, `killParticipation`, `lifetimeHours`, `discardRate`), but utility terms carry stronger absolute weights and no-op/spam/redundancy penalties reduce noisy activity value.
- So: **activity still contributes; pure volume is damped rather than dominant** in this evaluator.

### 3) Are meaningful outcomes distinguishable from no-ops?
- Yes: executor assigns `SUCCESS` for non-`FLAVOR_ONLY`, `NO_OP` for `FLAVOR_ONLY`; classifier gives explicit negative no-op score and better weighting for meaningful outcomes.

### 4) Are spam/redundancy penalties actually applied?
- Yes: classifier computes novelty/redundancy/spam, `OutcomeUtilityProfile` accumulates penalties, fitness subtracts aggregated no-op/spam/redundancy penalties.

### 5) Are analytics/telemetry sufficient?
- Runtime telemetry exposes meaningful/no-op/status/budget and rollup utility signals, including high-volume low-value detection.
- But utility telemetry appears **non-durable** (not persisted in player state stores). This limits long-horizon analytics integrity across restarts.

## Model Objects Audit

| Object | Instantiated | Consumed by runtime logic | Notes |
|---|---|---|---|
| `UtilityOutcomeRecord` | Created in `ArtifactUsageTracker.trackAbilityExecution` | Ingested by `ArtifactUsageProfile/OutcomeUtilityProfile` | Real runtime use. |
| `ValidatedOutcomeClassifier` | Field in `ArtifactUsageProfile` | Called by `OutcomeUtilityProfile.ingest` | Real runtime scoring. |
| `UtilityScoreContext` | Built by classifier | Used immediately in classifier score + profile accumulation | Internal but active. |
| `OutcomeUtilityProfile` | Created per mechanic@trigger in profile map | Produces snapshots + contributes to fitness inputs | Core state carrier. |
| `MechanicUtilitySignal` | Produced by profile snapshots/rollups | Used in telemetry/reporting and profile-level aggregate methods | Mostly telemetry + aggregate metric transport. |

No Phase-1 utility model objects found to be definition-only dead code.

## Utility Density and Related Ratios
- `utilityDensity = validatedUtility / budgetConsumed` is computed both per-mechanic signal and profile-wide aggregate.
- `meaningfulOutcomeRate = meaningfulOutcomes / attempts` computed profile-wide.
- `utilityBudgetEfficiency` duplicates the same ratio style as utility density at profile level.
- These are used by `ArtifactFitnessEvaluator`, hence they do affect evolution feedback.

## Remaining Defects / Gaps
1. **Durability gap**: utility telemetry not persisted with artifacts/reputation.
2. **Selection scope gap**: no clear utility-wired retention queues or direct mutation operator weighting tied to utility rollups.
3. **Mechanic-level feedback gap**: utility signals are aggregated and reported, but not clearly fed back into per-mechanic mutation desirability matrices.
4. **Analytics integration gap**: utility rollups are emitted in text reporting; broader dashboard/world-lab ingestion of these exact signals appears limited.

## Test Coverage Observed
- `ArtifactUtilityFitnessModelTest`: meaningful vs no-op, spam penalty, redundancy penalty, utility density, activity-vs-value fitness behavior, budget efficiency behavior.
- `ArtifactUsageTrackerUtilityTelemetryTest`: rollup + high-volume low-value detection.
- `ExperienceEvolutionEngineTest`: validates fitness can drive bounded genome adjustments.

Coverage is good at unit level for scoring/counters and evaluator behavior, but does not fully prove end-to-end long-running retention/ecosystem selection shifts caused by utility signals.
