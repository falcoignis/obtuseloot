# Organic niche membership, corrected: per-parent subniches, with and without recency

The first observed-children arm (`docs/organic-niche-membership.md`) froze, with Rules E 4/16 and T 0/16. It also had a defect: budding and membership used each artifact's overall dominant subniche, which can belong to a different tag. The owner chose to adopt the locks and inversion removal, and to redo option 2 (2026-10-27: "1 and two"). This note pre-registers the redo before the arms run.

## Arms

All arms run 72 windows, 16 seeds and 48 artifacts, on GitHub Actions.

| Arm | Commit | Change |
|---|---|---|
| control | d3cdade | Locks and inversion removed. Displacement, migration and variants still present (#432) |
| per_parent | 4e594f3 | Children budded from observed subniches (as in the first attempt), with a member's subniche measured within the parent tag: the subniche carrying most of its utility mass on that tag |
| recency | 830e44a | per_parent, plus membership and budding read the subniche of each member's latest active window (the change in its running totals since the previous evaluation window). A member with no new activity keeps its last window. |

In both treatment arms, displacement, migration, hash-parity assignment, the A/B variant profiles and the post-split adoption multiplier are absent (removed in aedf6a1). The evaluation window is the tracker's existing unit; no decay rate or window length was chosen for recency.

Not measured here: removing the variant profiles also affects live ability generation, support allocation and significance text. Every consumer treats a missing variant as neutral. Live play is not measured either.

## Pre-registered health rules

These are unchanged from the first comparison. Each rule is shown if it holds in at least 12 of 16 seeds; the guardrail must hold in all 16. There is no calculated prediction.
- **Guardrail:** no seed loses more than 25% Simpson diversity relative to its first 3 windows.
- **Rule S:** more than one split.
- **Rule E:** at least one child collapse.
- **Rule T (no freeze):** at least one child birth and at least one collapse in windows 48 to 71.

Also reported, without a tolerance: children born and collapsed, mean live children by 12-window block, and final Simpson diversity against the control.

Neutrality: the control must equal the locks_off arm of run 40449757912 byte for byte.

Summarizer: `scripts/summarize-niche-membership-v2.py CONTROL PER_PARENT RECENCY`.

## Result

GitHub Actions run 40540444079, 72 windows, 16 seeds per arm, all 25 jobs passed. The control's outputs equal the locks_off arm of run 40449757912 byte for byte (32 of 32 files). Full suite at 830e44a: 608 tests, 0 failures, 0 errors, 9 opt-in skipped.

| Arm | Guardrail | Rule S | Rule E | Rule T | Children born / collapsed per seed | Final Simpson | Mean live children by 12-window block |
|---|---|---|---|---|---|---|---|
| control | held | 15/16 | 15/16 | 13/16 | 111.2 / 98.6 | 0.8643 | 7.4, 11.1, 11.5, 12.3, 12.6, 12.8 |
| per_parent | held | 8/16, not shown | 0/16, not shown | 0/16, not shown | 1.6 / 0.0 | 0.8560 | 0.7, 1.1, 1.3, 1.4, 1.6, 1.6 |
| recency | held | 9/16, not shown | 1/16, not shown | 1/16, not shown | 1.8 / 0.1 | 0.8562 | 0.7, 1.1, 1.3, 1.4, 1.6, 1.7 |

Both corrected arms keep diversity within the guardrail but fail Rules S, E and T. They bud few children, and almost none go extinct.

Post hoc analysis, not a pre-registered claim:

The static-ability explanation below was later disproved by a direct probe; see
`organic-niche-environment-shift.md`. Retained here as historical analysis, not a
current causal finding.
- **The control's turnover is mostly empty children.** Of its 1,577 collapses, 1,491 (94.5%) happen at a lifetime of 6 or 7 windows. That is the earliest a child can collapse: the 4-window grace period plus 2 or 3 empty windows of hysteresis. Displacement fills A/B pairs with 10 to 20% of the parent, so one child often stays empty and dies at the first allowed moment, and the parent splits again.
- **Rules E and T therefore reward that churn.** In the organic arms a child exists only when members occupy its subniche, and it persists while they use it. The harness environment is static: each seed's success rates never change, and ability counts stop growing by window 12. Few stable niches may be the organic outcome there, and these rules cannot tell that apart from a freeze.
- Testing whether organic niches are born and go extinct in response to change needs a changing environment, which this harness does not have.
