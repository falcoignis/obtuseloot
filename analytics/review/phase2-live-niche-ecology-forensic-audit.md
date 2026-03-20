# Phase 2 Forensic Audit: Live Niche Ecology

## Summary of Integration Status
Phase 2 is **partially integrated** as live decision logic, not analytics-only. Niche classification, runtime saturation tracking, utility-aware pressure computation, and ecology-weighted template/evolution bias are implemented in production code paths. However, there is no explicit runtime branching mechanism that creates persistent subniche entities; specialization is represented as pressure + weighting rather than structural niche splitting.

## 1) Niche Taxonomy
- `MechanicNicheTag` defines the niche taxonomy (navigation, sensing, ritual, social, protection, adaptation, etc., plus `GENERALIST`).
- `NicheTaxonomy` maps real mechanics to niche tags and adds trigger-conditioned tags.
- `EcosystemRoleClassifier` computes niche scores from telemetry-derived utility mass (`validatedUtility`, `utilityDensity`, attempts) and emits `ArtifactNicheProfile` with dominant niche + specialization profile.
- Classification is dynamic because `NichePopulationTracker.recordTelemetry(...)` reclassifies from latest signals; niches can change as utility signals change.

## 2) Live Saturation Tracking
- `ArtifactUsageTracker.trackAbilityExecution(...)` records utility outcomes and immediately pushes telemetry to `NichePopulationTracker`.
- `NichePopulationTracker` keeps active artifact state, per-artifact signals, and per-artifact niche profiles.
- `rollups()` computes live niche population + attempts + meaningful outcomes + validated utility + budget from runtime telemetry, not static config.

## 3) Ecological Pressure on Evolution/Selection
- `EcosystemSaturationModel` computes:
  - saturation penalty
  - scarcity bonus
  - diversity incentive
  - ecological repulsion
  - specialization pressure
  - retention and mutation bias
- These pressures affect decisions via:
  - `ArtifactFitnessEvaluator.effectiveFitness(fitness, pressure)` (retention/specialization/repulsion adjustment)
  - `ExperienceEvolutionEngine.applyExperienceFeedback(...)` (trait multiplier includes ecological pressure)
  - `ExperienceEvolutionEngine.ecologyModifierFor(...)` (template selection modifier used by `ProceduralAbilityGenerator` scoring)

## 4) Utility-Aware Ecology
- Pressure is not based on population alone:
  - penalties require crowding plus below-mean utility density
  - scarcity bonuses require low share plus above-mean utility density
  - repulsion includes low outcome yield under crowding
- This implements utility-aware behavior: low-value low-share niches are not automatically protected, while useful low-share niches can gain positive pressure.

## 5) Specialization / Subniches
- `SubnicheClassifier` and `RoleDifferentiationHeuristics` produce subniche labels and specialization scores.
- `specializationPressure` and specialization tilt influence mutation/template weighting.
- Limitation: specialization is currently weighting/pressure-based; there is no explicit persistent subniche splitting model that creates separate tracked niche populations as first-class branches.

## 6) Analytics
- `NichePopulationTracker.analyticsSnapshot()` reports:
  - niche population
  - meaningful outcome yield
  - utility density
  - saturation pressure
  - scarcity bonus
  - specialization trends
- `TriggerSubscriptionIndexReporter` includes this live snapshot in reporting output.

## 7) Evolution Impact Verification
- Code path confirms ecological pressure affects real runtime scoring and evolution feedback.
- Expected outcomes are encoded and tested:
  - crowded weak niches receive worse net pressure
  - useful low-share niches gain retention support
  - crowded useful niches can get specialization pressure instead of only collapse pressure

## 8) Test Coverage
- `NicheEcologySystemTest` covers:
  - niche classification
  - runtime telemetry saturation tracking
  - utility-aware pressure
  - end-to-end crowded weak vs low-share useful + specialization signal behavior
- `ArtifactUtilityFitnessModelTest` validates utility-first scoring behavior used by ecology inputs.
- `ArtifactUsageTrackerUtilityTelemetryTest` validates telemetry rollups/high-volume low-value detection.
- `ExperienceEvolutionEngineTest` validates evolution feedback integration.

## Remaining Defects
1. No explicit persistent subniche branching object/model for population tracking; subniches are inferred labels, not durable ecological entities.
2. `mutationBias` is produced in `RolePressureMetrics` but mainline evolution paths primarily consume `templateWeightModifier`/retention terms; direct usage of `mutationBias` in core evolution is limited.
3. `NichePopulationTracker.rollups()` aggregates each artifact's full signal map into each niche it belongs to, which can inflate attempts/utility in multi-niche artifacts (possible overcounting coupling between niches).

## Final Verdict
**LIVE NICHE ECOLOGY: PARTIAL**

Reason: live runtime telemetry and utility-aware ecological pressure do influence real evolution/template decisions, but specialization is not yet implemented as explicit persistent subniche branching and some pressure outputs are not fully consumed as direct mutation operators.
