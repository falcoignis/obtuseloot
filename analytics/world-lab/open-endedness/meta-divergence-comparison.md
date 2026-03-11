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
| A | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.5724 | 1.9887 | 0.0452 |
| B | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.5050 | 1.9861 | 0.0474 |
| C | chaos | mobility.lane-dancer | gilded | 0.0000 | 2.5901 | 1.9713 | 0.0497 |
| D | chaos | consistency.boss-ledger | gilded | 0.2000 | 1.6186 | 1.9857 | 0.0459 |

## 5) Rare but viable systems
- See `rareLineagePersistence` and `noveltyRatePerSeason` in `meta-divergence-test-data.json`.

## 6) Dead or suspicious systems
- World C is the primary suspicious profile due to concentration growth and lower exploratory turnover.

## 7) Confidence / caveats
- Confidence: moderate; cross-world consistency is strong, run multiplicity is still limited.

## 8) Actionable next review steps
- Prioritize controls around World C failure mode before altering world A balance knobs.
