# Extended 5-Run Large-World Validation

## Configuration
- Runs: 5
- Seeds: 90212, 90213, 90214, 90215, 90216
- Players: 600
- Artifacts/player: 4
- Sessions/season: 24
- Seasons: 4
- Encounter density: 8

## Dominant outcomes per run
| Run | Seed | Dominant family | Family share | Dominant branch | Branch share |
|---|---:|---|---:|---|---:|
| 1 | 90212 | chaos | 44.07% | mobility.lane-dancer | 17.55% |
| 2 | 90213 | chaos | 39.03% | mobility.lane-dancer | 24.78% |
| 3 | 90214 | chaos | 49.85% | chaos.sprawl | 12.14% |
| 4 | 90215 | chaos | 47.85% | mobility.lane-dancer | 16.62% |
| 5 | 90216 | chaos | 45.07% | chaos.paradox | 14.12% |

## Variance between runs
- Dominant family share σ: 3.688%
- Dominant branch share σ: 4.310%
- World dominant-family-rate σ: 3.688%
- World branch-convergence-rate σ: 0.806%

## Attractor confirmation
- Dominant family frequency: {'chaos': 5}
- Dominant branch frequency: {'mobility.lane-dancer': 3, 'chaos.sprawl': 1, 'chaos.paradox': 1}
- **Confirmation:** Partial persistence — chaos remains the dominant family in 5/5 runs, but dominant branch concentration diversified (top branch appears in 3/5 runs).
