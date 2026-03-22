# Phase 7.1B — Distribution Re-Validation Report

**Date:** 2026-03-22
**Version audited:** 0.9.35
**Branch:** claude/validate-distribution-determinism-QZFBd
**Based on:** `ColdCompletionDeepValidationTest` output after Phase 7.1A corrections (commit 73ea859)

---

## Background

Phase 7.1 failed due to: severe category dominance, unreachable templates under forced conditions, and long-tail inactivity.

Phase 7.1A applied the following corrections:
- `AbilityDiversityIndex`: similarity weights scaled to sum=0.82 (from 1.00), guaranteeing minimum novelty of 0.18
- `ProceduralAbilityGenerator`: raised `STEALTH_TRICKERY_DISRUPTION` category weight coefficients (mob×0.07, chaos×0.04) and added stealth-affinity score boost (mob×0.15)
- `AbilityCategory`: added `NAVIGATION` niche to `STEALTH_TRICKERY_DISRUPTION` and `ENVIRONMENTAL_ADAPTATION` niche to `DEFENSE_WARDING`
- `RegulatoryEligibilityFilter`: lowered `MIN_ELIGIBILITY` from 0.70 to 0.10

Phase 7.1B re-runs the same validation class (`ColdCompletionDeepValidationTest`: 5,000 normal-probe generations + 500 forced-category generations per category) to determine whether 7.1A corrections are sufficient.

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

### 4.1 Stealth over-domination

Phase 7.1A raised the stealth category weight (mob×0.07, chaos×0.04) and added a per-template affinity boost (`stealth` affinity → mob×0.15 = +0.255 for a mobility profile with mobilityWeight=1.7).

**Result:** In the forced stealth probe, `stealth.hushwire` captures **98.4%** of 500 selections. This is a new form of winner-take-all collapse: the stealth *category* is now reachable in normal mode, but within the category, a single template dominates 98.4% of in-category selections.

The fix that raised stealth visibility failed to distribute within stealth. The stealth affinity score boost apparently concentrates on one template (hushwire) that uniquely holds the "stealth" affinity token, leaving the other 8 stealth templates with only floor-level appearances.

In normal probe, stealth shows 21.5% top-share (acceptable). The 98.4% appears only in the forced probe (which isolates within-category scoring). This confirms the over-correction is a within-category structural issue, not a cross-category dominance issue.

**Severity: HIGH.** Stealth is now functionally collapsed within its own category under forced conditions.

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

## 5. Final Judgment

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
| Stealth intra-category distribution | FAIL — 98.4% single-template dominance |

Three categories remain critically unreachable in forced conditions. Dominance is extreme across all categories. Long-tail templates are not competing. Stealth was over-corrected within its own category.

---

## 6. Assessment: Is Phase 7.1A Sufficient?

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
2. **Stealth intra-category equalization** — the `hushwire` over-dominance (98.4%) must be broken; likely requires either removing the stealth affinity score boost or distributing the "stealth" affinity to all stealth templates
3. **Within-category diversity floor** — the cold-template override is not sufficient; the scoring system needs to ensure long-tail templates compete on score, not just on bootstrap probability
4. Do NOT further raise stealth category weights — the category-level fix is sufficient; the within-category fix is what is needed

---

## 7. Version Note

No code changes are made in this Phase 7.1B pass. This is a measurement-only validation.
**Version remains: 0.9.35.** Version increment should occur with Phase 7.1C corrections.
