# Shared recovery: results

Pre-registered in `docs/shared-recovery-preregistration.md` (ca8c586). Run on
ca8c586 with `sharedRecoveryAblation`, 16 seeds, 40 generations.

## Primary rule: shown, 16/16

The share of births in generations 1..39 from parents with both abilities is
lower than in the 42df264 run in every seed. The mean falls from 0.497 to 0.390.
The generation-0 calculation predicted about 0.37. Across all generations the
parent loadout shares are 0.389 both, 0.483 insight only and 0.128 jump only. The
loadout share is about 1/3, so items with both abilities keep a smaller edge.

## Against the earlier formulas (descriptive)

| Generation 39 | Per attempt | Total | Recovery once (42df264) | Shared recovery |
| --- | ---: | ---: | ---: | ---: |
| Founder Simpson | 0.541 | 0.423 | 0.583 | 0.572 |
| Founders per site | 4.1 | 2.8 | 4.2 | 3.95 |
| Precision variance | 0.041 | 0.006 | 0.074 | 0.067 |
| Kinetic variance | 0.052 | 0.033 | 0.066 | 0.064 |
| Kinetic, low / sky | 0.62 / 0.67 | 0.41 / 0.14 | 0.40 / 0.60 | 0.22 / 0.50 |
| Precision, low / sky | 0.16 / 0.26 | 0.07 / 0.08 | 0.32 / 0.43 | 0.55 / 0.36 |
| Births from two-ability parents | 0.35 | 0.50 | 0.50 | 0.39 |

- Ceiling rule of the previous run (`D_kinetic < 0` and `|D_kinetic| > |D_precision|`):
  10/16 (42df264: 9/16). Kinetic is below 0.5 in the low pool in 15/16 seeds and
  above 0.5 under sky in 9/16. The two pools move in opposite directions in 8/16.
- Precision now differs by ceiling (0.55 low against 0.36 sky). In 42df264 it was
  higher under sky. This larger `D_precision` is what the ceiling rule compares
  kinetic against.
- Crop minus stone precision is still flat: mean 0.002, positive in 10/16 seeds.
  Crop and stone do not differentiate.

Interpretation, not tested here: under a shared gate, precision's longer
recovery costs a two-ability item jumps. Jumps are worth little at a low ceiling
and a lot under sky. That is consistent with precision falling under sky and
staying high at a low ceiling, which is the coupling the pre-registration named.

## Follow-up commit (after the run, not part of the run)

e7413bd extends the gate to Gentle Harvest, which has no recovery of its own but
is suppressed while another ability on the item recovers. It also injects the
ecology clock into AirJumpService so the native jump gate has a unit test. Both
new tests fail with the gate disabled. Neither path is exercised by this
experiment, so the result above is unaffected.

Passive speed records no outcomes (`PassiveEffectController` has no observation
path), so it never enters parent selection; it is not gated. Recovery is per
item: a player carrying two single-ability items avoids sharing, but each item
then competes with single-ability evidence, so no item gains the summed fitness.

To reproduce:

```sh
python3 scripts/summarize-shared-recovery.py target/shared-recovery target/no-recovery-divisor
python3 scripts/summarize-no-recovery-divisor.py target/shared-recovery target/total-utility-ablation target/larger-population
```

The second script labels its first argument `recovery_once`; here that column is
the shared-recovery run.
