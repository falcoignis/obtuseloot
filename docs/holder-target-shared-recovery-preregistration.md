# Holder target context on shared recovery: pre-registration

Written and committed before the 40-generation runs. The owner approved the run
on 2026-10-24 ("try it"). It is the crop/stone half of the owner's choice
("i like 1 and 2"); shared recovery (`docs/shared-recovery-results.md`) was the
first half.

## Change

The held holder-context commit b90a6f4 (`docs/holder-target-context.md`) is
ported onto 6fdb49d. A newborn's parents are weighted by the receiving holder's
recent action-target mix.

One deviation from b90a6f4: there, a parent's fitness on one target is its
utility on that target divided by its attempts there. That is the per-attempt
formula 42df264 removed, because it turns recovery into a benefit. Here a
target's fitness is the total validated utility measured on it, as for pooled
fitness on this branch. Nothing else in the rule changes.

## Experiment inputs

`holderSharedAblation` and `holderPerSiteAblation`, otherwise identical to the
shared-recovery run: 16 seeds (4722..19722), 64 items per site, zero cues, steady
conditions, measured selection, 40 generations.

- **Shared:** one holder owns every site, so its mix spans crop, stone and jumps
  everywhere. This is closer to a real player who both farms and mines.
- **Per-site:** each site has its own holder, whose mix tracks that site's target.
  This is the best case: no modeled holder moves between sites.

Jumps have no block target and share `none`.

## Calculation and regression check (generation 0, done before this document)

The fitness-weighted minus unweighted mean gene of generation-0 candidates, per
origin target, averaged over 16 seeds:

| Mode | Crop precision | Stone precision | Crop kinetic | Stone kinetic |
| --- | ---: | ---: | ---: | ---: |
| Pooled (6fdb49d) | -0.0075 | -0.0075 | -0.0141 | -0.0141 |
| Shared holder | -0.0076 | -0.0076 | -0.0115 | -0.0115 |
| Per-site holder | +0.0317 | -0.0540 | -0.0130 | -0.0084 |

Pooled generation-0 activities, candidates and births on the ported code are
byte-identical to the 6fdb49d output.

## Primary rule (one verdict per mode)

For each seed at generation 39:

    C_precision = mean precision on crop sites - mean precision on stone sites
    C_kinetic   = the same for the kinetic gene

Each site pairs a crop and a stone site at the same ceiling, so ceiling effects
cancel. A seed passes when `C_precision > 0` and `C_precision > |C_kinetic|`. The
kinetic gene is the control: jumps read no target. **Crop/stone differentiation
is shown** for a mode if at least 12/16 seeds pass.

Predictions: per-site **shown**; shared **not shown**. The 6fdb49d pooled run had
C_precision positive in 10/16 seeds with a mean of 0.002, and passes this rule in
7/16 (`scripts/summarize-holder-target.py target/shared-recovery`).

## Reported against the shared-recovery pooled run (descriptive, no verdict)

- Founder Simpson diversity and founders per site at generation 39; precision and
  kinetic variance.
- Parents from the other target's site (cross-target parents).
- Two-ability parent share, and the ceiling rule.
- Mean precision per target over time.

## Regression and tests

A full 40-generation pooled run on the ported code (`sharedRecoveryAblation`) runs
alongside and is compared byte for byte with the 6fdb49d `target/shared-recovery`.
The holder-context unit tests pass (ActionTargetTest, HabitatCompetitionTest, the
holder counterexample). One existing check fails: the stone witness in
`frequentOpportunities...` ("Identical stone facts favor shorter recovery"). It
also fails on 6fdb49d before the port: it pins the recovery divisor 42df264
removed. It would be updated in a reviewed PR with the other pinned tests.
