# Soft-cap ablation

The owner approved this ablation on 2026-10-25 ("do 1, 2, and 3"). It is the
next scaffolding removal after [score normalization](score-normalization-ablation.md),
which kept the cap for its own follow-up.

## Change

Within a category, template composite scores no longer pass through the
mean-relative soft cap. That cap left scores at or below the category mean alone
and squashed any excess with `mean + 0.28 * mean * tanh(excess / (0.28 * mean))`, so
no template could reach more than 1.28x the mean. Sampling now uses the composite
scores as measured. The `GLOBAL_CATEGORY_TOP_CAP_RATIO` constant is removed.

The empty, singleton, uniform and non-finite guards still return the input array.
A varied category gets a copy, because cold balancing edits the sampling array in
place and composite scores must stay untouched. Cold balancing, small-category
flattening, unseen-template overrides and the remaining scoring aids still apply.

## Pre-registration (written before the comparison ran)

`SoftCapAblationTest.seededUtilityFeedbackComparesTheCapWithItsAblation` uses the
same isolated scenario as the score-normalization ablation: 32 artifacts, 12
windows, the first three as each arm's baseline, real Java generation and utility
feedback, simulated outcomes. The control restores the removed cap from a
test-only copy (`LegacySoftCap`); there is no live restoration flag.

Required, as for the earlier ablations:

- The restored cap changes scores, and actual generated profiles differ between
  the arms. Diagnostics alone cannot establish an active ablation.
- The treatment replays exactly: occupancy windows and ordered generated profiles.
- The existing relative diversity-health check (maximum relative loss 25%) holds
  in both arms. If the ablated arm fails it, that is a finding for the owner, not
  something to tune.

Prediction (not calculated): the ablated arm stays within the health check. This
is one seed with other aids present; it cannot establish emergence or general
improvement.

`ScoreNormalizationAblationTest` keeps its historical comparison: both its arms
restore the cap. Its printed result must match main exactly.

## Recorded result

RESULT_PENDING
