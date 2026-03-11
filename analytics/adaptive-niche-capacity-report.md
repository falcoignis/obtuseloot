# Adaptive Niche Capacity Report

- Enabled: true
- Bounds: min=0.80, max=1.25
- Baseline capacity: 1.00
- Max seasonal delta: 0.05 (bounded, deterministic, conservative)

## Update rule

`capacityDelta = +noveltyWeight*novelty + diversityWeight*interactionDiversity + persistenceWeight*nichePersistence - overcrowdingWeight*chronicOvercrowding - stagnationWeight*prolongedDominanceWithoutNovelty`

Then:

`nicheCapacity = clamp(nicheCapacity + capacityDelta, minCapacity, maxCapacity)`

## Positive contributors

- Durable novelty signal
- Species/interaction diversity in the niche
- Multi-season persistence

## Negative contributors

- Chronic overcrowding above target occupancy
- Prolonged dominance without novelty renewal

## Most expanded niches

- Pending next world-lab simulation output (`seasonAdjustments` with positive deltas).

## Most constrained niches

- Pending next world-lab simulation output (`seasonAdjustments` with negative deltas).

## Expected ecosystem impact

- Durable niches can earn modest carrying-capacity headroom.
- Overcrowded/stagnant niches lose headroom gradually.
- Fitness sharing remains the primary crowding mechanism; capacity is a bounded modifier.

## Risk analysis

- Overweight novelty can amplify short-lived noise.
- Underweight stagnation may fail to reduce lock-in.
- Conservative bounds and maxSeasonDelta limit instability.
