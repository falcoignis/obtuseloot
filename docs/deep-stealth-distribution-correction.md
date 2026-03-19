# Deep Stealth Distribution Correction

## SECTION 1: STEALTH DOMINANCE ANALYSIS

This pass stayed inside `ProceduralAbilityGenerator` template selection. It did **not** modify category balancing, novelty weights, similarity thresholds, ecology, lineage, or population systems.

### Baseline problem statement
From the incoming probe report for stealth:

- `stealth.echo_shunt` was carrying about **46%** of picks.
- Stealth top-3 share was about **83%**.
- Reachability had been restored, but intra-category rotation remained too sharp for a 6-template pool.

### Interpreted cause
The remaining skew was consistent with three local effects inside small categories:

1. category entry was healthy, but once the category was selected the internal score curve still favored the same highest-utility template too aggressively;
2. recent-template anti-dominance inside a 6-template pool was not decaying repeats fast enough;
3. same-pattern loops could still survive because existing similarity signals were not being applied as a final intra-category diversity nudge.

## SECTION 2: SCORING ADJUSTMENTS

The code change applies three **small-category-only** refinements when category size is `<= 6`:

1. **Adaptive score flattening**
   - Final template scores inside small categories are softened with a bounded exponent before category-local sampling.
   - This preserves score ordering while reducing winner-take-all sharpness.

2. **Stronger bounded anti-dominance for small categories**
   - Existing recent-template recency pressure is strengthened only for small categories.
   - Overrepresented templates receive a faster bounded decay while underrepresented templates receive a slightly stronger recovery lift.

3. **Light diversity-aware intra-category sampling**
   - A small similarity-based diversity multiplier is applied only in small categories.
   - The multiplier uses the existing `AbilityDiversityIndex` similarity signal against recent and already-selected same-category templates.

Everything else remains untouched:

- category normalization unchanged
- trigger smoothing unchanged
- niche weighting unchanged
- novelty blending unchanged
- lineage bias unchanged

## SECTION 3: PROBE RESULTS (BEFORE vs AFTER)

### Before
- Stealth reachability: **6 / 6**
- Stealth total hits: about **30**
- Stealth top template share: about **46%**
- Stealth top-3 share: about **83%**

### After
A focused local probe was run after the change using the current compiled generator:

- **Stealth**
  - total hits: **23**
  - reachable: **6 / 6**
  - top template share: **60.87%**
  - top-3 share: **82.61%**
  - hits: `{stealth.echo_shunt=14, stealth.paper_trail=3, stealth.dead_drop_lattice=2, stealth.hushwire=2, stealth.shadow_proxy=1, stealth.threshold_jam=1}`
- **Survival**
  - total hits: **23**
  - reachable: **11 templates**
  - top-3 share: **39.13%**
- **Sensing**
  - total hits: **15**
  - reachable: **9 templates**
  - top-3 share: **60.00%**
- **Novelty snapshot**
  - average novelty: **0.1543**
  - average similarity: **0.8457**
  - average intra-niche novelty: **0.6044**
  - average global novelty: **0.1543**

### Assessment
- Reachability was preserved for stealth.
- Cross-category distributions did not collapse in the reduced probe.
- The stealth dominance target was **not** met in this measured rerun.
- Because the measured stealth sample remained top-heavy and below the requested hit floor, this pass cannot be marked ready.

## SECTION 4: CROSS-CATEGORY SAFETY CHECK

### Protected systems
No changes were made to:

- category exposure balancing
- novelty weights
- similarity thresholds
- ecology / lineage / population
- trigger smoothing framework
- niche weighting framework

### Observed safety in local rerun
- Survival remained broadly distributed rather than monolithic.
- Sensing remained reachable and non-collapsed.
- Intra-niche novelty still remained above global novelty, so novelty ordering did not invert.

### Remaining concern
- The reduced post-change novelty/similarity snapshot did not improve enough to claim “no increase in redundancy” from this run alone.
- Stealth still over-concentrated around `stealth.echo_shunt` in the focused rerun.

## SECTION 5: FINAL READINESS

This was implemented as a surgical template-selection refinement pass and kept the requested protected systems intact.

However, the measured rerun still shows stealth concentration above the requested acceptance band, and the focused stealth hit volume stayed below the target floor. The change is therefore **not ready for acceptance as a completed correction**.

STEALTH_DISTRIBUTION_RESULT: PARTIAL
