# Minimum Role Separation Gating Report

- Role-distance formula: weighted L1 distance over the 6 normalized role axes, divided by total weight, bounded to [0,1].
- roleSplitThreshold used: 0.40
- Mode used: hard_reject (hard ecological incompatibility)
- Soft role repulsion: still active and combined with gating.
- Hard rejection semantics: if `roleDistance >= roleSplitThreshold`, candidate niche compatibility is rejected.

## Previously merged strategies now forced apart
- burst-heavy direct trigger strategies vs persistence-heavy memory-driven strategies
- environment-agnostic solo damage strategies vs environment-dependent interaction-heavy support strategies
- high mobility burst profiles vs stationary persistence profiles when behavior overlap was otherwise close

## Risk analysis
- Over-fragmentation risk is bounded by existing margin/hysteresis/candidate-support/merge-prune controls.
- Hard gating only triggers on large role deltas; smaller deltas still use soft repulsion.
- Deterministic, interpretable decision path preserved.
