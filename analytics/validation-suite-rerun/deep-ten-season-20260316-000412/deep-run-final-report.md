# ObtuseLoot Deep Ecosystem Validation Report
## Run: deep-ten-season-20260316-000412
## Generated: 2026-03-16
## Configuration: 10 seasons × 4 sessions/season, 18 players, 5 scenarios

---

## SECTION 1: VALIDATION PIPELINE STATUS

### Pipeline Execution Summary

All six stages of the deep validation pipeline completed successfully.

| Stage | Status |
|---|---|
| matrix_execution | PASSED |
| dataset_contract | PASSED |
| completion_marker | PASSED |
| latest_run_pointer | PASSED |
| analytics_ingestion | PASSED (5/5 scenarios) |
| ecosystem_evaluation | PASSED |

### Run Parameters

| Parameter | Value |
|---|---|
| Harness | `obtuseloot.simulation.worldlab.WorldSimulationRunner` |
| Seasons | 10 |
| Sessions per season | 4 |
| Players | 18 |
| Artifacts per player | 3 |
| Encounter density | 5 |
| Telemetry sampling rate | 0.25 |
| Validation profile | true |
| Run root | `analytics/validation-suite-rerun/deep-ten-season-20260316-000412/` |

### Analytics Ingestion Results

| Scenario | Rollups loaded | Telemetry events | Branch survival half-life | Cohorts measured | Estimate status |
|---|---:|---:|---:|---:|---|
| explorer-heavy | 10 | 367,078 | 1.000 | 3 | complete |
| ritualist-heavy | 10 | 364,382 | 1.250 | 4 | complete |
| gatherer-heavy | 10 | 368,801 | 1.333 | 3 | complete |
| mixed | 10 | 408,291 | 1.167 | 6 | complete |
| random-baseline | 10 | 377,055 | 1.000 | 5 | complete |

All five scenarios loaded 10 rollup windows (up from 1 in the constrained run), enabling multi-cohort branch survival analysis for the first time. All estimates are `complete` with no censoring.

---

## SECTION 2: BRANCH SURVIVAL HALF-LIFE ANALYSIS

### Results by Scenario

| Scenario | Half-life (windows) | Cohorts | Censored | Interpretation |
|---|---:|---:|---:|---|
| explorer-heavy | 1.000 | 3 | 0 | Median branch dies within 1 window |
| ritualist-heavy | 1.250 | 4 | 0 | Moderate survival extension |
| gatherer-heavy | 1.333 | 3 | 0 | Longest branch survival |
| mixed | 1.167 | 6 | 0 | Middle-ground, most cohort depth |
| random-baseline | 1.000 | 5 | 0 | Same as explorer, no directional benefit |

### Interpretation

Branch survival half-life ranges 1.000–1.333 windows across the five scenarios. This means the median branch lifetime is between one and one-and-a-third 4-session observation windows (approximately 4–5 sessions). While real differentiation is now measurable between scenarios (gatherer-heavy 33% longer survival than explorer-heavy), the absolute values remain low. Branches form and collapse quickly; the ecosystem has not found stable specialization pathways that persist for multiple seasons.

**Constrained-run comparison**: The constrained run (3 seasons, 2 sessions/season) produced `branch_survival_half_life=1.000` for all scenarios with cohorts=1. The deep run improves on cohort depth (3–6) and reveals scenario separation, but does not produce materially longer branch survival.

**Key finding**: Scenario configuration does influence branch persistence. Ritualist-heavy and gatherer-heavy (high competition_pressure, high ecology_sensitivity) produce modestly longer branch lifetimes. Explorer-heavy's higher mutation intensity (1.20) drives faster branch churn.

---

## SECTION 3: NICHE ECOLOGY ANALYSIS

### Niche Population Distribution (Window 10)

| Scenario | GENERALIST % | niche-1 % | niche-1 pop | Niches active |
|---|---:|---:|---:|---:|
| explorer-heavy | 88.6 | 11.4 | 41,766 | 2 (GENERALIST, niche-1) |
| ritualist-heavy | 88.5 | 11.5 | 41,654 | 2 |
| gatherer-heavy | 88.7 | 11.3 | 41,512 | 2 |
| mixed | 88.6 | 11.3 | 46,079 | 2 |
| random-baseline | 88.9 | 11.1 | 41,625 | 2 |

### Niche Ecology Indicators (Window 10)

| Scenario | niche-1 saturation | niche-1 specialization pressure | niche-1 meaningful outcomes | Opportunity share (GENERALIST) |
|---|---:|---:|---:|---:|
| explorer-heavy | 3,716 | 60.4 | 31,528 | 323,299 |
| ritualist-heavy | 3,762 | 59.6 | 31,573 | 320,856 |
| gatherer-heavy | 3,720 | 56.9 | 31,522 | 325,250 |
| mixed | 4,098 | 65.4 | 35,045 | 359,727 |
| random-baseline | 3,844 | 59.1 | 31,566 | 333,574 |

### Niche Temporal Trend

`niche-1` grew from approximately 8.6–8.9% of the population in window 1 to 11.1–11.5% by window 10. Growth is linear and continuous — there is no sign of saturation at the 10-season horizon. Specialization pressure for `niche-1` climbed from 0 in window 1 to 57–65 in window 10. Saturation pressure rose from ~340–390 to ~3,720–4,100.

**Critical structural observation**: Only one non-GENERALIST niche (`niche-1`) is ever populated across all 10 seasons in all 5 scenarios. `emergingNiches=[]` for every scenario in the analytics long-term summary. The ecosystem produces no niche bifurcation or niche emergence events. GENERALIST remains the dominant niche (88–89%) throughout with essentially no change across windows or scenarios.

**Opportunity dynamics**: The analytics report that only `GENERALIST` holds non-zero opportunity share. `niche-1` and `unassigned` show zero opportunity share — they are fully saturated with no further expansion capacity in the model. This means `niche-1` is effectively locked at capacity and cannot attract additional meaningful specialization pressure to drive niche splitting.

### Lineage-Niche Separation

Every lineage (ashen, stormbound, graveborn, gilded, mirrored, wild-* and all UUID lineages) shows an essentially identical GENERALIST/niche-1 split of approximately **88.4–89.6% GENERALIST / 10.4–12.1% niche-1** across all scenarios. There is no lineage that specializes preferentially into `niche-1`. Niche competition is uniform and undifferentiated.

---

## SECTION 4: EVOLUTIONARY DYNAMICS

### Branch Activity (Window 10)

| Scenario | Births (w10) | Collapses (w10) | Net (w10) | Births (total) | Collapses (total) |
|---|---:|---:|---:|---:|---:|
| explorer-heavy | 269 | 275 | −6 | ~1,733 | ~1,711 |
| ritualist-heavy | 199 | 202 | −3 | ~1,305 | ~1,293 |
| gatherer-heavy | 237 | 245 | −8 | ~1,575 | ~1,562 |
| mixed | 311 | 317 | −6 | ~2,029 | ~2,006 |
| random-baseline | 201 | 200 | +1 | ~1,336 | ~1,322 |

Branch births and collapses are near-equal in every window, indicating a quasi-steady-state churn. Net branch balance across the run is near zero (slight net negative). This is consistent with `adaptationCycleStrength=-0.001 to -0.002` (weakly negative) and `recentDelta=-0.000/-0.000` in the analytics long-term summary — the system has no active adaptation cycles; it has reached equilibrium churn rather than evolutionary expansion.

### Lineage Momentum and Divergence (Window 10)

The named archetypes (ashen, stormbound) dominate by population and momentum across all scenarios. `ashen` maintains the highest branch divergence in all scenarios (54–69 divergence units), indicating it is the most internally differentiated lineage. `wild-35642` consistently ranks second in divergence.

**Specialization trajectories**: Some UUID lineages show extremely high specialization trajectory values (ritualist: `lineage-99f5db57` = 101.2; gatherer: `lineage-7ed3f7a2` = 108.8, `lineage-a27b16b8` = 105.6). These are newly-emerged branching lineages that are rapidly climbing a specialization gradient — but their populations are modest (~6,900–7,300) and the analytics engine flags them as runaway lineages rather than stable specialists.

### Mutation and Competition Events (Window 10)

| Scenario | MUTATION_EVENT | COMPETITION_ALLOCATION | ABILITY_EXECUTION | NICHE_CLASSIFICATION_CHANGE |
|---|---:|---:|---:|---:|
| explorer-heavy | 5,376 | 326,023 | 31,714 | 2,770 |
| ritualist-heavy | 5,400 | 323,596 | 31,737 | 2,745 |
| gatherer-heavy | 5,384 | 327,961 | 31,753 | 2,630 |
| mixed | 5,940 | 362,700 | 35,298 | 2,985 |
| random-baseline | 5,474 | 336,259 | 31,732 | 2,690 |

Mixed scenario drives the most mutation and ability execution activity, consistent with its higher total population and the broadest player behavior mix. `NICHE_CLASSIFICATION_CHANGE` events (2,630–2,985 per window) confirm active niche reclassification happening, but these changes cycle within the same two-niche structure rather than producing new niches.

### Diversity Index

`diversityIndex` drops from 0.0001–0.0002 in window 1 to 0.0000 by window 3–4 and remains at zero through window 10 in all scenarios. The diversity index measures ecological breadth, and its collapse to zero early in the run indicates that distinct ecological niches stopped forming within the first 3–4 seasons. This is not catastrophic collapse — the system stabilized — but it means there is no diversity accumulation occurring over evolutionary time.

---

## SECTION 5: SCENARIO DIFFERENTIATION ANALYSIS

### How Much Do the Scenarios Actually Differ?

| Metric | Explorer | Ritualist | Gatherer | Mixed | Random |
|---|---|---|---|---|---|
| Branch survival half-life | 1.000 | **1.250** | **1.333** | 1.167 | 1.000 |
| Branch births (w10) | **269** | 199 | 237 | **311** | 201 |
| Severity score | 1.242 | 1.354 | 1.294 | 1.269 | 1.347 |
| Runaway lineage count | 30 | 32 | 33 | 32 | 27 |
| niche-1 spec pressure | 60.4 | 59.6 | 56.9 | **65.4** | 59.1 |
| Cohorts (analytics) | 3 | 4 | 3 | **6** | 5 |
| Telemetry events | 367k | 364k | 369k | **408k** | 377k |
| ashen branch divergence | 54.2 | **56.2** | 53.7 | **69.1** | 58.7 |

**Scenario config effects are real but modest**:
- Explorer-heavy: drives highest branch churn. Mutation pressure (1.20×) generates more branch formation but less survival.
- Ritualist-heavy: produces longer branch survival. High ecology_sensitivity (1.10) and competition_pressure (1.15) may create more stable niches.
- Gatherer-heavy: highest branch survival half-life (1.333). Ecology_sensitivity and lineage_drift_window (1.25) appear to provide persistence.
- Mixed: broadest scenario, most activity, most cohort depth (6), highest specialization pressure. Most accurate real-world test.
- Random-baseline: indistinguishable from explorer-heavy in half-life, fewer runaway lineages. Confirms that undirected play approximates high-mutation exploration.

**Key limitation**: Despite meaningfully different player behavior mixes (explorer 70%, ritualist 70%, gatherer 70% of their respective archetypes), the resulting ecosystem structure is nearly identical across all five scenarios. The scenario configuration modulates rate and rhythm but does not produce qualitatively different niche outcomes.

---

## SECTION 6: STABILITY AND FAILURE-MODE CHECK

### Collapse Cascade
**Status: NOT OBSERVED**
`niche_collapse=[]` for all five scenarios. No niche reached zero population. niche-1 grew monotonically. No catastrophic collapse event detected.

### Diversity Crash
**Status: PRESENT (STRUCTURAL, NOT CATASTROPHIC)**
Diversity index collapsed to 0.0000 by window 3–4 in all scenarios and remained there. This indicates the ecosystem reached a monoculture equilibrium quickly. However, population volumes remain high and branch activity continues — this is stagnation rather than crash.

### Runaway Lineage Dominance
**Status: PRESENT**
Severity scores of 1.242–1.354 are elevated. 27–33 runaway lineages per scenario. The named archetypes (ashen, stormbound) consistently dominate by population (47k–55k each) and momentum. However, dominance is distributed — no single lineage takes >15% of total population, preventing true monopoly collapse.

### Branch Saturation / Stagnation
**Status: PRESENT**
Branch births ≈ collapses in every window. Net branch growth is near zero. `adaptationCycleStrength` is weakly negative. The ecosystem is not producing net new stable branches; it is cycling through formation-and-collapse at steady rate. This is the primary evolutionary limitation.

### Missing Niche Emergence
**Status: PRESENT (STRUCTURAL CONCERN)**
Only one niche (`niche-1`) plus `GENERALIST` exist across all 10 seasons and all 5 scenarios. No second specialized niche emerges. `emergingNiches=[]` throughout. The niche engine is not producing niche bifurcation despite 10 seasons of evolutionary pressure and ~365k–408k telemetry events per scenario.

### Carrying Capacity
**Status: STABLE**
`carryingCapacityUtilization=1.0000` in all final windows — the ecosystem runs at full capacity. No over-capacity failure. Population scales linearly with seasons.

---

## SECTION 7: FUSION AND ABILITY PATHWAY ANALYSIS

From prior constrained-run data (deep run does not output per-attempt fusion logs to telemetry):

| Scenario | Fusion adoption rate | Applied | Blocked | Prereq failed |
|---|---:|---:|---:|---:|
| explorer-heavy (3-season) | 0.463 | 54 | 580 | 986 |
| ritualist-heavy (3-season) | 0.451 | 53 | 582 | 985 |
| gatherer-heavy (3-season) | 0.392 | 49 | 492 | 1,079 |
| mixed (3-season) | 0.400 | 54 | 548 | 1,198 |
| random-baseline (3-season) | 0.389 | 38 | 488 | 1,094 |

Fusion adoption is non-trivial (39–46%) with substantial pathway friction (blocked + prereq-failed >> applied). Explorer and ritualist scenarios produce the highest fusion application rates, consistent with their higher branch formation rates observed in the deep run. The deep run's higher branch divergence values (ashen: 54–69 divergence units) suggest that fusion pathways are actively being explored, but the short branch survival half-life (1.0–1.333 windows) means fused lineages do not persist long enough to achieve stable niche separation.

---

## SECTION 8: LONG-TERM TREND SUMMARY

| Metric | Window 1 | Window 5 | Window 10 | Trend |
|---|---|---|---|---|
| niche-1 share (avg) | ~8.7% | ~11.2% | ~11.4% | Linear growth, decelerating |
| Specialization pressure (niche-1, avg) | 0 | ~31.8 | ~60.3 | Linear growth |
| Saturation pressure (niche-1, avg) | ~365 | ~1,894 | ~3,808 | Linear growth |
| Diversity index | 0.0001 | 0.0000 | 0.0000 | Collapsed by w4 |
| Branch birth/collapse ratio (avg) | ~2.3:1 | ~1.01:1 | ~0.99:1 | Converged to balance |
| Emerging niches | 0 | 0 | 0 | None ever |

The ecosystem is on a linear specialization trajectory within a fixed two-niche structure. Specialization pressure and saturation for `niche-1` grow proportionally with seasons. There is no inflection point — no sign that a second niche will emerge or that branch survival will improve substantially with more seasons. The system has reached a structural equilibrium: one real niche, one generalist pool, and steady-state branch churn.

---

## SECTION 9: DESIGN QUESTION RESPONSE

**Primary question**: *When the ecosystem is given enough evolutionary time (10 seasons × 4 sessions), does it deepen into real niche competition and differentiated survival, or does it remain shallow despite the new mechanics?*

**Answer: It deepens statistically but not structurally.**

**What improved with more seasons:**
- Branch survival half-life is now measurable and differentiated by scenario (1.000–1.333 vs. uniform 1.000 in 3-season run)
- Meaningful branch cohorts are now available for analysis (3–6 vs. 1)
- Specialization pressure for niche-1 is substantial (57–65) and growing
- Saturation pressure is high and increasing (~3,700–4,100), confirming active ecological competition
- Lineage branch divergence is significant (ashen: 54–69), indicating genuine internal lineage differentiation
- Scenario configs produce measurable differences in branch activity, survival, and severity

**What did not improve:**
- Only one specialized niche (`niche-1`) ever exists — no second niche emerges in 10 seasons
- Every lineage maintains identical GENERALIST/niche-1 ratios (~88/12); no lineage-niche specialization
- Diversity index collapsed to zero by season 3 and stayed there
- No adaptation cycles (`adaptationCycleStrength ≈ -0.001 to -0.002`)
- No emerging niches at any window
- Branch net growth ≈ zero (formation = collapse); no stable ecosystem expansion

**Interpretation**: The ecosystem is producing the early stages of ecological specialization (niche-1 is real, competitive, and growing) but cannot take the next step of niche bifurcation or lineage-niche separation. The `GENERALIST` pool is too large and too uniform; the only niche that forms does so because of the scenario config's built-in parameters, not as an emergent competitive outcome. All lineages behave identically in their niche allocation, meaning no lineage has found a stable competitive advantage in niche-1 over another.

---

## SECTION 10: NICHE ATTRIBUTION VERIFICATION

All five scenarios produce populated `meaningfulOutcomesByNiche` and `branchContributionByNiche` maps. These contain genuine signal:

| Scenario | meaningfulOutcomesByNiche (niche-1, w10) | branchContribution (niche-1, w10) | branchContribution (GENERALIST, w10) |
|---|---:|---:|---:|
| explorer-heavy | 31,528 | 150 | 117 |
| ritualist-heavy | 31,573 | 110 | 85 |
| gatherer-heavy | 31,522 | 149 | 87 |
| mixed | 35,045 | 142 | 169 |
| random-baseline | 31,566 | 113 | 86 |

**Notable**: The `mixed` scenario is the only one where `GENERALIST` branch contribution (169) exceeds `niche-1` (142). This reflects mixed's broader ecological participation — more lineages contributing branches across both pools simultaneously. All other scenarios show niche-1 contributing more branches than GENERALIST, which is ecologically meaningful: specialized branches form and compete within the one active niche at a higher rate than in the general pool.

---

## SECTION 11: RECOMMENDATIONS

Based on the deep-run findings, the following observations are offered for product direction:

### What is working
1. **Niche specialization engine** is functional. `niche-1` has real ecological pressure (saturation ~3,700–4,100, specialization ~57–65) and meaningful branch contribution.
2. **Scenario config differentiation** works. Explorer/mixed produce faster branch churn; ritualist/gatherer produce longer survival. Scenario configs do reach the simulation.
3. **Branch divergence** is real. ashen consistently achieves 54–69 divergence units — the lineage system is producing genuine internal differentiation.
4. **Analytics pipeline** now produces multi-cohort branch survival estimates with real variance. The rollup_history architecture works correctly when populated.

### What needs attention before production readiness
1. **Single niche ceiling**: The system cannot produce more than one specialized niche. `niche-1` saturates early and no second niche emerges. This limits the depth of competitive differentiation players can experience.
2. **Lineage-niche indistinction**: Every lineage occupies identical GENERALIST/niche-1 proportions. There is no evolutionary pressure causing lineages to specialize into different niches. Meaningful niche competition requires lineage-niche separation.
3. **Diversity index collapse**: Zero diversity index by season 3 suggests the ecological breadth measurement saturates immediately. Either the metric needs recalibration for multi-season runs or the niche classification logic needs broader resolution.
4. **Branch survival depth**: Half-life of 1.0–1.333 windows is short. Players making meaningful evolutionary investments (fusion, awakening) may not see those choices persist long enough to matter.
5. **Adaptation cycle strength**: `-0.001 to -0.002` is weakly declining rather than cycling. No active adaptation response to player behavior. The EDE system (experienceDrivenEvolution) is enabled but not producing measurable adaptation signals.

---

## FINAL DEEP-RUN VERDICT

```
VERDICT: MOSTLY READY
```

**Rationale**:

The ecosystem is mechanically complete and analytically coherent. The five scenarios run cleanly to completion, produce valid telemetry at scale (~365k–408k events per scenario), and the analytics pipeline ingests 10-window rollup histories and produces differentiated branch survival estimates for the first time. Scenario configs produce real, measurable effects on branch activity and survival.

The ecosystem is **NOT READY** for the "deep niche competition and differentiated survival" design goal. Only one specialized niche ever forms. All lineages behave identically in niche allocation. Branch survival is short (1.0–1.333 windows). Diversity index collapses in the first few seasons. No adaptation cycles are active.

The ecosystem **IS READY** for the more modest goal: an artifact progression system where player behavior influences evolutionary rhythm (branch rate, survival, specialization trajectory) even if it does not yet produce fully differentiated ecological niches.

**Gap to READY**: The primary gap is niche proliferation — the system needs a mechanism by which `niche-1` saturation triggers `niche-2` emergence, and by which different lineages develop different niche affinities. Without this, the ecosystem cannot demonstrate the "real niche competition" promised by the design.

**Recommended threshold for READY**:
- At least 2 specialized niches populated across all scenarios by season 10
- At least one lineage showing >20% niche-specific allocation (departing from the uniform 88/12 distribution)
- Branch survival half-life ≥ 2.0 windows in at least 3 of 5 scenarios
- Diversity index remaining above 0.0 through at least season 5
```
