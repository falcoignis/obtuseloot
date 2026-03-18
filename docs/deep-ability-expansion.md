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

## NOVELTY TUNING PASS
- Selection pressure in `ProceduralAbilityGenerator` was retuned so novelty contributes as a first-order multiplier instead of behaving like a weak tie-breaker.
- A soft novelty floor penalty now scales down near-duplicate candidates below `novelty < 0.15` without hard rejection.
- Motif anchoring was reduced to keep family resemblance while widening the allowable deviation radius.
- ALPHA variant handling now carries a wider mutation bias range, lower reinforcement anchoring, and stronger exploratory scaling; BETA remains comparatively stable.
- Active-pool pressure was reshaped so moderate similarity is tolerated more than before, while highly similar candidates are penalized more sharply.
- Similarity blending in `AbilityDiversityIndex` now emphasizes effect/signature/stat overlap more than broad family/trigger coincidence, improving duplicate sensitivity.

### Validation rerun
Probe rerun after the tuning pass reported:
- average novelty score vs active pool: `0.2103`
- minimum novelty score: `0.1267`
- maximum novelty score: `0.7481`
- average nearest-neighbor similarity: `0.7897`
- niche divergence (family-distribution Jensen-Shannon):
  - explorer vs ritualist: `0.0580`
  - explorer vs warden: `0.1150`
  - ritualist vs warden: `0.0082`
- lineage divergence (same-niche ritual probe): `0.3887`

### Interpretation
- Novelty pressure increased materially from the prior `0.1266` average novelty baseline and now sits inside the requested `0.20–0.35` band.
- Similarity improved from the prior `0.8734` baseline to `0.7897`, but it did not yet clear the `< 0.75` target.
- Lineage divergence remained healthy and slightly improved over the earlier `0.3534` ritual-lineage probe.
- Niche identity did not collapse, but the rerun still shows uneven niche separation: explorer vs warden remains distinct, while ritualist-vs-warden separation is still too weak in this pass.
- Stability remained bounded because the change only retuned scoring weights/scales; ecology and population systems were left untouched.

ABILITY_EXPANSION_TUNING_RESULT: PARTIAL

## NOVELTY GATING FIX
- Novelty scoring in `ProceduralAbilityGenerator` now splits similarity into same-niche and cross-niche layers before scoring candidates.
- Same-niche similarity drives the strong novelty term, while cross-niche similarity contributes only a weaker drift term so patterns can recur across niches without flattening niche identity.
- Selection ordering was reweighted so niche-weighted scoring is applied ahead of novelty amplification, and the niche multiplier was strengthened to keep novelty operating inside the niche space rather than redefining it.
- Added a bounded niche-consistency penalty that softly downweights candidates whose mechanic families, trigger patterns, or affinity mix drift too far from the current niche profile.
- When utility history is still sparse, scoring now infers a provisional niche context from the artifact memory profile so early-generation novelty pressure no longer defaults all candidates into the same generalist comparison pool.
- ALPHA/BETA handling remains differentiated: ALPHA keeps stronger intra-niche novelty pressure with moderate cross-niche drift, while BETA keeps moderate intra-niche novelty and lower cross-niche drift.

### Validation rerun
Latest targeted probe rerun reported:
- average novelty score vs active pool: `0.1759`
- minimum novelty score: `0.1055`
- maximum novelty score: `0.7714`
- average nearest-neighbor similarity: `0.8241`
- average intra-niche novelty: `0.5545`
- average global novelty: `0.1759`
- niche divergence (family-distribution Jensen-Shannon):
  - explorer vs ritualist: `0.1488`
  - explorer vs warden: `0.1771`
  - ritualist vs warden: `0.1408`
- lineage divergence (same-niche ritual probe): `0.4071`

### Interpretation
- The gating change restored materially stronger niche separation than the globally applied novelty pass, but it did not fully recover the original `~0.20+` niche-divergence target in this run.
- Intra-niche novelty is now clearly higher than global novelty, which confirms novelty pressure is being concentrated inside niche boundaries instead of pushing all niches away from one another equally.
- Lineage divergence remained healthy and improved relative to the earlier novelty-tuning rerun.
- Global novelty stayed elevated relative to the original pre-tuning baseline, but it landed below the desired `0.20–0.35` success band after niche identity was reinforced.
- Because niche divergence and lineage divergence improved while the final novelty band was only partially recovered, this pass is best classified as a bounded partial recovery rather than a full success.

ABILITY_EXPANSION_TUNING_RESULT: PARTIAL
