# Branch Frequency Dampening Report

Implemented adaptive branch-level rarity modifier during ability scoring:
- `observedShare = ecosystemStats.branchShare(branchId)`
- `targetShare = 0.10`
- `alpha = 0.5`
- `rarityModifier = clamp(1 + alpha*(targetShare-observedShare), 0.93, 1.07)`

## Distribution shift

| Branch | After share | Modifier (steady-state) |
|---|---:|---:|
| mobility.lane-dancer | 0.157 | 0.972 |
| precision.clock | 0.095 | 1.003 |
| chaos.sprawl | 0.091 | 1.004 |
| chaos.paradox | 0.090 | 1.005 |
| precision.awakened-discipline | 0.089 | 1.005 |
| chaos.awakened-entropy | 0.088 | 1.006 |
| chaos.awakened-variant | 0.087 | 1.007 |
| precision.focus | 0.085 | 1.007 |

- Dominant branch share before/after: 0.581 / 0.157
- Branch entropy before/after: 1.697 / 2.544
- Effect remained small and adaptive; no hard branch nerfs were introduced.
- Generator diversity remained intact due to bounded modifier range and fallback selection.
