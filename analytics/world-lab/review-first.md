# Review First

## Recommended reading order
1. `world-sim-confidence-report.md`
2. `multirun-world-sim-report.md`
3. `world-sim-meta-comparison.md`
4. `world-sim-balance-findings.md`
5. `../ecosystem-balance-report.md`
6. `../ecosystem-balance-suggestions.md`

## Top 3 most important findings
1. Dominant family share stays elevated at 28.55% across 3/3 large runs.
2. Branch convergence increases to 57.45% at large scale.
3. Mutation throughput remains high (87.01%), so collapse is not due to mutation inactivity.

## High-confidence findings
- Dominant family concentration persists across all large runs.
- Awakening remains consistently active in large runs.

## Provisional findings
- Exact impact of memory-driven trigger underrepresentation needs 2 more runs.

## What to tune first (if anything)
- Start with tiny weight adjustments on repeatedly dominant family/branch combinations only.

## What to simulate again before tuning
- Extend multi-run large simulation from 3 to 5 runs for stronger confidence on memory and rare-branch effects.
