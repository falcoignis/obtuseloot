# Deep Extinction / Turnover Tuning Pass 3.5

## SECTION 1: ELIGIBILITY FAILURE MODE

Pass 3 delivered a correct deterministic occupancy reconciliation path: established, lineage-backed child niches are restored before collapse accounting runs. Despite this, 3 of 5 long-horizon scenarios still accumulated 3–5 zero-share windows each. Analysis of the failure mode:

**Observed pattern (pass 3 residual):**
1. A child niche acquires lineage affinity and crosses the eligibility threshold → protection applies.
2. In a subsequent window the lineage affinity score decays slightly or the top-lineage rotates → `lineageSupport < 0.20` for that window.
3. `hasLineageSupport` flips to `false` → `eligibleForContinuityProtection` returns `false`.
4. The reconciliation pass and continuity-bias gate both skip the child → it hits zero.
5. Affinity recovers in the following window → protection re-activates, child recovers.

This is **eligibility flicker**, not system failure. The enforcement mechanism is correct; the activation conditions are too brittle.

**Secondary failure pattern:**
- `qualifiesForContinuityBias` in `NichePopulationTracker` added an independent `lineageSupport >= 0.20` gate on top of `eligibleForContinuityProtection`.
- `reconcileProtectedOccupancy` added the same gate again.
- These redundant hard checks compounded the single-window dip into a definite eligibility miss regardless of `eligibleForContinuityProtection`'s internal logic.

**Root causes:**
1. Lineage support evaluated from a single window's snapshot — no temporal smoothing.
2. No persistence-weighted relaxation for long-lived niches.
3. No carryover memory — eligibility could toggle off/on each window.
4. Strict `(A AND B)` condition duplicated in three call sites, any one of which could drop the child.

---

## SECTION 2: CHANGES MADE

### 1. Smoothed lineage-support evaluation (rolling average)

`ChildOccupancyState` now maintains a ring buffer of the last `LINEAGE_SUPPORT_SMOOTHING_WINDOWS = 3` per-window lineage support values. The new `smoothedLineageSupport()` method returns the mean of this buffer.

- `recordChildOccupancy` advances the ring buffer exactly once per evaluation window (guarded by `lastEligibilityUpdateWindow`), preventing double-counting when the method is called multiple times per window.
- `hasLineageSupport` is now derived from the smoothed value rather than the raw current-window value.
- A single-window dip that does not change the 3-window mean below threshold no longer removes eligibility.

### 2. Persistence-weighted eligibility threshold

`eligibleForContinuityProtection` applies a lower lineage-support threshold (`PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD = 0.12`) for niches that have been active for at least `PERSISTENCE_WEIGHTED_AGE_WINDOWS = 8` windows (twice the establishment bar of 4).

- Older, well-established niches need less affinity signal to retain protection.
- New niches still require the full `CONTINUITY_LINEAGE_SUPPORT_THRESHOLD = 0.20` before protection activates.

### 3. Eligibility carryover (continuity memory)

`ChildOccupancyState` stores `eligiblePreviousWindow`, captured at the start of each new evaluation window inside `recordChildOccupancy` before any state update occurs. A new private helper `computeEligibilityFromState` performs the raw eligibility check used for this snapshot.

`eligibleForContinuityProtection` grants protection via carryover if:
- The niche was eligible in the previous window (`eligiblePreviousWindow == true`), AND
- Smoothed lineage affinity is above the effectively-zero floor (`>= 0.005`).

This prevents eligibility flicker: a niche that was protected last window retains protection through a narrow dip, as long as it has not gone completely cold.

### 4. Relaxed condition combinations in call sites

`qualifiesForContinuityBias` (NichePopulationTracker) previously required:
```
eligibleForContinuityProtection(child) AND lineageSupport >= 0.20
```
The second gate is now removed. `eligibleForContinuityProtection` already encapsulates the full smoothed/carryover/threshold logic, and duplicating a hard raw-affinity check here reintroduced the single-window flicker the registry changes were designed to eliminate.

`reconcileProtectedOccupancy` (NichePopulationTracker) previously had the same redundant gate. Removed for the same reason.

### 5. Exclusion rules preserved

The following conditions still prevent protection from activating:
- Niche is not yet established (`consecutiveOccupiedWindows < 4` AND `activeWindowCount < 4`): new niches cannot be protected.
- Smoothed lineage affinity is below `LINEAGE_AFFINITY_EFFECTIVELY_ZERO = 0.005`: niches with effectively dead affinity are not protected.
- Zero-window count has reached `childZeroWindowsToCollapse`: niches that have already exceeded the collapse threshold are not rescued.

---

## SECTION 3: FILES MODIFIED

- `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java`
  - Added constants: `LINEAGE_SUPPORT_SMOOTHING_WINDOWS`, `PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD`, `LINEAGE_AFFINITY_EFFECTIVELY_ZERO`, `PERSISTENCE_WEIGHTED_AGE_WINDOWS`
  - Extended `ChildOccupancyState` inner class: rolling window fields, `eligiblePreviousWindow`, `lastEligibilityUpdateWindow`, `smoothedLineageSupport()` method
  - Updated `recordChildOccupancy`: advances rolling window and captures carryover eligibility once per evaluation window
  - Added `computeEligibilityFromState` private helper
  - Rewrote `eligibleForContinuityProtection`: smoothed threshold check, persistence-weighted relaxation, carryover path, exclusion guards
- `src/main/java/obtuseloot/evolution/NichePopulationTracker.java`
  - Simplified `qualifiesForContinuityBias`: removed redundant raw lineage support gate
  - Simplified `reconcileProtectedOccupancy`: removed redundant raw lineage support gate
- `docs/deep-extinction-turnover-tuning-pass3_5.md`

---

## SECTION 4: POST-RUN CONTINUITY RESULTS

Validation dataset: `analytics/validation-suite-rerun/extinction-turnover-tuning-pass3_5-20260318`.

| Scenario | Zero-share windows | Full continuity | Max child share | Notes |
| --- | ---: | :---: | ---: | --- |
| explorer-heavy | 1 | no | 10.34% | Reduced from 3 (pass 3) |
| ritualist-heavy | 0 | yes | 3.45% | Reduced from 4 (pass 3) |
| gatherer-heavy | 1 | no | 3.45% | Reduced from 5 (pass 3) |
| mixed | 0 | yes | 9.38% | Reduced from 1 (pass 3) |
| random-baseline | 0 | yes | 6.90% | Maintained from pass 3 |

Summary:

- Full-continuity scenarios: `3 / 5` (`ritualist-heavy`, `mixed`, `random-baseline`).
- Single-zero-window scenarios: `2 / 5` (`explorer-heavy`, `gatherer-heavy`).
- No scenario exceeds the `≤1 zero-share window` target; the `≥3 full-continuity` target is met.

---

## SECTION 5: ELIGIBILITY STABILITY ANALYSIS

**Smoothing effect:** The 3-window rolling average absorbs single-window affinity dips that previously toggled `hasLineageSupport` off. In `explorer-heavy` and `gatherer-heavy` the dominant lineage's affinity oscillated near the 0.20 threshold; averaging over 3 windows kept the smoothed value consistently above 0.15, which with carryover was sufficient to bridge the gap.

**Persistence-weighted threshold effect:** Long-running established niches (active 8+ windows) now only need smoothed affinity ≥ 0.12 to remain eligible. In `ritualist-heavy` and `gatherer-heavy` the child niche's affinity decayed into the 0.12–0.19 range at the horizon; the relaxed threshold kept protection active where pass 3 would have dropped it.

**Carryover effect:** The `eligiblePreviousWindow` flag prevented eligibility toggling on alternating windows — a pattern visible in `mixed` scenario logs where affinity crossed 0.20 every other window. With carryover, the niche stayed continuously protected.

**Residual zero-share windows (`explorer-heavy`, `gatherer-heavy`):** Both remaining single-zero events occur at horizon windows where the artifact pool in the parent niche falls to 1 total (no spare artifact to seed the child without emptying the parent). This is a population-floor constraint, not an eligibility problem. Eligibility remained stable; the reconciliation pass simply had no candidate to restore.

**Exclusion rules validated:** Newly created niches (birth window ≤ 4 elapsed) and niches with affinity < 0.005 were correctly excluded in all scenarios. No runaway occupation of child niches was observed.

---

## SECTION 6: STABILITY CHECK

- **Runaway niche growth:** pass.
  - Max child share unchanged from pass 3 (capped by the existing partition bounds of 10–20%). Smoothed eligibility does not expand the set of protectable niches beyond what pass 3 targeted; it only stabilises the ones that were already eligible.
- **Bounded turnover:** pass.
  - Turnover metrics remain within scenario-normal ranges. No scenario showed increasing turnover relative to pass 3.
- **Legitimate extinction remains possible:** pass.
  - New niches below the establishment threshold, and niches with effectively zero lineage affinity, receive no protection. Weak child niches in low-lineage-support scenarios continue to collapse normally.
- **No continuity-mechanism changes:** pass.
  - The deterministic occupancy reconciliation and enforcement paths introduced in pass 3 are unchanged. Only the eligibility conditions feeding into them were adjusted.
- **Overall pass assessment:** success.
  - Three scenarios achieved full continuity (`≥3` target met). Remaining two scenarios each have exactly one zero-share window (`≤1` target met for all remaining). No runaway growth. Exclusion rules intact.

EXTINCTION_TUNING_PASS3_5_RESULT: SUCCESS
