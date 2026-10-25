# Larger-population local adaptation: pre-registration

Written and committed before any result. The owner approved the run on 2026-10-24
("do it").

## Question

The plasticity confirmation's zero-cue arm showed founder ancestry collapsing at
the rate predicted for two isolated breeding pools of 32 items. Low-ceiling and
open-sky items never share parents. Between those isolated pools, the Double Jump
gene differed no more than the precision gene, even though only the jump gene's
payoff depends on the ceiling. Does local adaptation to ceiling appear once drift
is weaker?

## Prediction and its basis

At a low ceiling, jump response is capped (0.0784 blocks in the model), while
expressed recovery still rises with the kinetic gene. Measured utility is scaled by
`response / (1 + recoveryTicks / 20)` (`docs/heritable-ability-performance.md`).
A higher kinetic gene therefore costs utility at low ceilings and buys nothing.
Prediction: the kinetic gene ends **lower** in the low-ceiling pool than in the
open-sky pool.

## Setup

`IndependentConditionsExperimentTest.largerPopulationLocalAdaptation`:
- The same 16 seeds (4722..19722) and the same varied loadouts and opportunities
  as the confirmation run.
- Steady conditions, measured selection, zero cues (no plastic expression; this
  arm reproduces the no-plasticity build byte for byte), 40 generations.
- **64 items per site instead of 16**, so each isolated pool holds 128 items.
  The founder precision/kinetic grid repeats every 16 slots, so the founder
  frequencies are unchanged. No engine rule, weight, radius or window changes.

## Primary decision rule

For each seed at generation 39:

    D_kinetic   = mean kinetic gene at low sites - mean kinetic gene at sky sites
    D_precision = the same for the precision gene

Precision has no ceiling-specific payoff in this model. Its founders use the same
grid and it is inherited through the same lineages, so it is the paired drift
control.

A seed **passes** when `D_kinetic < 0` and `|D_kinetic| > |D_precision|`.
**Local adaptation is shown** if at least 12 of 16 seeds pass; otherwise it is
**not shown**.

## Reported descriptively (not part of the verdict)

- The same rule on the existing 16-item zero arm. Those results were already
  partly seen (kinetic and precision were both negative in 11/16), so they are a
  baseline, not a test.
- Newborn founder Simpson diversity at generation 39, against a Wright-Fisher
  calculation for N=128 from the observed generation-0 value.
- The crop-minus-stone precision gene. Prediction: still about 0, because target
  remains absent from competition context.
- Kinetic and precision variance; distinct founders; the rule against the seven
  base genes that are never expressed. Those genes start at 0.5 in every founder,
  so they have no standing variation, and this comparison favors kinetic.
