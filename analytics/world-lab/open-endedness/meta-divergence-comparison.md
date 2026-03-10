# Meta Divergence Comparison

## 1) Scope / sample size
- Comparison across four ablation worlds with shared seed pool.

## 2) Method summary
- Final-season snapshot + full-season trend metrics.

## 3) Key findings
- Divergence is strongest when ecosystem balancing subsystems are removed.
- Trait interactions contribute materially to sustained novelty and branch entropy.

## 4) Dominant families / branches / lineages / mechanics
| World | Dominant Family | Dominant Branch | Dominant Lineage | Family Turnover | Branch Entropy | Lineage Concentration |
|---|---|---|---|---:|---:|---:|
| A | chaos | mobility.lane-dancer | stormbound | 0.0000 | 2.3789 | 0.0389 |
| B | chaos | mobility.lane-dancer | stormbound | 0.0000 | 2.3897 | 0.0373 |
| C | chaos | mobility.lane-dancer | stormbound | 0.0000 | 2.3905 | 0.0417 |
| D | chaos | consistency.anchor | stormbound | 0.3000 | 1.5011 | 0.0442 |

## 5) Rare but viable systems
- See `rareLineagePersistence` and `noveltyRatePerSeason` in `meta-divergence-test-data.json`.

## 6) Dead or suspicious systems
- World C is the primary suspicious profile due to concentration growth and lower exploratory turnover.

## 7) Confidence / caveats
- Confidence: moderate; cross-world consistency is strong, run multiplicity is still limited.

## 8) Actionable next review steps
- Prioritize controls around World C failure mode before altering world A balance knobs.
