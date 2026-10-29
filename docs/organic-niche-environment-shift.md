# Organic niches under a changing environment

The corrected observed-membership arms (`docs/organic-niche-membership-v2.md`) built few, stable niches in a static harness. 94.5% of the control's collapses were children dying empty at the earliest allowed age, so the earlier Rules E and T rewarded churn. The owner chose a fairer test (2026-10-27, option A): change the environment partway through, and count only extinctions of niches that actually had members. This note pre-registers it before the arms run.

## Setup

- Harness as before (16 seeds, 48 artifacts, 72 windows on a synthetic clock), with `-Dobtuseloot.shiftWindow=36`. At window 36 each seed redraws every niche tag's success rate (uniform 0.2 to 0.9) from a separate stream, so windows 0 to 35 are identical to an unshifted run.
- `children.csv` records each child's peak membership. A child is **populated** if it ever had at least one member.

| Arm | Commit | Design |
|---|---|---|
| control | b384e24 | Locks and inversion off; displacement, migration and A/B variants present |
| per_parent | 2899e5b | Children budded from observed subniches, measured within the parent tag |
| recency | 1e3aa96 | per_parent, with membership from each member's latest active window |

## Pre-registered rules

Each rule is shown if it holds in at least 12 of 16 seeds; the guardrail must hold in all 16. There is no calculated prediction.
- **Guardrail:** no seed loses more than 25% Simpson diversity relative to its first 3 windows.
- **Rule R (response):** at least one populated child is born in windows 36 to 71.
- **Rule X (populated extinction):** at least one populated child collapses in windows 36 to 71.

Also reported, not rules:
- populated and empty births and collapses before and after the shift;
- mean live children and Simpson diversity by 12-window block;
- final Simpson diversity.

Neutrality: windows 0 to 35 of each arm's `windows.csv` must equal the same arm in run 40540444079 byte for byte.

Summarizer: `scripts/summarize-niche-environment-shift.py CONTROL PER_PARENT RECENCY`.

## Result

GitHub Actions run 40566824812, 24 jobs, all passed. Windows 0 to 35 equal run 40540444079 byte for byte for every arm and seed group (24 of 24).

| Arm | Guardrail | Rule R | Rule X | Populated births pre/post | Empty births pre/post | Populated collapses pre/post | Empty collapses pre/post |
|---|---|---|---|---|---|---|---|
| control | held | **12/16** | **12/16** | 12.2 / 7.1 | 40.8 / 49.7 | 8.5 / 7.2 | 32.8 / 48.6 |
| per_parent | held | 3/16 | 0/16 | 1.3 / 0.2 | 0 / 0 | 0 / 0 | 0 / 0 |
| recency | held | 3/16 | 0/16 | 1.3 / 0.3 | 0 / 0 | 0 / 0 | 0 / 0 |

Per seed, these are means. Mean live children by 12-window block: control 7.4, 11.1, 11.5, 12.2, 12.3, 12.7; per_parent 0.7, 1.1, 1.3, 1.4, 1.5, 1.5; recency 0.7, 1.1, 1.3, 1.3, 1.4, 1.5.

- **The control meets both response rules exactly at threshold.** About one child in five that it creates is ever populated. Its populated niches are born and go extinct before and after the change.
- **Both observed-membership arms fail.** They bud about 1.5 children per seed, every one populated, and none ever collapses, even after the environment changes.
- A temporary local probe on the recency harness (12 artifacts, 24 windows, shift at window 12) found that artifacts change ability sets almost every window: 118 changes before the shift and 103 after. So a static ability set is not the cause.
- Post hoc and unverified: with 48 artifacts churning abilities and only one to five subniches per tag, some artifact always occupies each budded subniche, so an observed child never empties.

Conclusion for this harness: children defined by observed subniche membership do not produce niche extinction, even under environmental change, so they are not adopted. Forced displacement and soft migration stay. The locks and inversion removal (#432) is unaffected.
