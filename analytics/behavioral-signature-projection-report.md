# Behavioral Signature Projection Report

## Behavioral features used
- Dimensions: [trigger_class_activation_distribution, mechanic_usage_distribution, support_action_ratio, damage_action_ratio, persistence_action_ratio, mobility_usage_ratio, environment_dependent_activation_ratio, memory_driven_activation_ratio, latent_trait_activation_rate, activation_temporal_density, encounter_persistence_behavior, interaction_diversity]
- Top separation contributors: [trigger_class_activation_distribution, interaction_diversity, latent_trait_activation_rate]
- Strategy-level signatures combine trigger/mechanic mixes, action ratios, mobility, environment/memory dependency, latent activation, temporal density, encounter persistence behavior, and interaction diversity.

## Normalization strategy
- All dimensions are normalized to [0,1] using entropy, marker shares, bounded rates, or capped proportions.
- Raw population counts are avoided; ratios and bounded signals dominate the signature.

## Trait vs behavior weighting
- Enabled: true
- traitEcologyWeight: 0.25
- behaviorWeight: 0.75
- Projection mode: behavior-dominated

## Previously merged strategies now separated
- Separation dimensions with strongest variance now: [trigger_class_activation_distribution, interaction_diversity, latent_trait_activation_rate]
- Niche interpretability map: {niche-1=stable niche with dominant branch=precision.awakened, dominant family=chaos, successRate=0.97, niche-2=stable niche with dominant branch=chaos.sprawl, dominant family=chaos, successRate=0.66}

## Impact on niche count and occupancy
- Niche count: 1
- Occupancy: {niche-1=160}
- Dominant niche share: 1.0

## Risk analysis
- Fragmentation warning: none
- Collapse warning: warning: broad niche collapse risk detected
- Stability controls preserved: margin/hysteresis/candidate promotion/merge-prune remain active.
