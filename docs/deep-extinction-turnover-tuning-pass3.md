# Deep Extinction / Turnover Tuning Pass 3

## SECTION 1: FINAL FAILURE MODE

After pass 2, the remaining continuity failure was no longer long-form child extinction. The residual issue was a short deterministic vacancy: an established, lineage-backed child niche could still record a zero-share window when its last artifact exited during structural reassignment or when multiple artifacts were demoted in the same evaluation window. Recovery paths remained active, but they happened one window too late for strict continuity accounting.

## SECTION 2: CHANGES MADE

- Added persistent per-child occupancy continuity state in `NicheBifurcationRegistry`.
  - State now records `lastOccupiedWindow`, `consecutiveOccupiedWindows`, `isEstablished`, and `hasLineageSupport`/current lineage-support value.
  - Continuity eligibility is computed from that persisted state instead of relying on short-lived sticky behavior alone.
- Reworked `NichePopulationTracker` continuity enforcement around deterministic occupancy reconciliation.
  - Each evaluation window now refreshes child occupancy state before and after assignment pressure.
  - Established, lineage-backed children are deterministically protected through `eligibleForContinuityProtection`.
  - A new reconciliation pass restores exactly one occupant before collapse accounting if a protected child would otherwise end the window empty.
- Preferred restoration source ordering is now:
  1. last known occupant when still valid within the parent niche,
  2. otherwise the lowest-continuity-score parent artifact.
- Removed the older bounded persistence-floor path as the main protection mechanism.
  - Pass 3 no longer depends on the narrow early-window persistence floor.
  - Deterministic end-of-window reconciliation is now the primary anti-gap mechanism.
- Preserved bounded behavior.
  - No total artifact growth was added.
  - No migration caps or partition caps were increased.
  - Continuity remains conditional on established occupancy plus lineage support, so weak/new niches are still allowed to disappear.

## SECTION 3: FILES MODIFIED

- `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java`
- `src/main/java/obtuseloot/evolution/NichePopulationTracker.java`
- `src/test/java/obtuseloot/evolution/NicheEcologySystemTest.java`
- `docs/deep-extinction-turnover-tuning-pass3.md`

## SECTION 4: POST-RUN CONTINUITY RESULTS

Validation dataset: `analytics/validation-suite-rerun/extinction-turnover-tuning-pass3-20260318`.

| Scenario | Zero-share windows | Full continuity | Max child share | Mean turnover | Final turnover | Dominant-lineage changes |
| --- | ---: | :---: | ---: | ---: | ---: | ---: |
| explorer-heavy | 3 | no | 10.34% | 8.6000 | 13.5172 | 6 |
| ritualist-heavy | 4 | no | 3.45% | 10.8345 | 16.7241 | 1 |
| gatherer-heavy | 5 | no | 3.45% | 10.6655 | 17.4828 | 3 |
| mixed | 1 | no | 9.38% | 12.4344 | 21.1562 | 5 |
| random-baseline | 0 | yes | 6.90% | 10.7000 | 17.4138 | 4 |

Summary:

- Full-continuity scenarios: `1 / 5` (`random-baseline`).
- Single-zero-window scenarios: `1 / 5` (`mixed`).
- Three scenarios still exceeded the `<=1 zero-share window` target.

## SECTION 5: ZERO-SHARE WINDOW ELIMINATION

Pass 3 successfully converted the remaining gap prevention logic from probabilistic/short-lived persistence into deterministic occupancy restoration, but the long-horizon rerun still did not eliminate all single-window and multi-window child vacancies.

Observed outcome against the target:

- `>=3 scenarios with zero zero-share windows`: **not met**.
- Remaining scenarios with `<=1` zero-share window: **not met**.
- Best case: `random-baseline` achieved full continuity.
- Near miss: `mixed` was reduced to a single zero-share window.
- Worst residual cases: `gatherer-heavy` and `ritualist-heavy`, where continuity protection remained too selective to carry every established child across the full horizon.

## SECTION 6: STABILITY CHECK

- **Runaway niche growth:** pass.
  - Max child share stayed between `3.45%` and `10.34%`, so deterministic continuity did not create runaway expansion.
- **Bounded turnover:** partial/pass leaning pass.
  - Turnover remained finite and scenario-bounded, but it was not reduced relative to the previous tuning passes in every scenario.
- **Legitimate extinction remains possible:** pass.
  - Weak or unsupported children were still allowed to disappear; only established, lineage-backed children were eligible for deterministic restoration.
- **Overall pass assessment:** partial.
  - The deterministic reconciliation path is implemented and bounded, but the validation outcomes still miss the required continuity bar.

EXTINCTION_TUNING_PASS3_RESULT: PARTIAL
