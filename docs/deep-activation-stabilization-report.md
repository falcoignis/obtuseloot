# Deep Activation Stabilization Report

Run command:

- `bash scripts/run-deep-validation.sh --run-id deep-activation-stabilization`

Dataset root:

- `analytics/validation-suite-rerun/deep-activation-stabilization`

## SECTION 1: ACTIVATION CONDITIONS

### Bifurcation triggering (`NicheBifurcationRegistry`)

A parent niche bifurcates only when all of these are true:

1. Dynamic niche headroom exists: global cap and per-parent cap must allow creation of two children.
2. Cooldown gates pass: per-parent and global cooldown windows must both be elapsed.
3. Pressure and eligibility gates pass in the same evaluation:
   - `saturationPressure >= 0.05`
   - `specializationPressure >= 0.00`
   - `activeArtifacts >= 2`
   - `nichePopulationShare >= 0.05`
4. Sustained high-pressure windows are met:
   - Default requirement is 2 windows.
   - Adaptive fast-path reduces requirement to 1 window when `nichePopulationShare >= 0.10`.

### Partition activation (`NichePopulationTracker`)

Child assignment pressure is applied after a bifurcation and is condition-gated:

1. Parent must have at least two active children.
2. Structural partition activation requires parent saturation pressure to meet:
   - `saturationPressure >= SATURATION_THRESHOLD * 0.90` (effectively `>= 0.045`)
3. Partition share bounds remain unchanged (10%–20%, target 15%).
4. Soft migration pressure requires:
   - total active artifacts >= 10
   - parent share >= 0.17
   - artifact utility below parent mean

## SECTION 2: FAILURE ANALYSIS (inactive scenarios)

### Historical baseline diagnosis (pre-adjustment target)

The prior bifurcation diagnostic identified no-child behavior in `explorer-heavy` and `mixed` (0 child share, no bifurcations), with sustained-pressure gating and pressure eligibility called out as blockers.

### Current run inactive scenario(s)

From `deep-activation-stabilization`, `explorer-heavy` remained inactive:

- child share peak/final: `0.00% / 0.00%`
- active child niches: `0`
- bifurcation count at final window: `0`

Observed implication:

- At least one activation prerequisite still failed in practice for `explorer-heavy` (most likely sustained accumulation against live-parent share dynamics), even though static rollup shares can appear sufficient.

## SECTION 3: ADJUSTMENTS MADE

Activation-only adjustments (no partition % increase, no redesign):

1. Lowered bifurcation saturation trigger from `0.06 -> 0.05`.
2. Lowered parent-share accumulation floor from `0.06 -> 0.05`.
3. Added adaptive sustained-window fast-path:
   - if parent share >= 0.10, require one fewer sustained window (2 -> 1 by default).
4. Slightly relaxed structural partition activation gate:
   - from `>= SATURATION_THRESHOLD` to `>= SATURATION_THRESHOLD * 0.90`.
5. Slightly relaxed soft migration parent share gate:
   - from `>= 0.20` to `>= 0.17`.

## SECTION 4: POST-RUN RESULTS

Child-share metrics from `rollup-snapshots.json` (child population keys `*_A#` / `*_B#`):

- explorer-heavy: peak `0.00%`, final `0.00%`, active child niches `0`
- ritualist-heavy: peak `3.45%`, final `0.00%`, active child niches `1`
- gatherer-heavy: peak `6.90%`, final `6.90%`, active child niches `2`
- mixed: peak `6.25%`, final `3.12%`, active child niches `2`
- random-baseline: peak `6.90%`, final `0.00%`, active child niches `1`

Parent-share shift (dominant bifurcated parent observed where children were active):

- ritualist-heavy (`RITUAL_STRANGE_UTILITY`): `+0.00 pp`
- gatherer-heavy (`RITUAL_STRANGE_UTILITY`): `+10.34 pp`
- mixed (`RITUAL_STRANGE_UTILITY`): `-3.12 pp`
- random-baseline (`RITUAL_STRANGE_UTILITY`): `+3.45 pp`

## SECTION 5: STABILITY CHECK

Safety outcomes:

- No uncontrolled niche explosion observed.
- Dynamic niche counts remained bounded (caps and cooldowns still enforced).
- Parent niches remain dominant in all scenarios where children appear.

Success criteria evaluation:

- All scenarios produce non-zero child share: **NOT MET** (`explorer-heavy` remained zero).
- At least 3 scenarios exceed 3% child share: **MET**.
- At least one scenario exceeds 5% child share: **MET**.
- No uncontrolled niche growth: **MET**.

## ACTIVATION STABILITY RESULT

**PARTIAL**
