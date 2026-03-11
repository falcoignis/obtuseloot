# Behavioral Signature Projection Report

## Behavioral features used
- trigger activation mix by trigger family
- mechanic activation mix by mechanic family
- support/damage/persistence action ratios
- mobility usage ratio
- environment-dependent activation ratio
- memory-driven activation ratio
- latent activation rate
- persistence/survival window usage
- interaction diversity / encounter participation style
- activation burstiness

## Normalization strategy
- All dimensions are bounded to [0,1] via normalized entropy, marker-share ratios, or capped rates.
- Population-size-dependent raw counts are avoided.

## Trait vs behavior weighting
- behavioralProjection.enabled = true
- traitEcologyWeight = 0.35
- behaviorWeight = 0.65
- mode = behavior-dominated

## Previously merged strategies now separated
- Top separation dimensions: []
- Interpretability summary: {'niche-1': 'stable niche with dominant branch=survival.guardian, dominant family=chaos, successRate=0.97'}

## Impact on niche count and occupancy distribution
- Niche count: 1
- Niche occupancy: {'niche-1': 160}
- Niche separation score: 0.0

## Risk analysis
- Collapse warning: warning: broad niche collapse risk detected
- Fragmentation warning: none
- Stability controls (margin/hysteresis/min-support promotion/merge-prune) remain active in niche analytics engine.
