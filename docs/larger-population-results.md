# Larger-population local adaptation: results

The rule in [the pre-registration](larger-population-preregistration.md) was
committed at `1c2da8c` before the run. Reproduce with:

```sh
python3 scripts/summarize-larger-population.py target/larger-population target/plasticity-confirmation --output target/larger-population-summary.json
```

## Verdict: local adaptation not shown

**4 of 16** seeds pass (threshold 12). The kinetic gene ends lower in the
low-ceiling pool in only 7/16 seeds (mean difference -0.053). The 16-item
baseline passes 9/16 under the same rule; those data were partly seen before
registration.

## What the larger pools changed

| | 16 per site | 64 per site |
| --- | ---: | ---: |
| Newborn founder Simpson, generation 39 | 0.121 | 0.541 |
| Newborn founders per site, generation 39 | 1.3 | 4.1 |
| Kinetic gene variance, generation 39 | 0.032 | 0.052 |
| Precision gene variance, generation 39 | 0.021 | 0.041 |

The Wright-Fisher calculation for a pool of 128, starting from the observed
generation-0 value, gives 0.717. Measured selection reduces diversity below that,
as it did at 16 per site. Cross-ceiling parentage stays at 0. The crop-minus-stone
precision gene stays near zero (mean +0.005, positive in 10/16).

## Selection is directional but the same at both ceilings

| Generation | Kinetic, low | Kinetic, sky | Precision, low | Precision, sky |
| --- | ---: | ---: | ---: | ---: |
| 0 | 0.500 | 0.500 | 0.500 | 0.500 |
| 19 | 0.574 | 0.629 | 0.223 | 0.307 |
| 39 | 0.617 | 0.670 | 0.163 | 0.257 |

At generation 39, kinetic is above 0.5 in 13/16 low pools and 14/16 sky pools.
This contradicts the pre-registered basis: at a low ceiling, a higher kinetic gene
was expected to cost recovery and gain no response. Why kinetic is favored at
both ceilings is not yet known. No neutral run exists at 64 per site, so it is
also not established that selection, rather than inheritance, causes the rise.

## Reproducing after later changes

The `largerPopulationLocalAdaptation` method was removed once later formula changes made it run the same
code as `sharedRecoveryAblation`. Reproduce this result from commit 1c2da8c, where the
method and its pre-registered formula are present.
