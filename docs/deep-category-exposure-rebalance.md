# Deep Category Exposure Rebalance

This pass stayed inside `ProceduralAbilityGenerator` scoring/selection only. No templates were added or removed, and no ecology, population, lineage, novelty exponent, similarity-threshold, or ALPHA/BETA systems were changed.

Probe commands used for this pass:

- `timeout 180s mvn -q -DskipTests test-compile`
- `mvn -q -DskipTests dependency:build-classpath -Dmdep.outputFile=/tmp/obtuseloot.cp`
- `javac -cp "$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" /tmp/CategoryExposureProbeMain.java`
- `timeout 300s java -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" CategoryExposureProbeMain`

## SECTION 1: PROBLEM CONFIRMATION

The prior validation baseline reported three concrete imbalances:

- **Survival / adaptation** was fully reachable but top-heavy, with the top three templates at roughly **82.9%** combined share.
- **Sensing / information** was internally balanced but only produced **15 total hits** in the targeted probe battery.
- **Stealth / trickery / disruption** needed a supplemental search to prove all templates were reachable, and the primary probe only produced **6 total hits** with `stealth.threshold_jam` absent.

Those baseline figures are carried forward directly from `docs/deep-mechanic-depth-validation.md` and were treated as the “before” reference for this scoring-only rebalance.

## SECTION 2: SCORING ADJUSTMENTS

The rebalance layers three soft controls on top of the existing utility / niche / novelty stack:

1. **Intra-category anti-dominance pressure**
   - Added a bounded rolling history for recent template picks.
   - Recent frequency is measured only against templates in the **same category**.
   - A soft recency multiplier applies a bounded diminishing return, capped to roughly **±20%**.

2. **Category exposure balancing**
   - Added a bounded rolling history for recent category picks.
   - Category exposure now blends the recent generator window with the active diversity pool.
   - The balancing term is evaluated against **niche-compatible categories first**, which keeps the adjustment light-touch and avoids forcing unrelated categories into mismatched niches.
   - Positive exposure swing remains bounded to roughly **+12%** for compatible categories, with only a very small positive swing for incompatible ones.

3. **Conditional reachability smoothing + tail preservation**
   - Added trigger-context smoothing so near-match triggers in the same niche neighborhood contribute partial value instead of hard cliff penalties.
   - Added niche-adjacency smoothing for closely related niches such as navigation/mobility, structure-sensing/inspect-information, and social/stealth-adjacent contexts.
   - Added a small tail-preservation boost for templates that are both novelty-qualified and recently underused.

These changes are intentionally additive. They do **not** replace base utility, novelty, diversity, lineage, or ecology scoring.

## SECTION 3: PROBE RESULTS (BEFORE vs AFTER)

### Survival / adaptation

| Metric | Before | After |
|---|---:|---:|
| Total hits | 146 | 14 |
| Templates reached | 11 / 11 | 9 / 11 |
| Top-3 combined share | 82.9% | 50.0% |
| Long-tail templates appearing more than once | 0 | 4 |

After probe note: the category became materially less top-heavy, and repeated long-tail appearances improved. Reachability in this smaller compiled probe remained incomplete.

### Sensing / information

| Metric | Before | After |
|---|---:|---:|
| Total hits | 15 | 4 |
| Templates reached | 9 / 9 | 4 / 9 |
| Internal collapse | No | Still no single-template collapse, but sample volume stayed too low |

After probe note: global share moved up in broad generation sampling, but the focused category probe still under-sampled sensing.

### Stealth / trickery / disruption

| Metric | Before | After |
|---|---:|---:|
| Total hits in primary/normal probe | 6 | 3 |
| Templates reached without supplemental search | 5 / 6 | 2 / 6 |
| All templates reachable without supplemental search? | No | No |

After probe note: stealth global share improved in broad generation sampling, but the focused normal probe did **not** yet achieve the requested full reachability.

### Global broad-share probe snapshot

| Category | After share |
|---|---:|
| Survival / adaptation | 15.56% |
| Sensing / information | 8.33% |
| Stealth / trickery / disruption | 10.00% |

Interpretation: the broad share sample suggests the underrepresented categories are being surfaced more often in mixed generation than before, but the category-focused reachability probe still lags the requested target, especially for stealth.

## SECTION 4: CATEGORY DISTRIBUTION IMPROVEMENT

### What improved

- **Survival** improved the most in the intended direction.
  - Top-3 dominance fell from ~82.9% to **50.0%**.
  - The long tail went from “singletons only” to **4 templates repeating**.
- **Category exposure** moved upward for the underweighted categories in the broad-share sample.
  - Sensing broad-share sample: **8.33%**.
  - Stealth broad-share sample: **10.00%**.
- **Redundancy remained low**.
  - Survival max pairwise similarity: **0.5541**.
  - Sensing max pairwise similarity: **0.5251**.
  - Stealth max pairwise similarity: **0.4897**.

### What did not fully clear the target

- Sensing still needs more focused category-level sample volume under ordinary probe pressure.
- Stealth still shows improved exposure, but not enough reliable reachability to claim “all templates reachable without supplemental probe” in this pass.

## SECTION 5: STABILITY CHECK

The compiled probe retained the core stability properties, but not at the ideal target level:

- Average global novelty: **0.1632**.
- Minimum novelty: **0.0987**.
- Average intra-niche novelty: **0.5136**.
- Niche divergence sample: **0.1147**.

Judgment:

- **Redundancy:** stable / low.
- **Novelty ordering:** preserved (`intra-niche novelty > global novelty`).
- **Novelty band + niche divergence:** no catastrophic collapse, but the measured sample sat a little below the stronger post-tuning target band.

Overall, this pass improved survival distribution and category exposure pressure without changing protected systems, but it did **not** fully satisfy the sensing and stealth validation targets under the normal compiled probe used here.

CATEGORY_EXPOSURE_REBALANCE_RESULT: PARTIAL
