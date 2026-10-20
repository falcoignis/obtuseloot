> Analysis of falcoignis/obtuseloot HEAD fab9f2b, produced 2026-10-19 by a doc-ingest agent. Read from git HEAD. Inferences are labeled "inference:". Historical snapshot: verify against current code before relying on any file:line.

# Slice C: Ability generation, templates, categories, normalization, authenticity, applicability

Repo: the audit checkout, read from git HEAD fab9f2b (2026-04-04, v0.9.50-beta). All docs were read with `git show HEAD:<path>`. Source spot-checks were also read from HEAD unless another commit is named. Nothing in the repo was modified, built, or run.

Evidence labels used in this report:
- **measured**: the doc reports numbers from a run.
- **estimated**: the doc reports analytically derived or "expected" numbers, not a run.
- **not completed**: the doc says the probe did not finish.
- **inference:** my own reasoning, not directly verified.

A scratch computation (`jrand.py`, `jrand2.py`, in this same directory) reimplements java.util.Random's documented 48-bit LCG in Python. It was used to check the forced-probe seeding (see Section 5, item C1). It does not touch the repo.

---

## 1. Per-document findings (chronological by last commit)

### 1.1 docs/deep-ability-expansion.md (2026-03-18, 433eab9)

**Problem:** Generation concentrated on a few families and mechanics. Utility, lineage and ecology weights dominated, and there was no novelty term against the active pool.

**Changes:**
- Added `AbilityDiversityIndex`. It builds signatures (category/family, trigger, mechanic, effect tokens, mechanic-signature tokens, metadata stat vector) and a weighted-blend similarity.
- The active pool is the currently loaded artifacts plus a rolling recent queue, `RECENT_SIGNATURE_LIMIT = 256`. Archives are excluded on purpose, "so extinct abilities do not suppress rediscovery".
- `ProceduralAbilityGenerator` selection became greedy and diversity-aware. It combines base utility, novelty, niche bias, lineage combination bias, motif-anchor bias and ALPHA/BETA variant scaling.
- `AbilityMutationEngine` now responds to lineage and variant.
- `SeededAbilityResolver` feeds results back into the index.

**Four sub-passes:**
1. Initial pass:
   - avg novelty 0.1266, NN similarity 0.8734.
   - Niche JSD: explorer/ritualist 0.2409, explorer/warden 0.5606, ritualist/warden 0.5295.
   - Lineage divergence 0.3534.
   - Verdict: PARTIAL (measured).
2. Novelty tuning:
   - Novelty became a first-order multiplier, with a soft floor penalty below `novelty < 0.15`.
   - Results: novelty 0.2103, similarity 0.7897, JSD fell to 0.0580 / 0.1150 / 0.0082, lineage 0.3887.
   - Verdict: PARTIAL (measured).
3. Gating fix:
   - Split same-niche and cross-niche similarity, and added `nicheConsistencyPenalty`.
   - When utility history is sparse, a provisional niche is inferred from the memory profile.
   - Results: novelty 0.1759, intra-niche novelty 0.5545, JSD 0.1488 / 0.1771 / 0.1408, lineage 0.4071.
   - Verdict: PARTIAL (measured).
4. Cross-niche rebalance:
   - `SAME_NICHE_NOVELTY_WEIGHT` 0.84 -> 0.68 and `GLOBAL_NOVELTY_WEIGHT` 0.16 -> 0.32.
   - Shared exponent 0.78.
   - ALPHA cross-niche pressure `clamp(0.60+mutBias*0.12, 0.52, 0.74)`, BETA `clamp(0.24+retBias*0.08, 0.20, 0.35)`, null 0.50.
   - `HIGH_SIMILARITY_THRESHOLD` 0.82 -> 0.80, with high penalty ALPHA 0.77 and BETA 0.83.
   - Consistency penalty cap 0.24 -> 0.18, mismatch weights 0.09 / 0.05 / 0.03, clamp min 0.82.
   - Verdict: `ABILITY_EXPANSION_TUNING_RESULT: SUCCESS`, but the table has only "Expected Direction", with **no post-change measurement** (estimated).

**Targets stated:** novelty 0.20-0.35 (later 0.20-0.30), similarity < 0.75, intra-niche novelty >= 0.45, niche JSD >= 0.15 -> 0.20+, lineage divergence >= 0.40.

**Status at HEAD:**
- All step-4 constants are present at ProceduralAbilityGenerator.java:40-46 and the helper methods at ~1359-1470.
- The novelty and similarity math is effectively altered by the later Phase 7.1A similarity-weight change (Section 5, C4).

**Superseded:** Partially. The weights survive, but C4 (similarity cap 0.82) neutralizes the novelty floor and most of the high-similarity penalty, and the novelty/JSD targets were restated by later tests.

### 1.2 docs/deep-broad-skill-category-expansion.md (2026-03-19, adb6d1a)

**Problem:** Categories were only implicit naming conventions, and coverage was narrow.

**Changes:**
- Added the first-class `AbilityCategory` enum with 10 values: TRAVERSAL_MOBILITY, SENSING_INFORMATION, SURVIVAL_ADAPTATION, COMBAT_TACTICAL_CONTROL, DEFENSE_WARDING, RESOURCE_FARMING_LOGISTICS, CRAFTING_ENGINEERING_AUTOMATION, SOCIAL_SUPPORT_COORDINATION, RITUAL_STRANGE_UTILITY, STEALTH_TRICKERY_DISRUPTION.
- Each category carries identity text, niche tags and motif tags. A category was attached to every `AbilityTemplate`.
- Added 20 templates, listed in the doc. Examples: mobility.rift_stride, sensing.contraband_tell, warding.sanctum_lock, stealth.hushwire, stealth.paper_trail.
- Added category crowding pressure, category-aware niche and lineage bias, and ALPHA/BETA category nudges.
- Added tests `BroadSkillCategoryExpansionProbeTest` and `NonCombatAbilityIntegrationTest`.

**Results:** Compile and registry tests passed. The deep probe was "heavier than the quick unit pass", so no numbers were captured. Verdict: PARTIAL (not completed).

**Superseded?** No. This is the foundation that later docs build on.

**Superseded:** No. It is the base of the category system; later docs revise the template counts.

### 1.3 docs/deep-mechanic-depth-expansion.md (2026-03-19, 92b3531)

**Problem:** Some categories were too thin or shallow to survive the planned "anti-vanilla" authenticity filter.

Pre-pass template counts: ritual 13, traversal 8, survival 8, sensing 5, defense 5, resource 5, social 5, combat 3, crafting 2, stealth 2 (56 total).

**Changes:** Added 11 templates to `AbilityRegistry`:
- 3 survival (storm_shelter_ledger, exposure_weave, scarcity_compass)
- 4 sensing (faultline_ledger, witness_lag, cache_resonance, route_grammar)
- 4 stealth (shadow_proxy, threshold_jam, dead_drop_lattice, echo_shunt)

New counts: survival 11, sensing 9, stealth 6.

**Results (measured, structural):**
- Spans and max pairwise similarity: survival 0.520, sensing 0.512, stealth 0.472.
- A "representation probe" reached 11/11, 9/9 and 6/6.

Verdict: `MECHANIC_DEPTH_RESULT: SUCCESS`.

**Superseded:** Counts superseded by 1.10, 1.18 and 1.19; the templates are still present.

### 1.4 docs/deep-mechanic-depth-validation.md (2026-03-19, b2a866d)

This was a measurement-only pass.

**Results (measured):**
- Internal redundancy was low: no pair at or above 0.80. Average pairwise similarity: survival 0.2941, sensing 0.3245, stealth 0.3226.
- Survival: 146 hits, 11/11 reached, but top-heavy. ember_keeper 39.7%, top-3 about 82.9%.
- Sensing: only 15 hits, 9/9 reached.
- Stealth: 6 hits, 5/6 reached (threshold_jam missing). Full reach only with a supplemental "StealthMultiSearch".

Verdict: PARTIAL. The doc recommends that authenticity enforcement be "graduated rather than immediately harsh".

This doc is the "before" baseline for 1.5.

**Superseded:** Its baseline is superseded by 1.5 and later probes.

### 1.5 docs/deep-category-exposure-rebalance.md (2026-03-19, 7d73da1)

**Problem:** Survival was top-heavy, and sensing and stealth were under-sampled.

**Changes (generator scoring only):**
1. Intra-category anti-dominance: a rolling recent-template window with a recency multiplier of about ±20%.
2. Category exposure balancing: a rolling category window blended with the active pool, judged against niche-compatible categories, with about +12% swing.
3. Trigger-context smoothing and niche-adjacency smoothing (navigation/mobility, structure-sensing/inspect, social/stealth).
4. A small tail-preservation boost for novelty-qualified, underused templates.

**Results (measured):**
- Survival: hits 146 -> 14, reach 9/11, top-3 82.9% -> 50.0%.
- Sensing: 4 hits, 4/9 reached.
- Stealth: 3 hits, 2/6 reached.
- Broad shares: survival 15.56%, sensing 8.33%, stealth 10.00%.
- Novelty 0.1632, niche divergence 0.1147.

Verdict: PARTIAL. This is the overcorrection that 1.6 addresses.

**Superseded:** Yes, by 1.6, which says 1.5 overcorrected. The constants changed later (C3).

### 1.6 docs/deep-category-exposure-stabilization.md (2026-03-19, 4655ee0)

**Problem:** The 1.5 controls stacked inside one composite score and collapsed category volume.

**Changes:**
1. Category exposure moved to the category-selection stage. Template anti-dominance stays template-local.
2. Category-mass normalization rescales category scores so total mass is stable. At HEAD this is `normalizeCategoryScores`, which scales to a total equal to the category count.
3. The combined penalty is capped at about 25%. At HEAD: `MAX_COMBINED_SELECTION_PENALTY = 0.25`, `boundedPenaltyMultiplier`.
4. A conditional low-volume category boost applies only if the novelty floor, niche compatibility and the high-similarity cutoff all pass.
5. Stronger trigger smoothing for categories with trigger span <= 4.
6. A "stealth-specific intra-category diversification path" (tempered sampling).

**Results (measured):**
- Survival: 49 hits, 11/11, top-3 51.02%.
- Sensing: 31 hits, 9/9, top-3 51.61%.
- Stealth: 30 hits, 6/6, top-3 83.33%, echo_shunt 14/30.
- Explorer-vs-builder separation regressed.

Verdict: PARTIAL.

**Status at HEAD:** No stealth-specific branch exists in `weightedTemplateSelection` (ProceduralAbilityGenerator.java:731-775). The only special path is the generic `isSmallCategory` one.

**Superseded:** Partially. The staging and the 25% cap survive; the stealth-specific path is gone (C14).

### 1.7 docs/deep-stealth-distribution-correction.md (2026-03-19, f3641ae)

**Problem:** stealth.echo_shunt held about 46% of stealth picks, and stealth top-3 was about 83%.

**Changes (small categories, `<= 6` templates, only):**
- Adaptive score flattening with an exponent.
- Stronger recency anti-dominance.
- A light similarity-based intra-category diversity multiplier. At HEAD: `SMALL_CATEGORY_RECENCY_STRENGTH = 1.35`, `SMALL_CATEGORY_DIVERSITY_STRENGTH = 0.20`, `intraCategoryDiversityBias` clamped at 0.78-1.0.

**Results (measured):**
- Stealth: 23 hits, 6/6, top 60.87% (worse), top-3 82.61%.
- Survival top-3 39.13%, sensing top-3 60%.
- Novelty 0.1543, similarity 0.8457.

Verdict: PARTIAL, "not ready".

**Superseded:** Yes, by 1.8 and 1.9. Stealth no longer qualifies as a small category (C10).

### 1.8 docs/deep-intracategory-normalization.md (2026-03-19, 74f37f0)

**Problem:** A small-category "score spread" was surviving the scoring stack.

**Changes:** Post-score, pre-sampling linear compression for categories of `<= 6` templates, `normalized = min + (score-min)*0.45`, plus a mean-relative soft cap targeting 1.30x the mean.

**Test:** `IntraCategoryNormalizationProbeTest` used for the probe.

**Results (measured):**
- Stealth: 22 hits, echo_shunt 59%, top-3 82%.
- Survival top 24%, top-3 57%.
- Sensing: 9 hits, top 33%.

Targets: stealth top < 40% and top-3 < 70%; neither met. Verdict: PARTIAL.

**Superseded** by 1.9 and 1.13.

**Superseded:** Yes, by 1.9 (linear compression replaced by nonlinear).

### 1.9 docs/deep-nonlinear-normalization.md (2026-03-19, 4a8c571)

**Problem:** "Linear compression preserved dominance geometry."

**Changes:** For small categories only, the scores are:
1. normalized to [0,1];
2. passed through a power curve with `gamma = 1.75`;
3. re-expanded;
4. mean-anchored;
5. sampled with a light uniform blend.

**Results (measured):**
- Stealth: 42 hits, 6/6, top 54.76%, top-3 83.33%.
- Survival: 55 hits, top 32.73%, top-3 63.64%.
- Sensing: 15 hits, top 26.67%, top-3 60%.

The validation bar was stealth hits >= 25 (pass), stealth top < ~40% (fail) and top-3 < ~70% (fail); survival top-3 < ~65% passed. Verdict: PARTIAL.

**Superseded** by 1.13 (made global) and by later constant changes.

**Superseded:** Yes, by 1.13 (made global) and by later constant changes.

### 1.10 docs/deep-low-template-category-expansion.md (2026-03-19, 63e330a)

**Problem:** Categories with 6 or fewer templates were structurally fragile.

**Changes:** Added 18 templates.

| Category | Before | After | Templates added |
|---|---:|---:|---|
| Crafting / engineering | 2 | 5 | sequence_splice, machine_rhythm, fault_isolate |
| Combat / tactical | 3 | 6 | reposition_snare, tempo_extract, rush_damper |
| Defense / warding | 5 | 8 | fault_survey, anchor_cadence, false_threshold |
| Resource / logistics | 5 | 8 | queue_sight, relay_mesh, spoilage_audit |
| Social / support | 5 | 8 | role_call, convoy_accord, cover_exchange |
| Stealth | 6 | 9 | ghost_shift, social_smoke, trace_fold |

Total after this pass: 85.

**Results:** The runtime probe "did not finish inside the bounded local execution window". Verdict: PARTIAL (not completed).

**Side effect (inference: not noted in any later doc):** Stealth went to 9 templates, so the `SMALL_CATEGORY_TEMPLATE_LIMIT = 6` layers from 1.7-1.9, built specifically for stealth, stopped applying to stealth.

**Superseded:** Counts revised by 1.18 and 1.19; it disabled the small-category layers for stealth (C10).

### 1.11 docs/deep-cold-template-bootstrapping.md (2026-03-19, e4ca160)

**Problem:** Valid but never-selected templates could stay invisible.

**Changes:**
- Cold detection (`isColdTemplate`): zero recent use with lifetime at or below the warm threshold, or a tiny recent share with lifetime <= 1. At HEAD: `COLD_LIFETIME_WARM_THRESHOLD = 2`, and "tiny" means observed share <= 0.25 x expected share.
- Cold score boost capped at +22% (`COLD_TEMPLATE_MAX_BOOST = 0.22`).
- Category-local cold balancing capped at +12% (`CATEGORY_LOCAL_COLD_BALANCE_MAX_BOOST = 0.12`).
- The boost decays once a template is exposed.
- Added `ColdTemplateBootstrappingProbeTest`.

**Results:** None; Maven bootstrap cost blocked the run. Verdict: PARTIAL (not completed).

**Superseded:** No. Still active (constants unchanged); measured later in 1.21 and 1.22.

### 1.12 docs/deep-cold-completion-validation.md (2026-03-19, 9a9e2c9)

**Changes:** A bounded first-exposure fallback. If the weighted pick has already been seen, then with `UNSEEN_TEMPLATE_OVERRIDE_PROBABILITY = 0.12` the pick is rerouted to an unseen template that passes:
- the novelty floor,
- the same-niche similarity ceiling,
- niche compatibility/adjacency.

The reroute is weighted by sampling score, and it self-disables once every template in a category has been seen. At HEAD: `applyUnseenTemplateOverride`, `qualifiesForUnseenOverride`.

**Results (measured, reduced probe only):**
- Stealth 5 hits, 5/9.
- Defense 3 hits, 3/8.
- Ritual 0 hits.
- Novelty 0.4804.

Verdict: PARTIAL (not completed).

The "-final" doc (1.22) continues this work.

**Superseded:** No. Still active; its final measurement is the generated 1.22.

### 1.13 docs/deep-global-normalization-validation.md (2026-03-19, 1bf19be)

**Changes:** Normalization moved from small categories only to every multi-template category:
1. normalize to [0,1];
2. gamma 1.75;
3. re-expand;
4. mild mean anchoring;
5. `tanh` soft top cap near 1.5x the mean;
6. weighted selection for every category with more than one template.

**Results (measured, custom ProbeRunner):** Top share was under 50% in every category.

| Category | Reached | Top-3 |
|---|---|---:|
| Sensing | 8/9 | |
| Defense | 7/8 | |
| Ritual | 11/13 | |
| Stealth | 8/9 | |
| Traversal | | 74.1% |
| Crafting | | 87.5% |

The novelty JUnit run timed out. Verdict: `GLOBAL_NORMALIZATION_RESULT: FAILED`.

**Kept anyway:** The layer is still present at HEAD as `normalizeCategoryTemplateScores` and `applyMeanRelativeSoftTopCap`, with different constants (Section 5).

**Superseded:** Partially. The layer remains, but with gamma 2.50 and cap 1.28 (C3), and later top-3 compression (9479d48) was added on top.

### 1.14 docs/deep-ability-authenticity-enforcement.md (2026-03-19, 0420bf0)

**Problem:** Eliminate abilities reducible to vanilla enchantments, potions or scalar buffs.

This is a design specification, not a measured pass. It defines:
- a vanilla-equivalence model (effect-token risk, stat-scalar purity, trigger simplicity, secondary-mechanic count);
- hard-reject rules with no penalty-only escape hatch;
- a minimum complexity gate (multi-step, tradeoff, branching, spatial, temporal, environment interaction, combinational);
- a mutation-first rescue path;
- three mandatory hook points (before final selection in `ProceduralAbilityGenerator`, after mutation in `AbilityMutationEngine`, before indexing in `AbilityDiversityIndex`);
- Tier A/B/C category-safe filtering.

Verdict: `ABILITY_AUTHENTICITY_RESULT: SUCCESS`, with no measurements.

**Status at HEAD (spot-check):** `git grep -niE 'vanilla|authentic|complexity gate|stat-only|statOnly' HEAD -- src` returns **nothing**. No runtime authenticity gate exists in main or test source (Section 5, C2).

**Superseded:** Not superseded, but never implemented (C2).

### 1.15 docs/deep-authenticity-validation.md (2026-03-19, b76bafa)

The doc says "measurement-only".

**Results:**
- 6,250 candidates, 1,252 rejected (20.03%), all for "failed complexity gate". Zero vanilla and zero stat-only.
- Rescue 0. Success rate 100% before and after.
- The category table shows reach losses (sensing 6/9, traversal 5/8, survival 7/11, resource 6/8, social 6/8). Verdict: PARTIAL.

**How it was produced (spot-check):** `scripts/deep_authenticity_validation.py` (commit b76bafa) writes this doc. The script:
- parses AbilityRegistry.java with a regex;
- samples a category by baseline shares from `analytics/post-expansion-probe-20260319.txt`, then picks a template uniformly at random;
- "classifies" by keyword matching on the effect-pattern text (`COMPLEXITY_TOKENS`, `VANILLA_TOKENS`, `STAT_TOKENS`) plus a mechanic allowlist;
- runs its "forced" probe round-robin (`items[i % len(items)]`).

It **never runs the Java generator**. The "rejections" are templates whose description lacks a listed keyword. The "reachability losses" are those same templates.

**Superseded:** Its baseline is used by 1.16. Its methodology is invalid as a generator measurement (C2).

### 1.16 docs/deep-post-authenticity-applicability-validation.md (2026-03-19, 84f2ce6)

This was a measurement of a "bounded under-sampled applicability" change, commit 5a67014 "Boost under-sampled template applicability", which has no doc in this slice. At HEAD it is `underSampledApplicabilityBoost` (cap 1.16), `underSampledTriggerRelief` (+0.08), `adjacentTriggerFamily`, and `HIGH_COMPLEXITY_TEMPLATE_FLOOR = 0.67`.

**Results (measured, harness unknown):**
- 14,750 selections, zero rejections.
- The forced probe collapsed: sensing 3/9 with artifact_sympathy 92.47%, crafting 1/5 at 100%, logistics.relay_mesh 92.82%, and others.
- Under normal pressure every category reached all its templates.
- Recovery under normal pressure: trace_fold 125 hits, threshold_jam 33, structure_echo 17, path_thread 4.

Verdict: FAILED.

**Open:** The doc says it "sampled the live generator", but no harness for it exists in the repo.

**Superseded:** Superseded by 1.17 (rank-aware dampening).

### 1.17 docs/deep-rank-aware-applicability-validation.md (2026-03-19, 9506da4)

This measured commit fa80436, "Dampen applicability boosts for dominant templates". At HEAD this is `CategoryApplicabilityProfile`: the rank-0 template gets no boost, and "dominant" templates (rank <= 2 or >= 1.25x the mean) get 25% of the boost.

**Results (measured):**
- Forced reach recovered for traversal, sensing, survival, combat and ritual.
- Resource regressed to 4/8, crafting 2/5, social 4/8.
- New dominants: echo_shunt 87.88%, sequence_splice 87.76%, stockpile_tide 83.53%.
- Normal-pressure persistence: trace_fold 87, threshold_jam 69, structure_echo 120, path_thread 90.

Verdict: FAILED. The layer was retained.

**Superseded:** Superseded by 1.18, 1.19 and 7.1A-C. The layer remains.

### 1.18 docs/deep-targeted-template-repair.md (2026-03-20, 39f66de)

**Problem:**
- Trigger-saturation asymmetry.
- Cross-category leakage: lineage_fortification and last_light_cache sitting in the wrong categories.
- Adjacency gaps leaving dust_memory, trace_fold and threshold_jam unreachable.

**Changes:**
1. Extended `adjacentTriggerFamily`. At HEAD it matches the doc: 9 cases, ProceduralAbilityGenerator.java:1227-1240.
2. Recategorized two templates:
   - `evolution.lineage_fortification`: RITUAL -> SURVIVAL.
   - `survival.last_light_cache`: DEFENSE -> SURVIVAL.
3. Added `HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING = 0.88` (`isHighFrequencyContextMisaligned`), which applies only to ON_LOW_HEALTH and ON_TIME_OF_DAY_TRANSITION templates with no niche overlap with their category.

**Results: estimated.** "After values are analytically derived from scoring delta". Verdict: SUCCESS.

**Residual:** path_thread and structure_echo in DEFENSE.

**Superseded:** Its residual was handled by 1.19. Its quoted test thresholds are stale (C13).

### 1.19 docs/deep-taxonomy-mismatch-correction.md (2026-03-20, 38d3c57)

**Changes:**
- `consistency.path_thread`: DEFENSE -> TRAVERSAL.
- `consistency.structure_echo`: DEFENSE -> SENSING.
- DEFENSE now has 5 templates.

**Results:** expected hits "1-3" and "1-4" (estimated). Verdict: SUCCESS. The doc is framed as "before Phase 3 freeze".

**Errors:** Some of its claims are contradicted elsewhere (Section 5, C5 and C6).

**Superseded:** No. The categories are as stated at HEAD. Its metric claims are contradicted (C5, C6).

### 1.20 docs/convergence-validation-v0.9.15.md (2026-03-20, 0f4427e)

This doc is off-topic for ability generation. It covers ConvergenceEngine recipe identity transformation:
- no-op target guard;
- reworked recipes `horizon-syndicate`, `sky-bastion`, `worldpiercer`, `citadel-heart` and `reaper-vow`;
- runtime snapshot fields no longer copied into the replacement artifact.

It also lists suggestions such as multi-variant outcomes and a recipe hit-rate audit. There are no distribution metrics. The version was set to 0.9.15.

**Superseded:** No later doc in this slice.

### 1.21 docs/phase-7.1b-distribution-revalidation.md (2026-03-22, ef0bd05, v0.9.35)

**Background:** Phase 7.1A (commit 73ea859, no doc in this slice) did the following:
- `AbilityDiversityIndex` similarity weights summed to 0.82, "guaranteeing minimum novelty of 0.18";
- raised the stealth category weight (mob x0.07, chaos x0.04) and added a stealth-affinity score boost (mob x0.15);
- added NAVIGATION to STEALTH's niches and ENVIRONMENTAL_ADAPTATION to DEFENSE's;
- lowered `RegulatoryEligibilityFilter.MIN_ELIGIBILITY` from 0.70 to 0.10.

**Results (measured with ColdCompletionDeepValidationTest, IntraCategoryNormalizationProbeTest, ColdTemplateBootstrappingProbeTest):**
- Normal probe: every category fully reached. Survival top 19.1%, defense top 41.6%.
- Forced probe: every category's top share > 50%. Resource 2/8, crafting 1/5, social 2/8, stealth hushwire 98.4%.
- Long tail: one hit each, "generated by the cold-template override".

**Conclusions the doc draws:**
- Stealth's 98.4% is a "forced-probe artifact", because "hushwire is the only template with `stealth` affinity".
- The Resource/Crafting/Social failures are "confirmed real".
- It flags a test-criteria mismatch: the strict criteria require defense == 8 and ritual == 13.
- Verdict: FAIL, and Phase 7.1C is required.

**Errors:** Both conclusions are contradicted by spot-checks (Section 5, C1 and C7).

**Superseded:** Followed by 7.1C (46b2fb1, no doc in slice) and by 1.22. Its root-cause conclusions are contradicted (C1, C7).

### 1.22 docs/deep-cold-completion-validation-final.md (2026-03-24, c194b00)

This file is **generated output**. `ColdCompletionDeepValidationTest` writes it (`OUTPUT = Path.of("docs/deep-cold-completion-validation-final.md")`), and the commit is "chore: update generated analytics artifacts and validation doc from phase-8.3 test runs". The working-tree copy is presumably rewritten by any test run, so read HEAD only.

**Normal probe (5,000 generations, measured):** All 10 categories were fully reached. Top share was 8.2%-21.5%, top-3 24.2%-61.4%, and every template had more than one hit.

**Forced probe (500 per category, measured):**
- Resource 2/8 (convoy_instinct 96%).
- Crafting 1/5 (pattern_forge 100%).
- Social 2/8.
- Stealth hushwire 98.4%.
- Survival gentle_harvest 87%.
- Combat reposition_snare 92.2%.

Verdict: `COLD_COMPLETION_RESULT: FAILED`.

**Errors:** The category counts do not match the HEAD registry (Section 5, C8), and the forced numbers are explained by seeding (C1).

**Superseded:** No. This is the latest. The file is regenerated by each test run, and HEAD's copy is stale relative to the HEAD registry (C8).

---

## 2. Consolidated current model of ability generation (HEAD source, ProceduralAbilityGenerator.java unless noted)

### 2.1 Pipeline: `generate(artifact, stage, memoryProfile)` (lines 210-310)

1. **Context.**
   - Utility history is parsed from `artifact.getLastUtilityHistory()`.
   - A lineage is assigned through `LineageRegistry`.
   - `AdaptiveSupportAllocation` and mutation-ecology pressure come from `ExperienceEvolutionEngine`.
2. **Family ranking.** `scoreFamily` ranks the families; this is used only for fallback picks and the profile name.
3. **Genome.** `GenomeResolver.resolve(seed)`, then `GenomeMutationEngine.mutate(stage, lineage influence, ecology pressure, mutation opportunity, lineage tendencies)`, then `LineageGenomeInheritance.inherit`, then `ExperienceEvolutionEngine.applyExperienceFeedback`, then `LatentTraitActivationResolver`.
4. **Regulatory gating.** `RegulatoryGateResolver.resolve(artifact, genome, memory, lineage, pressureEngine)` produces open gates. `RegulatoryEligibilityFilter.filter` then keeps templates whose multiplicative gate weight is >= `MIN_ELIGIBILITY = 0.18` (RegulatoryEligibilityFilter.java:7). Gate weights are 1.22/1.10 when the gate is open and 0.58/0.78 when it is closed. If nothing survives, the full registry is used.
   - Inference: by my reading of lines 245-256 and the `compositeTemplateScore` signature, this filter is the only place the genome influences template choice. `activeInterference` is only written to an artifact diagnostic field.
5. **Niche profile.** `EcosystemRoleClassifier.classify(utilityHistory)` sets the niche profile. Without history, `inferDominantNiche(memoryProfile)` applies hand-written formulas over the mobility, chaos, discipline, survival, trauma, boss and aggression weights. A lineage EXPLORATION_PREFERENCE above 0.20 can switch the dominant niche to NAVIGATION.
6. **Variant and pool.**
   - `resolveVariantProfile` provides ALPHA/BETA. It returns null when `ObtuseLoot.get()` is null.
   - The active pool is `AbilityDiversityIndex.activePool(seed)`, and the motif anchor comes from the same index.
7. **RNG.** `new Random(seed ^ archetype/evolution/drift/awakening/convergence path hashCodes ^ memory pressure)`.
8. **Picks.** `picks = stage >= 4 ? 3 : 2`. `selectTemplates` calls `selectNextTemplate` repeatedly. Fallback fills come from family pools via `pickTemplate`.
9. **Recording.**
   - `recordRecentSelections` updates the recent template window (96), the recent category window (144) and the lifetime counts.
   - `diversityIndex.record` updates the active pool.
   - `lineageRegistry.recordDescendantBias` uses the observed bias and ecological pressure.

### 2.2 `selectNextTemplate` (two-stage: category, then template)

**Stage A: category choice.**
1. Group the remaining templates by category.
2. For each category, compute composite template scores and take the best template's score.
3. Compute `categorySelectionScore` as best score x sqrt(categoryWeight) x nicheWeight^0.65 x bounded(categoryExposureBias) x lowVolumeCategoryBoost.
4. `normalizeCategoryScores` rescales so the scores sum to the number of categories.
5. Roll weighted.

**Stage B: template within the chosen category** (`weightedTemplateSelection`). Single-template categories skip this stage.
1. Compute composite scores (2.3).
2. `normalizeCategoryTemplateScores`: min-max normalize, then `pow(norm, 1/GLOBAL_CATEGORY_COMPRESSION_GAMMA)` with gamma = 2.50 (concave, which lifts the lower scores), then re-expand, then lerp toward the mean by `GLOBAL_CATEGORY_MEAN_ANCHOR_BLEND = 0.35`.
3. `applyMeanRelativeSoftTopCap`: a `tanh` cap above the mean with `GLOBAL_CATEGORY_TOP_CAP_RATIO = 1.28`.
4. `applyCategoryLocalColdBalancing`: up to +12% for cold templates, applied only when the category has a hot/cold split.
5. Weights:
   - Small category (<= 6 templates, currently only DEFENSE and CRAFTING): `score^0.20` x a [0.97, 1.03] relative factor, then lerp toward 1.0 by 0.45. This is nearly uniform.
   - Other categories: `sqrt(score)`.
6. `applyConditionalTopThreeCompression`: if the estimated top-3 weight share is above `CATEGORY_TOP_THREE_SHARE_THRESHOLD = 0.55`, binary-search a lerp of all weights toward their mean until top-3 <= `CATEGORY_TOP_THREE_SHARE_TARGET = 0.50`. This applies only to categories with more than 3 templates.
7. Weighted roll.
8. `applyUnseenTemplateOverride`: with probability 0.12, if the pick has already been seen, reroute to a qualifying unseen template, weighted by sampling score.

Niche weight and category weight are applied twice:
- inside the composite template score (`nicheWeight^1.58`, `categoryWeight`);
- again at the category stage (`nicheWeight^0.65`, `sqrt(categoryWeight)`).

This compounds a top-down niche control.

### 2.3 Composite template score

`baseCompositeTemplateScore`, lines 607-629, is a product of these terms, in code order:

1. `scoreTemplate` (lines 404-440). It contains:
   - `ecologicalYieldScore` clamped to [0.42, 1.28];
   - hand-coded additive bonuses for trigger/mechanic/affinity versus memory-profile weights;
   - a utility-history blend: `score*(1-0.45c) + utilityBias*1.9c`, where c is confidence;
   - multiplicative factors: ecology modifier, `triggerSaturationPenalty` (static registry trigger counts with `TRIGGER_SATURATION_WEIGHTS`, clamp [0.72, 1.02]), `nicheTemplatePenalty` (static niche and category template counts, clamp [0.74, 1.06]), lineage template influence, mechanic-lineage weight, ecological correction, mutation influence and support opportunity.
2. `nicheWeight ^ NICHE_WEIGHT_EXPONENT(1.58)`. This uses family-bias constants (for example MOBILITY 1.58 vs 0.63 under NAVIGATION) and a match/adjacent/miss factor of 1.42/1.10/0.70; clamp [0.48, 2.20].
3. `categoryWeight`: niche match 1.10/1.02/0.94, a memory bias per category, a lineage-tendency bias per category, and `variantCategoryBias`; clamp [0.80, 1.55].
4. `boundedPenaltyMultiplier(recentTemplateRecencyBias, nicheConsistencyPenalty)`. Recency is +0.28 / -0.45 versus the expected 1/N share, with extra strength for small categories. The combined penalty is capped at 25%.
5. `lineageCombinationBias`, then `motifAnchorBias`.
6. `noveltyBonus`: `1 + (intra^0.78*0.68 + global^0.78*0.32*crossNichePressure) * noveltyPressure`. Novelty pressure is 0.66 when there is no variant; ALPHA 0.80 x mutationBias; BETA 0.22 x retentionBias.
7. `noveltyFloorPenalty`: below `NOVELTY_FLOOR = 0.15`, down to 0.65.
8. `activePoolSimilarityPenalty`: bands at 0.62 and 0.80.
9. `intraCategoryDiversityBias`: small categories only.
10. `tailPreservationBias`: up to +10%.
11. `coldTemplateBoost`: up to +22%.
12. `contextAlignDampening`: x0.88.

A further multiplier comes from `underSampledApplicabilityBoost` (up to x1.16), which is rank-aware within the category.

### 2.4 Categories and template counts

| Category | Pre-broad (doc 1.3) | After 1.3 | After 1.10 | After 1.18/1.19 | 7.1B (03-22) | HEAD |
|---|---:|---:|---:|---:|---:|---:|
| TRAVERSAL_MOBILITY | 8 | 8 | 8 | 9 | 9 | 9 |
| SENSING_INFORMATION | 5 | 9 | 9 | 10 | 10 | 10 |
| SURVIVAL_ADAPTATION | 8 | 11 | 11 | 13 | 13 | 14 |
| COMBAT_TACTICAL_CONTROL | 3 | 3 | 6 | 6 | 6 | 8 |
| DEFENSE_WARDING | 5 | 5 | 8 | 5 | 5 | 5 |
| RESOURCE_FARMING_LOGISTICS | 5 | 5 | 8 | 8 | 8 | 8 |
| CRAFTING_ENGINEERING_AUTOMATION | 2 | 2 | 5 | 5 | 5 | 5 |
| SOCIAL_SUPPORT_COORDINATION | 5 | 5 | 8 | 8 | 8 | 8 |
| RITUAL_STRANGE_UTILITY | 13 | 13 | 13 | 12 | 12 | 12 |
| STEALTH_TRICKERY_DISRUPTION | 2 | 6 | 9 | 9 | 9 | 9 |
| **Total** | 56 | 67 | 85 | 85 | 85 | **88** |

- The HEAD count was verified by parsing `template(` calls in AbilityRegistry.java: 88 calls.
- `survival.press_through`, `combat.rupture_window` and `stealth.intercept_line` first appear in 4b74c37 (Phase 7.4, v0.9.39). I did not check whether they are new or renamed.
- bbcf0b4 (03-24) changed the families of several templates, and the mechanics of fault_survey, anchor_cadence, false_threshold and moon_debt. It did not change categories.
- Families: PRECISION, MOBILITY, SURVIVAL, CHAOS, CONSISTENCY, BRUTALITY.
- Category niche sets are in AbilityCategory.java. For example TRAVERSAL is {NAVIGATION, MOBILITY_UTILITY}, SENSING is {ENVIRONMENTAL_SENSING, STRUCTURE_SENSING, INSPECT_INFORMATION}, and SURVIVAL is {ENVIRONMENTAL_ADAPTATION, GENERALIST}.

### 2.5 Acceptance metrics and where they are enforced

| Metric | Threshold | Enforced where | Notes |
|---|---|---|---|
| Stealth hits | >= 25 | IntraCategoryNormalizationProbeTest (asserts) | |
| Stealth reach | all templates | IntraCategoryNormalizationProbeTest | |
| Stealth top share | < 0.50 | IntraCategoryNormalizationProbeTest | Doc 1.18 cites < 0.60 (stale) |
| Stealth top-3 | < 0.80 | IntraCategoryNormalizationProbeTest | Doc 1.18 cites < 0.85 (stale) |
| Survival hits / top-3 | >= 25 / < 0.66 | IntraCategoryNormalizationProbeTest | |
| Sensing hits / top / top-3 | >= 18 / <= 0.35 / < 0.65 | IntraCategoryNormalizationProbeTest | |
| Crafting, traversal top-3 | < 0.70 | IntraCategoryNormalizationProbeTest | |
| Ritual top-3 | < 0.55 | IntraCategoryNormalizationProbeTest | |
| trace_fold applicability boost | > 1.0 and <= 1.16; hushwire > 1.0; adjacency contains STRUCTURE_SENSE/DISCOVERY | IntraCategoryNormalizationProbeTest | Tests the mechanism itself |
| Stealth count | == 9 (hard-coded) | ColdTemplateBootstrappingProbeTest | |
| Stealth/defense/ritual | full reach, top < 0.50; defense contains false_threshold; ritual contains moon_debt and oath_circuit | ColdTemplateBootstrappingProbeTest | Names specific templates |
| Avg novelty | >= 0.17 | ColdTemplateBootstrappingProbeTest, AbilityNoveltyTuningProbeTest | Trivially met by construction (C4) |
| Avg similarity | < 0.83 | same two tests | Maximum possible similarity is 0.82 (C4) |
| intra-niche novelty > global novelty | ordering | same two tests | |
| Niche JSD | at least one pair >= 0.17 | AbilityNoveltyTuningProbeTest | Docs also cite 0.20+, 0.15, 0.08 |
| Lineage divergence | >= 0.30 | AbilityNoveltyTuningProbeTest | Doc 1.1 target 0.40; bbcf0b4 message says ">= 0.38" |
| Every category aggregate share | > 2% | BroadSkillCategoryExpansionProbeTest | |
| No category share | > 26% | BroadSkillCategoryExpansionProbeTest | |
| JSD explorer/builder, fighter/ritualist | >= 0.08 | BroadSkillCategoryExpansionProbeTest | |
| Lineage JSD pairs | >= 0.05 | BroadSkillCategoryExpansionProbeTest | |
| Forced-probe strict criteria: every template reached and multi-hit, top <= 0.50, top > 1/N + 0.02 ("no uniform flattening"), stealth == 9, defense == 8, ritual == 13 | as listed | ColdCompletionDeepValidationTest **computes but does not assert** | Only asserts the report file exists. Defense == 8 and ritual == 13 cannot pass against the 5/12 registry |
| Top-3 share target | <= 0.50 when > 0.55 | **Production runtime**: `applyConditionalTopThreeCompression` | A metric hard-coded as a control |
| Doc-only targets | stealth top < 40% and top-3 < 70% (1.8, 1.9); novelty 0.20-0.35 / 0.20-0.30 and similarity < 0.75 (1.1); "no template above 50%" (1.12, 1.13) | Not enforced | |

The Surefire plugin is declared in pom.xml with no excludes. Inference: all these tests run by default.

---

## 3. Layer-on-layer history

Each layer added, with the symptom it was patching. Commit dates are all 2026-03-18 to 03-24.

1. **Active-pool novelty and similarity penalty** (433eab9 and predecessors).
   - Symptom: family/mechanic convergence.
   - Retuned four times to chase novelty, similarity and JSD bands.
2. **Same-niche / cross-niche novelty split and niche-consistency penalty** (5c6bc8b).
   - Symptom: the global novelty push flattened niche identity.
3. **Category taxonomy, category crowding pressure and category bias** (adb6d1a).
   - Symptom: narrow coverage.
4. **Template additions** (92b3531 +11, 63e330a +18).
   - Symptom: thin categories at risk under the planned authenticity filter.
5. **Recency anti-dominance, category exposure balancing, trigger and niche-adjacency smoothing, tail preservation** (7d73da1).
   - Symptom: survival top-heavy; sensing and stealth under-sampled.
6. **Category-stage relocation, category-mass normalization, 25% penalty cap, low-volume boost, narrow-trigger smoothing** (4655ee0).
   - Symptom: layer 5 overcorrected and collapsed category volume.
7. **Small-category flattening, stronger recency, intra-category diversity bias** (f3641ae).
   - Symptom: stealth.echo_shunt dominance.
8. **Small-category linear compression and 1.30x cap** (74f37f0).
   - Symptom: same.
9. **Small-category nonlinear gamma compression, mean anchor, uniform blend** (4a8c571).
   - Symptom: same; "linear preserved dominance geometry".
10. **Cold-template boost and category-local cold balance** (e4ca160).
    - Symptom: templates never entering circulation.
11. **Unseen-template override at 12%** (9a9e2c9).
    - Symptom: cold templates still never chosen.
12. **Global nonlinear normalization and `tanh` soft cap** (1bf19be).
    - Symptom: intra-category dominance in larger categories. The pass reported FAILED, but the layer was kept.
13. **Conditional top-3 compression** (9479d48, "Tighten top-3 category concentration control"; no doc in this slice).
    - Symptom: traversal and crafting top-3 above 70%.
14. **Under-sampled applicability boost and trigger relief** (5a67014).
    - Symptom: trace_fold and threshold_jam unreachable.
15. **Rank-aware applicability dampening** (fa80436).
    - Symptom: layer 14 created new dominants. The pass reported FAILED, but the layer was kept.
16. **Adjacency expansion, two recategorizations, context-misalign dampening x0.88** (39f66de).
    - Symptom: reachability gaps and cross-category leakage.
17. **Two further recategorizations** (38d3c57).
    - Symptom: zero-hit DEFENSE templates, attributed to niche mismatch.
18. **Phase 7.1A** (73ea859):
    - similarity weights summed to 0.82 (symptom: the novelty band);
    - stealth category weight and affinity boost (symptom: stealth reachability);
    - niche-set additions;
    - MIN_ELIGIBILITY 0.70 -> 0.10 (later 0.18 in 6d4eb19).
19. **Phase 7.1C** (46b2fb1, "Fix intra-category score distribution", v0.9.36; no doc in this slice).
    - Probably the source of gamma 2.50, anchor 0.35 and cap 1.28 (inference: not verified by diff).
20. **bbcf0b4**: ALTAR_SIGNAL_BOOST added to the chaos-weight scoring branch.
    - Symptom: altar_resonance coverage gap in ColdTemplateBootstrappingProbeTest.

### Layers that exist mainly to make a distribution metric pass

Each item is stated from the docs' own framing plus the code.

- **Layer 13, `applyConditionalTopThreeCompression`.** The share target (0.50) sits directly in production code as a control.
- **Layers 7-9 and 12.** Each was justified by stealth top-share or top-3 numbers against the stated < 40% / < 70% targets.
- **Layers 10-11.** Stated goal: "all templates reachable", "9/9 reachable without supplemental probing".
- **Layer 18, similarity weights summing to 0.82.** The 7.1B doc says it is "guaranteeing minimum novelty of 0.18", against the test assertion novelty >= 0.17.
- **bbcf0b4.** Its commit message frames it as making ColdTemplateBootstrappingProbeTest 12/12 coverage pass.
- **Layers 14-15.** Framed as recovering specific named templates.

---

## 4. Emergence lens (classification only)

**Outcome-driven (selection from real play or ecosystem outcomes):**
- **Utility-history blend in `scoreTemplate`.** Up to 1.9 x confidence weight on `utilityScoreForTemplate(mechanic, trigger)`.
- **`EcosystemRoleClassifier`.** Derives the niche from the utility-history signal, when history exists.
- **`ExperienceEvolutionEngine` hooks.** `ecologyModifierFor`, `applyExperienceFeedback`, `adaptiveSupportFor` and mutation-ecology pressure. What these measure belongs to another slice; I did not verify it.
- **Lineage system.** `LineageInfluenceResolver` template, mutation and ecological correction; `recordDescendantBias` from the observed selections.
- **Genome mutation chain**, feeding the regulatory gates.
- **Which channels are live in the probes** (verified by grepping the five probe tests at HEAD for generator construction, `LineageRegistry` and `setLastUtilityHistory`):
  - `ExperienceEvolutionEngine` (ecology modifier, experience feedback, support allocation) and `ArtifactEcosystemSelfBalancingEngine` are null in every probe. All constructions are either the single-arg constructor (line 117, all null) or the 4-arg constructor with a null ecosystem engine, which passes a null experience engine.
  - No probe sets utility history (`setLastUtilityHistory` does not appear), so the utility-history blend and history-based niche classification are inactive.
  - Lineage is live only in some sub-probes: ColdCompletionDeepValidationTest normal-probe ritualist scenario; ColdTemplateBootstrappingProbeTest; the AbilityNoveltyTuningProbeTest lineage-divergence probe; the BroadSkillCategoryExpansionProbeTest lineage probes. Those tests inject the lineage bias tendencies by hand (for example `lineage.evolutionaryBiasGenome().add(SUPPORT_PREFERENCE, 0.12)`), so the values are not accumulated from play.
  - IntraCategoryNormalizationProbeTest and the forced probe use only the single-arg constructor.
- Inference: the acceptance metrics therefore measure the top-down layers plus hand-injected lineage bias, not outcome-driven selection.
- Inference: because normalization, the soft cap and top-3 compression run after scoring, they also bound how far a real-outcome score advantage can translate into selection share.

**Population-state feedback with fixed rules (frequency-dependent, not outcome-driven):**
- Active-pool novelty bonus, similarity penalty and motif anchor, against what is currently in circulation.
- Category exposure balancing, against the active pool and recent categories.

**Top-down control (quota, cap, floor, forcing, normalization), reacting to generator-internal selection counts or static registry structure:**
- Hand-authored template registry and category taxonomy, including manual recategorizations.
- Static trigger-saturation and niche/category template-count penalties.
- Hand-coded niche, family and category weight constants on the memory profile. These use an observed-play input with a fixed mapping; I classify them as top-down mapping.
- Niche-consistency penalty.
- Recency anti-dominance; small-category recency and diversity bias.
- Tail-preservation boost, low-volume category boost, cold boost and cold balancing.
- Unseen override (a forced exposure quota at a fixed 12% probability).
- Under-sampled applicability boost and its rank-aware dampening.
- Trigger smoothing and context-misalign dampening.
- Category-mass normalization, global nonlinear normalization, mean anchoring, soft top cap and top-3 compression; small-category near-uniform sampling.
- 25% combined-penalty cap.
- Novelty floor penalty; similarity weight sum 0.82 (a floor on novelty by construction).
- MIN_ELIGIBILITY, a pass/fail gate threshold. The gate inputs are genome-derived; classified as mixed.
- Authenticity: designed as a hard top-down filter, but not implemented at runtime (C2).

---

## 5. Contradictions and staleness

**C1. Forced-probe results are explained by correlated RNG seeding, not by score structure.** This is inference from source plus computation; the confirming JVM rerun was not done.

- `ColdCompletionDeepValidationTest.runForcedCategoryProbe`:
  - calls `weightedTemplateSelection` by reflection 500 times per category;
  - uses `new Random(artifactSeed ^ pressure ^ ordinal)` with sequential seeds `20_000_000 + ordinal*100_000 + i`.
- The roll is the first `nextDouble()` call.
- Reimplementing java.util.Random's LCG with each category's real profile pressure gives the following. Under near-uniform weights, which is what the compression layers push toward, the predicted pick index is close to what the doc reports:

| Category (pressure) | p5 / p50 / p95 roll | Predicted (uniform weights, registry order) | Observed in doc 1.22 |
|---|---|---|---|
| Sensing (8) | .063 / .084 / .148 | echo_locator 77%, vein_whisper 23% | 77.0% / 21.4% |
| Survival (8) | .003 / .029 / .964 | gentle_harvest 90%, press_through 10% (first and last) | 87.0% / 10.6% |
| Combat (6) | .655 / .722 / .742 | reposition_snare 100% | 92.2% (+ tempo_extract 6.6%) |
| Defense (8) | .274 / .570 / .591 | fault_survey 74%, sanctum_lock 26% | 74.6% / 24.8% |
| Resource (7) | .392 / .415 / .435 | convoy_instinct 96%, queue_sight 4% | 96.0% / 4.0% |
| Crafting (7) | .253 / .320 / .340 | pattern_forge 100% | 100% |
| Social (7) | .417 / .438 / .505 | rally_ledger 84%, mercy_link 16% | 68.6% / 31.4% |
| Ritual (8) | .890 / .910 / .931 | oath_circuit 65%, moon_debt 35% | 61.0% / 37.2% |
| Stealth (7) | .009 / .076 / .096 | hushwire 100% (first in registry) | 98.4% |
| Traversal (6) | .899 / .966 / .986 | skyline_fold (last) | 80% (+ rift_stride 18.4%) |

- The 0.2% singletons match the unseen override. The second draw falls below 0.12 about 57-62 times per category, and the override fires once per unseen template.
- Caveat on the table: doc 1.22 was generated from a registry with a different traversal/ritual order (C8). Its traversal and ritual rows used HEAD order for the prediction.
- Cross-run stability supports the seeding explanation, with no JVM run needed. Phase 7.1B (03-22, v0.9.35) and doc 1.22 (03-24) can be compared directly. Between them, 7.1C (46b2fb1) changed intra-category scoring, 7.4 (4b74c37) added templates, and bbcf0b4 changed mechanics and the ALTAR scoring.
  - Forced top shares stayed identical for defense (74.6%), resource (96.0%), crafting (100.0%) and stealth (98.4%). Survival was nearly identical (87.2% vs 87.0%).
  - They moved for combat (67.8% to 92.2%; 6 to 8 templates), traversal (67.4% to 80.0%; 9 to 10 templates in doc 1.22's registry) and ritual (90.4% to 61.0%; 12 to 11).
  - Sensing (71.4% to 77.0%) and social (74.4% to 68.6%) shifted moderately with unchanged counts. Inference: their roll bands straddle two template positions, so weight changes move the split between the same two templates.
  - The results stayed stable when scoring changed, and moved mainly when list length or order changed.
- This contradicts Phase 7.1B's "Resource/Crafting/Social failures are confirmed real" and "tail templates... lose every subsequent competition".
- It also suggests that the forced-probe columns in docs 1.16, 1.17 and 1.21 are unreliable. Their harness for 1.16 and 1.17 is not in the repo, so whether they used the same seeding is unknown.

**C2. No authenticity gate exists at runtime.**
- Doc 1.14 claims hooks in the generator, the mutation engine and the diversity index; `git grep` finds none.
- Doc 1.15's numbers come from the keyword classifier in `scripts/deep_authenticity_validation.py`, with uniform sampling and a round-robin "forced" probe. Its top-3 % is therefore arithmetic (for example 3/12 = 25% for ritual), compared against real-probe baselines.
- The "zero stat-only / vanilla" outputs in docs 1.16 and 1.17 come from "the existing detector shape", presumably that same keyword detector (inference).

**C3. Constants drifted from the docs to HEAD.**

| Constant | Doc value | HEAD value |
|---|---|---|
| Normalization gamma | 1.75 (1.9, 1.13) | `GLOBAL_CATEGORY_COMPRESSION_GAMMA = 2.50`, applied as `pow(x, 1/gamma)` (line 57, 848) |
| Soft top cap | ~1.5x (1.13); 1.30x (1.8) | 1.28 (line 59) |
| Recency swing | ±20% (1.5) | +0.28 / -0.45 (line 49, 1086) |
| Category exposure | +12% (1.5) | 0.08 (line 50) |
| MIN_ELIGIBILITY | 0.70 -> 0.10 (7.1A) | 0.18 (RegulatoryEligibilityFilter.java:7, changed in 6d4eb19) |

**C4. Similarity weights sum to 0.82, which disables several other layers.**
- The weights in `AbilityDiversityIndex.similarity` are 0.08+0.05+0.09+0.11+0.20+0.16+0.13 = 0.82 (AbilityDiversityIndex.java:140-154). Similarity can therefore never exceed 0.82.
- `NOVELTY_FLOOR = 0.15` is unreachable, because minimum novelty is 0.18. So `noveltyFloorPenalty` never fires, and the novelty gates in tail preservation, low-volume boost and unseen override always pass.
- The `HIGH_SIMILARITY_THRESHOLD = 0.80` band spans only 0.80-0.82. The maximum high-similarity penalty is therefore about x0.92, versus doc 1.1's "~69% multiplier at similarity 0.90".
- The test assertions novelty >= 0.17 and similarity < 0.83 hold by construction.

**C5. "0 hits in all probe history" is contradicted.**
- Doc 1.19 says path_thread and structure_echo had 0 hits, and doc 1.18 says "0/0 hits".
- Doc 1.16 reports structure_echo 17 and path_thread 4 normal-pressure hits. Doc 1.17 reports 120 and 90.

**C6. Doc 1.19 cites metadata fields that do not exist.**
- It names "authentic", "complexity", "novelty" and "scarcity" fields (for example path_thread "complexity: 0.75, authentic: 0.28").
- The `AbilityMetadata` record has discovery, exploration, information, ritual, social and worldUtility values. The quoted numbers are those positional values (path_thread's metadata is 0.75, 0.90, 0.62, 0.28, 0.30, 0.55).
- Complexity is computed by `templateComplexityScore` (line 1201) from metadata breadth and `utilityPotential`.

**C7. Phase 7.1B's stealth explanation is wrong.**
- It says "hushwire is the only template with `stealth` affinity". At 73ea859, 46b2fb1 and HEAD, 13 templates have "stealth" in their affinities set.
- hushwire's 98.4% matches C1 instead: it is the first stealth template in registry order, and the stealth roll band is 0.009-0.096.

**C8. Doc 1.22 matches no committed registry I checked.**
- Its category counts are traversal 10 (including `evolution.niche_architect`) and ritual 11.
- At c194b00, bbcf0b4, f9ea06d and 4b74c37, niche_architect is in RITUAL, giving traversal 9 and ritual 12.
- Inference: the doc was generated from an uncommitted working tree.
- Its "Defense reachable: 5/8" and "Ritual reachable: 11/13" lines are hard-coded denominators in the test.

**C9. Verdicts without measurement.** Of the five SUCCESS verdicts, four have no measured post-change generation results: 1.1 (rebalance), 1.14, 1.18 and 1.19. Three FAILED passes (1.13, 1.16, 1.17) left their layers in place.

**C10. Small-category layers no longer apply to stealth.** They were built for stealth when it had 6 templates; stealth now has 9. They currently apply only to DEFENSE (5) and CRAFTING (5), where sampling is nearly uniform.

**C11. Date inconsistencies.** Doc 1.18's header is dated 2026-03-20 and references a "2026-03-19 audit". Doc 1.19 cites the repair as "(2026-03-19)".

**C12. Lineage-divergence thresholds disagree.** Doc 1.1 says >= 0.40, the test asserts >= 0.30, and the bbcf0b4 commit message says >= 0.38.

**C13. Stale test thresholds in doc 1.18.** It quotes test thresholds (stealth top < 0.60, top-3 < 0.85) that differ from HEAD (< 0.50, < 0.80).

**C14. Doc 1.6's stealth-specific diversification path is absent at HEAD.**

---

## 6. Open questions for the project owner

1. **Forced-probe parity as a goal.** Is "every template fully reached, multi-hit, with top share <= 50% under single-category forced selection" a real design goal, or a historical tuning artifact? C1 indicates the forced probe mostly measures RNG seeding. Should the probe be decorrelated (for example by hashing seeds), and the 7.1B and 1.22 forced conclusions re-run, before any further tuning?
1a. **Do the asserting probes suffer the same seeding issue?** This matters more than the forced probe, because these tests are the enforced gates (IntraCategoryNormalization, ColdTemplateBootstrapping, AbilityNoveltyTuning, BroadSkillCategoryExpansion). They call `generate()` with `seedBase + i` artifact seeds. `generate()` seeds `new Random(seed ^ constant path hashCodes ^ pressure)`, and its first `nextDouble()` is the stage-A category roll in `selectNextTemplate`. That roll may be similarly correlated across sequential seeds. Inference: run-local state (recency, cold, active pool) changes the weights between calls and may mask it; unverified. Are live artifact seeds random 64-bit values (unverified, other slice)?
2. **The 0.50 top-3 target.** Is it (`CATEGORY_TOP_THREE_SHARE_TARGET`, enforced in production by `applyConditionalTopThreeCompression`) intended gameplay design? Or was it added to pass probe thresholds (commit 9479d48)? Given the "truly emergent" goal, should a share cap override real outcome advantage?
3. **Stealth < 40% / < 70% targets.** Where did they come from (docs 1.8, 1.9)? Are they still targets? The tests now use < 0.50 and < 0.80.
4. **Scope of "all templates reachable".** Is it a requirement per artifact context, per scenario, or in aggregate? The normal probe at HEAD already reaches all 88 with top share 8-21.5% (doc 1.22).
5. **"No uniform flattening".** Is top > 1/N + 0.02 (ColdCompletionDeepValidationTest) a deliberate requirement that some template must lead?
6. **Novelty and similarity bands.** Which, if any, are design targets: novelty 0.20-0.35 / 0.20-0.30 / >= 0.17; similarity < 0.75 / < 0.83? The 0.82 weight sum makes the test thresholds trivially true and disables the novelty floor and most of the high-similarity penalty (C4). Was that intended?
7. **Niche JSD target.** Doc 1.1 says 0.20+; doc 1.1 rebalance >= 0.15; doc 1.18 >= 0.08; tests >= 0.17 for one pair and >= 0.08 for two pairs. Which is real?
8. **Lineage-divergence target.** 0.40, 0.38 or 0.30 (C12)?
9. **Authenticity enforcement.** Is a runtime gate still intended? Currently there is only the design doc and a keyword-matching script (C2). Are the "0 stat-only / vanilla" claims in docs 1.15-1.17 acceptable as evidence?
10. **Hard-coded strict criteria.** Should ColdCompletionDeepValidationTest assert its criteria? Should defense == 8 and ritual == 13 be updated? 7.1B says they anticipated "3 additional defense templates". Should a test write into `docs/` at all, given the working-tree churn?
11. **Missing harness.** Which harness produced the forced columns of docs 1.16 and 1.17? None is in the repo.
12. **Small-category layers.** Is it intended that they now apply only to DEFENSE and CRAFTING, with near-uniform sampling (C10)?
13. **ALPHA/BETA in tests.** Is `resolveVariantProfile` null in all unit-test probes? Inference: `ObtuseLoot.get()` is null without the plugin, so the ALPHA/BETA novelty and category paths would be untested by every acceptance probe.
14. **Probe coverage of real feedback.** Should acceptance probes include utility history and the ecology/experience engine? No probe wires them, and lineage appears only with hand-injected bias (Section 4). For a "truly emergent" engine, should any acceptance gate measure selection driven by real outcomes?
15. **Persistence of selection state.** Recency, lifetime, cold and unseen state live in memory per generator instance. Do they persist or reset across server restarts? Is there one production generator instance? (Other slices.)
16. (Moved to question 1a.)
17. **Unrecorded runs.** Was doc 1.22 generated from an uncommitted registry (C8)? What did 7.1C (46b2fb1) change, and was its result ever recorded? The 03-24 forced probe still FAILS.
18. **Kept-after-FAILED layers.** Should the layers from FAILED passes (global normalization, applicability boost, rank-aware dampening) stay, or be re-evaluated once the probe is fixed?
19. **Metric-driven recategorization.** Recategorizations (1.18, 1.19) were driven by probe reachability. Should future category moves require a gameplay/semantic rationale independent of metrics?

---

## 7. Glossary (this slice)

**Templates and the registry**
- **Template**: a hand-authored `AbilityTemplate` in `AbilityRegistry` (id, name, category, family, trigger, mechanic, effect and variant text, metadata). There are 88 at HEAD.
- **Category**: `AbilityCategory`, one of 10 gameplay buckets, each with a niche set and motif tags.
- **Family**: `AbilityFamily`, an older stat-flavor grouping: PRECISION, MOBILITY, SURVIVAL, CHAOS, CONSISTENCY, BRUTALITY.
- **Trigger / mechanic**: `AbilityTrigger` (for example ON_BLOCK_INSPECT) and `AbilityMechanic` (for example MEMORY_ECHO).
- **Metadata**: `AbilityMetadata` utility domains, trigger classes, affinities, six value fields and a trigger-budget profile. `utilityPotential` and `ecologicalYieldScore` derive from it.
- **Recategorization**: moving a template to another category to fix a niche mismatch or leakage.
- **Phase 3 freeze**: the freeze milestone that doc 1.19 targets.
- **Protected systems / protected tests**: the docs' term for systems a pass must not change, and the commit term for the asserting probe tests.

**Niches and variants**
- **Niche**: a `MechanicNicheTag` (for example NAVIGATION, PROTECTION_WARDING, RITUAL_STRANGE_UTILITY). `NicheTaxonomy.nichesFor(mechanic, trigger)` maps templates to niches.
- **Dominant niche**: the artifact's primary niche, from utility history or inferred from the memory profile.
- **ALPHA / BETA variant**: `NicheVariantProfile` niche-population variants. ALPHA is exploratory (mutationBias); BETA is a refiner (retentionBias).
- **Memory profile**: `ArtifactMemoryProfile(pressure, chaos, discipline, aggression, survival, mobility, boss, trauma)`.

**Novelty and diversity**
- **Active pool**: signatures of loaded artifacts plus the recent queue (256). This is the novelty reference set.
- **Motif anchor**: an active-pool signature that candidates are biased to resemble, to form families.
- **Intra-niche (same-niche) novelty / global novelty**: 1 minus the max similarity to same-niche items, or to all items.
- **Cross-niche novelty pressure**: the weight on global novelty per variant.
- **Niche divergence**: Jensen-Shannon divergence between mechanic or family distributions of different niche profiles. **Lineage divergence** is the same between lineages.

**Distribution metrics and probes**
- **Reachability**: the count of templates with at least one hit in a probe.
- **Top share**: the share of the most-picked template. **Top-3 share**: the combined share of the top three.
- **Multi-hit / long tail**: templates with more than one hit.
- **Uniform flattening**: a distribution with no leader (top <= 1/N + 0.02).
- **Normal probe**: full `generate()` calls across scenario memory profiles (explorer, builder, fighter, ritualist, survivor; or explorer-heavy and others).
- **Forced (category-forced) probe**: repeated direct calls to `weightedTemplateSelection` with a single category's templates.
- **Supplemental probe**: an extra targeted search used to prove a template is reachable.

**Cold start**
- **Cold template**: low recent and lifetime usage (lifetime <= 2).
- **Unseen template**: lifetime usage 0.
- **Cold boost**: +22%. **Cold balancing**: +12%.
- **Unseen override**: a 12% reroute to an unseen template.
- **Warm-up**: exiting cold status after exposure.

**Applicability and triggers**
- **Under-sampled applicability**: a boost of up to 1.16 for high-complexity (>= 0.67), scarce (>= 0.35) templates whose trigger has an adjacent family compatible with the dominant niche.
- **Rank-aware**: the rank-0 template gets no applicability boost; dominant templates get 25% of it.
- **Adjacent trigger family**: `adjacentTriggerFamily`, a table of near triggers.
- **Trigger smoothing**: partial credit for near-match triggers. "Narrow" means category trigger span <= 4.
- **Trigger saturation**: a penalty for triggers shared by many templates (static).
- **Context-alignment dampening**: x0.88 for ON_LOW_HEALTH or ON_TIME_OF_DAY_TRANSITION templates with no niche overlap with their category.

**Balancing and normalization layers**
- **Small category**: <= 6 templates (`SMALL_CATEGORY_TEMPLATE_LIMIT`). **Low-volume category**: recent category frequency < 0.24, which earns a boost of up to 14%.
- **Category exposure balancing**: steering category share toward its expected share among niche-compatible categories.
- **Anti-dominance / recency bias**: a multiplier pushing a template's recent share toward 1/N in its category.
- **Tail preservation**: up to +10% for scarce templates that are novelty-, niche- and similarity-qualified.
- **Bounded penalty stacking**: the combined penalty is capped at 25%.
- **Category-mass normalization**: rescaling category scores to a fixed total.
- **Global nonlinear normalization**: the per-category min-max, power-curve, re-expand and mean-anchor transform.
- **Mean anchoring**: lerping scores toward the category mean (0.35).
- **Soft top cap**: `tanh` compression above the mean (1.28x).
- **Top-3 compression**: lerping weights toward the mean until top-3 <= 0.50.

**Gating**
- **Regulatory gate / eligibility**: genome-derived open gates. `RegulatoryEligibilityFilter` keeps templates with weight >= 0.18.
- **Authenticity / vanilla-equivalent / stat-only / complexity gate / mutation rescue / Tier A-B-C**: the doc 1.14 design vocabulary for rejecting scalar-buff abilities. Not implemented in source.

**Other**
- **Utility history**: `UtilityHistoryRollup`, per-artifact recorded utility signal by mechanic and trigger, with a confidence value.
- **Ecology modifier**: the `ExperienceEvolutionEngine.ecologyModifierFor` multiplier.
- **RESULT tokens**: per-doc verdict lines such as `GLOBAL_NORMALIZATION_RESULT: FAILED`.
