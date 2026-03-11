# Minimum Role Separation Gating Report

## Role axes used
- Axes: [support_vs_damage, burst_vs_persistence, mobility_vs_stationary, environment_dependent_vs_agnostic, memory_driven_vs_direct_trigger, interaction_heavy_vs_solo]
- Axis definitions:
  - support_vs_damage: weighted support marker ratio versus damage marker ratio.
  - burst_vs_persistence: temporal burst density versus sustained persistence markers.
  - mobility_vs_stationary: mobility trigger/mechanic usage and branch mobility usage.
  - environment_dependent_vs_agnostic: environment-linked gate/trigger usage share.
  - memory_driven_vs_direct_trigger: memory influence ratio against direct trigger tendency.
  - interaction_heavy_vs_solo: interaction diversity + ally/chain/shared markers.

## Role-distance + gating formula
- roleDistance = weightedL1(roleA, roleB) normalized to [0,1].
- roleRepulsion = beta * roleDistance, bounded to [0,beta].
- gate condition: roleDistance >= roleSplitThreshold => incompatible in hard_reject mode.
- finalNicheDistance = traitBehaviorDistance + roleRepulsion - coEvolutionBias (plus hard penalty only in hard_penalty mode).

## Weights, thresholds, and mode
- beta: 0.15
- axis weights: {support_vs_damage=1.0, burst_vs_persistence=0.9, mobility_vs_stationary=0.85, environment_dependent_vs_agnostic=0.8, memory_driven_vs_direct_trigger=0.95, interaction_heavy_vs_solo=0.85}
- average repulsion applied: 0.02091935539961411
- roleSplitThreshold: 0.4
- minimumRoleSeparationMode: hard_reject
- hardGateRejections: 0

## Examples of strategies previously merged now forced apart
- High-burst/low-persistence versus high-persistence/low-burst behaviors now receive role-distance separation.
- High-memory/high-environment strategies versus direct-trigger environment-agnostic strategies no longer cluster by family alone.
- Interaction-heavy support signatures versus solo damage signatures now repel when trait distance alone was ambiguous.

## Risk analysis
- Bounded beta keeps role repulsion subordinate to trait/behavior distance.
- Existing margin, hysteresis, candidate promotion, merge, and prune controls remain unchanged.
- Fragmentation remains monitored via niche stability and fragmentation warnings in diagnostics.
