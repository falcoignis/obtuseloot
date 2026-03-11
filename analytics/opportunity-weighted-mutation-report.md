# Opportunity-Weighted Mutation Pressure Report

## Opportunity signals used
- occupancy scarcity (1 - occupancy share)
- persistence scarcity (1 - niche persistence)
- novelty scarcity (1 - novelty success + NSER blend)
- capacity scarcity (1 - adaptive capacity utilization)
- interaction scarcity (1 - interaction diversity support)

## Opportunity-score formula
- `score = occScarcity*wOcc + persistScarcity*wPersist + noveltyScarcity*wNovelty + capacityScarcity*wCapacity + interactionScarcity*wInteraction`
- Weights: occupancy=0.03, persistence=0.02, novelty=0.02, capacity=0.02, interaction=0.01
- Score bound: `[0, 0.10]`

## Mutation / activation bias rules
- mutation pressure multiplier: `1 + (normalizedScore * maxBias)`
- latent activation multiplier: `1 + (normalizedScore * maxBias * 0.6)`
- maxBias cap: `0.10` (10% max layer pressure)

## Conservative bounds
- no hard role forcing or dominant-role lockout
- deterministic, simulation-compatible inputs
- soft multiplicative tilt only (bounded 5–15% envelope, default 10%)

## Underfilled-role pressure examples
- highest opportunity roles from distribution: niche-2, niche-3
- example mutation bias range: 1.06–1.064

## Risk analysis
- overreaction risk is constrained by maxBias cap
- false novelty spikes damped by weighted blend and bounded score
- complements fitness sharing / memory / co-evolution without duplicating their logic
