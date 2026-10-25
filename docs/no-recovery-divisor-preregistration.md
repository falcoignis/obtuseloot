# Recovery counted once: pre-registration

Written and committed before any result. The owner approved the run on 2026-10-24
("do it").

## Change

On top of total-utility parent fitness (`docs/total-utility-ablation-preregistration.md`),
remove the recovery divisor from `AbilityPerformanceMeasurement.adjustUtility`.
Positive utility becomes `utility * benefit` instead of
`utility * benefit / (1 + recoveryTicks / 20)`. Failure penalties are unchanged.
Recovery then costs only what it actually costs: fewer uses inside the evidence
window. Both changes remove hand-chosen formulas; nothing is added.

## Predictions (calculated, `docs/total-utility-ablation-results.md`)

Total value per item without the divisor:

- **Crop:** favors depth, best near precision 0.75 (14.5 against 10.4 at 0).
- **Stone:** favors speed (10.5 at 0 against 5.2 at 1).
- **Jump, low ceiling:** favors a low gene (0.49 against 0.24).
- **Jump, open sky:** favors a high gene (4.61 against 5.73).

Each breeding pool mixes crop and stone about 50/50. Averaged over the two
targets, precision values are nearly flat from 0 to 0.75, so precision should
not collapse to its floor. Crop and stone are still predicted not to differentiate.

## Setup

`IndependentConditionsExperimentTest.noRecoveryDivisorAblation`. Identical to
the previous two runs: 16 seeds (4722..19722), 64 items per site, zero cues,
steady conditions, measured selection, 40 generations.

## Primary rule

At generation 39:

    D_kinetic   = mean kinetic gene, low pool - sky pool
    D_precision = the same for precision

A seed passes when `D_kinetic < 0` and `|D_kinetic| > |D_precision|`. **Ceiling
adaptation is shown** if at least 12/16 seeds pass. The direction is the reverse
of the total-utility run, because open sky should now favor a high gene.

## Reported against both earlier runs (descriptive, no verdict)

These are the ecosystem costs the owner asked to avoid:

- Founder Simpson diversity and founders per site at generation 39. Per-attempt
  baseline: 0.541 and 4.1; total utility: 0.423 and 2.8.
- Precision and kinetic variance at generation 39. Per-attempt: 0.041 and 0.052;
  total utility: 0.006 and 0.033.
- Mean genes per pool over time, and the number of seeds where kinetic is above
  0.5 under sky and below 0.5 at a low ceiling.
- Parent loadout shares. Total utility raised parents with both abilities from 35%
  to 50%.
- The crop-minus-stone precision gene.

Tests that pin the old formula's values (`docs/heritable-ability-performance.md`
and the habitat-competition witnesses) are expected to fail. They are not run, and
would be updated in a reviewed PR if the owner adopts the change.
