# Holder specialization sweep: pre-registration

Written and committed before the 40-generation runs. The owner approved the run
on 2026-10-25 ("do 1, 2, and 3").

## Question

Holder target context split crop from stone when each site had its own holder
(16/16), but not when one holder worked every site (5/16;
`docs/holder-target-shared-recovery-results.md`). Real players sit in between.
How specialized must a holder be for the split to appear?

## Setup

`holderSpecialization75` and `holderSpecialization50`, on main after #424. They
are otherwise identical to `holderPerSiteAblation`: 16 seeds (4722..19722), 64
items per site, steady conditions, measured selection, 40 generations, shared
recovery.

Each site has its own holder, and births at a site are weighted by that site's
holder. A site's holder owns a share `q` of the site's items. The rest belong to
the holder of the paired site, which has the other target under the same ceiling
and the same breeding pool. A holder's activity mix is therefore about `q` on its
own site's target and `1 - q` on the other.

- `q` is 0.75 or 0.5. `q = 1` is the existing per-site mode.
- Each item's owner is drawn from the input-seed stream, never from the selection
  random. It is a test input.
- At `q = 0.5` both holders in a pool have the same expected mix.

## Calculation and regression check (generation 0, done before this document)

Fitness-weighted minus unweighted mean precision of generation-0 candidates, per
origin target, averaged over 16 seeds:

| q | Crop | Stone | Crop - stone |
| ---: | ---: | ---: | ---: |
| 1.00 | +0.0317 | -0.0540 | 0.086 |
| 0.75 | +0.0126 | -0.0324 | 0.045 |
| 0.50 | -0.0056 | -0.0098 | 0.004 |

Kinetic differentials are -0.008 to -0.014 at every `q` and both targets.
The `q = 1` mode on the new code reproduces the archived per-site
generation-0 activities, candidates and births byte for byte.

## Primary rule (one verdict per q)

As in `docs/holder-target-shared-recovery-preregistration.md`, at generation 39:

    C_precision = mean precision on crop sites - mean precision on stone sites
    C_kinetic   = the same for the kinetic gene

A seed passes when `C_precision > 0` and `C_precision > |C_kinetic|`. **Crop/stone
differentiation is shown** for a `q` if at least 12/16 seeds pass.

Predictions, from the generation-0 gap only (selection over 40 generations can
amplify or erase it):

- `q = 0.75`: **shown**, with a smaller split than `q = 1` (C_precision 0.33).
- `q = 0.5`: **not shown**.

## Reported (descriptive, no verdict)

C_precision mean and positive seeds, founder Simpson, founders per site,
precision and kinetic variance, cross-target parents and the two-ability parent
share, against `q = 1` and the shared holder.
