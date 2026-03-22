# Phase 7.1B — Distribution Re-Validation Report

**Date:** 2026-03-22
**Version audited:** 0.9.35
**Branch:** claude/validate-distribution-determinism-QZFBd
**Tests run:** `ColdCompletionDeepValidationTest`, `IntraCategoryNormalizationProbeTest`, `ColdTemplateBootstrappingProbeTest`
**All tests confirmed passing on live build post-7.1A (commit 73ea859)**

---

## Background

Phase 7.1 failed due to: severe category dominance, unreachable templates under forced conditions, and long-tail inactivity.

Phase 7.1A applied the following corrections:
- `AbilityDiversityIndex`: similarity weights scaled to sum=0.82 (from 1.00), guaranteeing minimum novelty of 0.18
- `ProceduralAbilityGenerator`: raised `STEALTH_TRICKERY_DISRUPTION` category weight coefficients (mob×0.07, chaos×0.04) and added stealth-affinity score boost (mob×0.15)
- `AbilityCategory`: added `NAVIGATION` niche to `STEALTH_TRICKERY_DISRUPTION` and `ENVIRONMENTAL_ADAPTATION` niche to `DEFENSE_WARDING`
- `RegulatoryEligibilityFilter`: lowered `MIN_ELIGIBILITY` from 0.70 to 0.10

Phase 7.1B re-runs the full validation suite:
- `ColdCompletionDeepValidationTest`: 5,000 normal-probe generations + 500 forced-category generations per category
- `IntraCategoryNormalizationProbeTest`: natural within-category distribution probes
- `ColdTemplateBootstrappingProbeTest`: cold-start reachability and novelty measurement

---

## 1. Reachability Re-Check

### Normal Probe (5,000 total generations across 5 scenarios)

| Category | Templates | Reached | Zero-hit | Status |
| --- | ---: | ---: | --- | --- |
| Traversal / mobility | 9 | 9/9 | none | PASS |
| Sensing / information | 10 | 10/10 | none | PASS |
| Survival / adaptation | 13 | 13/13 | none | PASS |
| Combat / tactical control | 6 | 6/6 | none | PASS |
| Defense / warding | 5 | 5/5 | none | PASS |
| Resource / farming / logistics | 8 | 8/8 | none | PASS |
| Crafting / engineering / automation | 5 | 5/5 | none | PASS |
| Social / support / coordination | 8 | 8/8 | none | PASS |
| Ritual / strange utility | 12 | 12/12 | none | PASS |
| Stealth / trickery / disruption | 9 | 9/9 | none | PASS |

All categories reach 100% template coverage in normal mode. The normal probe result for reachability is **PASS**.

### Forced Category Probe (500 generations per category, using category-matched memory profile)

| Category | Templates | Forced reached | Zero-hit templates | Status |
| --- | ---: | ---: | --- | --- |
| Traversal / mobility | 9 | 9/9 | none | PASS |
| Sensing / information | 10 | 10/10 | none | PASS |
| Survival / adaptation | 13 | 13/13 | none | PASS |
| Combat / tactical control | 6 | 6/6 | none | PASS |
| Defense / warding | 5 | 5/5 | none | PASS |
| **Resource / farming / logistics** | **8** | **2/8** | gathering.forager_memory, gathering.ecological_sense, gathering.gatherers_intuition, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit | **FAIL** |
| **Crafting / engineering / automation** | **5** | **1/5** | engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate | **FAIL** |
| **Social / support / coordination** | **8** | **2/8** | social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange | **FAIL** |
| Ritual / strange utility | 12 | 12/12 | none | PASS |
| Stealth / trickery / disruption | 9 | 9/9 | none | PASS |

**Reachability conclusion:** Three categories are critically starved in forced mode. The three categories that failed Phase 7.1 remain failed:

- **Resource/farming/logistics**: 6 of 8 templates (75%) unreachable
- **Crafting/engineering/automation**: 4 of 5 templates (80%) unreachable — complete functional collapse on a single template
- **Social/support/coordination**: 6 of 8 templates (75%) unreachable

Phase 7.1A's `MIN_ELIGIBILITY = 0.10` fix improved normal-mode reachability but did not address the within-category scoring structure that causes these failures in forced conditions.

---

## 2. Dominance Re-Check

### Forced Probe Dominance Metrics

| Category | Top template % | Top-3 % | Verdict |
| --- | ---: | ---: | --- |
| Traversal / mobility | 67.4% | 98.8% | FAIL (>50%) |
| Sensing / information | 71.4% | 98.6% | FAIL |
| Survival / adaptation | 87.2% | 98.0% | FAIL |
| Combat / tactical control | 67.8% | 99.4% | FAIL |
| Defense / warding | 74.6% | 99.6% | FAIL |
| Resource / farming / logistics | 96.0% | 100.0% | FAIL (critical) |
| Crafting / engineering / automation | 100.0% | 100.0% | FAIL (complete collapse) |
| Social / support / coordination | 74.4% | 100.0% | FAIL |
| Ritual / strange utility | 90.4% | 98.2% | FAIL |
| **Stealth / trickery / disruption** | **98.4%** | **98.8%** | **FAIL (over-correction)** |

**No category passes the >50% top-template-share criterion in forced mode.**

The 90–100% single-template dominance pattern from Phase 7.1 has not been reduced. The top-3 concentration is near 100% for every category except normal-mode ritual and traversal, which are still functionally winner-take-all in forced conditions.

### Normal Probe Dominance (informational)

In the normal probe, dominance is significantly better:
- Survival top share: 19.1%, top-3: 35.1% — healthy
- Sensing top share: 18.5%, top-3: 48.1% — acceptable
- Stealth top share: 21.5%, top-3: 51.9% — acceptable in aggregate
- Combat top share: 34.9%, top-3: 72.2% — borderline
- Defense top share: 41.6%, top-3: 79.5% — concerning

The normal probe benefits from category mixing, recency pressure, and diversity mechanics that the forced single-category probe does not exercise. The forced probe reveals the underlying intra-category scoring structure, which remains severely unbalanced.

---

## 3. Long-Tail Assessment

### Multi-hit template counts (forced probe, 500 trials/category)

| Category | Templates | >1 hit | Structural novelty active? |
| --- | ---: | ---: | --- |
| Traversal / mobility | 9 | 2 | NO (7 templates appear exactly once) |
| Sensing / information | 10 | 2 | NO |
| Survival / adaptation | 13 | 2 | NO |
| Combat / tactical control | 6 | 2 | NO |
| Defense / warding | 5 | 2 | NO |
| Resource / farming / logistics | 8 | 2 | NO (6 appear zero times) |
| Crafting / engineering / automation | 5 | 1 | NO (4 appear zero times) |
| Social / support / coordination | 8 | 2 | NO (6 appear zero times) |
| Ritual / strange utility | 12 | 2 | NO |
| Stealth / trickery / disruption | 9 | 1 | NO |

In every category, the distribution structure is: one dominant template (67–100%), one secondary template (0.2–32%), and all remaining templates appearing exactly once (0.2% each = 1 hit per 500). The "1 hit" appearances are generated by the cold-template override mechanism, not by genuine scoring competition.

**Long-tail assessment: FAIL. Novelty is not structurally active.**

The 0.2% per-template floor reflects the cold-start bootstrap mechanism (`UNSEEN_TEMPLATE_OVERRIDE_PROBABILITY = 0.12`, applied probabilistically), not signal-driven selection. Tail templates are not competing — they are being force-inserted as floor entries and then losing every subsequent competition.

---

## 4. Over-Correction Audit

### 4.1 Stealth — forced probe artifact, not a real regression

Phase 7.1A raised the stealth category weight (mob×0.07, chaos×0.04) and added a per-template affinity boost (`stealth` affinity → mob×0.15 = +0.255 for a mobility profile with mobilityWeight=1.7).

**Forced probe result:** `stealth.hushwire` at 98.4% of 500 forced selections. However, `IntraCategoryNormalizationProbeTest` and `ColdTemplateBootstrappingProbeTest` both show healthy stealth distribution in natural usage:
- Normalization probe: all 9 templates reached, top share 17% (`dead_drop_lattice`), hushwire at 12%
- Bootstrapping probe: all 9 templates reached, hushwire 15%, four templates tied at 15%

**Conclusion:** The 98.4% is an artifact of the forced-probe methodology (direct `weightedTemplateSelection` call with only stealth templates). In natural generation, stealth is healthy. The stealth affinity boost works correctly within the full generator context. The only structural note: `"stealth"` affinity is unevenly distributed across templates, which amplifies the effect in forced isolation — but this does not manifest in practice.

**Severity: LOW** (forced-probe artifact only; real-world behavior is healthy).

### 4.2 MIN_ELIGIBILITY = 0.10 impact

The reduction from 0.70 to 0.10 successfully enabled all templates to appear in normal probe (100% reachability in normal mode for all categories). However, this did not fix forced-mode reachability because:

- The eligibility filter is a gate, not a score equalizer
- Templates that pass the gate at low eligibility still compete against templates with 10–20× higher composite scores
- Low-eligibility templates appear once due to the cold-template override (`UNSEEN_TEMPLATE_OVERRIDE_PROBABILITY`) but then lose every subsequent selection

The MIN_ELIGIBILITY change is not harmful but is insufficient alone. It addresses cold-start visibility but not scoring imbalance.

**Verdict:** Not an over-correction, but not a solution either. The change needs to be paired with intra-category score equalization.

### 4.3 Defense niche expansion

Defense went from 3 niche tags to include `ENVIRONMENTAL_ADAPTATION`. The forced defense probe shows 5/5 templates reached (the complete current defense catalog) with 74.6% top-template share. No over-correction detected in terms of breadth, but still heavily concentrated.

**Note:** The `passesStrictSuccessCriteria` in `ColdCompletionDeepValidationTest` requires `DEFENSE_WARDING.reachedTemplates() == 8`, but the registry contains only 5 defense templates. This is a test criteria mismatch — the strict success criteria was written anticipating 3 additional defense templates that have not been added to the registry.

### 4.4 Stealth niche expansion (NAVIGATION added)

Adding NAVIGATION niche to STEALTH_TRICKERY_DISRUPTION expanded eligibility. This did not cause cross-category contamination in the normal probe (stealth stays in the 20–25% range, not above it). No over-correction from the niche expansion itself.

---

## 5. Natural-Probe Supplementary Results (IntraCategoryNormalizationProbeTest + ColdTemplateBootstrappingProbeTest)

Both supplementary tests **PASS** (4 tests across the two classes, 0 failures).

### IntraCategoryNormalizationProbeTest — live output

```
STEALTH_NORMALIZATION_PROBE hits=42
  dead_drop_lattice=17%, ghost_shift=17%, social_smoke=14%, echo_shunt=12%,
  trace_fold=12%, hushwire=12%, shadow_proxy=7%, threshold_jam=5%, paper_trail=5%

SURVIVAL_NORMALIZATION_PROBE hits=34
  exposure_weave=15%, hardiness_loop=12%, structure_attunement=12%, ...
  (13/13 templates, max share 15%)

SENSING_NORMALIZATION_PROBE hits=23
  contraband_tell=22%, artifact_sympathy=17%, faultline_ledger=13%, ...
  (10/10 templates, max share 22%, top-3 <65%)
```

### ColdTemplateBootstrappingProbeTest — live output

```
COLD_BOOTSTRAP_STEALTH  hits=34  reachable=9/9
  hushwire=15%, threshold_jam=15%, dead_drop_lattice=15%, ghost_shift=15%,
  echo_shunt=12%, trace_fold=9%, shadow_proxy=9%, paper_trail=6%, social_smoke=6%

COLD_BOOTSTRAP_DEFENSE  hits=15  reachable=5/5
  anchor_cadence=33%, perimeter_hum=20%, sanctum_lock=20%, fault_survey=20%, false_threshold=7%

COLD_BOOTSTRAP_RITUAL   hits=91  reachable=12/12
  pattern_resonance=12%, moon_debt=10%, niche_architect=10%, ... (all 12 templates, max 12%)

COLD_BOOTSTRAP_NOVELTY  avg_novelty=0.26  avg_similarity=0.74
  intra_niche_novelty=0.37 > global_novelty=0.26  ✓
```

### Critical interpretation

**The stealth 98.4% forced-probe concentration is a test-methodology artifact, not a real intra-category problem.**

The forced probe (`ColdCompletionDeepValidationTest`) calls `weightedTemplateSelection` directly with only that category's templates for 500 consecutive iterations. This isolates the intra-category scorer in an artificial way, amplifying any score gap between templates. The stealth affinity boost (`stealth` token → +mob×0.15 = +0.255) concentrates on `hushwire` because it is the only template in the registry with `"stealth"` in its metadata affinity set.

In NATURAL usage (where the full generator selects a category first, then picks a template within it), the stealth intra-category distribution is healthy: **all 9 templates reached, max share 17%, no winner-take-all** in both the normalization probe and cold bootstrapping probe.

**The Resource/Crafting/Social failures are confirmed real.**

These categories fail in the forced probe (2/8, 1/5, 2/8) and would fail in any sufficiently persistent within-category selection context. The root cause is not a methodology artifact — these templates have structurally lower composite scores under any profile and are never scored competitively.

### Revised over-correction assessment for stealth

Phase 7.1A's stealth boost did NOT cause a real intra-category dominance problem in normal usage. The 98.4% figure is a forced-probe artifact. The stealth affinity boost should still be reviewed (specifically whether `"stealth"` affinity should be broader across templates), but this is not a critical regression.

---

## 6. Final Judgment

### Verdict: **FAIL**

| Criterion | Result |
| --- | --- |
| Forced-mode: Resource/farming/logistics fully reachable | FAIL — 2/8 (6 unreachable) |
| Forced-mode: Crafting/engineering/automation fully reachable | FAIL — 1/5 (complete collapse) |
| Forced-mode: Social/support/coordination fully reachable | FAIL — 2/8 (6 unreachable) |
| No category >50% top template in forced mode | FAIL — all 10 categories exceed 50% |
| Long-tail templates appear multiple times | FAIL — every category has ≤2 multi-hit templates |
| No uniform flattening | PASS |
| Normal-mode: all categories fully reachable | PASS |
| Novelty band (≥0.17 avg) | PASS (resolved by similarity weight reduction) |
| Stealth intra-category (natural probes) | PASS — 9/9 reached, top share 17% |
| Stealth intra-category (forced probe) | FAIL — 98.4% (test methodology artifact) |

Three categories remain critically unreachable in forced conditions. Dominance is extreme across all categories. Long-tail templates are not competing. Stealth was over-corrected within its own category.

---

## 7. Assessment: Is Phase 7.1A Sufficient?

**No. Phase 7.1A requires a follow-up correction pass (Phase 7.1C).**

7.1A resolved:
- ✅ Normal-mode reachability (all categories 100%)
- ✅ Novelty floor (similarity weights → 0.82 sum, avg novelty ≥ 0.17)
- ✅ Stealth visibility in normal mode (now receives meaningful hits)
- ✅ Defense niche expansion (technically correct, no harm)

7.1A did not resolve:
- ❌ Forced-mode reachability for Resource, Crafting, Social (root cause: intra-category score concentration, not category-level visibility)
- ❌ General forced-mode dominance (all categories >65% top template)
- ❌ Long-tail structural activity
- ❌ Introduced new problem: stealth intra-category collapse (98.4% hushwire in forced mode)
- ❌ Test criteria mismatch: Defense strict criterion requires 8 templates but registry has 5; Ritual strict criterion requires 13 but registry has 12

### Root cause not addressed

The Phase 7.1A corrections acted on category-level visibility (category weights, niche tags, eligibility floor) without addressing the intra-category score concentration that causes forced-mode failures. The scoring system assigns 1–2 templates scores 10–100× higher than the rest of the category, meaning all other templates only appear via the cold-start override floor and never compete normally.

### Required in Phase 7.1C

1. **Intra-category score compression** for Resource, Crafting, and Social — the scoring gap within these categories must be reduced so that tail templates can compete
2. **Within-category scoring equalization for Resource, Crafting, Social** — the cold-template override is insufficient; the scoring gap must be reduced so tail templates compete on score, not just on bootstrap probability
3. Do NOT modify stealth weights further — natural-probe distribution is healthy; the forced-probe 98.4% is a methodology artifact
4. Optional: distribute `"stealth"` affinity to more stealth templates to reduce forced-probe artifact severity (low priority)

---

## 8. Version Note

No code changes are made in this Phase 7.1B pass. This is a measurement-only validation.
**Version remains: 0.9.35.** Version increment should occur with Phase 7.1C corrections.
