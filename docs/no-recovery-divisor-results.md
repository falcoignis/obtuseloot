# Recovery counted once: results

The rule in [the pre-registration](no-recovery-divisor-preregistration.md) was
committed at `4b3e844` before the run. Reproduce with:

```sh
python3 scripts/summarize-no-recovery-divisor.py target/no-recovery-divisor target/total-utility-ablation target/larger-population --output target/no-recovery-divisor-summary.json
```

## Verdict: ceiling adaptation not shown, 9/16

The threshold was 12. The kinetic gene ends higher under open sky than at a low
ceiling in 13/16 seeds, but in four of those the precision gap between the pools
is larger. That sign count is post hoc. Kinetic ends below 0.5 at the low ceiling
and above 0.5 under sky in 8/16 seeds.

## Ecosystem measures at generation 39

| | Per-attempt mean | Total utility | Total, recovery counted once |
| --- | ---: | ---: | ---: |
| Founder Simpson diversity | 0.541 | 0.423 | **0.583** |
| Founders per site | 4.1 | 2.8 | **4.2** |
| Precision variance | 0.041 | 0.006 | **0.074** |
| Kinetic variance | 0.052 | 0.033 | **0.066** |
| Kinetic, low / sky | 0.617 / 0.670 | 0.410 / 0.140 | 0.396 / 0.599 |
| Precision, low / sky | 0.163 / 0.257 | 0.074 / 0.084 | 0.318 / 0.431 |
| Parents with both abilities | 35% | 50% | 50% |
| Crop-minus-stone precision, positive seeds | 10/16 | 9/16 | 8/16 |

Counting recovery once keeps more diversity and more gene variation than either
earlier formula. Precision no longer collapses. The jump gene moves in the
predicted direction at each ceiling on average. The precision gap between pools
is also larger; that is unexplained, and is likely drift on a gene that now keeps
more variance (unverified). Two costs remain. Total utility still gives parents
with both abilities half the births, because owning more abilities and getting
more chances to act are rewarded. Crop and stone still do not differentiate,
because each competition pool mixes the two targets.

## Reproducing after later changes

The `noRecoveryDivisorAblation` method was removed once later formula changes made it run the same
code as `sharedRecoveryAblation`. Reproduce this result from commit 4b3e844, where the
method and its pre-registered formula are present.
