# Recovery, extra abilities and holder targets: research chain

This branch carries the changes from five pre-registered experiments. Each
pre-registration was committed before its run, and each has a results document.
They ran in this order on a local research branch.

| Step | Change | Primary result | Documents |
| --- | --- | --- | --- |
| 1 | 64 items per site, per-attempt fitness | Local adaptation not shown, 4/16. A payoff check found the per-attempt mean rewarded recovery | `larger-population-*` |
| 2 | Parent fitness is the total utility in the window | Ceiling adaptation shown, 14/16, but precision collapsed. Calculation: recovery was counted twice | `total-utility-ablation-*` |
| 3 | Recovery no longer divides a use's utility | Ceiling rule not shown, 9/16; best diversity of the three formulas | `no-recovery-divisor-*` |
| 4 | Abilities on one item share recovery | Two-ability birth share lowered in 16/16 seeds (0.50 to 0.39) | `shared-recovery-*` |
| 5 | Parents weighted by the receiving holder's targets | Crop/stone split shown with per-site holders, 16/16; not with one shared holder, 5/16 | `holder-target-shared-recovery-*` |

The branch keeps changes 2 to 5. Change 1 changed only the experiment's population.

## Provenance

The research branch also carried an inherited target-plasticity mechanism. Its
own pre-registered confirmation was not supported, so the owner kept it out of
this branch (2026-10-25). All five experiments above ran with plasticity
switched off (no target cues). On that setting the research build's zero-cue
arm had reproduced the no-plasticity build byte for byte.

The research commits named in the results documents are not published. The
experiments are reproducible on this branch with
`-Dobtuseloot.experiments=true`:

| Run | Method | Output |
| --- | --- | --- |
| Step 4 | `sharedRecoveryAblation` | `target/shared-recovery` |
| Step 5, shared holder | `holderSharedAblation` | `target/holder-shared` |
| Step 5, per-site holders | `holderPerSiteAblation` | `target/holder-per-site` |

Steps 1 to 3 need their own formulas, which this branch replaces, so they are
reproducible only from the research commits. The larger-population summary also
reads a plasticity-confirmation run as its baseline.

Trait means are written to `genes.csv` here. The research runs wrote the same
trait columns, plus reaction weights, to `plasticity.csv`. The summary scripts
read either.

## Regression on this branch

The three runs above, on this branch without plasticity, reproduce the research
outputs exactly: `generations.csv` and `parentage.csv` are byte-identical, and
every row of `genes.csv` (163,840 per run) matches the trait columns of the
research `plasticity.csv`.
