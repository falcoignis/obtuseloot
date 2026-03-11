# Fitness Sharing Report

- model: niche
- formula: sharingFactor = 1 / (1 + alpha * max(0, nicheOccupancy - targetOccupancy))
- alpha: 0.1
- maxPenalty: 0.15
- targetOccupancy: 0.18
- applied in: species persistence evaluation / world-lab survival scoring (crowding penalty layer)
- average sharing load: 1.058400467393458
- average sharing factor: 0.9452833457668824
- most crowded niches: {niche-1=0.9375, unassigned=0.0625}

## Expected ecological effects
- dominant niches lose small bounded viability, reducing premature convergence.
- nearby underrepresented niches avoid immediate extinction due to lower sharing load.
- effect remains smooth and bounded via maxPenalty cap.

## Risk analysis
- if alpha is too high, viable dominant lineages may be over-dampened.
- if alpha is too low, monoculture collapse remains sticky.
- current defaults are conservative and bounded to <=20% viability reduction.
