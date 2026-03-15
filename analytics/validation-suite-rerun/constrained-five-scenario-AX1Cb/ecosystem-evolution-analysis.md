# Ecosystem Evolution Analysis
# Run: constrained-five-scenario-AX1Cb
# Dataset root: analytics/validation-suite-rerun/constrained-five-scenario-AX1Cb
# Generated: 2026-03-15

---

## SECTION 1: PER-SCENARIO RESULTS

### Simulation Parameters (all scenarios)
- Players: 18 | Artifacts/player: 3 | Seasons: 3 | Sessions/season: 2
- Telemetry sampling rate: 0.25 | Validation profile: true

---

### explorer-heavy
| Metric | Value |
|--------|-------|
| Telemetry line count | 53,005 |
| Branch births | 71 |
| Branch collapses | 67 |
| Net branch accumulation | **+4** |
| Branch collapse rate | 94.4% |
| Lineage updates | 262 |
| Mutation events | 834 |
| Ability executions | 1,077 |
| Niche classification changes | 410 |
| Distinct lineages | 28 |
| Dominant family rate | 0.406 |
| Branch convergence rate | 0.327 |
| Dead branch rate | 0.048 |
| Diversity index | 0.000105 |
| Turnover rate | 0.00484 |
| Utility density mean (survival proxy) | 39.41 |
| Specialization trajectory mean (cost proxy) | 3.558 |
| Awakening adoption | 52.2% |
| Fusion adoption | **0.0%** |
| Latent activation rate | 33.8% |
| Ability branch diversity | 2.693 |

**Scenario config:** competition_pressure=1.15, mutation_intensity=1.2, ecology_sensitivity=1.1, lineage_drift_window=1.25
**Behavior mix:** EXPLORER=70%, RITUALIST=10%, GATHERER=10%, RANDOM=10%

**Niche populations:** GENERALIST=49,939 (95.6%), niche-1=2,159 (4.1%), unassigned=167 (0.3%)
**niche-1 saturation pressure:** 430.1 | specialization pressure: 8.16
**Dominant lineages:** stormbound=6,912, ashen=6,632, wild-4233=5,830
**Top abilities:** ritual_channel 23.3%, social_attunement 15.4%, altar_signal_boost 8.2%, pattern_resonance 8.0%
**Top branch holder:** ashen=10 branches, wild-35642=10 branches
**Diversity over time:** [3.45 → 3.90 → 4.11] (growing)

---

### ritualist-heavy
| Metric | Value |
|--------|-------|
| Telemetry line count | 52,143 |
| Branch births | 78 |
| Branch collapses | 66 |
| Net branch accumulation | **+12** |
| Branch collapse rate | 84.6% |
| Lineage updates | 268 |
| Mutation events | 807 |
| Ability executions | 1,051 |
| Niche classification changes | 398 |
| Distinct lineages | 34 |
| Dominant family rate | **0.387** (lowest) |
| Branch convergence rate | **0.322** (lowest) |
| Dead branch rate | 0.048 |
| Diversity index | 0.000107 |
| Turnover rate | 0.00515 |
| Utility density mean | 41.99 |
| Specialization trajectory mean | **1.571** (lowest) |
| Awakening adoption | 50.0% |
| Fusion adoption | **0.0%** |
| Latent activation rate | 32.4% |
| Ability branch diversity | **2.712** (highest) |

**Scenario config:** competition_pressure=1.25, mutation_intensity=0.95, ecology_sensitivity=1.2, lineage_drift_window=0.9
**Behavior mix:** RITUALIST=70%, EXPLORER=10%, GATHERER=10%, RANDOM=10%

**Niche populations:** GENERALIST=49,111 (95.5%), niche-1=2,192 (4.3%), unassigned=90 (0.2%)
**niche-1 saturation pressure:** 417.8 | specialization pressure: 8.44
**Dominant lineages:** ashen=6,959, stormbound=6,883, graveborn=3,843
**Top abilities:** ritual_channel 22.4%, social_attunement 14.9%, guardian_pulse 8.7%
**Top branch holder:** ashen=16 branches
**Diversity over time:** [3.34 → 3.91 → **4.15**] (highest final value)

---

### gatherer-heavy
| Metric | Value |
|--------|-------|
| Telemetry line count | 51,293 |
| Branch births | 73 |
| Branch collapses | 65 |
| Net branch accumulation | **+8** |
| Branch collapse rate | 89.0% |
| Lineage updates | 261 |
| Mutation events | 780 |
| Ability executions | 986 |
| Niche classification changes | 390 |
| Distinct lineages | 31 |
| Dominant family rate | 0.401 |
| Branch convergence rate | **0.359** (highest) |
| Dead branch rate | **0.053** (highest) |
| Diversity index | **0.000108** (highest) |
| Turnover rate | 0.00499 |
| Utility density mean | 41.05 |
| Specialization trajectory mean | 2.055 |
| Awakening adoption | **40.1%** (lowest directed) |
| Fusion adoption | **0.0%** |
| Latent activation rate | 33.6% |
| Ability branch diversity | **2.564** (lowest) |

**Scenario config:** competition_pressure=1.05, mutation_intensity=1.1, ecology_sensitivity=0.95, lineage_drift_window=1.4
**Behavior mix:** GATHERER=70%, EXPLORER=10%, RITUALIST=10%, RANDOM=10%

**Niche populations:** GENERALIST=48,415 (95.8%), niche-1=1,979 (3.9%), unassigned=148 (0.3%)
**niche-1 saturation pressure:** 358.7 (lowest among all) | specialization pressure: 8.21
**Dominant lineages:** ashen=6,971, stormbound=6,407, graveborn=3,829
**Top abilities:** ritual_channel 23.4%, social_attunement 15.2%, altar_signal_boost 8.0%
**Standout:** lineage-00514c64 has utility density 56.4 (highest single-lineage density in run)
**Diversity over time:** [3.34 → 3.75 → 3.97] (lowest final value)

---

### mixed
| Metric | Value |
|--------|-------|
| Telemetry line count | **58,152** (highest) |
| Branch births | **80** (highest) |
| Branch collapses | **62** (lowest) |
| Net branch accumulation | **+18** (highest) |
| Branch collapse rate | **77.5%** (lowest — healthiest) |
| Lineage updates | 267 |
| Mutation events | **899** (highest) |
| Ability executions | **1,163** (highest) |
| Niche classification changes | **469** (highest) |
| Distinct lineages | 33 |
| Dominant family rate | **0.454** (highest) |
| Branch convergence rate | 0.337 |
| Dead branch rate | 0.048 |
| Diversity index | **0.0000963** (lowest) |
| Turnover rate | **0.00456** (lowest) |
| Utility density mean | 39.64 |
| Specialization trajectory mean | 2.797 |
| Awakening adoption | 45.8% |
| Fusion adoption | **0.0%** |
| Latent activation rate | 32.4% |
| Ability branch diversity | 2.654 |

**Scenario config:** competition_pressure=1.2, mutation_intensity=1.05, ecology_sensitivity=1.15, lineage_drift_window=1.1 | artifact_population_size=60, generations=27
**Behavior mix:** EXPLORER=30%, GATHERER=30%, RITUALIST=30%, RANDOM=10%

**Niche populations:** GENERALIST=54,828 (95.6%), niche-1=2,385 (4.2%), unassigned=147 (0.3%)
**niche-1 saturation pressure:** 457.2 (highest) | specialization pressure: 9.39 (highest)
**Dominant lineages:** ashen=7,770, stormbound=6,589, wild-35642=4,787
**Top abilities:** ritual_channel 24.2%, social_attunement 14.2%, guardian_pulse 10.5%
**Top branch holder:** ashen=19 branches (highest single-lineage branch count in run)
**Diversity over time:** [3.30 → 3.77 → 3.96]
**Note:** mixed scenario has larger population (60 artifacts, 27 generations) — some metrics not directly comparable.

---

### random-baseline
| Metric | Value |
|--------|-------|
| Telemetry line count | 54,045 |
| Branch births | 74 |
| Branch collapses | 72 |
| Net branch accumulation | **+2** (lowest) |
| Branch collapse rate | **97.3%** (highest — worst) |
| Lineage updates | **275** (highest) |
| Mutation events | 826 |
| Ability executions | 1,088 |
| Niche classification changes | 379 |
| Distinct lineages | 30 |
| Dominant family rate | 0.424 |
| Branch convergence rate | 0.343 |
| Dead branch rate | 0.050 |
| Diversity index | 0.000103 |
| Turnover rate | 0.00503 |
| Utility density mean | 41.11 |
| Specialization trajectory mean | 3.004 |
| Awakening adoption | 37.4% |
| Fusion adoption | **8.6%** (only non-zero) |
| Latent activation rate | **35.9%** (highest) |
| Ability branch diversity | 2.627 |

**Scenario config:** competition_pressure=1.0, mutation_intensity=1.0, ecology_sensitivity=1.0, lineage_drift_window=1.0
**Behavior mix:** RANDOM_BASELINE=100%

**Niche populations:** GENERALIST=50,991 (95.7%), niche-1=2,161 (4.1%), unassigned=124 (0.2%)
**niche-1 saturation pressure:** 415.6 | specialization pressure: 7.61 (lowest)
**Dominant lineages:** stormbound=6,854, ashen=6,756, wild-4233=5,125
**Top abilities:** ritual_channel 21.8%, social_attunement 11.8%, guardian_pulse 9.7%
**Archetype diversity:** deadeye/ravager/strider/vanguard distribution is most even across all scenarios
**Fusion:** bloodstorm=25, convergence=3 (only scenario with fusion events)
**Diversity over time:** [3.32 → 3.83 → 4.04]
**ashen specialization trajectory:** -2.99 (de-specializing — only negative value in run)

---

## SECTION 2: CROSS-SCENARIO COMPARISON

### Consolidated Metrics Table

| Metric | explorer | ritualist | gatherer | mixed | baseline |
|--------|----------|-----------|----------|-------|----------|
| Telemetry lines | 53,005 | 52,143 | 51,293 | 58,152 | 54,045 |
| Branch births | 71 | 78 | 73 | 80 | 74 |
| Branch collapses | 67 | 66 | 65 | 62 | 72 |
| Net branches | +4 | +12 | +8 | +18 | +2 |
| Collapse rate | 94.4% | 84.6% | 89.0% | 77.5% | 97.3% |
| Mutation events | 834 | 807 | 780 | 899 | 826 |
| Distinct lineages | 28 | 34 | 31 | 33 | 30 |
| Dominant family rate | 0.406 | 0.387 | 0.401 | 0.454 | 0.424 |
| Branch convergence | 0.327 | 0.322 | 0.359 | 0.337 | 0.343 |
| Dead branch rate | 0.048 | 0.048 | 0.053 | 0.048 | 0.050 |
| Utility density mean | 39.41 | 41.99 | 41.05 | 39.64 | 41.11 |
| Awakening adoption | 52.2% | 50.0% | 40.1% | 45.8% | 37.4% |
| Fusion adoption | 0.0% | 0.0% | 0.0% | 0.0% | 8.6% |
| Ability branch diversity | 2.693 | 2.712 | 2.564 | 2.654 | 2.627 |
| niche-1 population | 2,159 | 2,192 | 1,979 | 2,385 | 2,161 |
| Final diversity value | 4.11 | 4.15 | 3.97 | 3.96 | 4.04 |

### Directed vs. Baseline Comparison

**Branch formation:** All directed scenarios produce comparable branch counts to baseline (71–80 vs. 74). The directed conditions do not dramatically amplify or suppress branching activity. Mixed is highest (+8% over baseline), gatherer-heavy nearly identical (+1%).

**Collapse pressure:** Baseline has the worst collapse rate (97.3%), indicating that without behavioral direction, branches are extremely fragile. Directed scenarios reduce this, with mixed being most effective (77.5%). Explorer-heavy (94.4%) barely improves over baseline.

**Dominant family concentration:** Mixed (45.4%) slightly exceeds random-baseline (42.4%) — counterintuitively, mixed behaviors do not reduce dominance. Ritualist-heavy is the only scenario reducing dominance below baseline (38.7% vs. 42.4%), suggesting ritual behavior disperses population pressure.

**Niche diversity:** All scenarios are virtually identical: ~4% in niche-1, ~96% in GENERALIST. Directed behavioral scenarios produce no measurable niche differentiation relative to baseline.

**Behavior separation (ability adoption):** The clearest separation signal is awakening adoption — explorer/ritualist substantially exceed baseline (52.2%/50.0% vs. 37.4%). Gatherer-heavy has the lowest (40.1%), notably above baseline but distinctly lower than the other directed scenarios. This is a genuine behavioral signal.

---

## SECTION 3: NICHE DYNAMICS

### Observed Niche Structure

All five scenarios exhibit an identical 3-category niche structure:
- **GENERALIST** (~95.5–95.8% of population): No saturation, no specialization pressure, absorbs all overflow
- **niche-1** (~3.9–4.3% of population): Extremely high saturation (358–457x), moderate specialization pressure (7.6–9.4)
- **unassigned** (~0.2–0.3%): Transient, low utility density (8.8–16.7 vs. niche-1: 49.9–52.3)

### Critical Findings

1. **Niche monoculture:** Only one specialized niche (niche-1) is active across all scenarios and all behavioral configurations. The ecosystem cannot generate or sustain multiple competing niches.

2. **Saturation pathology:** niche-1 operates at 358–457× saturation pressure. This indicates it is genuinely overcrowded — but the system fails to generate additional niches to relieve this pressure. The specialization pressure (7.6–9.4) is present but produces no niche fission.

3. **Empty meaningful outcomes:** `meaningfulOutcomesByNiche` is empty in all scenarios. No ability or behavioral event is being registered as producing a meaningful niche-level outcome. This is a fundamental gap in the feedback loop.

4. **Empty branch contribution by niche:** `branchContributionByNiche` is empty in all scenarios. Branch formations are not being attributed to niche-level dynamics, suggesting niche ecology is disconnected from branching mechanics.

5. **GENERALIST as default sink:** ~95.6% of artifacts sit in GENERALIST with zero saturation and zero specialization pressure. This acts as a pressure-free default that removes competitive incentive to enter niche-1.

6. **Gatherer-heavy shows lowest niche-1 saturation (358.7):** This may reflect gatherer behavior's reduced specialization tendency — consistent with lowest ability branch diversity (2.564) and lowest awakening adoption (40.1%).

---

## SECTION 4: LINEAGE DYNAMICS

### Lineage Counts
- ritualist-heavy: 34 (highest) — ritualist behavior generates most distinct lineage splits
- mixed: 33
- gatherer-heavy: 31
- random-baseline: 30
- explorer-heavy: 28 (lowest)

### Branch Distribution
Lineage branching is concentrated in a small number of lineages across all scenarios:
- **ashen** dominates branch accumulation: 10–19 branches depending on scenario (mixed=19, ritualist=16, gatherer=14, explorer=10, baseline=11)
- **graveborn** maintains consistently 5–6 branches across all scenarios — stable mid-tier brancher
- **wild-4233** is the most variable: baseline=14, mixed=9, ritualist=6 — suggesting behavioral context significantly affects this lineage
- **gilded** is persistently low (1–2 branches) — structurally constrained

### Branch Divergence
Ashen shows the highest branch divergence (6.8–11.1 across scenarios), while gilded is consistently the lowest (1.6–1.9). This 4–7× difference in divergence between lineages suggests meaningful morphological differentiation is occurring within the lineage tree, but is restricted to a subset of lineages.

### Lineage Extinction
Zero across all scenarios (lineage_extinction_rate = 0.0). No lineage has gone extinct. This signals:
- Either insufficient selection pressure to eliminate weaker lineages
- Or population floor protections preventing extinction
- Lack of extinction removes the death side of evolutionary dynamics, flattening selection pressure

### Diversity Over Time
All scenarios show consistent upward trajectory across 3 windows:
- Growth rate ~0.3–0.7 units per window
- Ritualist-heavy achieves highest final diversity (4.15)
- Gatherer-heavy and mixed cluster lower (3.97, 3.96)
- Pattern is monotonic growth — no plateaus, no reversals

---

## SECTION 5: COLLAPSE PRESSURE

### Branch Survival Half-Life
All scenarios report: `branch_survival_half_life = 1.000` (single cohort, complete status)

This metric is immature — only 1 rollup window was available for the half-life cohort analysis. The "complete" status means the full cohort was observed, but with only 1 window there is no longitudinal basis for a reliable half-life estimate.

### Net Branch Pressure (Births - Collapses)
| Scenario | Births | Collapses | Net | Survival ratio |
|----------|--------|-----------|-----|----------------|
| mixed | 80 | 62 | **+18** | 77.5% |
| ritualist-heavy | 78 | 66 | **+12** | 84.6% |
| gatherer-heavy | 73 | 65 | **+8** | 89.0% |
| explorer-heavy | 71 | 67 | **+4** | 94.4% |
| random-baseline | 74 | 72 | **+2** | 97.3% |

Directed scenarios consistently outperform random-baseline in net branch survival. Mixed and ritualist show the strongest net accumulation, while explorer-heavy provides only marginal improvement over baseline despite the amplified mutation_intensity (1.2).

### Collapse Cascade Assessment
No collapse cascades detected. The `runaway_lineages` list is empty and `niche_collapse` list is empty in all analysis reports. While individual branch collapse pressure is high (77–97%), collapses are distributed evenly and do not trigger cascading lineage failures.

### Collapse by Scenario Character
- **Explorer-heavy** despite highest mutation intensity (1.2) has one of the highest collapse rates (94.4%). High mutation without niche pressure amplification does not preserve branches — mutations do not translate to stable evolutionary directions.
- **Ritualist-heavy** with lowest mutation intensity (0.95) achieves second-lowest collapse rate (84.6%). Ritualist behaviors appear to stabilize artifact identity through pattern consistency, reducing branch mortality.
- **Gatherer-heavy** with largest drift window (1.4) sees 89% collapse — wide drift tolerance does not meaningfully reduce pressure.

---

## SECTION 6: BEHAVIOR SEPARATION

### Ability Adoption as Behavioral Signal

**Awakening adoption (strongest signal):**
- explorer-heavy: 52.2% ← clearly elevated
- ritualist-heavy: 50.0% ← elevated
- mixed: 45.8% ← moderate
- gatherer-heavy: 40.1% ← reduced
- random-baseline: 37.4% ← baseline floor

Awakening events are triggered by experience milestones and combat behavior. Explorer and ritualist behaviors, which emphasize environmental engagement and repeated actions respectively, drive awakening consistently above baseline. Gatherer-heavy's lower rate reflects a more resource-conservative strategy that minimizes encounter-driven awakening triggers.

**Fusion adoption (critical gap):**
- random-baseline: 8.6% (bloodstorm=25, convergence=3 observed)
- all directed scenarios: 0.0%

Fusion is entirely absent from all four directed scenarios. This is a strong signal that behavioral specialization is suppressing the fusion mechanic. Possible causes: directed scenarios starve the fusion prerequisite conditions (fusion requires compatible artifact pairs, memory co-activation, or specific encounter sequences that directed behaviors avoid or route around). This represents a fundamental ability gap.

**Memory-driven ability frequency:**
| Scenario | Memory-driven freq |
|----------|--------------------|
| mixed | 1,073 (highest) |
| random-baseline | 974 |
| ritualist-heavy | 965 |
| gatherer-heavy | 883 |
| explorer-heavy | 845 (lowest) |

Explorer-heavy generates the least memory-driven ability activations despite being the most active in exploration. Mixed generates the most, consistent with diverse encounter triggering across behavior types.

**Latent activation rate:**
- random-baseline: 35.9% (highest)
- explorer-heavy: 33.8%
- gatherer-heavy: 33.6%
- ritualist-heavy: 32.4%
- mixed: 32.4%

Directed scenarios show slightly lower latent activation than baseline. This could reflect that directed behaviors narrow the trigger profile, reducing accidental cross-activation of latent abilities.

**Mechanic distribution separation:**
All scenarios share the same top-2: ritual_channel (~22–24%) and social_attunement (~12–15%). The distribution is nearly identical across behavioral conditions. This is a concern — if the mechanic distribution is indistinguishable between ritualist-heavy and explorer-heavy, behavioral identity is not meaningfully encoded in the ability mechanics being exercised.

The only notable mechanic separation:
- guardian_pulse: mixed=10.5%, ritualist=8.7% vs. explorer=7.4%, gatherer absent in top 5
- navigation_anchor: ritualist=5.3%, gatherer=5.0% vs. not in top 7 for mixed or explorer
- collective_relay: gatherer=4.8% — only significant appearance in top mechanics

These differences are modest (1–3%) and do not constitute strong behavioral fingerprinting.

**Archetype distributions reveal strongest behavioral separation:**
- explorer-heavy: strider=186, deadeye=103 → mobility-dominant
- ritualist-heavy: deadeye=205, strider=80 → precision-dominant
- gatherer-heavy: deadeye=162, strider=111 → balanced but deadeye-leaning
- random-baseline: ravager=50, vanguard=45, strider=109 → most diverse archetype distribution

Archetype differences are substantial and represent genuine behavioral encoding in artifact identity.

---

## SECTION 7: FAILURE MODES

### FM-1: GENERALIST MONOCULTURE (ACTIVE)
**Severity: HIGH**
~95.6% of all artifact populations are in GENERALIST across all scenarios. A single named niche (niche-1) captures ~4% of population. The ecosystem has effectively one niche. No scenario produces additional niche emergence. The niche ecology system is either under-powered or its emergence conditions are not being met during these simulation scales.

### FM-2: FUSION DORMANCY IN DIRECTED SCENARIOS (ACTIVE)
**Severity: HIGH**
Fusion adoption = 0.0 in all four directed scenarios. Only random-baseline shows fusion (8.6%). The entire fusion ability path — bloodstorm, convergence — is effectively disabled by behavioral direction. This means one of the newly implemented ability classes produces zero evolutionary pressure under directed play, eliminating a significant potential evolutionary pathway.

### FM-3: EMPTY MEANINGFUL OUTCOMES TRACKING (ACTIVE)
**Severity: MODERATE-HIGH**
`meaningfulOutcomesByNiche` and `branchContributionByNiche` are empty across all five scenarios. These data structures are allocated but never populated. Without outcome attribution by niche or branch contribution, there is no analytical basis for understanding which niches or branches are producing fitness advantages — and no feedback loop to reinforce them.

### FM-4: ZERO LINEAGE EXTINCTION (ACTIVE)
**Severity: MODERATE**
No lineage goes extinct (extinction_rate = 0.0 in all scenarios). In natural evolutionary systems, extinction is a key mechanism for pruning unfit strategies and freeing niche space. The absence of extinction here means all lineages persist regardless of fitness, which flattens selection pressure and reduces the stakes of evolutionary differentiation.

### FM-5: HIGH BRANCH COLLAPSE PRESSURE (PRESENT, PARTIALLY ADDRESSED)
**Severity: MODERATE**
87.1% average collapse rate across all scenarios. Random-baseline at 97.3% is near-total collapse. Directed scenarios reduce this (mixed: 77.5%) but all scenarios are well above a healthy branch persistence threshold. Branches form but rarely survive long enough to establish differentiated evolutionary paths.

### FM-6: RITUAL_CHANNEL HEGEMONY (PRESENT)
**Severity: LOW-MODERATE**
ritual_channel is the #1 ability mechanic in every scenario (21.8–24.2%), including gatherer-heavy and explorer-heavy where ritual behavior should be minimal. This suggests ritual_channel has a broad activation condition that fires universally regardless of behavioral intent, potentially overriding behavioral differentiation in ability expression.

### FM-7: STAGNANT DOMINANCE ACROSS SCENARIOS (PRESENT)
**Severity: LOW**
Dominant family rate across scenarios ranges 0.387–0.454. Variation exists (ritualist notably lower) but no scenario achieves healthy diversity (DFR < 0.35). Mixed scenario — which might be expected to dilute dominance through behavioral diversity — actually produces the highest DFR (0.454).

### FM-8: RUNAWAY DOMINANCE (NOT OBSERVED)
Runaway lineage lists are empty. No single lineage is consuming population in a runaway fashion. The system does have persistent concentration in ashen and stormbound, but at balanced levels (~25% each, which is expected for 2 of ~30 lineages).

### FM-9: COLLAPSE CASCADE (NOT OBSERVED)
No cascade collapse patterns. Individual branch collapses are distributed and independent.

---

## SECTION 8: EVOLUTIONARY DEPTH ASSESSMENT

### Evidence Summary

**For depth (green signals):**
- All-scenarios diversity growing over time (monotonic, positive)
- Ritualist-heavy achieves 34 distinct lineages — meaningful population fragmentation
- Mixed scenario shows best net branch accumulation (+18) — real branching persistence
- Awakening adoption shows genuine behavioral differentiation (37–52% spread)
- Archetype distributions differ substantially by scenario
- Branch divergence within ashen lineage (up to 11.1x in mixed) — internal structural complexity
- Mutation intensity and ecology_sensitivity parameters produce measurable downstream effects
- No runaway dominance, no collapse cascade
- Interference effect diversity active (18–25 distinct types per scenario)

**Against depth (red signals):**
- Single niche (niche-1 ~4% participation) — niche diversity effectively absent
- Fusion ability completely dormant in all directed scenarios (0.0% adoption)
- Empty meaningful outcomes and branch contribution by niche — feedback loops unconnected
- Zero lineage extinction — no selection pressure finality
- 87% average branch collapse rate — most evolutionary experiments immediately erased
- Mechanic distribution nearly identical across all behavioral scenarios (ritual_channel dominates all)
- Branch survival half-life insufficient data (1 cohort, no longitudinal signal)
- All carrying capacity utilization at ~99.99% — population is saturated with no headroom for new strategies to grow before displacing others

### Classification

**MODERATE**

The ecosystem is generating real evolutionary activity: lineage branching, mutation pressure, behavioral differentiation in awakening adoption and archetype distribution, and growing diversity trajectories. These are genuine evolutionary signals.

However, the system operates at shallow ecological depth. The single active niche, zero niche fission, absent meaningful outcome attribution, fusion dormancy, and zero extinction collectively mean the ecosystem lacks the multi-dimensional selection pressure required for deep evolutionary dynamics. Strategies do not occupy meaningfully distinct fitness landscapes — most of the population sits undifferentiated in GENERALIST, and the one available niche is so oversaturated it cannot serve as a selective attractor.

The newly implemented abilities (awakening, fusion, and the expanded mechanic suite including ritual_channel, guardian_pulse, pattern_resonance, social_attunement, etc.) show partial effectiveness:
- **Awakening**: Working — creates behavioral differentiation signal across all scenarios
- **Fusion**: Broken for directed play — zero adoption in all four behavioral scenarios
- **ritual_channel / social_attunement**: Active but scenario-agnostic — not behaviorally gated
- **guardian_pulse / pattern_resonance**: Show modest scenario variation but not strong separation

The evolutionary machinery is present and running, but lacks the ecological depth to convert ability diversity into persistent evolutionary differentiation.

---

## SECTION 9: TOP 5 FOLLOW-UP ACTIONS

### Action 1: Diagnose and Repair Fusion Dormancy in Directed Scenarios
**Priority: CRITICAL**
Fusion adoption drops to zero in all four directed scenarios while registering 8.6% in random-baseline. Investigate fusion prerequisite conditions — specifically whether artifact-pairing eligibility, memory co-activation gates, or encounter density requirements are being systematically bypassed by directed behaviors (EXPLORER, GATHERER, RITUALIST). Fusion represents a major evolutionary pathway; its absence in directed play is a design failure, not a design choice.

### Action 2: Enable Niche Fission — Introduce niche-2, niche-3 Emergence Conditions
**Priority: HIGH**
A single niche capturing 4% of population at 358–457× saturation, with 95%+ sitting in GENERALIST, indicates the niche emergence conditions are either unreachable or too restrictive at this simulation scale. Audit `NicheEcologySystem` for niche birth thresholds. Consider whether specialization pressure alone (7–9) is sufficient trigger, or whether additional behavioral signal diversity is required. Target: ≥3 active niches with <50× saturation each before the next analysis cycle.

### Action 3: Connect Meaningful Outcomes and Branch Contribution Attribution
**Priority: HIGH**
`meaningfulOutcomesByNiche` and `branchContributionByNiche` are empty in all scenarios. These data structures are instrumented but unwritten. Identify whether this is a pipeline omission (tracking code present but not fired) or an architectural gap (the concept of "meaningful outcome" has no defined trigger). Until outcome attribution works, there is no data for the analytics engine to evaluate niche fitness advantage or branch-level evolutionary pressure.

### Action 4: Introduce Lineage Extinction Pressure
**Priority: MODERATE**
Zero extinction across all scenarios means all lineages persist indefinitely regardless of fitness. Introduce floor-level extinction: lineages with population below a threshold for multiple consecutive windows should enter endangered status and eventually be removed. This frees niche space, creates extinction events that sharpen selection pressure, and produces more realistic evolutionary episodics (radiations, die-offs).

### Action 5: Decouple ritual_channel from Scenario-Agnostic Activation
**Priority: MODERATE**
ritual_channel accounts for 21.8–24.2% of all ability mechanics across every scenario, including explorer-heavy and gatherer-heavy where ritual behavior is <10% of the behavior mix. Audit the activation conditions for ritual_channel — if it fires on universal triggers (any session start, any artifact upgrade, any combat), behavioral scenarios will never produce meaningfully different mechanic distributions. True behavior separation requires that behavioral abilities have exclusive or heavily weighted triggers tied to their behavior archetype.

---

## FINAL READINESS VERDICT

```
MOSTLY READY
```

**Rationale:**

The simulation infrastructure is sound, datasets are complete and verified, and the ecosystem is producing real evolutionary signals across all five scenarios. Lineage branching, diversity growth, behavioral differentiation in awakening adoption, archetype variation, and scenario-responsive metrics all function correctly.

However, two critical deficiencies prevent a READY verdict:

1. **Fusion dormancy in directed scenarios** — a complete ability pathway is non-functional under behavioral direction. This is not a minor gap; it means one of the newly implemented ability families produces zero evolutionary impact under the conditions that matter most.

2. **Niche monoculture** — with only one active niche and 95.6% of the population in GENERALIST, the ecosystem lacks the multi-niche ecology required for deep evolutionary competition. The ability system cannot generate meaningful tradeoffs if there is only one specialized environment to compete for.

The ecosystem can support directed development and iterative improvements, but it is not yet producing the depth of evolutionary dynamics that would constitute a fully realized ecosystem. The newly implemented abilities show promise in awakening mechanics and partial behavioral differentiation, but require the above corrections before the ecosystem can be considered evolutionarily deep.

---
*Analysis generated from dataset: constrained-five-scenario-AX1Cb (2026-03-15)*
*Run verification status: SUCCESS (all 5 scenarios)*
*Branch: claude/analyze-ecosystem-evolution-0zgPL*
