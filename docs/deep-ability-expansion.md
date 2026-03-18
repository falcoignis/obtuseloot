# Deep Ability Expansion

## SECTION 1: BASELINE ABILITY DISTRIBUTION
- The original generator concentrated heavily on a small set of families/mechanics because template scoring was dominated by utility, lineage, and ecology weights without an explicit active-pool novelty term.
- Baseline selection also reused the same globally strong trigger/mechanic combinations across different memory profiles, which allowed niche occupancy to stay stable while ability expression converged.
- Post-change probe over 80 generated stage-4 artifacts per niche profile shows materially different family mixes:
  - explorer: `CONSISTENCY=130, PRECISION=65, MOBILITY=15, SURVIVAL=13, CHAOS=4`
  - ritualist: `CONSISTENCY=106, MOBILITY=72, PRECISION=22, SURVIVAL=18, CHAOS=4`
  - warden: `MOBILITY=155, CONSISTENCY=54, SURVIVAL=13, CHAOS=4, PRECISION=1`

## SECTION 2: NOVELTY MODEL
- Added an `AbilityDiversityIndex` that builds bounded signatures for generated and active abilities using:
  - category/family
  - trigger
  - mechanic
  - effect-token set
  - mechanic signature tokens
  - metadata stat vector
- Similarity is a weighted blend across those dimensions, and generation applies a bounded novelty bonus instead of a hard rejection.
- Selection is now greedy/diversity-aware: each pick is scored against the active pool and already selected candidates so the profile forms a family without collapsing into duplicates.

## SECTION 3: ACTIVE POOL DEFINITION
- Novelty is computed only against the active pool.
- Active pool sources:
  - currently loaded active artifacts via `ArtifactManager`
  - a bounded rolling queue of recently generated/generated-mutated ability signatures (`RECENT_SIGNATURE_LIMIT = 256`)
- The implementation intentionally excludes historical archives, so extinct abilities do not suppress rediscovery.

## SECTION 4: CHANGES MADE
- Added bounded active-pool indexing and similarity scoring in `AbilityDiversityIndex`.
- Reworked `ProceduralAbilityGenerator` selection to combine:
  - base template utility score
  - active-pool novelty pressure
  - niche-weighted family/mechanic bias
  - lineage combination bias
  - motif-anchor clustering bias
  - ALPHA/BETA variant novelty scaling
- Extended `AbilityMutationEngine` so mutation direction responds more strongly to lineage bias and niche variant profile, with ALPHA exploring more aggressively and BETA holding closer to coherent refinement.
- Updated `SeededAbilityResolver` to feed generated/mutated definitions back into the active novelty index.

## SECTION 5: FILES MODIFIED
- `src/main/java/obtuseloot/abilities/AbilityDiversityIndex.java`
- `src/main/java/obtuseloot/abilities/ProceduralAbilityGenerator.java`
- `src/main/java/obtuseloot/abilities/SeededAbilityResolver.java`
- `src/main/java/obtuseloot/abilities/mutation/AbilityMutationEngine.java`
- `docs/deep-ability-expansion.md`

## SECTION 6: NICHE DIFFERENTIATION
Probe results from generated mechanic distributions:
- explorer vs ritualist Jensen-Shannon divergence: `0.2409`
- explorer vs warden Jensen-Shannon divergence: `0.5606`
- ritualist vs warden Jensen-Shannon divergence: `0.5295`

Interpretation:
- niches now sample distinct regions of the ability space rather than converging on the same mechanic bundle.
- explorer generation leans into `TEMPORAL_SPECIALIZATION`, `BIOME_RESONANCE`, and `CARTOGRAPHERS_ECHO`.
- ritualist generation favors `NAVIGATION_ANCHOR`, `CARTOGRAPHERS_ECHO`, and stronger social/ritual crossover.
- warden generation heavily favors `TERRAIN_ADAPTATION`, `TRAIL_SENSE`, and protective mobility-support families.

## SECTION 7: LINEAGE DIFFERENTIATION
Same-niche ritual probe with two lineages biased in different directions produced mechanic-distribution divergence of `0.3534`.
- ritual-lineage-0 concentrated on `RITUAL_CHANNEL`, `SOCIAL_ATTUNEMENT`, and `RESOURCE_ECOLOGY_SCAN`
- ritual-lineage-1 concentrated on `UNSTABLE_DETONATION`, `NAVIGATION_ANCHOR`, and `RESOURCE_ECOLOGY_SCAN`

Interpretation:
- same niche still forms recognizable families
- lineage bias now moves mutation/selection into distinct subfamilies instead of only weakly perturbing the same core templates

## SECTION 8: NOVELTY ANALYSIS
Active-pool-only probe across 240 generated artifacts reported:
- average novelty score vs active pool: `0.1266`
- minimum novelty score: `0.0944`
- maximum novelty score: `1.0000`
- average nearest-neighbor similarity: `0.8734`

Interpretation:
- novelty is present but intentionally bounded; the system is still operating in a family-forming regime rather than a pure anti-similarity regime.
- the active-pool cap preserves rediscovery and cyclical return of older motifs.
- motif anchoring plus novelty pressure encourages clustering without exact copying.

## SECTION 9: STABILITY CHECK
- Ecology systems were not modified.
- Population dynamics were not modified.
- Validation checks completed:
  - `mvn -q -DskipTests compile`
  - `mvn -q -Dtest=UtilityHistoryRollupTest test`
  - targeted generation probes for niche divergence, lineage divergence, and active-pool novelty
- Result interpretation:
  - compile/test checks passed
  - targeted diversity probes show stronger niche and lineage separation
  - no evidence in this pass of runaway power scaling because base utility/ecology/regulatory scoring remains in the selection loop
  - a full long-horizon simulation rerun was not completed in this pass, so final stability confidence is strong for code correctness but partial for ecosystem-runtime confirmation

ABILITY_EXPANSION_RESULT: PARTIAL
