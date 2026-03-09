# Review First

## Open files in this order
1. `small-world-sim-summary.md`
2. `large-world-sim-summary.md`
3. `world-sim-meta-comparison.md`
4. `large-world-sim-report.md`
5. `../ecosystem-balance-report.md`
6. `large-world-sim-data.json` (spot checks)

## Most important findings
- Large-run dominant family concentration: 28.59%.
- Large-run branch convergence: 58.08%.
- Large-run dead branch rate: 0.00%.

## Actionable metrics
- `world.dominant_family_rate`
- `world.branch_convergence_rate`
- `world.dead_branch_rate`
- `artifact.mutation_counts`
- `artifact.memory_profile_summaries`

## Healthy signals
- Memory, mutation, awakening, and fusion systems appear in both runs.
- Diversity timeline remains non-zero over all simulated seasons.

## Risk signals
- Any elevated late-season concentration plus dead-branch growth suggests meta lock-in.
- Rare path drop-off in the large run is a likely balancing hotspot.

## Suggested reruns
- Re-run large scale with 2-3 new seeds.
- Run focused tests around dominant family + top branch combinations.
