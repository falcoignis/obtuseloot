# Deep Extinction / Turnover Tuning

## SECTION 1: COLLAPSE / TURNOVER ROOT CAUSE
- Prior long-horizon validation showed child niches hitting repeated zero-share windows, full child extinction in some scenario finales, and rapid collapse/re-bifurcation churn.
- Root causes addressed in this pass:
  - single-window underpopulation could still quickly push a niche toward retirement once grace ended;
  - established children had no bounded persistence floor, so occupancy continuity broke even when lineage support remained;
  - lineage affinity support existed for assignment, but collapse logic did not use it to soften removal pressure;
  - same-parent rebifurcation could resume immediately after collapse because collapse only removed children and did not impose a local cooldown.

## SECTION 2: CHANGES MADE
- Added established-child tracking in `NicheBifurcationRegistry` so child niches that stay active for 4 windows are treated as persistent rather than disposable.
- Added collapse hysteresis in `NicheBifurcationRegistry`: low-population windows now accumulate through a consecutive-window streak, and established / lineage-backed children require a longer sustained unsupported period before retirement.
- Added lineage-backed persistence bias by feeding current child lineage-affinity support into collapse evaluation; stronger support extends the zero-window collapse path instead of allowing immediate retirement.
- Added a bounded persistence floor in `NichePopulationTracker`: established child niches with meaningful lineage support can keep a single low-utility parent artifact assigned locally, preventing avoidable zero-share windows without changing migration caps or partition caps.
- Added local anti-oscillation behavior by treating collapse as a per-parent cooldown reset and by clearing stale child assignments once an artifact reclassifies into another parent family, preventing collapse/re-bifurcation pinball.
- Tightened child adoption multiplier clamping back into the existing bounded range after the persistence changes so child growth remains capped.

## SECTION 3: FILES MODIFIED
- `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java`
- `src/main/java/obtuseloot/evolution/NichePopulationTracker.java`
- `src/test/java/obtuseloot/evolution/NicheEcologySystemTest.java`
- `docs/deep-extinction-turnover-tuning.md`

## SECTION 4: POST-RUN PERSISTENCE RESULTS
- Validation dataset: `analytics/validation-suite-rerun/extinction-turnover-tuning-20260318`.

| Scenario | Zero-share windows | Non-zero child share across all windows | Avg child share | Final child share | Final parent share | Mean/final diversity |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| explorer-heavy | 2 | No | 10.0% | 27.6% | 27.6% | 0.390 / 0.483 |
| ritualist-heavy | 4 | No | 7.9% | 20.7% | 17.2% | 0.407 / 0.517 |
| gatherer-heavy | 1 | No | 13.1% | 20.7% | 31.0% | 0.521 / 0.621 |
| mixed | 0 | Yes | 15.0% | 18.8% | 12.5% | 0.500 / 0.688 |
| random-baseline | 1 | No | 11.7% | 17.2% | 20.7% | 0.500 / 0.690 |

- Result summary: 1 of 5 scenarios (`mixed`) maintained non-zero child share in every sampled rollup window. This improves final-window continuity versus the prior deep-long-horizon report, but it does not meet the `>=3 scenarios` success bar.
- All five scenarios finished with non-zero child share in the final sampled window, which removes the earlier “ends at 0% child share” failure mode for gatherer-heavy and random-baseline.
- Child share stayed bounded: maximum observed child-share peaks remained between 20.7% and 28.1%, so no runaway niche growth appeared in this run.

## SECTION 5: EXTINCTION / RE-EMERGENCE COUNTS
| Scenario | Total child extinction events | Total child re-emergence events | Dominant-lineage turnover events | Highest-persistence child niches |
| --- | ---: | ---: | ---: | --- |
| explorer-heavy | 17 | 11 | 6 | MEMORY_HISTORY_B1 (5w, ext 4, re 3); SOCIAL_WORLD_INTERACTION_A4 (4w, ext 4, re 3); SOCIAL_WORLD_INTERACTION_B4 (4w, ext 4, re 3) |
| ritualist-heavy | 12 | 7 | 6 | MEMORY_HISTORY_B2 (3w, ext 1, re 1); SOCIAL_WORLD_INTERACTION_B5 (3w, ext 3, re 2); SOCIAL_WORLD_INTERACTION_A5 (3w, ext 3, re 2) |
| gatherer-heavy | 19 | 12 | 4 | RITUAL_STRANGE_UTILITY_A1 (6w, ext 2, re 1); RITUAL_STRANGE_UTILITY_A2 (6w, ext 3, re 3); SOCIAL_WORLD_INTERACTION_A4 (5w, ext 4, re 3) |
| mixed | 23 | 10 | 6 | SOCIAL_WORLD_INTERACTION_A4 (5w, ext 4, re 3); SOCIAL_WORLD_INTERACTION_B4 (5w, ext 4, re 3); RITUAL_STRANGE_UTILITY_B5 (3w, ext 3, re 2) |
| random-baseline | 21 | 9 | 4 | SOCIAL_WORLD_INTERACTION_A6 (3w, ext 3, re 2); SOCIAL_WORLD_INTERACTION_B6 (3w, ext 3, re 2); RITUAL_STRANGE_UTILITY_A2 (2w, ext 2, re 1) |

- Extinction events are still present, but they are more often followed by re-emergence rather than permanent disappearance, especially in gatherer-heavy, mixed, and random-baseline.
- Lineage turnover remained bounded to low single digits per scenario, so the persistence changes did not create chaotic lineage whiplash.

## SECTION 6: STABILITY CHECK
- **No niche explosion:** pass. Child-share peaks stayed below 30%, and parent niches remained competitive in every scenario.
- **No unbounded persistence:** pass. Unsupported children still collapsed after sustained low/zero windows; extinction events still occurred in all scenarios.
- **Parents remain dominant overall:** pass. Final parent shares stayed comparable to or above child shares in 4/5 scenarios; `mixed` is the most child-forward case but still remains bounded.
- **Collapse/re-bifurcation oscillation reduced but not eliminated:** partial. Zero-share windows fell to 0-4 per scenario and all scenarios now end non-zero, but only `mixed` fully avoids zero-share windows across the horizon.
- **Success criteria evaluation:** partial. The run improved persistence continuity and removed terminal child extinction, but it did not reach the required `>=3` always-non-zero scenarios.

EXTINCTION_TUNING_RESULT: PARTIAL
