# Total-utility parent fitness ablation: results

The rules in [the pre-registration](total-utility-ablation-preregistration.md)
were committed at `a041973` before the run. Reproduce with:

```sh
python3 scripts/summarize-total-utility-ablation.py target/total-utility-ablation target/larger-population --output target/total-utility-summary.json
```

## Verdicts

- **Rule B, ceiling-specific adaptation: shown, 14/16 seeds** (per-attempt
  baseline 4/16). The kinetic gene ends lower under open sky than under a low
  ceiling, by more than precision differs between the pools.
- **Rule A, recovery acts as a cost: not shown, 11/16** (threshold 12; baseline
  0/16). Kinetic is below 0.5 in all 16 sky pools but stays above 0.5 in 5 of the
  16 low pools. The calculation predicted weak selection at a low ceiling (about
  4% of fitness).

**Caveat on B:** precision falls to near its floor in both pools (below). That
makes `|D_precision|` small and the paired drift control weak. The kinetic
difference itself is large: mean low-minus-sky is +0.27, and kinetic ends at
0.41 in the low pool versus 0.14 under sky.

## Mean genes over time

| Generation | Kinetic, low | Kinetic, sky | Precision, low | Precision, sky |
| --- | ---: | ---: | ---: | ---: |
| 0 | 0.500 | 0.500 | 0.500 | 0.500 |
| 9 | 0.468 | 0.350 | 0.093 | 0.110 |
| 19 | 0.444 | 0.208 | 0.080 | 0.083 |
| 39 | 0.410 | 0.140 | 0.074 | 0.084 |

The per-attempt baseline at generation 39 was kinetic 0.617 / 0.670 and
precision 0.163 / 0.257.

## Side effects (descriptive)

- **Precision collapses toward speed.** Interpretation, checked by calculation below: total utility rewards how often an item
  inspects, so the shortest Material Insight recovery wins on both targets.
  Precision variance at generation 39 is 0.006, against 0.041 under the baseline.
  The crop-minus-stone precision gene stays near zero (mean -0.0001, positive in
  9/16): the target gap is unchanged.
- **Less ancestry diversity.** Founder Simpson diversity is 0.423 (baseline
  0.541), with 2.8 founders per site (baseline 4.1).
- **More abilities means more offspring.** Parents with both abilities supply 50%
  of births, against 35% under the baseline. Parents with only Double Jump fall
  from 13% to 9%. Loadouts are not heritable in this experiment.
- The recovery-versus-frequency tradeoff depends on the fixed experiment
  schedule (12 rounds 20 ticks apart). In live play, how often a player gets the
  chance to act sets this, and it has not been measured.

## Recovery is counted twice (calculation, no run)

`AbilityPerformanceMeasurement.adjustUtility` already divides every sample's
utility by `1 + recoveryTicks / 20`. Under total utility, longer recovery also
means fewer samples, so recovery is penalized twice. The probe values
back-calculate to base utility of exactly 1.5 per fact (inspection) and 1.8 per
block (jump). The table combines those with the zero-arm successes per item for
each precision bin. Those counts come from the per-attempt regime with mixed
loadouts, so the totals are approximate.

| Precision gene | Crop, with divisor | Crop, without | Stone, with divisor | Stone, without |
| ---: | ---: | ---: | ---: | ---: |
| 0 | 4.14 | 10.35 | 4.18 | 10.46 |
| 0.25 | 3.77 | 12.26 | 2.52 | 8.18 |
| 0.5 | 3.42 | 13.66 | 1.69 | 6.75 |
| 0.75 | 3.06 | 14.52 | 1.23 | 5.85 |
| 1 | 2.33 | 12.84 | 0.94 | 5.18 |

With the divisor, the fastest inspection wins on both targets, which matches the
observed collapse. Without it, crop favors depth (best near 0.75) and stone favors
speed, so the targets would select in opposite directions. The same calculation for
Double Jump without the divisor favors a low gene at a low ceiling (0.49 versus 0.24)
and a high gene under open sky (4.61 versus 5.73). This is a calculation, not a
run. Removing the divisor still would not separate crop from stone while competition
pools mix the two targets about 50/50.

## Reproducing after later changes

The `totalUtilityAblation` method was removed once later formula changes made it run the same
code as `sharedRecoveryAblation`. Reproduce this result from commit a041973, where the
method and its pre-registered formula are present.
