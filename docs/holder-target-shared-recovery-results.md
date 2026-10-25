# Holder target context on shared recovery: results

Pre-registered in `docs/holder-target-shared-recovery-preregistration.md` (8fb0a2f).
All three runs on 8fb0a2f, 16 seeds, 40 generations.

## Primary rule: per-site shown 16/16; shared not shown 5/16

Both match the predictions.

| Generation 39 | Pooled (shared recovery) | Shared holder | Per-site holder |
| --- | ---: | ---: | ---: |
| Rule passes | 7/16 | 5/16 | **16/16** |
| Precision, crop / stone | 0.456 / 0.453 | 0.374 / 0.371 | **0.563 / 0.229** |
| C_precision mean (positive seeds) | 0.002 (10) | 0.003 (7) | 0.334 (16) |
| C_kinetic mean (control) | 0.000 | 0.005 | 0.022 |
| Founder Simpson | 0.572 | 0.604 | 0.471 |
| Founders per site | 3.95 | 4.31 | 3.64 |
| Precision variance | 0.067 | 0.059 | 0.075 |
| Kinetic variance | 0.064 | 0.063 | 0.053 |
| Cross-target parents per site | 32.1 | 31.7 | 9.5 |
| Two-ability parent share (all births) | 0.3892 | 0.3891 | 0.3890 |

With per-site holders the split appears by generation 9 (crop 0.575, stone
0.258) and holds through generation 39. No founder births occur in any run.

## Regression

The pooled replay on 8fb0a2f is byte-identical to the 6fdb49d shared-recovery
run (generations, parentage and plasticity CSVs).

## Costs and limits

- Per-site differentiation costs founder diversity (Simpson 0.47 against 0.57
  pooled), because parents from the other target's site mostly stop breeding
  (9.5 against 32 per site). The split is not imposed: nothing but measured
  utility on the holder's own targets weights parents.
- Per-site is the best case. The shared holder, closer to a player who both farms
  and mines, gives no differentiation, as `docs/holder-target-context.md` found
  under the earlier formula.
- The two-ability share is unchanged by holder weighting (counts 63,737, 63,750
  and 63,770 of 163,840 births). Interpretation: it is set by shared recovery,
  not by target weighting.

How often real players specialize by site is unmeasured. This result shows the
mechanism separates niches when holder activity does; it does not show that real
activity will.

To reproduce:

```sh
python3 scripts/summarize-holder-target.py target/holder-per-site target/holder-shared target/shared-recovery
```
