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
| A | chaos | chaos.awakened-variant | ashen | 0.0000 | 2.5645 | 1.9758 | 0.0444 |
| B | chaos | chaos.awakened-variant | ashen | 0.0000 | 2.6148 | 1.9718 | 0.0416 |
| C | chaos | survival.guardian | ashen | 0.0000 | 2.6572 | 1.9661 | 0.0453 |
| D | chaos | survival.guardian | ashen | 0.0000 | 1.7581 | 1.9679 | 0.0437 |

## 5) Rare but viable systems
- See `rareLineagePersistence` and `noveltyRatePerSeason` in `meta-divergence-test-data.json`.

## 6) Dead or suspicious systems
- World C is the primary suspicious profile due to concentration growth and lower exploratory turnover.

## 7) Confidence / caveats
- Confidence: moderate; cross-world consistency is strong, run multiplicity is still limited.

## 8) Actionable next review steps
- Prioritize controls around World C failure mode before altering world A balance knobs.
