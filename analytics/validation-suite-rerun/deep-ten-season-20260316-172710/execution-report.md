# ObtuseLoot Deep Ecosystem Validation — Phase 8 Final Report
**Run ID:** `deep-ten-season-20260316-172710`  
**Date:** 2026-03-16  
**Commit under test:** `34332e0` (dynamic niche bifurcation wired into real simulation path)  
**Configuration:** 10 seasons × 4 sessions/season · 18 players · encounterDensity=5 · telemetrySampling=0.25  

---

## SECTION 1: PIPELINE STATUS

| Stage | Status | Detail |
|-------|--------|--------|
| Maven build | PASSED | `mvn -q -DskipTests compile` succeeded; classes under `target/classes` |
| Deep validation runner config | CONFIRMED | `DEEP_SEASONS=10`, `DEEP_SESSIONS_PER_SEASON=4`, `DEEP_PLAYERS=18`, `DEEP_ENCOUNTER_DENSITY=5`, `DEEP_TELEMETRY_SAMPLING=0.25` |
| Call chain wiring | CONFIRMED | `ArtifactUsageTracker.trackAbilityExecution → NichePopulationTracker.recordTelemetry → evaluateBifurcations` verified in source |
| Scenario matrix execution | PASSED | All 5 scenarios completed; all 4 required artifacts present |
| Analytics ingestion | PASSED | 10 rollup windows loaded per scenario via JSON→properties converter |
| Telemetry artifact generation | PASSED | `ecosystem-events.log`, `rollup-snapshot.properties`, `rollup-snapshots.json`, `scenario-metadata.properties` present for all scenarios |

**Scenarios completed:** explorer-heavy, ritualist-heavy, gatherer-heavy, mixed, random-baseline

**Known infrastructure gap:** Validation profile mode (`validationProfile=true`) suppresses `rollup_history/*.properties` generation in `WorldSimulationHarness` (early-return path, lines 662–682). Analytics ingestion required an offline JSON→properties converter to reconstruct the 10-window series from `rollup-snapshots.json`. The converter is not part of the committed codebase.

---

## SECTION 2: BIFURCATION RESULTS

**Bifurcations observed: ZERO across all 5 scenarios across all 10 windows.**

| Scenario | Bifurcation Events (telemetry) | bifurcationCount (W10) | dynamicNiches (W10) | dynamicNichePopulation (W10) |
|----------|-------------------------------|------------------------|---------------------|------------------------------|
| explorer-heavy | 0 | 0 | `[]` | `{}` |
| ritualist-heavy | 0 | 0 | `[]` | `{}` |
| gatherer-heavy | 0 | 0 | `[]` | `{}` |
| mixed | 0 | 0 | `[]` | `{}` |
| random-baseline | 0 | 0 | `[]` | `{}` |

**Root cause — structural logic error in `EcosystemSaturationModel.pressureFor()`:**

```java
// saturationPenalty > 0 only when utilityDensity < mean
double saturationPenalty = share > 0.10D && nicheRollup.utilityDensity() < meanUtilityDensity
    ? clamp((share - 0.10D) * 1.8D, 0.0D, 0.45D)
    : 0.0D;

// specializationPressure > 0 only when utilityDelta >= 0 (i.e., utilityDensity >= mean)
double specializationPressure = share > 0.20D && utilityDelta >= 0.0D
    ? clamp((share - 0.20D) * 1.3D + nicheRollup.outcomeYield() * 0.25D, 0.0D, 0.35D)
    : 0.0D;
```

These conditions are **mutually exclusive**:
- `saturationPenalty > 0` requires `utilityDensity < mean`
- `specializationPressure > 0` requires `utilityDelta >= 0` ↔ `utilityDensity >= mean`

Dominant niches (RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION) have **high** utility density (above mean), so `saturationPenalty` is always `0.0`. Neither threshold (`SATURATION_THRESHOLD=0.15`, `SPECIALIZATION_THRESHOLD=0.10`) is ever simultaneously satisfied. Bifurcation is **structurally impossible** under this model regardless of population share or encounter volume.

The commit `34332e0` correctly wired the call chain. The fault is in the pressure model, not the wiring.

---

## SECTION 3: CHILD NICHE POPULATION AND PERSISTENCE

**N/A — No child niches were created in any scenario.**

Since `NicheBifurcationRegistry.evaluateBifurcation()` was never invoked with qualifying pressure values, no dynamic niches were registered, spawned, or tracked. The fields `dynamicNiches`, `bifurcationCount`, and `dynamicNichePopulation` are present in all snapshot windows but remain at `[]`, `0`, and `{}` respectively from window 1 through window 10 in all scenarios.

Persistence of child niches across rollup windows cannot be assessed. No data exists to evaluate rollup window survival, population share inheritance, or collapse cascades of dynamic niches.

---

## SECTION 4: CROSS-SCENARIO EVOLUTIONARY RESULTS

### Final State — Window 10

| Scenario | Artifacts | Active Lineages | Shannon Diversity | Branch Births | Branch Collapses | Net Branches | Carrying Capacity |
|----------|-----------|-----------------|-------------------|---------------|------------------|--------------|-------------------|
| explorer-heavy | 183,699 | 33 | 6.0×10⁻⁵ | 240 | 243 | −3 | 99.99% |
| ritualist-heavy | 182,774 | 33 | 5.5×10⁻⁵ | 208 | 211 | −3 | 99.99% |
| gatherer-heavy | 184,344 | 30 | 4.9×10⁻⁵ | 183 | 190 | −7 | 99.99% |
| mixed | 199,594 | 30 | 5.0×10⁻⁵ | 271 | 272 | −1 | 99.99% |
| random-baseline | 185,666 | 32 | 5.4×10⁻⁵ | 218 | 220 | −2 | 99.99% |

### Niche Dominance — Top 2 Niches (W1 → W10)

| Scenario | Dominant Niche W1 | Share W1 | Dominant Niche W10 | Share W10 | Runner-up W10 | Share W10 |
|----------|-------------------|----------|--------------------|-----------|---------------|-----------|
| explorer-heavy | RITUAL_STRANGE_UTILITY | 28.6% | RITUAL_STRANGE_UTILITY | 31.1% | SOCIAL_WORLD_INTERACTION | 27.6% |
| ritualist-heavy | RITUAL_STRANGE_UTILITY | 32.4% | RITUAL_STRANGE_UTILITY | 33.8% | SOCIAL_WORLD_INTERACTION | 32.3% |
| gatherer-heavy | RITUAL_STRANGE_UTILITY | 38.3% | RITUAL_STRANGE_UTILITY | 39.8% | SOCIAL_WORLD_INTERACTION | 31.7% |
| mixed | SOCIAL_WORLD_INTERACTION | 28.9% | SOCIAL_WORLD_INTERACTION | 30.2% | RITUAL_STRANGE_UTILITY | 27.2% |
| random-baseline | RITUAL_STRANGE_UTILITY | 29.8% | RITUAL_STRANGE_UTILITY | 30.9% | SOCIAL_WORLD_INTERACTION | 25.4% |

**Key observation:** RITUAL_STRANGE_UTILITY and SOCIAL_WORLD_INTERACTION together control **57–72% of all artifacts** in every scenario at W10. Their combined share increases monotonically from W1 to W10. No scenario shows niche turnover, niche emergence, or reallocation away from this duopoly.

**GENERALIST population:** Frozen across all 10 windows — never grows, never shrinks. Values: explorer-heavy=1,705; ritualist-heavy=1,746; gatherer-heavy=1,678; mixed=1,875; random-baseline=1,783. The `unassigned` bucket also shows negligible change (141→149 in explorer-heavy). This indicates artifact classification routes are fully deterministic and no overflow into GENERALIST occurs under pressure.

**Scenario differentiation:** Minimal. Gatherer-heavy shows stronger RITUAL dominance (39.8% vs ~30% in others), and mixed shows SOCIAL leading over RITUAL — both consistent with the player composition inputs. However, the overall structural pattern (2-niche duopoly, declining diversity, frozen GENERALIST) is identical across all 5 scenarios.

---

## SECTION 5: BRANCH SURVIVAL HALF-LIFE ANALYSIS

**`branchSurvivalHalfLife` field is not present in window properties output** — the field exists in `EcosystemSnapshot` but is not serialized by the JSON→properties converter or emitted by `TelemetryRollupSnapshotStore`. Analytics cannot track this metric in its current form.

Proxy analysis using birth/collapse ratio:

| Scenario | W1 Births | W1 Collapses | W10 Births | W10 Collapses | Turnover Rate W10 |
|----------|-----------|--------------|------------|---------------|-------------------|
| explorer-heavy | 55 | 27 | 240 | 243 | 0.00263 |
| ritualist-heavy | 47 | 22 | 208 | 211 | 0.00229 |
| gatherer-heavy | 52 | 33 | 183 | 190 | 0.00202 |
| mixed | 61 | 32 | 271 | 272 | 0.00272 |
| random-baseline | 57 | 23 | 218 | 220 | 0.00236 |

**Trend:** In W1, births significantly exceed collapses (ratio 1.9–2.5×), indicating early lineage expansion. By W10, births ≈ collapses (ratio 0.96–0.99), indicating a saturated carrying capacity with turnover but no net growth. Net branch accumulation is slightly negative in all scenarios by W10 (−1 to −7), consistent with late-ecosystem lineage attrition rather than expansion.

This pattern — rapid early growth followed by steady-state attrition — is a sign of a **closed ecosystem with no ecological differentiation pressure**. Lineages compete but do not speciate or migrate to new niches. The half-life of a branch is approximately 1 full season under this regime.

---

## SECTION 6: LINEAGE–NICHE DIFFERENTIATION

**Finding: Zero lineage-niche specialization. All lineages allocate uniformly across niches.**

Top 5 niche allocations (all lineages combined, W10):

| Scenario | #1 Niche | #2 Niche | #3 Niche | #4 Niche | #5 Niche |
|----------|----------|----------|----------|----------|----------|
| explorer-heavy | RITUAL (31.1%) | SOCIAL (27.6%) | MEMORY (17.3%) | NAVIGATION (9.7%) | ENV_SENSING (5.2%) |
| ritualist-heavy | RITUAL (33.8%) | SOCIAL (32.3%) | MEMORY (14.5%) | NAVIGATION (6.9%) | FARMING (3.7%) |
| gatherer-heavy | RITUAL (39.8%) | SOCIAL (31.7%) | MEMORY (14.1%) | ENV_SENSING (7.3%) | niche-1 (2.9%) |
| mixed | SOCIAL (30.2%) | RITUAL (27.2%) | MEMORY (20.8%) | NAVIGATION (8.3%) | FARMING (4.8%) |
| random-baseline | RITUAL (30.9%) | SOCIAL (25.4%) | MEMORY (17.6%) | NAVIGATION (13.9%) | ENV_SENSING (5.3%) |

Named lineages (ashen, stormbound, graveborn, wild-*) show **identical distribution patterns** relative to their population size. No lineage has developed a preferential affinity for any niche. The `niche.branchContribution` metrics show each niche hosting 1–23 branch contributions at W1, but these are driven by niche population size, not by lineage specialization signals.

**Cause:** There is currently no lineage-niche affinity mechanic. Artifacts are classified into niches based on ability type/context, not lineage membership. A lineage that bifurcates into a new branch takes artifacts with it, but those artifacts retain their original niche classification. Without a mechanism to preferentially steer lineage members toward (or away from) specific niches, all lineages converge on the global niche distribution.

**Result:** The ecosystem has 30–33 active lineages but only 1 effective niche allocation pattern. There is taxonomic diversity (many lineage names) but zero ecological differentiation.

---

## SECTION 7: FAILURE MODES

| Failure Mode | Status | Evidence |
|-------------|--------|----------|
| **Bifurcation not triggering** | CONFIRMED | 0 events across 5 scenarios × 10 windows. Root cause: mutual exclusivity in `EcosystemSaturationModel.pressureFor()`. |
| **Uncontrolled niche spawning** | NOT APPLICABLE | Cannot assess — bifurcation never fires. Cap of `DEFAULT_MAX_DYNAMIC_NICHES=8` was never reached. |
| **Child niches disappearing** | NOT APPLICABLE | No child niches were created. |
| **Uniform lineage allocation** | CONFIRMED | All lineages distribute identically to global niche proportions. No lineage-niche specialization gradient exists. |
| **Collapse cascade** | NOT OBSERVED | Branch collapses are proportional to births. No runaway collapse detected. Turnover rate 0.002–0.003 is stable. |
| **Analytics regression (half-life field)** | CONFIRMED | `branchSurvivalHalfLife` not present in rollup properties output. Field serialization gap between `EcosystemSnapshot` and `TelemetryRollupSnapshotStore`. |

**Additional failure mode identified — Diversity monotonic collapse:**

Shannon diversity index drops from ~5×10⁻⁴ (W1) to ~5×10⁻⁵ (W10) in every scenario — a 90% decline over 10 windows. This is an artifact of cumulative artifact accumulation diluting the diversity signal without any new niche differentiation to counteract it. In a healthy bifurcating ecosystem, new niches would introduce fresh diversity signals; in the current state, diversity converges toward zero as artifact counts grow.

---

## SECTION 8: ECOLOGICAL STRUCTURE ASSESSMENT

**The ecosystem is structurally frozen.**

Despite 10 seasons × 18 players × 4 sessions/season of ecological pressure, no ecological structure has differentiated beyond what was present at window 1. The following structural patterns are consistent across all 5 scenarios:

1. **Permanent duopoly:** RITUAL_STRANGE_UTILITY + SOCIAL_WORLD_INTERACTION control 57–72% of all artifacts and increase their combined share monotonically. No mechanism disrupts this equilibrium.

2. **GENERALIST is inert:** Population is fixed at initialization values and never fluctuates. GENERALIST acts as a literal overflow bucket with no ecological role — it neither grows under pressure nor shrinks as niches absorb artifacts.

3. **Diversity extinction trajectory:** Shannon diversity declines ~90% over 10 windows in all scenarios. Without bifurcation creating new niche slots, the ecosystem is converging toward a two-niche monoculture at infinite time.

4. **Player composition has no structural impact:** Explorer-heavy vs. ritualist-heavy vs. gatherer-heavy show different dominant niche orders but identical structural patterns. Ecological pressure from different player behaviors cannot propagate into niche differentiation.

5. **Lineage abundance without ecological function:** 30–33 active lineages exist, but they are all ecologically equivalent — allocated proportionally to the same niche distribution. The lineage system produces taxonomic variety without ecological variety.

6. **Zero response to saturation:** RITUAL_STRANGE_UTILITY holds 30–40% artifact share (well above `SATURATION_THRESHOLD=0.15` cutoff) but generates `saturationPressure=0.0` because its utility density is above mean. The saturation signal that should trigger bifurcation is structurally suppressed by the model's own logic.

**Verdict on the design question:**

> *Does the fixed bifurcation system now convert ecological pressure into real ecological structure?*

**No.** The wiring is correct — `evaluateBifurcations()` is called on every `recordTelemetry()` invocation. But `EcosystemSaturationModel.pressureFor()` delivers `saturationPressure=0.0` for every niche in every window. Because dominant niches have above-average utility density, they satisfy the `specializationPressure` condition (utilityDelta ≥ 0) but fail the `saturationPenalty` condition (utilityDensity < mean), and vice versa for subdominant niches. The model cannot produce both a non-zero saturationPenalty AND a non-zero specializationPressure for the same niche simultaneously. This is a **hard arithmetic impossibility**, not a tuning problem.

The ecosystem remains trapped in a RITUAL\_STRANGE\_UTILITY + SOCIAL\_WORLD\_INTERACTION equilibrium. The fixed bifurcation system converts zero ecological pressure into zero ecological structure.

---

## SECTION 9: TOP 5 NEXT ACTIONS

### Action 1 — Fix `EcosystemSaturationModel.pressureFor()` [CRITICAL — BLOCKING]

**File:** `src/main/java/obtuseloot/evolution/EcosystemSaturationModel.java`  
**Problem:** `saturationPenalty` and `specializationPressure` conditions are mutually exclusive.  
**Fix:** Decouple the two signals so they can independently fire. Replace `utilityDelta >= 0` with a meaningful specialization signal (e.g., low outcome diversity within the niche, or high artifact-per-slot ratio). Both signals must be able to be non-zero for the same niche simultaneously.

Suggested replacement logic:
```java
// saturationPenalty: fires when niche is oversized AND underperforming
double saturationPenalty = share > 0.10D && nicheRollup.utilityDensity() < meanUtilityDensity
    ? clamp((share - 0.10D) * 1.8D, 0.0D, 0.45D)
    : 0.0D;

// specializationPressure: fires independently when niche is oversized AND internally diverse
// (Replace utilityDelta >= 0 with a specialization signal that can fire alongside saturationPenalty)
double outcomeVariance = nicheRollup.outcomeVariance(); // NEW: add to NicheRollup
double specializationPressure = share > 0.20D && outcomeVariance > SPECIALIZATION_VARIANCE_THRESHOLD
    ? clamp((share - 0.20D) * 1.3D + outcomeVariance * 0.25D, 0.0D, 0.35D)
    : 0.0D;
```

### Action 2 — Add `branchSurvivalHalfLife` to rollup serialization [HIGH]

**Files:** `TelemetryRollupSnapshotStore.java`, JSON→properties converter  
**Problem:** `EcosystemSnapshot.branchSurvivalHalfLife` exists but is not serialized to either `rollup-snapshots.json` or rollup `.properties` files.  
**Fix:** Add `snapshot.branchSurvivalHalfLife` serialization in `TelemetryRollupSnapshotStore.writeSnapshotProperties()` and include the field in the JSON snapshot writer. This is required for analytics Phase 5 half-life tracking.

### Action 3 — Introduce lineage-niche affinity signals [HIGH]

**Files:** `NichePopulationTracker.java`, `EvolutionaryLineageTracker.java`  
**Problem:** All lineages distribute uniformly across niches — no lineage-niche specialization gradient exists, making ecological differentiation structurally impossible even if bifurcation fires.  
**Fix:** When a dynamic child niche is created via bifurcation, associate founding lineages with that niche via an affinity score. New artifacts from those lineages should have an elevated probability of niche-classification toward the child niche. This creates the ecological differentiation that bifurcation is supposed to produce.

### Action 4 — Write rollup_history in validation profile mode [MEDIUM]

**File:** `src/main/java/obtuseloot/simulation/worldlab/WorldSimulationHarness.java` (lines 662–682)  
**Problem:** The `validationProfile=true` early-return path skips writing `rollup_history/*.properties` files, requiring an offline converter workaround for analytics ingestion.  
**Fix:** Call `writeRollupHistoryFiles()` before the early return in the validation profile path. This eliminates the analytics gap and makes the deep validation pipeline self-contained.

### Action 5 — Add bifurcation trigger integration test [MEDIUM]

**File:** New test in `src/test/java/obtuseloot/evolution/`  
**Problem:** The bifurcation logic has a structural bug that went undetected because there are no unit tests that directly verify the `EcosystemSaturationModel → NicheBifurcationRegistry` pressure pipeline.  
**Fix:** Add a test that:
1. Creates a `NicheRollup` with share > 0.20, low utilityDensity (below mean), and high outcomeVariance
2. Calls `EcosystemSaturationModel.pressureFor()`
3. Asserts both `saturationPressure >= 0.15` and `specializationPressure >= 0.10`
4. Calls `NicheBifurcationRegistry.evaluateBifurcation()` and asserts a bifurcation is registered

This test would have caught the mutual exclusivity bug before deployment.

---

## FINAL DEEP-RUN VERDICT

```
╔══════════════════════════════════════════════════════════════════╗
║                    FINAL VERDICT: NOT READY                     ║
╠══════════════════════════════════════════════════════════════════╣
║  Bifurcations triggered:      0 / 5 scenarios                   ║
║  Dynamic niches created:      0                                  ║
║  Ecological differentiation:  NONE                              ║
║  Lineage specialization:      NONE                              ║
║  Diversity trajectory:        -90% (W1→W10, all scenarios)      ║
║  Half-life field:             NOT SERIALIZED                    ║
╠══════════════════════════════════════════════════════════════════╣
║  Root blocker: EcosystemSaturationModel.pressureFor() contains  ║
║  mutually exclusive conditions. saturationPenalty requires      ║
║  utilityDensity < mean; specializationPressure requires         ║
║  utilityDelta >= 0 (utilityDensity >= mean). Both signals       ║
║  cannot simultaneously be non-zero for any niche. Bifurcation   ║
║  is mathematically impossible under the current model.          ║
╠══════════════════════════════════════════════════════════════════╣
║  Commit 34332e0 wired the call chain correctly.                 ║
║  The wiring is not the problem.                                 ║
║  Fix EcosystemSaturationModel before re-running.                ║
╚══════════════════════════════════════════════════════════════════╝
```

**Answer to design question:** The ecosystem remains trapped in a RITUAL_STRANGE_UTILITY + SOCIAL_WORLD_INTERACTION equilibrium. The bifurcation system is correctly wired but receives a pressure signal of exactly 0.0 for every niche in every window due to the mutual exclusivity bug. No ecological structure beyond the initial 2-niche duopoly has formed. Ecological pressure is generated (dominant niches hold 30–40% share, well above thresholds) but is silently discarded by the pressure model before it reaches the bifurcation decision point.
