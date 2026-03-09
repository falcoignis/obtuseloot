# Multi-Run Large World Simulation Report

## 1) Scope / sample size
- Large runs completed: 3
- Seeds: 90212, 90213, 90214
- Each run: 600 players, 4 artifacts/player, 24 sessions/season, 4 seasons.

## 2) Method summary
- Independent large-world runs executed with distinct seeds and identical config.
- Per-run metrics compared for average, range, and run-to-run variance.

## 3) Key findings
- Average dominant family share: 28.55% (stable).
- Average branch convergence: 57.45% (stable).
- Average mutation frequency: 87.01% (stable).

## 4) Per-run metrics
| run | seed | dom family | convergence | mutation | memory events | awakening | fusion | top family | top branch |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| 1 | 90212 | 29.04% | 57.56% | 86.52% | 904104 | 73.16% | 21.38% | chaos (29.04%) | brutality.quarry (58.14%) |
| 2 | 90213 | 28.03% | 57.55% | 87.54% | 912456 | 73.58% | 22.70% | chaos (28.03%) | brutality.quarry (58.15%) |
| 3 | 90214 | 28.57% | 57.23% | 86.97% | 905679 | 72.91% | 22.59% | chaos (28.57%) | brutality.quarry (57.07%) |

## 5) Rare but viable systems
- chaos.awakened-entropy: rare-but-present in 3/3 runs.
- chaos.awakened-variant: rare-but-present in 3/3 runs.
- chaos.paradox: rare-but-present in 3/3 runs.
- chaos.sprawl: rare-but-present in 3/3 runs.
- consistency.anchor: rare-but-present in 3/3 runs.
- consistency.boss-ledger: rare-but-present in 3/3 runs.
- consistency.discipline: rare-but-present in 3/3 runs.
- mobility.awakened-variant: rare-but-present in 3/3 runs.
- precision.awakened-variant: rare-but-present in 3/3 runs.
- survival.awakened-variant: rare-but-present in 3/3 runs.

## 6) Dead or suspicious systems
- No branch met the strict dead candidate threshold (<=2 occurrences per run).
- Trigger concentration check: top trigger varied across runs? no.
- Mechanic concentration check: top mechanic varied across runs? no.

## 7) Confidence / caveats
- 3-run minimum achieved; confidence is stronger than single-run but still below preferred 5-run validation.
- Memory event frequency is count-based and scale-sensitive, so comparisons use relative stability and not absolute thresholds.

## 8) Suggested next review steps
- If a family remains top in >=3/3 runs with low variance, treat as high-confidence dominance.
- Expand to 5 runs before applying any medium/high-impact mechanics changes.
