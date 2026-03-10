# Extended Multi-Run World Simulation Report

## 1) Scope / sample size
- Independent large-world runs: 5
- Run config: ~600 players, 4 artifacts/player, 24 sessions/season, 4 seasons
- Seeds: 92011, 92012, 92013, 92014, 92015

## 2) Per-run summaries
| Run | Seed | Dominant family (share) | Dominant branch (share) | Family entropy | Branch entropy | Lineage concentration | Mutation frequency | Awakening adoption | Fusion adoption | Niche count | Trait variance |
|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 92011 | chaos (0.418) | mobility.lane-dancer (0.119) | 1.488 | 2.567 | 0.039 | 1.000 | 0.975 | 0.300 | 24 | 0.017415 |
| 2 | 92012 | chaos (0.432) | mobility.lane-dancer (0.113) | 1.576 | 2.666 | 0.037 | 1.000 | 0.972 | 0.290 | 24 | 0.014826 |
| 3 | 92013 | chaos (0.449) | mobility.lane-dancer (0.166) | 1.423 | 2.539 | 0.040 | 1.000 | 0.971 | 0.279 | 24 | 0.020822 |
| 4 | 92014 | chaos (0.486) | mobility.lane-dancer (0.154) | 1.392 | 2.550 | 0.039 | 1.000 | 0.973 | 0.306 | 24 | 0.024024 |
| 5 | 92015 | chaos (0.420) | mobility.lane-dancer (0.158) | 1.448 | 2.557 | 0.038 | 1.000 | 0.975 | 0.283 | 24 | 0.018241 |

## 3) Aggregate averages + variance across runs
| Metric | Mean | StdDev | Min | Max |
|---|---:|---:|---:|---:|
| dominant_family_share | 0.4409 | 0.0252 | 0.4177 | 0.4862 |
| dominant_branch_share | 0.1420 | 0.0219 | 0.1127 | 0.1660 |
| family_entropy | 1.4653 | 0.0639 | 1.3916 | 1.5764 |
| branch_entropy | 2.5761 | 0.0461 | 2.5392 | 2.6665 |
| lineage_concentration | 0.0385 | 0.0010 | 0.0370 | 0.0395 |
| mutation_frequency | 0.9999 | 0.0000 | 0.9999 | 0.9999 |
| awakening_adoption | 0.9732 | 0.0015 | 0.9714 | 0.9750 |
| fusion_adoption | 0.2916 | 0.0099 | 0.2793 | 0.3057 |
| niche_count | 24.0000 | 0.0000 | 24.0000 | 24.0000 |
| trait_variance | 0.0191 | 0.0031 | 0.0148 | 0.0240 |

## 4) Convergence trends
- Top family = `chaos` in 5/5 runs.
- Top branch = `brutality.quarry` in 0/5 runs.
- Trend classification: **partial convergence**.
- Confidence: **moderate** (5 runs + dominant-share variance).

## 5) Confidence notes
- high: repeated in >=4/5 runs with low variance.
- moderate: consistent direction but noisier run-to-run behavior.
