# Total-utility parent fitness ablation: pre-registration

Written and committed before any result. The owner approved the run on 2026-10-24
("try it").

## Change

Parent fitness was the parent's validated utility **divided by its attempt
count** (`docs/local-artifact-inheritance.md`). The ablation removes the divisor,
so parent fitness becomes the **total** validated utility in the radius/window.
The classifier's repeat and no-op penalties, the habitat competition, the radius,
the window and proportional sampling are all unchanged.

## Why

A payoff probe on real code (`payoff-probe-20261024.csv`, private review archive)
gave these per-sample pool fitness values for Double Jump, gene 0 to gene 1:

- low ceiling: 0.056 down to 0.026;
- open sky: 0.546 up to 0.606.

Material Insight samples are worth 0.55 to 1.58 each. A higher kinetic gene
lengthens recovery and roughly halves the number of jumps per item (3.5 down to
1.7). Under the per-attempt mean, fewer low-value jumps raise the average, so the
recovery cost acted as a benefit at both ceilings. The kinetic gene then rose in
both pools in the 64-per-site run.

## Predictions (calculated from the probe values and the measured jump counts)

Total jump value per item, gene 0 versus gene 1:

| Ceiling | Gene 0 | Gene 1 |
| --- | ---: | ---: |
| Low | about 0.20 | about 0.04 |
| Sky | about 1.85 | about 1.04 |

Insight value per item is about 3.3. Low kinetic is therefore favored at both
ceilings: by about 4% of total fitness at a low ceiling and by about 16% under open
sky. The kinetic gene should fall in both pools, faster under open sky.

## Setup

`IndependentConditionsExperimentTest.totalUtilityAblation`. Identical to
`largerPopulationLocalAdaptation`:

- 16 seeds, 4722..19722;
- 64 items per site;
- zero cues, steady conditions, measured selection;
- 40 generations.

The per-attempt baseline is that run's output.

## Primary rules

Each rule gets its own verdict, applied at generation 39.

**A. Recovery acts as a cost.** A seed passes when the mean kinetic gene is below
0.5 in **both** the low pool and the sky pool. **Shown** if at least 12/16 seeds
pass.

**B. Ceiling-specific adaptation.** `D_kinetic` is the low-minus-sky mean kinetic
gene. A seed passes when `D_kinetic > 0` and `|D_kinetic| > |D_precision|`.
**Shown** if at least 12/16 seeds pass. This direction follows from the stronger
predicted selection under open sky. If both pools reach the gene floor by
generation 39, B can fail even when A holds; that limit is stated in advance.

## Reported descriptively

- Kinetic and precision means per pool over time, against the per-attempt
  baseline.
- Founder Simpson diversity, founders per site and gene variances.
- The crop-minus-stone precision gene. Prediction: still about 0, because target
  remains absent from competition context.
- The share of births from parents with each loadout. Total utility also rewards
  having more abilities and more opportunities, which are not heritable here.

Other tests that pin per-attempt fitness values (the habitat-competition witness
weights and similar) are expected to fail under this change. They are not run. If
the owner adopts the change, they would be updated in a reviewed PR.
