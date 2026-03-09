# World Simulation Meta Comparison

## Scale comparison
- Small: 40 players, 2 artifacts/player, 12 sessions/season x 2 seasons.
- Large: 600 players, 4 artifacts/player, 24 sessions/season x 4 seasons.

## Behavioral shifts (small -> large)
- Dominant family rate: 31.51% -> 28.59%.
- Branch convergence: 52.92% -> 58.08%.
- Dead branch rate: 0.00% -> 0.00%.
- Mutation frequency: 91.25% -> 87.33%.
- Awakening adoption: 65.26% -> 73.41%.
- Fusion adoption: 21.72% -> 20.44%.

## Interpretation
- Large-scale run exposes long-horizon concentration and branch viability more clearly than the small smoke pass.
- Memory and mutation systems remain active in both scales, with larger sample size reducing noise.
- Rare branches that disappear in large runs are higher-confidence balancing candidates.
