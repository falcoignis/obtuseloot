# Holder specialization sweep: results

Pre-registered in `docs/holder-specialization-preregistration.md` (24e9245). Runs
on 24e9245, 16 seeds, 40 generations. The `q = 1` and shared-holder columns are
the merged #424 runs.

## Primary rule: q = 0.75 shown 12/16; q = 0.5 not shown 1/16

Both match the predictions. The 0.75 verdict sits exactly at the threshold.

| Generation 39 | q = 1 | q = 0.75 | q = 0.5 | One shared holder |
| --- | ---: | ---: | ---: | ---: |
| Rule passes | 16/16 | **12/16** | **1/16** | 5/16 |
| Precision, crop / stone | 0.563 / 0.229 | 0.408 / 0.360 | 0.309 / 0.324 | 0.374 / 0.371 |
| C_precision mean (positive seeds) | 0.334 (16) | 0.048 (14) | -0.015 (5) | 0.003 (7) |
| C_kinetic mean (control) | 0.022 | 0.000 | 0.002 | 0.005 |
| Founder Simpson | 0.471 | 0.535 | 0.523 | 0.604 |
| Founders per site | 3.64 | 3.64 | 3.75 | 4.31 |
| Precision / kinetic variance | 0.075 / 0.053 | 0.080 / 0.042 | 0.056 / 0.048 | 0.059 / 0.063 |
| Cross-target parents per site | 9.5 | 19.9 | 32.4 | 31.7 |
| Two-ability parent share | 0.389 | 0.387 | 0.388 | 0.389 |

## Reading

- The split falls off far faster than the share. A quarter of a holder's activity
  on the other target shrinks the generation-39 split about sevenfold (0.334 to
  0.048). The generation-0 push only halved (0.086 to 0.045). Interpretation, not
  tested: with q = 0.75 each pool still mixes parents across targets (19.9
  cross-target parents per site), so gene flow erodes most of the push.
- At q = 0.5 the two holders in a pool have the same expected mix, and there is
  no split, as calculated.
- Founder diversity costs less at q = 0.75 than at q = 1 (Simpson 0.535 against 0.471).

A real player's activity mix is still unmeasured. These results say holder
context separates niches strongly only for players who mostly work one target.

To reproduce:

```sh
python3 scripts/summarize-holder-target.py target/holder-per-site-75 target/holder-per-site-50 target/holder-per-site target/holder-shared
```
