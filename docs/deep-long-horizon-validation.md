# Deep Long-Horizon Validation
## Scope and dataset
- Source dataset: `analytics/validation-suite-rerun/deep-long-horizon-test/*`.
- Horizon: 32 windows across the five requested scenarios (`explorer-heavy`, `ritualist-heavy`, `gatherer-heavy`, `mixed`, `random-baseline`).
- Method: measurement-only pass over existing pipeline artifacts (`rollup-snapshots.json`, `world-sim-data.json`, and scenario metadata). No mechanics, parameters, or run scripts were changed.
- Window metrics collected: child total share, per-child share, active child niche count, parent share, parent-vs-children ratio, dominant lineage per active child niche, lineage turnover, lineage concentration, persistence, extinction/reemergence, active niches above 1% share, and diversity index.
## Scenario classification summary
| Scenario | Classification | Parents with child niches | Non-zero child share in every window | Late-window child share avg | Final child share | Final parent share | Turnover events | Diversity index (mean/final) |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| explorer-heavy | DRIFTING | RITUAL_STRANGE_UTILITY | No | 5.0% | 6.9% | 6.9% | 0 | 0.524 / 0.690 |
| ritualist-heavy | DRIFTING | MEMORY_HISTORY | No | 3.9% | 6.9% | 17.2% | 3 | 0.504 / 0.621 |
| gatherer-heavy | COLLAPSING | MEMORY_HISTORY | No | 3.2% | 0.0% | 3.4% | 5 | 0.510 / 0.655 |
| mixed | DRIFTING | RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION | No | 7.0% | 6.2% | 15.6% | 8 | 0.479 / 0.625 |
| random-baseline | CHAOTIC | MEMORY_HISTORY | No | 4.3% | 0.0% | 6.9% | 2 | 0.476 / 0.655 |

## Time-series summary
### explorer-heavy
- Child total share by window (%): 3.4, 6.9, 3.4, 3.4, 3.4, 6.9, 0.0, 6.9, 3.4, 6.9, 6.9, 6.9, 3.4, 0.0, 0.0, 6.9, 6.9, 6.9, 3.4, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 6.9, 6.9.
- Parent share by window (%): 17.2, 10.3, 17.2, 13.8, 13.8, 13.8, 13.8, 17.2, 17.2, 6.9, 10.3, 17.2, 17.2, 17.2, 13.8, 10.3, 17.2, 13.8, 13.8, 6.9, 10.3, 6.9, 17.2, 10.3, 17.2, 13.8, 20.7, 20.7, 6.9, 13.8, 24.1, 6.9.
- Interpretation: One child branch (RITUAL_STRANGE_UTILITY_B1) persists through 25/32 windows, but total child share still hits zero in four windows and ends with a new B6 coexisting alongside B1.
### ritualist-heavy
- Child total share by window (%): 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 0.0, 6.9, 0.0, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 6.9, 3.4, 3.4, 6.9, 0.0, 0.0, 6.9, 0.0, 3.4, 3.4, 3.4, 0.0, 6.9, 6.9, 6.9.
- Parent share by window (%): 13.8, 6.9, 13.8, 10.3, 3.4, 13.8, 3.4, 13.8, 13.8, 13.8, 13.8, 13.8, 13.8, 6.9, 10.3, 3.4, 17.2, 13.8, 3.4, 3.4, 13.8, 6.9, 13.8, 17.2, 17.2, 10.3, 6.9, 13.8, 20.7, 20.7, 6.9, 17.2.
- Interpretation: Children repeatedly reappear around MEMORY_HISTORY_A5/B5, with a small number of lineage takeovers but no permanent parent reversion.
### gatherer-heavy
- Child total share by window (%): 6.9, 3.4, 3.4, 6.9, 6.9, 3.4, 6.9, 6.9, 6.9, 3.4, 0.0, 3.4, 0.0, 0.0, 6.9, 3.4, 6.9, 0.0, 0.0, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 3.4, 0.0.
- Parent share by window (%): 10.3, 10.3, 13.8, 13.8, 13.8, 13.8, 3.4, 17.2, 13.8, 6.9, 6.9, 10.3, 10.3, 17.2, 17.2, 6.9, 13.8, 6.9, 3.4, 6.9, 10.3, 3.4, 13.8, 10.3, 13.8, 3.4, 13.8, 6.9, 10.3, 6.9, 6.9, 3.4.
- Interpretation: Child niches repeatedly form and collapse; the run ends with zero active children, so long-horizon continuity is weakest here.
### mixed
- Child total share by window (%): 6.2, 0.0, 3.1, 6.2, 3.1, 0.0, 6.2, 0.0, 3.1, 3.1, 3.1, 3.1, 6.2, 3.1, 3.1, 6.2, 3.1, 3.1, 3.1, 3.1, 0.0, 9.4, 6.2, 6.2, 6.2, 6.2, 6.2, 3.1, 9.4, 18.8, 21.9, 6.2.
- Parent share by window (%): 28.1, 25.0, 37.5, 31.2, 34.4, 37.5, 37.5, 40.6, 40.6, 43.8, 37.5, 37.5, 37.5, 43.8, 25.0, 31.2, 43.8, 37.5, 34.4, 37.5, 31.2, 46.9, 37.5, 34.4, 25.0, 37.5, 21.9, 15.6, 25.0, 28.1, 18.8, 15.6.
- Interpretation: Two parent families retain concurrent child niches late in the run, and lineage switching appears without a winner-take-all collapse.
### random-baseline
- Child total share by window (%): 3.4, 3.4, 6.9, 3.4, 0.0, 3.4, 0.0, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 6.9, 6.9, 6.9, 3.4, 6.9, 3.4, 0.0, 0.0, 6.9, 6.9, 0.0, 0.0, 3.4, 6.9, 10.3, 6.9, 6.9, 6.9, 0.0.
- Parent share by window (%): 10.3, 13.8, 3.4, 3.4, 3.4, 10.3, 10.3, 6.9, 6.9, 10.3, 3.4, 3.4, 10.3, 6.9, 6.9, 17.2, 13.8, 3.4, 6.9, 6.9, 10.3, 3.4, 6.9, 10.3, 3.4, 17.2, 6.9, 3.4, 13.8, 3.4, 0.0, 6.9.
- Interpretation: Children keep resurfacing but never settle; multiple zero-share windows and a zero-share finish indicate unstable coexistence rather than durable collapse-proof persistence.

## Per-scenario tables
### explorer-heavy
| Metric | Value |
| --- | --- |
| Classification | DRIFTING |
| Parent niche set | RITUAL_STRANGE_UTILITY |
| Child total share range | 0.0% to 6.9% |
| Child total share average (all / late 16 windows) | 4.6% / 5.0% |
| Parent share average (all / late 16 windows) | 14.0% / 13.8% |
| Parent vs children share ratio (final window) | 1.00 |
| Active child niche count (min-max) | 0-2 |
| Multi-niche coexistence | Yes, but usually only 1-2 children at a time. |
| Active niches above 1% share (final window) | 7 |
| Diversity index (mean / final) | 0.524 / 0.690 |
| Convergence to parent dominance | No |

Final-window child shares:

| Child niche | Share | Top lineage | Dominant share | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| RITUAL_STRANGE_UTILITY_B1 | 3.4% | wild-4233 | 82.8% | 25 | 5 | 5 |
| RITUAL_STRANGE_UTILITY_B6 | 3.4% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.3% | 1 | 0 | 0 |

Highest-persistence child niches across the full horizon:

| Child niche | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | ---: | ---: |
| RITUAL_STRANGE_UTILITY_B1 | 25 | 5 | 5 |
| RITUAL_STRANGE_UTILITY_A2 | 2 | 2 | 1 |
| RITUAL_STRANGE_UTILITY_A4 | 2 | 1 | 0 |
| RITUAL_STRANGE_UTILITY_B3 | 2 | 1 | 0 |

### ritualist-heavy
| Metric | Value |
| --- | --- |
| Classification | DRIFTING |
| Parent niche set | MEMORY_HISTORY |
| Child total share range | 0.0% to 6.9% |
| Child total share average (all / late 16 windows) | 3.8% / 3.9% |
| Parent share average (all / late 16 windows) | 11.6% / 12.7% |
| Parent vs children share ratio (final window) | 2.50 |
| Active child niche count (min-max) | 0-2 |
| Multi-niche coexistence | Yes, but narrow and intermittent. |
| Active niches above 1% share (final window) | 7 |
| Diversity index (mean / final) | 0.504 / 0.621 |
| Convergence to parent dominance | No |

Final-window child shares:

| Child niche | Share | Top lineage | Dominant share | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| MEMORY_HISTORY_A5 | 3.4% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 78.8% | 6 | 1 | 1 |
| MEMORY_HISTORY_B5 | 3.4% | lineage-021c2ef9-570a-3be6-b59d-12722837c715 | 100.0% | 3 | 0 | 0 |

Highest-persistence child niches across the full horizon:

| Child niche | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | ---: | ---: |
| MEMORY_HISTORY_A3 | 6 | 1 | 0 |
| MEMORY_HISTORY_A5 | 6 | 1 | 1 |
| MEMORY_HISTORY_B2 | 6 | 1 | 0 |
| MEMORY_HISTORY_A4 | 3 | 2 | 1 |

### gatherer-heavy
| Metric | Value |
| --- | --- |
| Classification | COLLAPSING |
| Parent niche set | MEMORY_HISTORY |
| Child total share range | 0.0% to 6.9% |
| Child total share average (all / late 16 windows) | 3.8% / 3.2% |
| Parent share average (all / late 16 windows) | 10.0% / 8.4% |
| Parent vs children share ratio (final window) | ∞ |
| Active child niche count (min-max) | 0-2 |
| Multi-niche coexistence | Only intermittently; not durable into the final window. |
| Active niches above 1% share (final window) | 6 |
| Diversity index (mean / final) | 0.510 / 0.655 |
| Convergence to parent dominance | No |

Final-window child shares:

| Child niche | Share | Top lineage | Dominant share | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| None active in final window | 0.0% | n/a | n/a | n/a | n/a | n/a |

Highest-persistence child niches across the full horizon:

| Child niche | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | ---: | ---: |
| MEMORY_HISTORY_B2 | 6 | 1 | 0 |
| MEMORY_HISTORY_A5 | 5 | 3 | 2 |
| MEMORY_HISTORY_B3 | 5 | 3 | 2 |
| MEMORY_HISTORY_A4 | 4 | 1 | 0 |

### mixed
| Metric | Value |
| --- | --- |
| Classification | DRIFTING |
| Parent niche set | RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION |
| Child total share range | 0.0% to 21.9% |
| Child total share average (all / late 16 windows) | 5.3% / 7.0% |
| Parent share average (all / late 16 windows) | 33.1% / 30.7% |
| Parent vs children share ratio (final window) | 2.50 |
| Active child niche count (min-max) | 0-4 |
| Multi-niche coexistence | Yes, strongest multi-niche coexistence in the matrix. |
| Active niches above 1% share (final window) | 7 |
| Diversity index (mean / final) | 0.479 / 0.625 |
| Convergence to parent dominance | No |

Final-window child shares:

| Child niche | Share | Top lineage | Dominant share | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| RITUAL_STRANGE_UTILITY_B6 | 3.1% | lineage-a9f55308-ffe9-37c5-af6c-dccdcc4c6e11 | 58.4% | 2 | 1 | 1 |
| SOCIAL_WORLD_INTERACTION_A5 | 3.1% | ashen | 50.6% | 7 | 1 | 1 |

Highest-persistence child niches across the full horizon:

| Child niche | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | ---: | ---: |
| RITUAL_STRANGE_UTILITY_A4 | 9 | 4 | 3 |
| SOCIAL_WORLD_INTERACTION_A5 | 7 | 1 | 1 |
| SOCIAL_WORLD_INTERACTION_B5 | 6 | 4 | 3 |
| RITUAL_STRANGE_UTILITY_A2 | 5 | 5 | 4 |

### random-baseline
| Metric | Value |
| --- | --- |
| Classification | CHAOTIC |
| Parent niche set | MEMORY_HISTORY |
| Child total share range | 0.0% to 10.3% |
| Child total share average (all / late 16 windows) | 4.2% / 4.3% |
| Parent share average (all / late 16 windows) | 7.7% / 7.3% |
| Parent vs children share ratio (final window) | ∞ |
| Active child niche count (min-max) | 0-2 |
| Multi-niche coexistence | Intermittent only. |
| Active niches above 1% share (final window) | 4 |
| Diversity index (mean / final) | 0.476 / 0.655 |
| Convergence to parent dominance | No |

Final-window child shares:

| Child niche | Share | Top lineage | Dominant share | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| None active in final window | 0.0% | n/a | n/a | n/a | n/a | n/a |

Highest-persistence child niches across the full horizon:

| Child niche | Persistence windows | Extinction events | Reemergence events |
| --- | ---: | ---: | ---: |
| MEMORY_HISTORY_A4 | 7 | 3 | 2 |
| MEMORY_HISTORY_A5 | 6 | 1 | 0 |
| MEMORY_HISTORY_A3 | 5 | 2 | 1 |
| MEMORY_HISTORY_B3 | 5 | 2 | 1 |

## Lineage turnover analysis
| Scenario | Turnover rate | Behavior | Notable takeover events |
| --- | ---: | --- | --- |
| explorer-heavy | 0.000 | stable dominance | none |
| ritualist-heavy | 0.097 | moderate switching | w13 MEMORY_HISTORY_A3: lineage-021c2ef9-570a-3be6-b59d-12722837c715 -> lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e; w18 MEMORY_HISTORY_B3: lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e -> lineage-52a88415-0a21-3cc9-b650-91238362467e; w19 MEMORY_HISTORY_A4: lineage-021c2ef9-570a-3be6-b59d-12722837c715 -> lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e |
| gatherer-heavy | 0.167 | moderate switching | w2 MEMORY_HISTORY_A1: lineage-86339066-6dc9-3d85-b28e-0aa7764337c1 -> lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8; w10 MEMORY_HISTORY_B2: gilded -> lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8; w15 MEMORY_HISTORY_B3: gilded -> lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8; w26 MEMORY_HISTORY_A5: gilded -> lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 |
| mixed | 0.182 | frequent but bounded switching | w7 RITUAL_STRANGE_UTILITY_A2: lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 -> lineage-273526f2-6bb1-3161-bd43-223fed00d87a; w13 RITUAL_STRANGE_UTILITY_B3: lineage-919ed84e-030d-36a4-b148-d07afbcaa018 -> lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6; w20 RITUAL_STRANGE_UTILITY_A4: lineage-919ed84e-030d-36a4-b148-d07afbcaa018 -> lineage-273526f2-6bb1-3161-bd43-223fed00d87a; w24 SOCIAL_WORLD_INTERACTION_B5: mirrored -> ashen |
| random-baseline | 0.056 | moderate switching | w13 MEMORY_HISTORY_A3: lineage-921d5682-d620-32f5-85f5-6055392a6f2b -> lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95; w22 MEMORY_HISTORY_B4: lineage-921d5682-d620-32f5-85f5-6055392a6f2b -> lineage-76e85293-cea8-3da1-b9c8-d72e0cc8fe62 |

Key lineage findings:

- Explorer-heavy is lineage-stable inside active child niches: no dominant-lineage changes were observed despite repeated niche disappearance/reappearance.
- Ritualist-heavy and gatherer-heavy show moderate switching concentrated in MEMORY_HISTORY children rather than system-wide churn.
- Mixed has the highest turnover count, but takeovers remain localized to a small set of child niches and do not produce single-lineage lock-in.
- Final dominant-lineage concentration stays bounded rather than fully monopolistic: the mixed final window ends near 50-58% dominance in its two active children, while explorer-heavy and ritualist-heavy end with narrower but more concentrated single-child ownership.

## Structural behavior
| Scenario | Collapse/reformation pattern | Converges to parent dominance? | Multi-niche coexistence? |
| --- | --- | --- | --- |
| explorer-heavy | repeated child collapse/reformation | No | Yes, but usually only 1-2 children at a time. |
| ritualist-heavy | repeated child collapse/reformation | No | Yes, but narrow and intermittent. |
| gatherer-heavy | repeated child collapse/reformation | No | Only intermittently; not durable into the final window. |
| mixed | repeated child collapse/reformation | No | Yes, strongest multi-niche coexistence in the matrix. |
| random-baseline | repeated child collapse/reformation | No | Intermittent only. |

## Success criteria evaluation
- `>=3 scenarios maintain non-zero child share across ALL windows`: **failed** (0/5).
- `>=2 scenarios maintain >3% child share long-term`: **passed** (5/5).
- `No scenario permanently collapses to 0 after bifurcation`: **inconclusive/partial**. No confirmed permanent post-bifurcation collapse inside the observed horizon, but gatherer-heavy and random-baseline both end at 0.0% child share in the final window.
- `Lineage turnover exists but is not chaotic`: **passed**. Turnover is present in four scenarios, but events are sparse and localized.
- `No convergence to single dominant niche`: **passed**. Parent niches never exceed 50% share in any scenario.

Overall assessment: the system shows durable diversity and bounded lineage competition, but child-niche continuity is not strong enough across the full horizon to satisfy the strict stability bar.

LONG_HORIZON_RESULT: PARTIAL
