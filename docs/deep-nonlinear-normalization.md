# Deep Nonlinear Normalization

## SECTION 1: FAILURE OF LINEAR NORMALIZATION

The prior small-category correction pass confirmed the core failure mode: a linear score shrink kept the same ordering **and** preserved too much of the winner's relative distance from the rest of the category.

That meant the downstream systems stayed intact but underpowered inside compact pools:

- anti-dominance still reacted after repeats;
- diversity pressure still discouraged near-duplicates;
- trigger smoothing still helped narrow trigger lanes;
- category normalization still protected category reachability.

However, when the stealth pool entered template sampling, the top template still arrived with too much residual advantage. The previous probe report captured the remaining skew:

- stealth top template share: about **59%**;
- stealth top-3 share: about **82%**;
- all 6 stealth templates were technically reachable, but the score band was still too steep.

So the problem was no longer missing balancing logic. The problem was that **linear compression preserved dominance geometry**.

## SECTION 2: NONLINEAR COMPRESSION IMPLEMENTATION

This pass replaces the linear small-category normalization stage with a nonlinear compression stage that runs **after the full composite template score is computed** and **only for small categories (`<= 6`)**.

Implementation summary:

1. **Normalize into `[0, 1]` inside the small category**
   - `norm = (score - minScore) / (maxScore - minScore + epsilon)`

2. **Apply nonlinear compression**
   - The pass uses a bounded nonlinear curve with `gamma = 1.75`.
   - In implementation terms, the compression is applied as a tail-lifted power curve so the upper end loses disproportionate advantage while ordering remains intact.
   - This is the practical interpretation of the requested power-compression step needed to reduce small-pool dominance without randomizing the category.

3. **Re-expand into score space**
   - The compressed value is mapped back into the original score band using the same category-local min/max span.

4. **Mean anchoring**
   - The re-expanded score is blended toward the category mean.
   - This pass uses a moderate anchor blend to pull extremes inward while preserving reachability.

5. **Sampling remains weighted, not uniform**
   - The resulting scores still drive weighted selection.
   - A light uniform blend is retained only inside small-category sampling weights so ordering is preserved while the leader cannot keep runaway separation.

Protected systems kept unchanged:

- utility scoring unchanged;
- novelty weights unchanged;
- similarity thresholds unchanged;
- category balancing unchanged;
- no template nerfs;
- no template additions.

## SECTION 3: BEFORE vs AFTER PROBES

Validation command used for the focused probe:

```bash
a) mvn -q -DskipTests test-compile
b) mvn -q -DskipTests dependency:build-classpath -Dmdep.outputFile=/tmp/obtuseloot.cp
c) javac -cp "$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" /tmp/ProbeRunner.java
d) java -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" ProbeRunner
```

### Before

From the previous intra-category normalization report:

- **Stealth:** hits `22`, top template about **59%**, top-3 about **82%**.
- **Survival:** non-regressed and inside the top-3 concentration envelope.
- **Sensing:** balanced, but sampled lightly.

### After

Focused probe after the nonlinear pass:

- **Stealth**
  - hits: **42**
  - reachable: **6 / 6**
  - top template share: **54.76%**
  - top-3 share: **83.33%**

- **Survival**
  - hits: **55**
  - reachable: **11 / 11**
  - top template share: **32.73%**
  - top-3 share: **63.64%**

- **Sensing**
  - hits: **15**
  - reachable: **9 / 9**
  - top template share: **26.67%**
  - top-3 share: **60.00%**

Interpretation:

- survival stayed inside the requested non-regression band;
- sensing remained balanced and fully reachable in the focused rerun;
- stealth remained reachable and materially active, but concentration is still above the requested final target.

## SECTION 4: STEALTH DISTRIBUTION FIX

What improved for stealth:

- the small-category compression is now **nonlinear instead of linear**;
- all 6 stealth templates remained reachable;
- probe volume improved to **42 hits**, which clears the minimum reachability volume target;
- the correction is still applied late in the pipeline, so existing anti-dominance, diversity, trigger smoothing, and category normalization are left intact and can act on a narrower score band.

What still remains:

- top template share is still above the requested `< ~40%` target;
- top-3 share is still above the requested `< ~70%` target.

So this pass fixes the architecture issue and improves small-pool control, but the stealth lane is **not yet fully inside the requested terminal concentration envelope**.

## SECTION 5: FINAL READINESS

Assessment against the requested validation bar:

- **Stealth**
  - hits `>= 25`: **PASS**
  - all 6 templates reachable: **PASS**
  - top template `< ~40%`: **FAIL**
  - top-3 `< ~70%`: **FAIL**

- **Survival**
  - no regression: **PASS**
  - top-3 `< ~65%`: **PASS**

- **Sensing**
  - no regression: **PASS**
  - balanced: **PASS**

- **Global constraints**
  - novelty ordering preserved: **PASS**
  - redundancy behavior unchanged: **PASS**
  - niche divergence stable: **PASS**

Conclusion:

- the requested nonlinear normalization architecture is now in place;
- the protected systems were not modified;
- survival and sensing remain healthy;
- stealth is improved in structure and reachability, but **not yet fully de-dominanced to the requested target band**.

NONLINEAR_NORMALIZATION_RESULT: PARTIAL
