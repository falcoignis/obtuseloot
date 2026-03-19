# Deep Global Normalization Validation

## SECTION 1: IMPLEMENTATION SUMMARY

This pass moved the late-stage intra-category normalization from a small-category-only rule into a global post-score sampling layer inside `ProceduralAbilityGenerator`.

Applied changes:

1. **Global post-score nonlinear normalization**
   - After full composite template scoring, every multi-template category now normalizes scores into `[0, 1]`.
   - The normalized values are compressed with a nonlinear gamma curve (`gamma = 1.75`) and then re-expanded back into score space.
   - Ordering is preserved because the transform is monotonic.

2. **Global mean anchoring**
   - The re-expanded scores are blended mildly toward the category mean.
   - This reduces outlier separation without flattening the category.

3. **Relative soft top-cap**
   - A smooth `tanh`-based compression is applied above the category mean.
   - This creates a soft asymptotic cap near `1.5x` the category mean while avoiding a hard clamp or discontinuity.

4. **Weighted selection extended to all multi-template categories**
   - Once a category is chosen, template selection now stays weighted across every category with more than one template.
   - Single-template fallbacks remain unchanged.

Protected systems left intact:

- novelty weights unchanged
- similarity thresholds unchanged
- category balancing unchanged
- cold boost unchanged
- cold completion unchanged
- diversity logic unchanged
- anti-dominance unchanged

## SECTION 2: VALIDATION COMMANDS

Commands used in this pass:

```bash
timeout 180s mvn -q -DskipTests test-compile
javac -cp "$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" /tmp/GlobalNormalizationProbeRunner.java
java -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" GlobalNormalizationProbeRunner
```

Notes:

- `mvn -q -Dtest=ColdTemplateBootstrappingProbeTest,AbilityNoveltyTuningProbeTest test` was also attempted, but it exceeded the 240s timeout in this environment.
- The custom probe runner was used to complete a full-category distribution readout with the new normalization logic.

## SECTION 3: FULL PROBE RESULTS

### Category distribution snapshot

| Category | Hits | Reached | Top template % | Top-3 % | Templates with >1 hit | Result |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Traversal / mobility | 27 | 8 / 8 | 33.3% | 74.1% | 4 | Mixed |
| Sensing / information | 21 | 8 / 9 | 28.6% | 61.9% | 5 | Reachability miss |
| Survival / adaptation | 49 | 11 / 11 | 32.7% | 63.3% | 9 | Pass |
| Combat / tactical control | 17 | 6 / 6 | 29.4% | 64.7% | 6 | Pass |
| Defense / warding | 21 | 7 / 8 | 23.8% | 66.7% | 5 | Reachability miss |
| Resource / farming / logistics | 20 | 8 / 8 | 25.0% | 60.0% | 5 | Pass |
| Crafting / engineering / automation | 16 | 5 / 5 | 43.8% | 87.5% | 3 | Long-tail weak |
| Social / support / coordination | 19 | 8 / 8 | 26.3% | 57.9% | 6 | Pass |
| Ritual / strange utility | 45 | 11 / 13 | 15.6% | 42.2% | 10 | Reachability miss |
| Stealth / trickery / disruption | 25 | 8 / 9 | 28.0% | 64.0% | 7 | Reachability miss |

### Raw probe output

- `TRAVERSAL_MOBILITY hits=27 reached=8/8 top=0.333 top3=0.741 multi=4`
- `SENSING_INFORMATION hits=21 reached=8/9 top=0.286 top3=0.619 multi=5`
- `SURVIVAL_ADAPTATION hits=49 reached=11/11 top=0.327 top3=0.633 multi=9`
- `COMBAT_TACTICAL_CONTROL hits=17 reached=6/6 top=0.294 top3=0.647 multi=6`
- `DEFENSE_WARDING hits=21 reached=7/8 top=0.238 top3=0.667 multi=5`
- `RESOURCE_FARMING_LOGISTICS hits=20 reached=8/8 top=0.250 top3=0.600 multi=5`
- `CRAFTING_ENGINEERING_AUTOMATION hits=16 reached=5/5 top=0.438 top3=0.875 multi=3`
- `SOCIAL_SUPPORT_COORDINATION hits=19 reached=8/8 top=0.263 top3=0.579 multi=6`
- `RITUAL_STRANGE_UTILITY hits=45 reached=11/13 top=0.156 top3=0.422 multi=10`
- `STEALTH_TRICKERY_DISRUPTION hits=25 reached=8/9 top=0.280 top3=0.640 multi=7`

## SECTION 4: VALIDATION AGAINST REQUESTED TARGETS

### 1) Reachability

**Target:** all templates reachable, no zeros.

**Observed:** improved substantially, but not complete in the full probe.

Categories still missing at least one template in the probe sample:

- Sensing / information: 8 / 9
- Defense / warding: 7 / 8
- Ritual / strange utility: 11 / 13
- Stealth / trickery / disruption: 8 / 9

**Result:** FAIL

### 2) Distribution

**Target:** no template above 50%, top-3 below 70% in most categories.

**Observed:**

- No category exceeded 50% top-share.
- Most categories stayed below the 70% top-3 threshold.
- Exceptions remained in:
  - Traversal / mobility: 74.1%
  - Crafting / engineering / automation: 87.5%

**Result:** PARTIAL PASS

### 3) Long-tail participation

**Target:** majority of templates appear multiple times.

**Observed:**

- Strong improvement in Survival, Combat, Resource, Social, and Ritual.
- Crafting remained thin: only 3 of 5 templates had more than one hit.
- Traversal was also still shallow relative to category size.

**Result:** PARTIAL PASS

### 4) No flattening

**Target:** best templates still lead, but distribution is not uniform.

**Observed:**

- Every category retained a clear leader.
- No category collapsed into uniform sampling.
- Top-share values remained meaningfully above flat-share baselines.

**Result:** PASS

### 5) Stability

**Target:** preserve novelty ordering and niche divergence.

**Observed:**

- The implementation does not alter novelty weights, similarity thresholds, or category balancing.
- The new layer is strictly post-score and pre-sampling.
- The targeted novelty JUnit suite was attempted but timed out in this environment, so this pass relies on architectural inspection plus non-regression of weighted, non-uniform category outputs rather than a completed novelty test run.

**Result:** PARTIAL / UNCONFIRMED

## SECTION 5: CONCLUSION

What this pass clearly achieved:

- removed extreme single-template collapse across the sampled categories
- brought every measured top-template share under 50%
- kept weighted selection intact
- preserved ordering and avoided uniform flattening
- improved long-tail participation materially versus prior reports

What it did **not** fully achieve yet:

- full all-category reachability in probe sampling
- consistently strong multi-hit participation in every large category
- full top-3 control in traversal and crafting
- completed novelty/stability confirmation via the timed-out JUnit probe

Overall assessment:

The global normalization layer is architecturally in the correct place and materially improves intra-category dominance across the full category set, but the validation bar in the request is **not fully met yet**.

GLOBAL_NORMALIZATION_RESULT: FAILED
