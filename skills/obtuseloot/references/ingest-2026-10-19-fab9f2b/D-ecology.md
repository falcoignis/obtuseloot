> Analysis of falcoignis/obtuseloot HEAD fab9f2b, produced 2026-10-19 by a doc-ingest agent. Read from git HEAD. Inferences are labeled "inference:". Historical snapshot: verify against current code before relying on any file:line.

# D-ecology: ObtuseLoot ecology / niche / bifurcation / turnover / long-horizon slice

Source: `git show HEAD:<path>` in the audit checkout (HEAD fab9f2b). Nothing in the repo was modified. No builds, tests or docker were run.

Evidence beyond the docs, all read-only from HEAD:
- Rollup JSON from `analytics/validation-suite-rerun/*`, parsed with small Python one-liners.
- Source under `src/main/java/obtuseloot/{evolution,ecosystem,species}`.
- Small reads **outside the permitted source dirs** (flagged so the caller knows):
  - `telemetry/ScheduledEcosystemRollups.java` lines 45-75, for the formulas behind the diversity and turnover metrics.
  - `bootstrap/EngineBootstrap.java` lines 40-110, for live wiring.
  - Grep hits in `artifacts/ArtifactManager.java`, `abilities/ItemAbilityManager.java` and `simulation/worldlab/WorldSimulationHarness.java`, to tell live paths from simulation-only ones.

Timestamps: `git log %ai` mixes -0400 and +0000 offsets. Everything below is normalized to EDT (-0400). Pass 3.5 (22:02 +0000) is therefore 18:02 EDT, after pass 3 (17:47) and before the post-ability docs.

---

## 1. Per-document findings (chronological, EDT)

Context commits between the slice docs:
- **48ba2be, 03-16 19:55** (`analytics/niche-bifurcation-autocalibration-report.md`): first bifurcation calibration.
  - `EcosystemSaturationModel.SATURATION_THRESHOLD` 0.20 -> 0.14.
  - `EcosystemSaturationModel.SPECIALIZATION_THRESHOLD` 0.079 -> 0.070.
  - `NicheBifurcationRegistry.SATURATION_THRESHOLD` 0.15 -> 0.08.
  - `NicheBifurcationRegistry.SPECIALIZATION_THRESHOLD` 0.10 -> 0.00.
  - New `MIN_PARENT_NICHE_SHARE` 0.08, a per-parent cap, and a child-collapse lifecycle.
- **4dab440, 03-17 21:44 EDT** ("fix: bifurcation niche collapse and child niche stability (BL5YT)", dataset `deep-stability-final`). No slice doc covers this commit. It added:
  - Forced bifurcation: `FORCED_BIFURCATION_WINDOW=3`, `FORCED_BIFURCATION_MIN_SHARE=0.10`, `FORCED_BIFURCATION_MIN_ARTIFACTS=5`.
  - The `highShare = nichePopulationShare >= 0.13` substitute gate.
  - `MIN_CHILD_ARTIFACT_COUNT` 2 -> 1.
  - A grace period.
  - Telemetry fixes so COMPETITION_ALLOCATION carries the child niche name (in `WorldSimulationHarness` and `AdaptiveSupportAllocator`).
  - Moving `run-deep-validation.sh` to direct Java invocation, bypassing exec-maven-plugin.

### 1.1 docs/deep-run-bifurcation-diagnostic-20260316.md (03-16 17:04, 8bf2688)
- **Problem.** Does `NicheBifurcationRegistry` fire in a real deep run?
- **Changed.** Nothing (diagnostic). It confirmed that `EcosystemSaturationModel`, `NicheBifurcationRegistry`, the `NICHE_BIFURCATION` event type and the snapshot fields `dynamicNiches`, `bifurcationCount` and `dynamicNichePopulation` exist.
- **Runner.** `scripts/run-deep-validation.sh` with 10 seasons x 4 sessions, 18 players, density 5, sampling 0.25.
- **Results.** On `deep-ten-season-20260316-172710`: 0 bifurcations in all 5 scenarios, `telemetry_buffer_size=0`, and `telemetry/ecosystem-events.log` missing even though the manifest said true. Verdict: READY FOR FULL ECOLOGICAL INTERPRETATION: NO.
- **Status.** Superseded (the gates were recalibrated 3 hours later in 48ba2be).

### 1.2 docs/deep-validation-runtime-verification-20260316.md (03-16 18:14, f0831aa)
- **Problem.** Phased verification of the deep run.
- **Changed.** Nothing.
- **Results.** `mvn -DskipTests compile` succeeded. The deep run failed on the first scenario: offline Maven could not resolve `org.codehaus.mojo:exec-maven-plugin:3.5.0`. Verdict: READY FOR THRESHOLD AUTO-CALIBRATION: NO.
- **Status.** Superseded. 4dab440 later switched the runner to direct Java invocation.

### 1.3 docs/deep-niche-adoption-test-report.md (03-16 20:17, 15b66d5 "Add lineage-driven child niche adoption signals")
- **Problem.** Do child niches attract population?
- **Changed.** The commit adds lineage-driven adoption signals. The doc does not name the classes or constants.
- **Results.**
  - explorer-heavy and ritualist-heavy had no child niches.
  - gatherer-heavy, mixed and random-baseline had `RITUAL_STRANGE_UTILITY_A1/A2/B1/B2` with final shares of 0.00-0.99%.
  - Top-2 niche share was 58-62%.
  - Lineage concentration within niches was non-uniform. Examples: `ashen` held 33-41% of SOCIAL_WORLD_INTERACTION; some STRUCTURE_SENSING and FARMING children were 100% single-lineage.
  - Verdict: NICHE ADOPTION RESULT: PARTIAL.
- **Dataset.** `deep-niche-adoption-test` is not present at HEAD or anywhere in git history.
- **Status.** Superseded. Its shares use the old cumulative denominator (see section 5).

### 1.4 docs/deep-post-signal-fix-ecology-status.md (03-17 07:05, b3cb282)
- **Problem.** Validation after a telemetry signal fix.
- **Changed.** `TelemetryAggregationBuffer` now uses `normalized(event.niche(), event.attributes().get("niche"))`, so the event-level niche wins. Regression test: `aggregationPrefersEventNicheOverLegacyAttributeNicheForDynamicChildren`.
- **Results.**
  - Child niches appeared only in gatherer-heavy (0.35% -> 0.78%) and random-baseline (0.33% -> 0.77%).
  - Parent share stayed flat or rose (31.94% -> 32.77%).
  - The same lineage, `wild-4233`, led both the A and B sides, so A/B separation was weak.
  - Verdict: POST-FIX ECOLOGY STATUS: PARTIAL.
- **Dataset.** `deep-post-signal-fix` is absent at HEAD and in history.
- **Status.** Superseded.

### 1.5 docs/deep-mild-adoption-calibration-report.md (03-17 12:15, fd1f6a3)
- **Problem.** Child adoption is too weak.
- **Changes to `NichePopulationTracker`:**

  | Setting | Before | After |
  |---|---|---|
  | Forced migration rate | 0.40 | 0.58 |
  | Forced migration band | 0.30-0.50 | 0.50-0.70 |
  | Lock windows | 2-4 | 4-6 (`NICHE_LOCK_MIN/MAX_WINDOWS`) |
  | Lock utility boost | 1.12 | 1.30 (`NICHE_LOCK_UTILITY_BOOST`) |
  | `INVERSION_PARENT_MULTIPLIER` | 0.93 | 0.88 |
  | `INVERSION_CHILD_MULTIPLIER` | 1.12 | 1.30 |
  | Child clamp | 1.05-1.15 | 1.10-1.32 |
  | Parent clamp | 0.90-0.97 | 0.86-0.95 |
  | Lock+inversion clamp | 1.05-1.20 | 1.10-1.35 |
  | Underrepresented-child bias | 0.65 | 0.85 |
  | Soft migration cap | 4% | 8% |
  | Soft migration chance max | 0.18 | 0.30 |

- **Changes to `NicheBifurcationRegistry`:** `SATURATION_THRESHOLD` 0.08 -> 0.06, parent-share gate 0.08 -> 0.06, child zero-window collapse 2 -> 3 (`DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE`).
- **Test file.** `NicheEcologySystemTest.java`.
- **Results.**
  - All 5 scenarios now had children. Peak child share was 0.67-0.85%, short of the 1-5% target.
  - Parent share still rose by 0.26 to 0.89 pp.
  - A/B siblings had different dominant lineages more often.
  - Verdict: CALIBRATION RESULT: PARTIAL.
- **Dataset.** `deep-mild-adoption-calibration` is present. The rollups show `activeArtifactCount` growing cumulatively, from 19,026 at w1 to 188,285 at w10.
- **Status.** Superseded for its thresholds and its metric basis. The INVERSION/LOCK constants it set are still the values at HEAD.

### 1.6 docs/deep-activation-stabilization-report.md (03-17 19:53, a773183)
- **Problem.** Child niches never activate in some scenarios, explorer-heavy in particular.
- **Documented gates.**
  - Headroom: global and per-parent caps.
  - Per-parent and global cooldowns.
  - Pressure gates: `saturationPressure >= 0.05`, `specializationPressure >= 0.00`, `activeArtifacts >= 2`, `nichePopulationShare >= 0.05`.
  - Sustained windows: 2, or 1 when share >= 0.10.
  - Partition: at least 2 children; `saturationPressure >= SATURATION_THRESHOLD*0.90` (0.045); share bounds 10-20%, target 15%.
  - Soft migration: total >= 10, parent share >= 0.17, utility below the parent mean.
- **Changes.**
  - `SATURATION_THRESHOLD` 0.06 -> 0.05.
  - Parent-share floor 0.06 -> 0.05.
  - Fast path added: 1 fewer sustained window when share >= 0.10.
  - Structural gate set to x0.90.
  - Soft-migration parent share 0.20 -> 0.17.
- **Results.**
  - explorer-heavy 0%.
  - ritualist-heavy peak 3.45%, final 0.
  - gatherer-heavy 6.90%.
  - mixed peak 6.25%, final 3.12%.
  - random-baseline peak 6.90%, final 0.
  - Verdict: ACTIVATION STABILITY RESULT: PARTIAL.
- **Dataset.** `deep-activation-stabilization` is absent at HEAD and in history.
- **Status.** Partly stale. HEAD constants match, but 4dab440 added the `highShare >= 0.13` substitute gate and forced bifurcation two hours later, and this doc describes neither.

### 1.7 docs/deep-long-horizon-validation.md (first 1862c47 at 03-18 00:17 EDT as a "40-season" report; refreshed 71db033 at 07:21)
- **Problem.** Long-horizon stability, measured only.
- **Dataset.** `deep-long-horizon-test` has 32 rollup windows. Its log reaches generation=160, which is 40 seasons x 4 sessions, with `in_memory_rollup_count=32`.
  - inference: the 32 windows are the last 32 of 40 season rollups, retained under a cap.
  - Last data commit: ca401cb at 03-18 06:52 EDT, before tuning pass 1. **This dataset predates all the extinction/turnover tuning.**
- **Results (refreshed version).**
  - Classifications: explorer, ritualist and mixed DRIFTING; gatherer COLLAPSING; random-baseline CHAOTIC.
  - No scenario had non-zero child share in every window.
  - Late-window averages were 3.2-7.0%.
  - Gatherer and random-baseline ended at 0% child share.
  - Dominant-lineage turnover was 0 to 8 events.
  - Diversity index mean 0.48-0.52, final 0.62-0.69.
  - Criteria: 0/5 always non-zero (fail); >3% long-term in 5/5 (pass); permanent collapse inconclusive; turnover bounded (pass); no single dominant niche (pass, parents always <50%).
  - Verdict: LONG_HORIZON_RESULT: PARTIAL.
- **The two versions disagree.**
  - 1862c47 reported explorer non-zero child windows as 14/32; the refresh implies 28/32.
  - 1862c47 stated "No lineage extinction ... No spontaneous new lineages ... Lineage turnover = zero". The refresh dropped that statement.
  - The explorer data was regenerated by a0343fa (00:29 EDT) twelve minutes after 1862c47, so the original numbers describe data that is no longer committed.
- **Status.** This is the baseline later docs cite, but it measures pre-tuning code.

### 1.8 docs/deep-extinction-turnover-tuning.md (pass 1; 03-18 09:11, d1a8887)
- **Problem.** Repeated zero-share windows, terminal child extinction, and collapse/re-bifurcation churn.
- **Changes.**
  - `NicheBifurcationRegistry`: established-child tracking (active 4 windows, now `ESTABLISHED_CHILD_WINDOWS=4`), collapse hysteresis through a consecutive low-population streak (`COLLAPSE_HYSTERESIS_WINDOWS=2` at HEAD), and lineage-affinity support fed into collapse (stronger support lengthens the path to collapse).
  - `NichePopulationTracker`: a bounded persistence floor (keep one low-utility parent artifact in an established, supported child).
  - Collapse resets the per-parent cooldown, and stale child assignments are cleared on reclassification.
  - Child adoption multiplier clamp re-tightened.
- **Results.**
  - Zero-share windows: 2/4/1/0/1. Only mixed was fully continuous.
  - Final child share 17.2-27.6%. Peak child share 20.7-28.1%.
  - 12-23 extinction and 7-12 re-emergence events per scenario.
  - Diversity mean 0.39-0.52.
  - Verdict: EXTINCTION_TUNING_RESULT: PARTIAL.
- **Dataset.** `extinction-turnover-tuning-20260318` is absent at HEAD and in history.
- **Status.** Superseded by passes 2 and 3. Pass 3 removed the persistence floor as the main mechanism.

### 1.9 docs/deep-extinction-turnover-tuning-pass2.md (03-18 17:16, 4e02891)
- **Problem.** Single-window dropouts in supported children, `0 -> re-entry -> 0` oscillation, and synchronized exits.
- **Changes.**
  - `NicheBifurcationRegistry`: re-entry stabilization window (`REENTRY_STABILIZATION_WINDOWS=2` at HEAD) and one more required empty window before established, supported children retire.
  - `NichePopulationTracker`: sticky continuity for one artifact (`ASSIGNMENT_CONTINUITY_STICKY_WINDOWS=2`); proactive occupancy seeding with a low-utility parent artifact plus a sticky lock (`REENTRY_STICKY_WINDOWS=3`); synchronized-exit damping that protects one continuity artifact from being demoted first.
- **Results.**
  - Zero-share windows: 2/1/1/1/1. No scenario fully continuous.
  - Max child share 6.9-13.8%.
  - 0-3 lineage turnovers.
  - Verdict: PASS2_RESULT: PARTIAL.
- **Dataset.** `-pass2-20260318` is absent.
- **Status.** Superseded.

### 1.10 docs/deep-extinction-turnover-tuning-pass3.md (03-18 17:47, 9d3e8be)
- **Problem.** Deterministic one-window vacancies when the last occupant exits.
- **Changes.**
  - Per-child `ChildOccupancyState` in the registry: `lastOccupiedWindow`, `consecutiveOccupiedWindows`, `isEstablished`, `hasLineageSupport`/`lineageSupport`.
  - `eligibleForContinuityProtection`.
  - A tracker reconciliation pass (`reconcileProtectedOccupancy`) that restores **exactly one occupant** to a protected child before collapse accounting. It prefers the last known occupant, otherwise the parent artifact with the lowest continuity score.
  - Pass 1's persistence floor was removed as the primary path.
- **Results.**
  - Zero-share windows: 3/4/5/1/0. Only random-baseline fully continuous.
  - Max child share 10.34/3.45/3.45/9.38/6.90%.
  - Mean `turnoverRate` 8.6-12.4.
  - Verdict: PASS3_RESULT: PARTIAL.
- **Dataset.** `extinction-turnover-tuning-pass3-20260318` is present. It has 10 rollup windows, 18 players x 3 artifacts, and 29-32 active artifacts in the rollup.
- **Status.** Its mechanism is current; its results were superseded by pass 3.5.

### 1.11 docs/deep-extinction-turnover-tuning-pass3_5.md (03-18 18:02 EDT, 7c4c8a1, +0000 author tz)
- **Problem.** "Eligibility flicker": a single-window dip of `lineageSupport` below 0.20 removed protection. The same raw >= 0.20 gate was duplicated in `qualifiesForContinuityBias` and `reconcileProtectedOccupancy`.
- **Changes.**
  - `LINEAGE_SUPPORT_SMOOTHING_WINDOWS=3`: a ring buffer, `smoothedLineageSupport()`, advanced once per window and guarded by `lastEligibilityUpdateWindow`.
  - `PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD=0.12` for children aged at least `PERSISTENCE_WEIGHTED_AGE_WINDOWS=8`. The standard threshold remains `CONTINUITY_LINEAGE_SUPPORT_THRESHOLD=0.20`.
  - Carryover: if `eligiblePreviousWindow` and smoothed support >= `LINEAGE_AFFINITY_EFFECTIVELY_ZERO=0.005`, protection stays.
  - `computeEligibilityFromState` added.
  - The redundant raw gates were removed from the two tracker call sites.
  - Exclusions: the child is not established (<4 consecutive and <4 active windows), affinity <0.005, or the zero-window count has reached `childZeroWindowsToCollapse`.
  - All of these constants match HEAD.
- **Results.**
  - Zero-share windows: 1/0/1/0/0, so 3/5 fully continuous.
  - Max child share **10.34/3.45/3.45/9.38/6.90%, identical to pass 3**.
  - Verdict: EXTINCTION_TUNING_PASS3_5_RESULT: SUCCESS. This is the only SUCCESS in the ecology chain.
- **Unsupported claims.**
  - Dataset `extinction-turnover-tuning-pass3_5-20260318` is **absent at HEAD and in all git history**.
  - The doc cites "mixed scenario logs where affinity crossed 0.20 every other window", and no such logs are committed.
- **Status.** Current mechanism. Its results cannot be checked from committed evidence. Window semantics: see section 5.

### 1.12 docs/deep-long-horizon-post-ability-expansion.md (03-18 20:29, 960fa4e)
- **Problem.** Long-horizon validation after the ability expansion.
- **Changed.** Nothing (measurement only). The doc admits that no newer dataset existed, so it re-analyzed `deep-long-horizon-test` (the pre-tuning, pre-expansion data). Its own words: "zero-by-construction ... verification control".
- **Results.**
  - Zero-share windows: 4/6/7/4/8.
  - Classifications: DRIFTING, DRIFTING, COLLAPSING, **CHAOTIC (mixed)**, **COLLAPSING (random)**.
  - Criteria pass count 2/5, including "no convergence to a single dominant niche: FAIL".
  - Verdict: LONG_HORIZON_RESULT: PARTIAL.
- **Status.** It shares data with 1.7 but reaches different verdicts; see section 5. Not a valid post-expansion measurement.

### 1.13 docs/deep-long-horizon-post-ability-expansion-fresh.md (03-18 20:58, 8e85034)
- **Problem.** A fresh run with run id `deep-long-horizon-post-ability-fresh`.
- **Results.** The harness stalled in explorer-heavy (manifest IN_PROGRESS, no `rollup-snapshots.json`). Log at generation 8: `active_artifact_count=54`, `lineage_count=35`. Verdict: LONG_HORIZON_RESULT: FAILED.
- **Status.** Superseded: 1.14 re-ran the same run id to completion. **No doc analyzes the completed fresh dataset.**

### 1.14 docs/deep-validation-harness-fix.md (03-18 22:08, 20538df)
- **Problem.** The wrapper ran 10 x 4 = 40 windows instead of 32, had no completion markers, and had no runtime guard.
- **Changes.**
  - Run shape changed to 8 seasons x 4 sessions (32 session windows).
  - Expected-window validation, plus runtime and iteration guards.
  - `timeout --foreground` around each scenario.
  - Markers: `[validation] scenario_start/window_complete/writing_rollup_snapshots/run_complete/scenario_complete`.
  - Runtime configs with a **reduced artifact population**.
- **Results.** All 5 scenarios completed, `READY_FOR_ANALYSIS`. Verdict: VALIDATION_HARNESS_RESULT: SUCCESS.
- **Verified at HEAD.** The explorer log has 32 `window_complete` markers. The execution report gives `world.players=4`, `artifactsPerPlayer=2`, `artifact_population_size=6`, `encounterDensity=3`, `seasonCount=8`. The rollups contain **8 windows** with activeArtifactCount 3 at w1 and 3 lineages, and the final log shows `active_artifact_count=8`, `lineage_count=6`.
- **Status.** Current harness shape. Its dataset is a smoke test, not an ecological sample.

### 1.15 docs/deep-post-expansion-validation.md (03-19 12:18, 698627e)
- **Problem.** Ability generator category and template sampling after the low-template category expansion. This is generator scope, not ecology.
- **Changed.** Nothing.
- **Evidence.** `analytics/post-expansion-probe-20260319.txt` and `AbilityRegistry` lines 173-284. Scenarios: explorer, ritualist, **warden**-heavy, mixed, random. Note that this is not the deep suite's gatherer-heavy.
- **Results.**
  - Template reachability failed in ritual (`chaos.dust_memory`), defense (`consistency.path_thread`, `consistency.structure_echo`) and stealth (`stealth.threshold_jam`, `stealth.trace_fold`).
  - Top template share was at most 35%, under the 50% hard-fail line.
  - Top-3 share ranged from 46.2% (survival) to 73.3% (stealth).
  - Novelty: average 0.3657, nearest-neighbor similarity 0.6343, intra-niche 0.8264, global 0.3657.
  - Jensen-Shannon divergence: explorer-ritualist 0.0458, explorer-warden 0.0712, ritualist-warden 0.0734. The doc calls these weaker than the earlier bands in `docs/deep-ability-expansion.md`.
  - Verdict: POST_EXPANSION_VALIDATION_RESULT: PARTIAL.
- **Status.** Current for the generator. The doc uses Codex-style citation markers (`【F:...†L..】`).

---

## 2. Consolidated current model (HEAD), with live vs simulation-only

Coverage note: the slice docs cover only niches, bifurcation, child-niche continuity and the harness. They do not cover the progression engines: evolution stages, drift, awakening, convergence, memory, species. For those I confirmed only how they are wired; their mechanics belong to other slices.

### 2.1 Wiring (live)
`EngineBootstrap.initialize` builds the following live at plugin start:
- `EvolutionEngine(ArchetypeResolver, HybridEvolutionResolver)`
- `ArtifactUsageTracker`, which internally does `new NichePopulationTracker()` and so a `NicheBifurcationRegistry`
- `ArtifactEcosystemSelfBalancingEngine`, which owns an `EnvironmentPressureEngine`
- `ExperienceEvolutionEngine(usageTracker, ArtifactFitnessEvaluator, pressureEngine, new AdaptiveSupportAllocator(), EvolutionParameterRegistry)`
- `DriftEngine`, `AwakeningEngine`, `ConvergenceEngine`, `ArtifactMemoryEngine`
- `LineageRegistry`, which contains `LineageSpeciationEngine`; species are restored from `playerStateStore.loadSpeciesSnapshot()`

Feed paths:
- Live: `ItemAbilityManager:267` -> `ArtifactUsageTracker.trackAbilityExecution` -> `NichePopulationTracker.recordTelemetry` -> throttled `evaluateBifurcations`. `ArtifactManager` calls `trackCreated`.
- Simulation: `WorldSimulationHarness:492` calls the same `trackAbilityExecution`.

So the niche/bifurcation code runs in both live and simulation. inference: there are no simulation-only branches in `NichePopulationTracker`, but I did not read the whole file (1309 lines).

Persistence: I found no persistence or restore of `NicheBifurcationRegistry` or `NichePopulationTracker` state.
- The grep for `dynamicNiche|bifurcat` outside `evolution/Niche*` hits only:
  - readers in `AdaptiveSupportAllocator:106-107`
  - telemetry aggregation (`TelemetryAggregationBuffer`)
  - the rollup snapshot writer (`TelemetryRollupSnapshotStore:52-53`, which writes `dynamicNiches`/`bifurcationCount` into telemetry rollup properties)
  - harness output
- `telemetry/RollupStateHydrator.java` exists, but I did not read it. It may restore telemetry rollups; nothing in the grep feeds anything back into the registry.
- inference: child niches, the forced-bifurcation flag and the occupancy state reset on every server restart. Species snapshots are persisted; child niches are not.

### 2.2 Niche detection
- Parent niches are `MechanicNicheTag` enum values: RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION, MEMORY_HISTORY, NAVIGATION, ENVIRONMENTAL_SENSING, STRUCTURE_SENSING, FARMING_WORLDKEEPING, GENERALIST, and others.
- They are assigned per artifact from utility signals (`EcosystemRoleClassifier`, `ArtifactNicheProfile.dominantNiche()`, subniche).
- `niche-1` in docs is a separate species-niche ID from `SpeciesNicheAnalyticsEngine` (4dab440 commit message).
- Parent niche identities are fixed by the enum. Only children (`<PARENT>_A<n>`, `<PARENT>_B<n>`) are created at runtime.

### 2.3 Saturation pressure (`EcosystemSaturationModel.pressureFor`, HEAD)
- `share = parent activeArtifacts / total`
- `saturationPenalty = clamp(share - 0.14, 0, 0.45)`
- `scarcityBonus`: applies when share < 0.10 and utility is above the mean
- `specializationPressure = clamp(outcomeYield - 0.070, 0, 0.35)`
- `diversityIncentive = (1 - share) * 0.08`
- `ecologicalRepulsion`: applies when share > 0.18 and outcomeYield < 0.25
- `retentionBias` is clamped to 0.70-1.35 and `mutationBias` to 0.60-1.50

### 2.4 Bifurcation (`NicheBifurcationRegistry.evaluateBifurcation`, lines 186-250)
Guard order:
1. Global cap: `DEFAULT_MAX_DYNAMIC_NICHES=8`, counted in pairs.
2. Per-parent cap: `DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT=4`.
3. Per-parent cooldown and global cooldown: `DEFAULT_COOLDOWN_MS=60_000`, **wall-clock ms**.
4. Pressure gate: `(saturationPressure >= 0.05 OR share >= 0.13) AND specializationPressure >= 0.00 AND activeArtifacts >= 2 AND share >= 0.05`.
5. Sustained-window counter: 2, or 1 when share >= 0.10.
6. Children are named `parent_A<seq>` and `parent_B<seq>`.

inference (from the tracker passing `pressure.saturationPenalty()` as `saturationPressure`, NichePopulationTracker:219-224, together with the saturation model formula):
- `saturationPressure >= 0.05` means parent share >= 0.19. The `share >= 0.13` substitute therefore dominates, and the effective gate is share >= 0.13.
- `MIN_PARENT_NICHE_SHARE=0.05` is redundant.
- `SPECIALIZATION_THRESHOLD=0.00` is compared against `meanSpecializationFor(nicheTag)` (NichePopulationTracker:217, 487-498), the mean of `profile.specialization().specializationScore()`. It is not the saturation model's clamped `specializationPressure`. The gate passes for any non-negative mean score. Unverified: whether `specializationScore` can be negative (not checked). inference: it is effectively vacuous.
- Any share >= 0.13 also satisfies the >= 0.10 fast path, so the effective sustained requirement is always 1 window.

Forced bifurcation (NichePopulationTracker:245-254, 440-475):
- Fires if no bifurcation has happened by evaluation window 3, total population >= 5, and the top niche share >= 0.10.
- It calls `forceBifurcation`, which bypasses the pressure and sustained gates but keeps the caps and cooldowns.
- At most once per tracker instance (`forcedBifurcationDone`).

Evaluation cadence:
- `evaluateBifurcations` is throttled to `BIFURCATION_EVAL_INTERVAL_MS=5_000` of wall-clock time (NichePopulationTracker:168-171).
- Each call increments `evaluationWindowCount`. This is the "window" used for grace (`DEFAULT_GRACE_PERIOD_WINDOWS=4`), establishment (4), zero-window collapse (3), smoothing (3) and age (8).

### 2.5 Child-niche assignment, competition and support
On bifurcation, every artifact in the parent is reassigned through `refreshDynamicNicheAssignment`, and lineage affinity is seeded (`seedLineageAffinityForBifurcation`).

Partition:
- `STRUCTURAL_CHILD_PARTITION_MIN/TARGET/MAX = 0.10/0.15/0.20`
- Gate `STRUCTURAL_ACTIVATION_SATURATION_GATE = 0.045` on saturationPenalty. inference: this means parent share >= 0.185.

Forced displacement (`applyForcedDisplacement`) and soft migration of below-mean performers, with rates in 1.5. `migrationRandom` is seeded with `0xC0FFEE1234ABCDEFL`.

Locks: 4-6 windows, utility boost x1.30.

Inversion multipliers:
- Child x1.30 and parent x0.88, decaying over `INVERSION_DECAY_WINDOWS=5` (`INVERSION_WINDOW_MS=5_000`).
- Gated on saturation >= `SATURATION_THRESHOLD`.
- They apply to fitness through `nicheAdoptionFitnessMultiplier`. `ArtifactUsageTracker` divides relevance by `nicheCompetitionFactor` (weights: share 3.0, saturation 1.0, density 0.60) and multiplies by the adoption multiplier.

Lineage affinity per (parent, child):
- +`LINEAGE_AFFINITY_OUTCOME_BOOST=0.05` per successful outcome in the child.
- x`LINEAGE_AFFINITY_DECAY_RATE=0.93` per window.
- Capped at `LINEAGE_AFFINITY_MAX=0.90`; pruned below 0.005.
- `STRONG_LINEAGE_AFFINITY_SUPPORT=0.40` in the registry.

`AdaptiveSupportAllocator.allocateFor` builds an `EvolutionOpportunityPool` from the usage tracker and lineage registry and reads child `NicheVariantProfile`s. I did not inspect the details.

### 2.6 Extinction and turnover of child niches
- `collapseUnderpopulatedChildren(dynamicPopulation, lineageSupportByChild(), evaluationWindowCount)` runs at the end of each evaluation, after `reconcileProtectedOccupancy`.
- Collapse needs consecutive zero windows (3, longer for established or lineage-backed children), after the grace period and the re-entry stabilization period.
- Continuity protection (pass 3/3.5) keeps one occupant in established, lineage-supported children.
- "Extinction" in these docs means child-niche vacancy or retirement. It does not mean any artifact or lineage dying:
  - Rollups show a constant `activeArtifactCount` of 29 or 32 in pass 3 and long-horizon.
  - The lineage set is identical from first window to last in every run I checked: deep-long-horizon-test 20/21/16 lineages, pass 3 17/18/18, fresh 3. Zero new and zero gone.

### 2.7 Environment pressure (`EnvironmentPressureEngine`, live)
- 4 hard-coded event templates (`DriftStorm`, `PrecisionAge`, `SurvivalWinter`, `MobilityBloom`) with fixed `FitnessLandscapeModifier` multipliers between 0.84 and 1.25 on `GenomeTrait`s.
- Seeded `Random(7331)`. Each event lasts 2-5 seasons; then a random template is picked.
- `advanceSeason()` is called live by the scheduled task in `EngineBootstrap:115` and in simulation by `ArtifactEcosystemSelfBalancingEngine.evaluate:54`.

### 2.8 Self-balancing (`ArtifactEcosystemSelfBalancingEngine`, live)
- Holds family weights (`weightForFamily`), branch shares, candidate-pool collapse detection, `ProductionSafetyGuards`/`ProductionSafetyConfig`, and `EcosystemHealthMonitor`.
- `evaluate(WorldEcosystemProfile, EcosystemHealthReport, SimulationMetricsCollector)` takes simulation metric types. inference: full evaluate is driven by the simulation harness, while live use is the snapshot, guards and pressure. I did not verify this.

### 2.9 Lineage and species
- `LineageSpeciationEngine` constants: `COMPATIBILITY_THRESHOLD=0.66`, `STRICT_COMPATIBILITY_THRESHOLD=0.74`, `PERSISTENCE_GENERATIONS=5`, `MIN_OBSERVATIONS=8`, `MIN_COMPONENT_DIVERGENCE=0.09`, `MIN_NICHE_DIVERGENCE=0.14`, `MIN_ENV_DIVERGENCE=0.10`, `STABLE_NICHE_TENDENCY=0.62`. Not covered by slice docs.
- In the deep-run data, lineages such as `ashen`, `gilded`, `graveborn`, `stormbound`, `mirrored`, `wild-<n>` and `lineage-<uuid>` are present from window 1 and never change.

### 2.10 Progression (stages, drift, awakening, convergence, memory)
- Live-instantiated (2.1). Not described by any slice doc.
- `ArtifactUsageTracker` has `trackConvergenceParticipation` and `trackAwakening` hooks.

---

## 3. Validation methodology

### Harness
- `scripts/run-deep-validation.sh` runs `obtuseloot.simulation.worldlab.WorldSimulationRunner`. It was originally `mvn exec`; since 4dab440 it is direct Java.
- One process per scenario, writing to `analytics/validation-suite-rerun/<run-id>/<scenario>/`.
- Artifact contract:
  - `rollup-snapshots.json`
  - `telemetry/rollup-snapshot.properties`
  - `telemetry/ecosystem-events.log`
  - `scenario-metadata.properties`
  - `world-sim-*.md/json`
  - `logs/<scenario>.log`
  - `.dataset-complete.properties`
  - `execution-report.md`
  - run manifest status `READY_FOR_ANALYSIS`

### Run shapes
- Pre-03-18: 10 seasons x 4 sessions, 18 players x 3 artifacts, density 5, sampling 0.25. Gives 10 rollup windows.
- Long-horizon test: 40 seasons, 32 retained rollups.
- Post-harness-fix: 8 seasons x 4 sessions, 4 players x 2 artifacts (population 6), density 3. Gives 8 rollup windows and 32 session "windows".

### Scenarios
- `explorer-heavy`, `ritualist-heavy`, `gatherer-heavy`, `mixed`, `random-baseline`.
- Configs: `analytics/validation-suite-rerun/configs/*.properties`, plus `stress-lite`.
- `gatherer-heavy` parameters per `analytics/niche-bifurcation-validation-report-JN0jO.md`: `lineage_drift_window=1.40`, `ecology_sensitivity=0.95`, `competition_pressure=1.05`.
- The generator probe (1.15) uses `warden-heavy` instead of `gatherer-heavy`.

### Rollup metrics as computed (`telemetry/ScheduledEcosystemRollups.java:50-58`)
- `diversityIndex = (# keys in populationByNiche) / activeArtifactCount`.
  - Keys include zero-population entries and every child niche ever created.
  - deep-long-horizon-test explorer: keys go from 11 to 20 while non-zero niches stay at 7, and diversity rises monotonically from 0.379 to 0.690.
  - Fresh run: 1.667. The measure is unbounded.
  - inference: the "diversity index" in 1.7, 1.8, 1.9 and 1.12 mostly measures how many child names have accumulated, not ecological diversity.
- `turnoverRate = (branchBirthCount + branchCollapseCount) / activeArtifactCount`, with **cumulative** counters (births 214 -> 794 over 32 windows). It grows with run length.
- `carryingCapacityUtilization = active / (active + #niche keys)`, capped at 1.
- Child share = sum of `populationByNiche` entries whose key matches `_[AB]\d+$`, divided by the total. In current runs that total is 29 or 32 artifacts, so one artifact is 3.45% or 3.13%.
- `dynamicNichePopulation` holds cumulative event counts: `TelemetryAggregationBuffer:160-161` increments per telemetry event whose niche is a known dynamic niche (grep line only). Values such as 3565 are therefore not artifact counts.

### Doc-level success bars (team-chosen thresholds)
- Niche adoption: some child >5%; top-2 <85%; non-uniform lineage allocation.
- Activation: all scenarios non-zero; >=3 scenarios >3%; >=1 scenario >5%; no niche explosion.
- Mild calibration target: 1-5% child adoption.
- Long horizon:
  - >=3 scenarios non-zero in every window.
  - >=2 scenarios >3% long-term.
  - No permanent post-bifurcation collapse.
  - "Lineage turnover exists but not chaotic", where turnover means changes of the dominant lineage within a child niche.
  - No convergence to a single dominant niche (parents <50%).
- Turnover passes: >=3 scenarios with zero zero-share windows; the rest <=1.
- Auto-calibration: bifurcations <=4 per scenario; a child persists >=2 windows; no explosion.
- Classification labels: DRIFTING, COLLAPSING, CHAOTIC. Their criteria are **not defined** in any slice doc.

### Open-endedness metrics (from analytics/, 03-11 era, headers only)
- **NSER** (Novel Strategy Emergence Rate; `NovelStrategyEmergenceAnalyzer`).
  - A strategy signature is [type, niche, regulatory gate profile, trigger class, mechanic class, branch role, co-evolution tendency].
  - Significant when observations >= 3, occupancy >= 0.05 and persistence >= 2 seasons. Novel when max similarity to prior signatures is < 0.75.
  - Bands: <0.05 none; 0.05-0.15 weak; 0.15-0.30 healthy; 0.30-0.50 strong; >=0.50 likely noise.
- **PNNC** (persistent novel niche count).
  - A niche counts when signature distance exceeds the novelty threshold, it persists >= 3 seasons, occupancy >= 0.05 and species support >= 2.
  - Bands: 0 none; 1-2 weak; 3-5 real expansion; >5 strong. `analytics/pnnc-priority-review.md` calls it the long-horizon milestone metric.
- **END** (Effective Niches) and **TNT** (Temporal Niche Turnover) are dashboard labels. I found no formula in the slice.
- **Branch survival half-life** (`analytics/ecosystem/BranchSurvivalHalfLifeAnalyzer`): 1.000-1.333 windows at 03-16 against a readiness target of >= 2.0 (`analytics/niche-bifurcation-validation-report*.md`).
- **Novelty and divergence** (1.15): average novelty, nearest-neighbor similarity, intra-niche vs global novelty, and Jensen-Shannon divergence between scenario category distributions.
- **Last recorded verdicts:**
  - `analytics/world-lab/open-endedness/open-endedness-classification.md` (03-11): "designer-controlled".
  - `reconciled-open-endedness-classification.md` (03-11): STAGNANT_ATTRACTOR, END=1.0, TNT=0.0, NSER=1.0 over 2 seasons, PNNC=0.
  - Both predate bifurcation. None of the 03-16 to 03-19 slice docs computes NSER, PNNC, END, TNT or half-life.

### Generated evidence in analytics/ (943 files at HEAD)
- `validation-suite-rerun/` (685 files): run datasets (deep-ten-season-*, deep-long-horizon-test, deep-long-horizon-post-ability-fresh, deep-mild-adoption-calibration, deep-stability-final, deep-niche-identity-test, deep-lineage-divergence-test, extinction-turnover-tuning-pass3-20260318, constrained-five-scenario-*, archive-fix-rerun-*, manifest-test-*), plus configs and execution reports. Some runs also have `rollup_history/rollup-snapshot-window-NN.properties` and `analytics-output-*` analysis reports.
- `world-lab/` (73 files): world-sim reports and data, impact reviews per mechanism, season dashboards, and `open-endedness/` (ablation worlds A-D, NSER/PNNC reviews, classifications).
- `review/` (15 files): `PHASE_LEDGER.md` and phase 1-6 forensic audits.
- `meta/`: multirun validation and analysis confidence.
- `performance/`: runtime benchmarks.
- `validation-suite`, `-fresh`, `-constrained`: older harness reports.
- `dashboard/ecosystem-dashboard.html` (03-11).
- About 90 top-level reports and JSON (niche crowding, speciation, fitness sharing, regulatory gates, environment pressure, NSER/PNNC, and others).

---

## 4. Emergence lens (classification only, no judgment)

Labels: TD = top-down/designer-imposed; EM = selection from real outcomes; MIX = outcome-driven input passing through designer-set gates or targets.

| Mechanism | Class | Evidence |
|---|---|---|
| Parent niche set (`MechanicNicheTag` enum) | TD | Fixed enum. Children only subdivide existing parents. |
| Niche assignment from utility signals | MIX | Artifact outcomes (`UtilityOutcomeRecord`) produce the signals; the taxonomy is fixed. |
| Organic bifurcation | MIX | Triggered by population share (effectively >= 0.13) with caps 8 and 4 and a 60 s cooldown. The specialization gate (mean specialization score >= 0.00) is likely vacuous, so the trigger is crowding, not differentiation (inference, 2.4; non-negativity of the score unverified). |
| Forced bifurcation | TD | Guaranteed at evaluation window 3 regardless of pressure (4dab440). |
| Child creation | TD | Always exactly two children (A/B) with sequential names. |
| Child assignment, partition 10-20%, forced migration 0.50-0.70, soft migration | TD | Fixed rates and bands. `migrationRandom` uses a fixed seed. |
| Inversion multipliers (child x1.30, parent x0.88) and lock boost x1.30 | TD | Constant fitness bonuses given to children for being children. |
| `nicheCompetitionFactor` | MIX | Computed from live share, saturation and density with designer weights. |
| Lineage affinity | EM-leaning MIX | Grows +0.05 on success in the child and decays x0.93. Seeded at bifurcation. |
| Collapse / retirement | MIX | Driven by observed zero occupancy, extended by lineage support. |
| Continuity reconciliation (pass 3/3.5) | TD | Restores exactly one occupant before collapse is counted. The success metric (zero-share windows) counts exactly one occupant, so the metric is satisfied by construction for protected children. Eligibility depends on affinity, which is outcome-derived. |
| Persistence floor, sticky seeding, synchronized-exit damping | TD | Designer-inserted occupancy. |
| Tuning loop (passes 1-3.5, calibration, stabilization) | TD | Thresholds were changed until the team's success bars were met. Section 1 records each step. |
| Environment pressure | TD (exogenous) | 4 fixed event templates, fixed multipliers, seeded random rotation. Organisms do not change it. |
| Saturation, scarcity, repulsion biases | MIX | Outcome and population data feed fixed formulas. |
| Lineage set | TD (static) | No lineage births or extinctions in any checked run (2.6). |
| Population size | TD (static) | `activeArtifactCount` is constant within runs. |
| Speciation | Not assessed | Constants only (2.9). |

---

## 5. Contradictions and staleness

1. **The child-share metric changed basis between 03-17 and 03-18.**
   - `deep-mild-adoption-calibration` rollups have a cumulative `activeArtifactCount` (19k -> 188k) and child share of about 0.7%.
   - pass 3 and long-horizon rollups have 29-32 artifacts, so shares move in steps of 3.45%.
   - The jumps from 0.85% (1.5) to 3-7% (1.6, 1.7) to 17-28% (1.8) are therefore not comparable across docs.
   - The commit that changed rollup semantics was not identified. Candidates: 4dab440, or another commit in that window.
2. **"Diversity index" and "turnover rate" are cumulative or inflated** (section 3 formulas). Docs read their rise as ecology improving.
3. **The same unchanged dataset received different verdicts.** 1.7 (07:21) and 1.12 (20:29) both analyze `deep-long-horizon-test`. The explorer data was last changed at 00:29 EDT and gatherer at 06:52 EDT, both before either analysis.
   - Genuine disagreements:
     - mixed: DRIFTING vs CHAOTIC.
     - random-baseline: CHAOTIC vs COLLAPSING.
     - "No convergence to single dominant niche": PASS vs FAIL.
     - Explorer B1 re-emergence: 5 vs 6.
   - Not disagreements, just metrics that share a name:
     - Turnover "0.000" (1.7, dominant-lineage switch rate) vs "34.70" (1.12, rollup `turnoverRate` average).
     - "Active niches >1% (final)": 7 (1.7, all niches) vs 2 (1.12, child niches).
   - The original 1862c47 version (00:17 EDT) reported explorer non-zero child windows as 14/32. Twelve minutes later, a0343fa ("update long-horizon-test outputs from background re-run", 00:29 EDT) **regenerated the explorer data underneath that doc**, and ca401cb later re-ran gatherer. So the 14/32 vs 28/32 gap reflects regenerated data, not a different analysis.
4. **Long-horizon evidence predates all tuning.** `deep-long-horizon-test` was last committed 03-18 06:52 EDT, before pass 1 (09:11). No committed full-population (18 x 3) 32-window run of the current code exists.
5. **Pass 3.5 SUCCESS is unsupported by committed data.** The dataset is absent from HEAD and history, and its max-child-share column is identical to pass 3. It covered 10 windows, not 32.
6. **The fresh-run doc (1.13) says FAILED; the harness-fix doc (1.14) says the same run id SUCCEEDED.** 1.13 was never updated. The successful dataset used 6 artifacts and 3 lineages and has 8 rollup windows, while the doc reports `window_complete: 32` (session count).
7. **"Window" means four different things.**
   - Harness session (`window_complete`).
   - Season rollup (`rollup_history_windows`).
   - Retained rollups (32 of 40).
   - Registry/tracker evaluation calls (wall-clock throttled to 5 s, 60 s cooldown).
   - Pass 3.5's "8 windows" and "4 windows" are registry windows. inference: in simulation, how many evaluation windows fall within a season depends on CPU speed, and live it depends on server time.
8. **The activation-stabilization gate description is stale.** It omits `highShare >= 0.13` and forced bifurcation, both added by 4dab440 two hours later. It presents `saturationPressure >= 0.05` as if it were a share threshold; it is a penalty above 0.14 (inference, 2.4).
9. **The auto-calibration report's pressure scale does not match the model.** It reports saturationPressure p50=2637 and p90=31345, but the current model clamps to 0-0.45. Either it measured a different quantity or the rollup changed.
10. **Harness population vs rollup population.** The harness log has `active_artifact_count=54`, `lineage_count=33`; the rollups have 29 artifacts and 20 lineages (deep-long-horizon-test explorer). The rollup apparently counts a subset.
11. **The original long-horizon doc found lineage turnover zero.** The refreshed doc and later docs use "lineage turnover" for dominant-lineage switches inside child niches.
12. **Several cited datasets are absent from HEAD and from all git history:** `deep-niche-adoption-test`, `deep-post-signal-fix`, `deep-activation-stabilization`, `extinction-turnover-tuning-20260318`, `-pass2-`, `-pass3_5-`. The working tree has the same set of directories as HEAD.
13. **Open-endedness classifications are stale.** "designer-controlled" and STAGNANT_ATTRACTOR (03-11) predate the bifurcation work and have not been recomputed since.
14. **The generator probe (1.15) uses warden-heavy; the ecology suite uses gatherer-heavy.** The scenario sets differ.

---

## 6. Open questions for the project owner

1. **What does "truly emergent" mean operationally?** No slice doc defines it. Every success bar so far is a threshold the team chose (child share >3%, zero-share windows <=1, and so on), and mechanisms were tuned until the bars passed (section 1). Which of these would count as proof?
   - (a) Sustained NSER in the 0.15-0.30 band and PNNC >= 3 over N seasons.
   - (b) Child niches whose occupancy survives with the continuity, inversion, forced-bifurcation and migration aids switched off (ablation).
   - (c) New lineages arising and old lineages going extinct. Neither has occurred in any run (2.6).
   - (d) New parent-level niches beyond the `MechanicNicheTag` enum.
   - (e) Divergence across seeds and scenarios beyond a stated JSD floor.
   - Prompted by: the absence of any definition, plus the "designer-controlled" verdict of 03-11.
2. **Should forced bifurcation and continuity reconciliation remain in the live plugin, or only in validation runs?** Both are active in the live wiring. Forced bifurcation fires once per plugin lifetime (inference), and child niches do not persist across restarts (2.1). Prompted by: 4dab440 and pass 3.
3. **Is the zero-share metric meant to be satisfiable by reconciliation that restores exactly one occupant?** If not, what continuity measure should replace it? Prompted by: pass 3 and 3.5.
4. **Where is the pass 3.5 dataset?** Is the SUCCESS reproducible? Why does its max-child-share column match pass 3 exactly?
5. **Should `diversityIndex` and `turnoverRate` be redefined?** For example: Shannon or Simpson over non-zero niches, and per-window rather than cumulative births and collapses. Prompted by: the `ScheduledEcosystemRollups:55-58` formulas and the monotonic series.
6. **Which artifact count is authoritative?** The harness reports 54; the rollup reports 29/32. Why do they differ? And which rollup change moved child share from cumulative counts to artifact counts?
7. **Should bifurcation timing be wall-clock?** The 5 s eval throttle and 60 s cooldown make simulated "windows" depend on machine speed. Prompted by: `NichePopulationTracker:168`, `NicheBifurcationRegistry:53`.
8. **Is crowding alone meant to trigger bifurcation?** inference: the specialization gate (mean `specializationScore` >= 0.00) is likely vacuous and `share >= 0.13` dominates, so bifurcation fires on crowding with no internal differentiation.
9. **Should a full-population, 32+ window long-horizon run of current code be produced?** No such run exists, and the post-ability long-horizon doc admits it reused pre-tuning data. If so, at what population? The harness fix cut it to 6.
10. **What are the definitions of DRIFTING, COLLAPSING and CHAOTIC?** Two docs applied them differently to the same data.
11. **What are the formulas for END and TNT?** Should NSER, PNNC and branch half-life be recomputed on the post-bifurcation datasets?
12. **What should happen when a parent's population is 1** (pass 3.5's residual case)? Is that a scenario-size artifact? The rollup population is only 29-32.
13. **Should child niches stay restricted to binary A/B splits of fixed parents?** Or is open-ended niche creation (new axes, or merges) in scope for "emergent"?
14. **Should environment pressure respond to the ecosystem** (co-evolution), or stay exogenous with 4 fixed templates and seeded rotation?

---

## 7. Glossary

- **Parent niche:** a `MechanicNicheTag` enum value, for example RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION, MEMORY_HISTORY, NAVIGATION, ENVIRONMENTAL_SENSING, STRUCTURE_SENSING, FARMING_WORLDKEEPING, GENERALIST.
- **niche-1:** a species-niche ID from `SpeciesNicheAnalyticsEngine`. It is distinct from mechanic niches.
- **Child / dynamic niche:** `<PARENT>_A<n>` or `<PARENT>_B<n>`, created by bifurcation. `n` is a global sequence.
- **Bifurcation:** a parent splits into two children. Telemetry event `NICHE_BIFURCATION`. Fields `dynamicNiches`, `bifurcationCount`, `dynamicNichePopulation`.
- **Forced bifurcation:** a guaranteed split at evaluation window 3 when no organic split has occurred.
- **Saturation pressure / penalty:** `clamp(share - 0.14, 0, 0.45)`.
- **Specialization pressure:** `clamp(outcomeYield - 0.07, 0, 0.35)`.
- **Sustained windows / fast path:** consecutive high-pressure evaluations required (2, or 1 when share >= 0.10).
- **Structural partition:** the target fraction of parent artifacts moved into children (10-20%, target 15%).
- **Forced / soft migration:** moving below-mean parent artifacts into children.
- **Lock:** an artifact pinned to a child for 4-6 windows with a x1.30 utility boost.
- **Inversion multipliers:** post-bifurcation fitness boost for children (x1.30) and penalty for parents (x0.88), decaying over 5 windows.
- **Lineage affinity / lineage support:** per (lineage, child) score: +0.05 on success, x0.93 decay, max 0.90.
- **Smoothed support:** the 3-window mean of lineage support.
- **Established child:** occupied for at least 4 consecutive or active windows.
- **Grace period:** 4 windows after birth with no collapse.
- **Collapse hysteresis:** consecutive low-population streak needed before retirement.
- **Re-entry stabilization:** 2-window protection after a child returns from zero.
- **Continuity protection / reconciliation:** restore one occupant to an eligible child before collapse accounting.
- **Eligibility flicker / carryover:** protection toggling on and off; the previous-window flag that bridges a dip.
- **Persistence-weighted threshold:** 0.12 support threshold for children aged at least 8 windows (otherwise 0.20).
- **Zero-share window:** a rollup window in which total child share = 0.
- **Full continuity:** a scenario with no zero-share windows.
- **Extinction / re-emergence (in these docs):** a child niche's occupancy going to zero and returning. Not organism death.
- **Lineage turnover (in these docs):** a change in a child niche's dominant lineage.
- **Rollup window:** one `rollup-snapshots.json` entry, one per season.
- **Harness window:** a session (`[validation] window_complete: n`).
- **Registry window:** one `evaluateBifurcations` call, wall-clock throttled.
- **diversityIndex (rollup):** #niche keys / activeArtifactCount. Not a standard diversity index.
- **turnoverRate (rollup):** cumulative (branch births + collapses) / activeArtifactCount.
- **carryingCapacityUtilization:** active / (active + #niche keys).
- **Scenarios:** explorer-heavy, ritualist-heavy, gatherer-heavy, mixed, random-baseline. warden-heavy appears only in generator probes.
- **READY_FOR_ANALYSIS:** run-manifest status meaning all scenario artifacts were written.
- **DRIFTING / COLLAPSING / CHAOTIC:** doc-level scenario classifications. Undefined.
- **NSER:** Novel Strategy Emergence Rate.
- **PNNC:** Persistent Novel Niche Count.
- **END:** Effective Niches.
- **TNT:** Temporal Niche Turnover.
- **STAGNANT_ATTRACTOR:** diagnostic state from the ecology diagnostic.
- **Branch survival half-life:** median branch lifetime in windows (target >= 2.0).
- **JSD:** Jensen-Shannon divergence between scenario category distributions.
- **Reachability:** fraction of a category's templates sampled in a probe.
