# Deep Long-Horizon Post-Ability-Expansion Validation

- Measurement-only analysis over existing artifacts in `analytics/validation-suite-rerun/deep-long-horizon-test/*`.
- No code, configs, or simulation inputs were changed for this report.
- Important limitation: the repository does not contain a separate newer 32-window post-expansion dataset under the requested path. Accordingly, the comparison section uses the existing pre-ability baseline report for the same dataset family as the only direct comparator, so most deltas are zero-by-construction and should be treated as a verification control rather than an independent fresh rerun.

## SECTION 1: SCENARIO SUMMARY TABLE

| Scenario | Parent niche(s) | Zero-share windows | Peak child share | Final child share | Final parent share | Turnover events | Extinction / Re-emergence | Longest continuous persistence | Final diversity | Classification |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| explorer-heavy | RITUAL_STRANGE_UTILITY | 4 | 6.9% | 6.9% | 6.9% | 0 | 17 / 18 | 9 | 0.690 | DRIFTING |
| ritualist-heavy | MEMORY_HISTORY | 6 | 6.9% | 6.9% | 17.2% | 3 | 12 / 13 | 6 | 0.621 | DRIFTING |
| gatherer-heavy | MEMORY_HISTORY | 7 | 6.9% | 0.0% | 3.4% | 5 | 15 / 13 | 6 | 0.655 | COLLAPSING |
| mixed | RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION | 4 | 21.9% | 6.2% | 15.6% | 8 | 23 / 23 | 5 | 0.625 | CHAOTIC |
| random-baseline | MEMORY_HISTORY | 8 | 10.3% | 0.0% | 6.9% | 2 | 17 / 16 | 6 | 0.655 | COLLAPSING |

## SECTION 2: TIME-SERIES SUMMARY

### explorer-heavy
- Child total share by window (%): 3.4, 6.9, 3.4, 3.4, 3.4, 6.9, 0.0, 6.9, 3.4, 6.9, 6.9, 6.9, 3.4, 0.0, 0.0, 6.9, 6.9, 6.9, 3.4, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 6.9, 6.9.
- Parent share by window (%): 17.2, 10.3, 17.2, 13.8, 13.8, 13.8, 13.8, 17.2, 17.2, 6.9, 10.3, 17.2, 17.2, 17.2, 13.8, 10.3, 17.2, 13.8, 13.8, 6.9, 10.3, 6.9, 17.2, 10.3, 17.2, 13.8, 20.7, 20.7, 6.9, 13.8, 24.1, 6.9.
- Active child niche count by window: 1, 2, 1, 1, 1, 1, 0, 2, 1, 2, 2, 1, 1, 0, 0, 2, 2, 2, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 0, 1, 2, 2.
- Diversity index by window: 0.379, 0.414, 0.414, 0.414, 0.414, 0.414, 0.414, 0.448, 0.483, 0.483, 0.483, 0.483, 0.483, 0.483, 0.517, 0.517, 0.517, 0.552, 0.552, 0.552, 0.552, 0.552, 0.586, 0.586, 0.621, 0.621, 0.621, 0.621, 0.621, 0.621, 0.655, 0.690.

### ritualist-heavy
- Child total share by window (%): 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 0.0, 6.9, 0.0, 3.4, 6.9, 3.4, 3.4, 6.9, 3.4, 6.9, 3.4, 3.4, 6.9, 0.0, 0.0, 6.9, 0.0, 3.4, 3.4, 3.4, 0.0, 6.9, 6.9, 6.9.
- Parent share by window (%): 13.8, 6.9, 13.8, 10.3, 3.4, 13.8, 3.4, 13.8, 13.8, 13.8, 13.8, 13.8, 13.8, 6.9, 10.3, 3.4, 17.2, 13.8, 3.4, 3.4, 13.8, 6.9, 13.8, 17.2, 17.2, 10.3, 6.9, 13.8, 20.7, 20.7, 6.9, 17.2.
- Active child niche count by window: 1, 1, 1, 1, 1, 1, 1, 1, 0, 2, 0, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 2, 2, 2.
- Diversity index by window: 0.345, 0.414, 0.414, 0.414, 0.414, 0.414, 0.414, 0.414, 0.414, 0.448, 0.483, 0.483, 0.483, 0.483, 0.483, 0.483, 0.483, 0.517, 0.552, 0.552, 0.552, 0.552, 0.552, 0.552, 0.552, 0.586, 0.586, 0.621, 0.621, 0.621, 0.621, 0.621.

### gatherer-heavy
- Child total share by window (%): 6.9, 3.4, 3.4, 6.9, 6.9, 3.4, 6.9, 6.9, 6.9, 3.4, 0.0, 3.4, 0.0, 0.0, 6.9, 3.4, 6.9, 0.0, 0.0, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 3.4, 0.0.
- Parent share by window (%): 10.3, 10.3, 13.8, 13.8, 13.8, 13.8, 3.4, 17.2, 13.8, 6.9, 6.9, 10.3, 10.3, 17.2, 17.2, 6.9, 13.8, 6.9, 3.4, 6.9, 10.3, 3.4, 13.8, 10.3, 13.8, 3.4, 13.8, 6.9, 10.3, 6.9, 6.9, 3.4.
- Active child niche count by window: 2, 1, 1, 2, 2, 1, 1, 1, 2, 1, 0, 1, 0, 0, 1, 1, 2, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0.
- Diversity index by window: 0.345, 0.345, 0.379, 0.379, 0.414, 0.414, 0.414, 0.414, 0.448, 0.448, 0.483, 0.483, 0.483, 0.483, 0.483, 0.483, 0.517, 0.552, 0.552, 0.552, 0.552, 0.552, 0.552, 0.586, 0.621, 0.621, 0.621, 0.621, 0.621, 0.621, 0.621, 0.655.

### mixed
- Child total share by window (%): 6.2, 0.0, 3.1, 6.2, 3.1, 0.0, 6.2, 0.0, 3.1, 3.1, 3.1, 3.1, 6.2, 3.1, 3.1, 6.2, 3.1, 3.1, 3.1, 3.1, 0.0, 9.4, 6.2, 6.2, 6.2, 6.2, 6.2, 3.1, 9.4, 18.8, 21.9, 6.2.
- Parent share by window (%): 28.1, 25.0, 37.5, 31.2, 34.4, 37.5, 37.5, 40.6, 40.6, 43.8, 37.5, 37.5, 37.5, 43.8, 25.0, 31.2, 43.8, 37.5, 34.4, 37.5, 31.2, 46.9, 37.5, 34.4, 25.0, 37.5, 21.9, 15.6, 25.0, 28.1, 18.8, 15.6.
- Active child niche count by window: 2, 0, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 0, 2, 1, 2, 2, 2, 2, 1, 2, 4, 3, 2.
- Diversity index by window: 0.344, 0.344, 0.375, 0.375, 0.375, 0.375, 0.375, 0.406, 0.438, 0.438, 0.438, 0.438, 0.438, 0.438, 0.469, 0.469, 0.500, 0.500, 0.500, 0.500, 0.500, 0.531, 0.531, 0.531, 0.562, 0.562, 0.562, 0.562, 0.562, 0.625, 0.625, 0.625.

### random-baseline
- Child total share by window (%): 3.4, 3.4, 6.9, 3.4, 0.0, 3.4, 0.0, 3.4, 3.4, 6.9, 3.4, 0.0, 6.9, 6.9, 6.9, 6.9, 3.4, 6.9, 3.4, 0.0, 0.0, 6.9, 6.9, 0.0, 0.0, 3.4, 6.9, 10.3, 6.9, 6.9, 6.9, 0.0.
- Parent share by window (%): 10.3, 13.8, 3.4, 3.4, 3.4, 10.3, 10.3, 6.9, 6.9, 10.3, 3.4, 3.4, 10.3, 6.9, 6.9, 17.2, 13.8, 3.4, 6.9, 6.9, 10.3, 3.4, 6.9, 10.3, 3.4, 17.2, 6.9, 3.4, 13.8, 3.4, 0.0, 6.9.
- Active child niche count by window: 1, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 0, 2, 2, 2, 2, 1, 2, 1, 0, 0, 2, 2, 0, 0, 1, 2, 2, 1, 2, 2, 0.
- Diversity index by window: 0.310, 0.345, 0.345, 0.379, 0.379, 0.379, 0.379, 0.379, 0.448, 0.448, 0.448, 0.448, 0.448, 0.448, 0.448, 0.483, 0.483, 0.517, 0.517, 0.517, 0.517, 0.517, 0.517, 0.517, 0.517, 0.552, 0.552, 0.586, 0.586, 0.586, 0.586, 0.655.

## SECTION 3: PER-SCENARIO TABLES

### explorer-heavy

| Metric | Value |
| --- | --- |
| Parent niche set | RITUAL_STRANGE_UTILITY |
| Child niche count observed | 12 |
| Zero-share windows | 4 |
| Late-window child share average | 5.0% |
| Final parent vs child ratio | 1.00 |
| Final active niches above 1% share | 2 |
| Avg / final diversity index | 0.524 / 0.690 |
| Classification | DRIFTING |

| Child niche | Persistence windows active | Longest continuous persistence | Extinction events | Re-emergence events | Final share | Final dominant lineage | Final dominant share |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| RITUAL_STRANGE_UTILITY_A1 | 0 | 0 | 0 | 0 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.9% |
| RITUAL_STRANGE_UTILITY_A2 | 2 | 1 | 2 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 100.0% |
| RITUAL_STRANGE_UTILITY_A3 | 1 | 1 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.8% |
| RITUAL_STRANGE_UTILITY_A4 | 2 | 2 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 100.0% |
| RITUAL_STRANGE_UTILITY_A5 | 1 | 1 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.9% |
| RITUAL_STRANGE_UTILITY_A6 | 1 | 1 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.7% |
| RITUAL_STRANGE_UTILITY_B1 | 25 | 9 | 5 | 6 | 3.4% | wild-4233 | 82.8% |
| RITUAL_STRANGE_UTILITY_B2 | 1 | 1 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.8% |
| RITUAL_STRANGE_UTILITY_B3 | 2 | 2 | 1 | 1 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.9% |
| RITUAL_STRANGE_UTILITY_B4 | 2 | 1 | 2 | 2 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 100.0% |
| RITUAL_STRANGE_UTILITY_B5 | 2 | 1 | 2 | 2 | 0.0% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 100.0% |
| RITUAL_STRANGE_UTILITY_B6 | 1 | 1 | 0 | 1 | 3.4% | lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 | 99.3% |

### ritualist-heavy

| Metric | Value |
| --- | --- |
| Parent niche set | MEMORY_HISTORY |
| Child niche count observed | 10 |
| Zero-share windows | 6 |
| Late-window child share average | 3.9% |
| Final parent vs child ratio | 2.50 |
| Final active niches above 1% share | 2 |
| Avg / final diversity index | 0.504 / 0.621 |
| Classification | DRIFTING |

| Child niche | Persistence windows active | Longest continuous persistence | Extinction events | Re-emergence events | Final share | Final dominant lineage | Final dominant share |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| MEMORY_HISTORY_A1 | 0 | 0 | 0 | 0 | 0.0% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 42.4% |
| MEMORY_HISTORY_A2 | 2 | 1 | 2 | 2 | 0.0% | lineage-021c2ef9-570a-3be6-b59d-12722837c715 | 100.0% |
| MEMORY_HISTORY_A3 | 6 | 6 | 1 | 1 | 0.0% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 57.3% |
| MEMORY_HISTORY_A4 | 3 | 2 | 2 | 2 | 0.0% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 39.4% |
| MEMORY_HISTORY_A5 | 6 | 3 | 1 | 2 | 3.4% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 78.8% |
| MEMORY_HISTORY_B1 | 1 | 1 | 1 | 0 | 0.0% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 44.7% |
| MEMORY_HISTORY_B2 | 6 | 6 | 1 | 1 | 0.0% | lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e | 100.0% |
| MEMORY_HISTORY_B3 | 2 | 1 | 2 | 2 | 0.0% | lineage-52a88415-0a21-3cc9-b650-91238362467e | 44.6% |
| MEMORY_HISTORY_B4 | 2 | 1 | 2 | 2 | 0.0% | lineage-021c2ef9-570a-3be6-b59d-12722837c715 | 60.3% |
| MEMORY_HISTORY_B5 | 3 | 3 | 0 | 1 | 3.4% | lineage-021c2ef9-570a-3be6-b59d-12722837c715 | 100.0% |

### gatherer-heavy

| Metric | Value |
| --- | --- |
| Parent niche set | MEMORY_HISTORY |
| Child niche count observed | 11 |
| Zero-share windows | 7 |
| Late-window child share average | 3.2% |
| Final parent vs child ratio | ∞ |
| Final active niches above 1% share | 0 |
| Avg / final diversity index | 0.510 / 0.655 |
| Classification | COLLAPSING |

| Child niche | Persistence windows active | Longest continuous persistence | Extinction events | Re-emergence events | Final share | Final dominant lineage | Final dominant share |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| MEMORY_HISTORY_A1 | 2 | 2 | 1 | 0 | 0.0% | lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 | 34.4% |
| MEMORY_HISTORY_A2 | 3 | 3 | 1 | 1 | 0.0% | lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 | 46.2% |
| MEMORY_HISTORY_A3 | 0 | 0 | 0 | 0 | 0.0% | lineage-86339066-6dc9-3d85-b28e-0aa7764337c1 | 48.6% |
| MEMORY_HISTORY_A4 | 4 | 4 | 1 | 1 | 0.0% | gilded | 49.6% |
| MEMORY_HISTORY_A5 | 5 | 3 | 3 | 3 | 0.0% | gilded | 54.6% |
| MEMORY_HISTORY_A6 | 0 | 0 | 0 | 0 | 0.0% | lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 | 100.0% |
| MEMORY_HISTORY_B1 | 2 | 1 | 2 | 1 | 0.0% | gilded | 48.2% |
| MEMORY_HISTORY_B2 | 6 | 6 | 1 | 1 | 0.0% | lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 | 48.0% |
| MEMORY_HISTORY_B3 | 5 | 3 | 3 | 3 | 0.0% | lineage-bdb7896b-c23b-3eed-b274-8956bad4e2c8 | 49.4% |
| MEMORY_HISTORY_B4 | 1 | 1 | 1 | 1 | 0.0% | lineage-86339066-6dc9-3d85-b28e-0aa7764337c1 | 41.5% |
| MEMORY_HISTORY_B5 | 2 | 1 | 2 | 2 | 0.0% | lineage-86339066-6dc9-3d85-b28e-0aa7764337c1 | 68.5% |

### mixed

| Metric | Value |
| --- | --- |
| Parent niche set | RITUAL_STRANGE_UTILITY, SOCIAL_WORLD_INTERACTION |
| Child niche count observed | 12 |
| Zero-share windows | 4 |
| Late-window child share average | 7.0% |
| Final parent vs child ratio | 2.50 |
| Final active niches above 1% share | 2 |
| Avg / final diversity index | 0.479 / 0.625 |
| Classification | CHAOTIC |

| Child niche | Persistence windows active | Longest continuous persistence | Extinction events | Re-emergence events | Final share | Final dominant lineage | Final dominant share |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| RITUAL_STRANGE_UTILITY_A1 | 1 | 1 | 1 | 0 | 0.0% | lineage-a9f55308-ffe9-37c5-af6c-dccdcc4c6e11 | 36.8% |
| RITUAL_STRANGE_UTILITY_A2 | 5 | 1 | 5 | 4 | 0.0% | lineage-273526f2-6bb1-3161-bd43-223fed00d87a | 47.1% |
| RITUAL_STRANGE_UTILITY_A3 | 4 | 4 | 1 | 1 | 0.0% | lineage-a9f55308-ffe9-37c5-af6c-dccdcc4c6e11 | 60.4% |
| RITUAL_STRANGE_UTILITY_A4 | 9 | 4 | 4 | 4 | 0.0% | lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 | 33.2% |
| RITUAL_STRANGE_UTILITY_A6 | 1 | 1 | 1 | 1 | 0.0% | lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 | 49.0% |
| RITUAL_STRANGE_UTILITY_B1 | 0 | 0 | 0 | 0 | 0.0% | lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 | 37.4% |
| RITUAL_STRANGE_UTILITY_B2 | 3 | 3 | 1 | 1 | 0.0% | lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 | 46.8% |
| RITUAL_STRANGE_UTILITY_B3 | 3 | 2 | 2 | 2 | 0.0% | lineage-3311aca8-d1ac-3b8f-bea1-1dd52674a0b6 | 52.5% |
| RITUAL_STRANGE_UTILITY_B4 | 3 | 2 | 2 | 2 | 0.0% | lineage-919ed84e-030d-36a4-b148-d07afbcaa018 | 53.7% |
| RITUAL_STRANGE_UTILITY_B6 | 2 | 1 | 1 | 2 | 3.1% | lineage-a9f55308-ffe9-37c5-af6c-dccdcc4c6e11 | 58.4% |
| SOCIAL_WORLD_INTERACTION_A5 | 7 | 5 | 1 | 2 | 3.1% | ashen | 50.6% |
| SOCIAL_WORLD_INTERACTION_B5 | 6 | 3 | 4 | 4 | 0.0% | ashen | 40.1% |

### random-baseline

| Metric | Value |
| --- | --- |
| Parent niche set | MEMORY_HISTORY |
| Child niche count observed | 12 |
| Zero-share windows | 8 |
| Late-window child share average | 4.3% |
| Final parent vs child ratio | ∞ |
| Final active niches above 1% share | 0 |
| Avg / final diversity index | 0.476 / 0.655 |
| Classification | COLLAPSING |

| Child niche | Persistence windows active | Longest continuous persistence | Extinction events | Re-emergence events | Final share | Final dominant lineage | Final dominant share |
| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| MEMORY_HISTORY_A1 | 4 | 3 | 2 | 2 | 0.0% | lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95 | 65.1% |
| MEMORY_HISTORY_A2 | 2 | 1 | 2 | 2 | 0.0% | lineage-921d5682-d620-32f5-85f5-6055392a6f2b | 100.0% |
| MEMORY_HISTORY_A3 | 5 | 3 | 2 | 2 | 0.0% | lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95 | 64.4% |
| MEMORY_HISTORY_A4 | 7 | 4 | 3 | 3 | 0.0% | lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95 | 48.6% |
| MEMORY_HISTORY_A5 | 6 | 6 | 1 | 1 | 0.0% | lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95 | 64.4% |
| MEMORY_HISTORY_A6 | 0 | 0 | 0 | 0 | 0.0% | lineage-4d856ef4-c7b7-327d-8d14-ca6dfeac3a95 | 100.0% |
| MEMORY_HISTORY_B1 | 1 | 1 | 1 | 0 | 0.0% | lineage-921d5682-d620-32f5-85f5-6055392a6f2b | 70.1% |
| MEMORY_HISTORY_B2 | 0 | 0 | 0 | 0 | 0.0% | lineage-921d5682-d620-32f5-85f5-6055392a6f2b | 99.6% |
| MEMORY_HISTORY_B3 | 5 | 4 | 2 | 2 | 0.0% | lineage-921d5682-d620-32f5-85f5-6055392a6f2b | 64.9% |
| MEMORY_HISTORY_B4 | 3 | 2 | 2 | 2 | 0.0% | lineage-76e85293-cea8-3da1-b9c8-d72e0cc8fe62 | 44.2% |
| MEMORY_HISTORY_B5 | 3 | 2 | 2 | 2 | 0.0% | lineage-76e85293-cea8-3da1-b9c8-d72e0cc8fe62 | 99.9% |
| MEMORY_HISTORY_B6 | 0 | 0 | 0 | 0 | 0.0% | lineage-921d5682-d620-32f5-85f5-6055392a6f2b | 100.0% |

## SECTION 4: LINEAGE TURNOVER ANALYSIS

| Scenario | Turnover events | Avg turnover-rate metric | Notes |
| --- | ---: | ---: | --- |
| explorer-heavy | 0 | 34.70 | bounded |
| ritualist-heavy | 3 | 29.96 | bounded |
| gatherer-heavy | 5 | 42.51 | elevated but not explosive |
| mixed | 8 | 41.91 | elevated but not explosive |
| random-baseline | 2 | 42.61 | bounded |

Key takeaways:
- explorer-heavy: 0 dominant-lineage turnovers across child niches, 17 extinction events, and 18 re-emergence events.
- ritualist-heavy: 3 dominant-lineage turnovers across child niches, 12 extinction events, and 13 re-emergence events.
- gatherer-heavy: 5 dominant-lineage turnovers across child niches, 15 extinction events, and 13 re-emergence events.
- mixed: 8 dominant-lineage turnovers across child niches, 23 extinction events, and 23 re-emergence events.
- random-baseline: 2 dominant-lineage turnovers across child niches, 17 extinction events, and 16 re-emergence events.

## SECTION 5: STRUCTURAL BEHAVIOR

### explorer-heavy
- Classification: **DRIFTING**.
- Final dominance structure: RITUAL_STRANGE_UTILITY_B1: 3.4% via wild-4233 (82.8% lineage concentration within niche); RITUAL_STRANGE_UTILITY_B6: 3.4% via lineage-58669209-8d02-3c2a-9b99-61d5be2d0cb2 (99.3% lineage concentration within niche).
- Zero-share windows: 4; longest continuous child-niche persistence: 9 windows; final child share: 6.9%.

### ritualist-heavy
- Classification: **DRIFTING**.
- Final dominance structure: MEMORY_HISTORY_A5: 3.4% via lineage-fede99e6-01de-3824-9cf8-69e7f61acc2e (78.8% lineage concentration within niche); MEMORY_HISTORY_B5: 3.4% via lineage-021c2ef9-570a-3be6-b59d-12722837c715 (100.0% lineage concentration within niche).
- Zero-share windows: 6; longest continuous child-niche persistence: 6 windows; final child share: 6.9%.

### gatherer-heavy
- Classification: **COLLAPSING**.
- Final dominance structure: no active child niches in final window.
- Zero-share windows: 7; longest continuous child-niche persistence: 6 windows; final child share: 0.0%.

### mixed
- Classification: **CHAOTIC**.
- Final dominance structure: RITUAL_STRANGE_UTILITY_B6: 3.1% via lineage-a9f55308-ffe9-37c5-af6c-dccdcc4c6e11 (58.4% lineage concentration within niche); SOCIAL_WORLD_INTERACTION_A5: 3.1% via ashen (50.6% lineage concentration within niche).
- Zero-share windows: 4; longest continuous child-niche persistence: 5 windows; final child share: 6.2%.

### random-baseline
- Classification: **COLLAPSING**.
- Final dominance structure: no active child niches in final window.
- Zero-share windows: 8; longest continuous child-niche persistence: 6 windows; final child share: 0.0%.

## SECTION 6: COMPARISON VS BASELINE

Baseline source: `docs/deep-long-horizon-validation.md` (pre-ability-expansion report for the same dataset family).

| Scenario | Δ zero-share windows | Δ persistence windows active (max) | Δ turnover events | Δ diversity index (final) | Δ final child share | Comparison note |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| explorer-heavy | +0 | +0 | +0 | -0.000 | -0.0pp | Control-style comparison only; no independent newer 32-window post-expansion dataset is present under the requested source path. |
| ritualist-heavy | +0 | +0 | +0 | -0.000 | -0.0pp | Control-style comparison only; no independent newer 32-window post-expansion dataset is present under the requested source path. |
| gatherer-heavy | +1 | +1 | +0 | +0.000 | +0.0pp | Control-style comparison only; no independent newer 32-window post-expansion dataset is present under the requested source path. |
| mixed | +0 | -13 | +0 | +0.000 | +0.1pp | Control-style comparison only; no independent newer 32-window post-expansion dataset is present under the requested source path. |
| random-baseline | +0 | +1 | +0 | +0.000 | +0.0pp | Control-style comparison only; no independent newer 32-window post-expansion dataset is present under the requested source path. |

Interpretation: because the requested data source resolves to the existing deep-long-horizon dataset, the baseline comparison acts mainly as a reproducibility/control check. It does **not** establish a new independent post-expansion ecological delta on its own.

## SECTION 7: SUCCESS CRITERIA EVALUATION

- FAIL: ≥3 scenarios have zero zero-share windows.
- PASS: ≥2 scenarios maintain >3% child share long-term.
- FAIL: no scenario permanently collapses after bifurcation.
- PASS: lineage turnover remains bounded.
- FAIL: no convergence to a single dominant niche.

Overall pass count: 2 / 5.

LONG_HORIZON_RESULT: PARTIAL
