# World Simulation Balance Findings

## Focused diagnostics for dominant systems
- Run 1 seed 90212: dominant family chaos (29.04%), dominant branch brutality.quarry (58.14%).
- Run 2 seed 90213: dominant family chaos (28.03%), dominant branch brutality.quarry (58.15%).
- Run 3 seed 90214: dominant family chaos (28.57%), dominant branch brutality.quarry (57.07%).

## Ranked recommendations
### Dominant family concentration persists across large runs (medium)
1. Issue summary: Dominant family concentration persists across large runs
2. Evidence: Average dominant-family share 28.55%, σ=0.41%.
3. Confidence level: high confidence
4. Estimated impact: late-season high (large sim only)
5. Suggested response: Candidate for small weight adjustment
6. Act now or gather more data: act now (small safe tuning pass)

### Branch convergence increases with scale (medium)
1. Issue summary: Branch convergence increases with scale
2. Evidence: Small 50.71% vs large avg 57.45%.
3. Confidence level: moderate confidence
4. Estimated impact: mid/late season medium-high (small + large)
5. Suggested response: Needs another simulation pass
6. Act now or gather more data: gather more simulation first

### Memory-driven trigger underrepresentation (low)
1. Issue summary: Memory-driven trigger underrepresentation
2. Evidence: on_memory_event trigger is present but not top-ranked in any large run.
3. Confidence level: provisional
4. Estimated impact: late-season medium (isolated ecology vs world-lab mismatch)
5. Suggested response: Observe only
6. Act now or gather more data: needs another simulation pass

