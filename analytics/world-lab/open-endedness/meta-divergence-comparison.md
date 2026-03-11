# Meta Divergence Comparison

## 1) Scope / sample size
- Comparison across four ablation worlds with shared seed pool.

## 2) Method summary
- Final-season snapshot + full-season trend metrics.

## 3) Key findings
- Divergence is strongest when ecosystem balancing subsystems are removed.
- Trait interactions contribute materially to sustained novelty and branch entropy.

## 4) Dominant families / branches / lineages / mechanics
| World | Dominant Family | Dominant Branch | Dominant Lineage | Family Turnover | Branch Entropy | Gate Diversity | Lineage Concentration |
|---|---|---|---|---:|---:|---:|---:|
| A | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.6264 | 1.9757 | 0.0483 |
| B | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.5367 | 1.9837 | 0.0460 |
| C | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.6451 | 1.9590 | 0.0444 |
| D | chaos | consistency.boss-ledger | gilded | 0.0000 | 1.6279 | 1.9903 | 0.0464 |

## 5) Rare but viable systems
- See `rareLineagePersistence` and `noveltyRatePerSeason` in `meta-divergence-test-data.json`.

## 6) Dead or suspicious systems
- World C is the primary suspicious profile due to concentration growth and lower exploratory turnover.

## 7) Confidence / caveats
- Confidence: moderate; cross-world consistency is strong, run multiplicity is still limited.

## 8) Actionable next review steps
- Prioritize controls around World C failure mode before altering world A balance knobs.
