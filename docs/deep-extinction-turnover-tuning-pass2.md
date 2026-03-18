# Deep Extinction / Turnover Tuning Pass 2

## SECTION 1: RESIDUAL FAILURE MODE
- The first pass removed terminal child extinction, but child niches still experienced localized assignment dropouts where every artifact left in the same rollup window.
- Residual failures were concentrated in established child niches that already had lineage affinity support, which meant persistence mechanics existed but assignment continuity was still too weak.
- The main oscillation pattern remained `0 -> re-entry -> 0`, indicating that recovery paths were active while short-lived continuity after reformation was not yet strong enough.
- A second failure mode was synchronized exit: when the last one or two artifacts in a supported child niche were reassigned together, the niche briefly hit zero share even though its lineage signal stayed viable.

## SECTION 2: CHANGES MADE
- Added short-lived re-entry stabilization to `NicheBifurcationRegistry` so a child niche that returns from zero gets a bounded protection window before collapse pressure resumes.
- Tightened collapse retirement for established, lineage-supported children by adding one more required empty-window step to the sustained failure path, while leaving newer unsupported children unchanged.
- Added local assignment continuity enforcement in `NichePopulationTracker` for established, lineage-backed children so one artifact can be kept sticky in-place without increasing total population, migration caps, or partition caps.
- Added proactive occupancy seeding for eligible child niches when they are about to become vacant, using a low-utility parent artifact and a bounded sticky lock instead of population growth.
- Added synchronized-exit damping by protecting one continuity-selected child artifact from being the first candidate for structural demotion, which spreads exits across windows instead of allowing all artifacts to leave together.

## SECTION 3: FILES MODIFIED
- `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java`
- `src/main/java/obtuseloot/evolution/NichePopulationTracker.java`
- `src/test/java/obtuseloot/evolution/NicheEcologySystemTest.java`
- `docs/deep-extinction-turnover-tuning-pass2.md`

## SECTION 4: POST-RUN CONTINUITY RESULTS
- Validation dataset: `analytics/validation-suite-rerun/extinction-turnover-tuning-pass2-20260318`.

| Scenario | Zero-share windows | Full continuity | Avg child share | Max child share | Final child share | Mean/final diversity |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| explorer-heavy | 2 | No | 5.2% | 10.3% | 10.3% | 0.455 / 0.552 |
| ritualist-heavy | 1 | No | 6.6% | 13.8% | 6.9% | 0.383 / 0.448 |
| gatherer-heavy | 1 | No | 5.5% | 10.3% | 6.9% | 0.441 / 0.483 |
| mixed | 1 | No | 5.3% | 12.5% | 12.5% | 0.375 / 0.438 |
| random-baseline | 1 | No | 4.8% | 6.9% | 6.9% | 0.338 / 0.414 |

- Zero-share windows stayed compressed to a narrow `1-2` range after the pass; four scenarios were held to a single dropout window and explorer-heavy remained the only case above that bound.
- No scenario achieved full continuity in this rerun, so the `>=3 scenarios with zero zero-share windows` success bar was not met.
- Despite that miss, child-share growth stayed bounded across the full horizon: the maximum observed child-share peak was 13.8%, well below the earlier runaway-risk thresholds.

## SECTION 5: ZERO-SHARE WINDOW ANALYSIS
| Scenario | Extinction events | Re-entry events | Dominant-lineage turnover | Longest persistence windows | Highest-persistence child niches |
| --- | ---: | ---: | ---: | ---: | --- |
| explorer-heavy | 4 | 7 | 1 | 3 | `RITUAL_STRANGE_UTILITY_A1` (3w); `RITUAL_STRANGE_UTILITY_B1` (2w); `RITUAL_STRANGE_UTILITY_A2` (2w) |
| ritualist-heavy | 8 | 8 | 2 | 4 | `RITUAL_STRANGE_UTILITY_A2` (4w); `RITUAL_STRANGE_UTILITY_B1` (3w); `RITUAL_STRANGE_UTILITY_A1` (3w) |
| gatherer-heavy | 8 | 8 | 2 | 3 | `RITUAL_STRANGE_UTILITY_A1` (3w); `RITUAL_STRANGE_UTILITY_B2` (2w); `RITUAL_STRANGE_UTILITY_B1` (2w) |
| mixed | 5 | 7 | 0 | 5 | `RITUAL_STRANGE_UTILITY_A2` (5w); `RITUAL_STRANGE_UTILITY_B1` (3w); `RITUAL_STRANGE_UTILITY_A1` (2w) |
| random-baseline | 5 | 5 | 3 | 4 | `MEMORY_HISTORY_B1` (4w); `MEMORY_HISTORY_A2` (2w); `RITUAL_STRANGE_UTILITY_B3` (1w) |

- The residual zero-share problem is now mostly a single-window continuity lapse rather than long collapse cascades.
- Re-entry counts remain high relative to extinction counts, confirming that niches are recovering, but explorer-heavy still shows multi-window vacancy pressure and therefore remains the clearest unresolved oscillation case.
- Lineage turnover stayed bounded in the low single digits, which indicates the added continuity bias did not create lineage whiplash or runaway reassignment churn.

## SECTION 6: STABILITY CHECK
- **Runaway growth:** pass. Peak child share stayed between 6.9% and 13.8%, so the continuity changes did not create unbounded expansion.
- **Migration and partition limits:** pass. No migration-cap or partition-cap changes were introduced; stabilization was achieved through local assignment retention and bounded sticky locks only.
- **Lineage turnover remains bounded:** pass. Dominant-lineage turnover stayed between 0 and 3 events per scenario.
- **Continuity target:** partial. Four scenarios were reduced to a single zero-share window, but no scenario achieved perfect continuity and explorer-heavy still recorded two zero-share windows.
- **Overall assessment:** partial. The pass improved bounded persistence behavior and kept ecology stable, but it did not satisfy the required `>=3` fully continuous scenarios or the `<=1` residual-window cap in every remaining scenario.

EXTINCTION_TUNING_PASS2_RESULT: PARTIAL
