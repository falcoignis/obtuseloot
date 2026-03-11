# Fitness Sharing Report

- model: niche-based fitness sharing (`mode=niche`; distance mode scaffolded for future extension)
- formula: `sharingFactor = 1 / (1 + alpha * max(0, nicheOccupancy - targetOccupancy))`
- adjusted viability: `adjustedFitness = baseFitness * sharingFactor`
- parameters: `alpha=0.10`, `targetOccupancy=0.18`, `maxPenalty=0.15`
- integration points:
  - species persistence / viability scoring in world-lab ecology loop
  - niche crowding adjustment before co-evolution and ecological memory modifiers
- bounds:
  - sharing factor is clamped to `[1-maxPenalty, 1.0]`
  - maximum viability reduction from sharing alone is 15%

## Distribution summary
- average sharing load: see `analytics/fitness-sharing-distribution.json`
- most crowded niches: see `occupancyByNiche`
- sharing load by niche: see `nicheSharingLoad`

## Expected ecological impact
- reduce dominant niche lock-in pressure
- keep underrepresented niches viable long enough for persistence testing
- improve END/NSER and increase chance of durable PNNC gains

## Risk analysis
- low alpha may under-correct monoculture collapse
- high alpha may over-dampen genuinely adaptive dominant niches
- conservative defaults selected to avoid hard rubber-band behavior
