# Organic niche membership: no locks, no inversion, children defined by observed subniches

The owner ruled on 2026-10-26: "Lets try to remove them all and make sure we retain a healthy ecology", covering forced displacement and soft migration, niche locks and inversion multipliers. They then chose option 2: children defined by the behavior their members show. This note pre-registers the comparison before the arms run.

## What was found first

`refreshDynamicNicheAssignment` never assigned members to child niches. Children were filled only by forced displacement (the parent's lower-utility half pushed in until children held 10 to 20% of it, toward the emptier child 85% of the time) and by soft migration. Child identity was hand-written: an A variant was mutation-heavy, a B variant retention-heavy, and subniches were split between A and B by name-hash parity.

## Arms

All arms run 72 windows (`-Dobtuseloot.windows=72`), 16 seeds and 48 artifacts, on GitHub Actions.

| Arm | Commit | Change |
|---|---|---|
| control | 917e960 | main: organic splitting with relief; displacement, migration, locks, inversion and variants present |
| locks_off | d3cdade | Niche locks (4-6 random windows, x1.30), the continuity sticky lock and the inversion multipliers (child x1.30, parent x0.88) removed |
| observed | aedf6a1 | locks_off, plus children defined by observed subniches. See below. |

In the observed arm:
- A split buds one child for the subniche the parent's unabsorbed members occupy most (ties by name).
- A parent occupying a single subniche does not split.
- Membership follows each artifact's measured dominant subniche, with no quota, balancing, lock or forced placement.
- Displacement, migration, hash-parity assignment and the variant profiles are removed, along with the post-split adoption multiplier.
- A lineage's starting affinity for a bud is its measured share of parent members in that subniche.

The niche count is bounded by the fixed subniche vocabulary: 1 to 5 per tag, and a tag with one subniche never splits. That bound comes from the taxonomy, as the tags themselves do. It is not emergent.

Not measured by this harness:
- The removed variant profiles also biased live ability generation (`ProceduralAbilityGenerator`), `AdaptiveSupportAllocator` and `ArtifactSignificanceResolver`. Every consumer already treats a missing variant as neutral, and offline the generator always saw none. Removing variants changes live selection in a way this harness cannot see.
- Live play.

## Pre-registered health rules

Each rule is shown if it holds in at least 12 of 16 seeds. There is no calculated prediction.
- **Guardrail:** no seed loses more than 25% Simpson diversity relative to its first 3 windows. Required in 16/16 seeds.
- **Rule S:** more than one split.
- **Rule E:** at least one child collapse.
- **Rule T (no freeze):** at least one child birth and at least one child collapse within windows 48 to 71.

Also reported, not rules: births and collapses per seed, mean live children by 12-window block, final Simpson and effective niches. Final Simpson is compared with the control without a tolerance.

Neutrality: the control at 72 windows must equal the `long/relief` arm of the 72-window organic splitting check byte for byte. That also confirms that #431's constructor cleanup changed no behavior.

Summarizer: `scripts/summarize-niche-membership.py CONTROL LOCKS_OFF OBSERVED`.

## Result

GitHub Actions run 40449757912, 72 windows, 16 seeds per arm, all 25 jobs passed. The control's outputs are byte-identical to the relief arm of the 72-window organic splitting check (32 of 32 files), which also confirms that #431's constructor cleanup changed no behavior. Full suite at aedf6a1: 606 tests, 0 failures, 0 errors, 9 opt-in skipped.

| Arm | Guardrail | Rule S | Rule E | Rule T | Children born / collapsed per seed | Final Simpson | Mean live children by 12-window block |
|---|---|---|---|---|---|---|---|
| control | held (0/16 over) | 15/16 | 14/16 | 10/16, not shown | 92.5 / 80.9 | 0.8748 | 7.2, 12.1, 12.1, 12.1, 11.9, 11.8 |
| **locks_off** | **held (0/16 over)** | **15/16** | **15/16** | **13/16** | 111.2 / 98.6 | 0.8643 | 7.4, 11.1, 11.5, 12.3, 12.6, 12.8 |
| observed | held (0/16 over) | 14/16 | 4/16, not shown | 0/16, not shown | 3.4 / 0.3 | 0.8631 | 1.4, 2.5, 2.8, 2.8, 3.0, 3.1 |

- **locks_off meets every health rule.** It has more turnover than the control, final Simpson 0.0105 lower, and live children rising slowly (11.1 to 12.8) rather than flat.
- **observed fails Rules E and T.** Splits continue at a trickle after window 24, but collapses stop after window 47 (5 in total).
- The observed arm also has an implementation defect, found after the run. Budding and membership used each artifact's overall dominant subniche, which the classifier picks across all tags, instead of its dominant subniche within the parent tag. That produced cross-tag buds, for example a RITUAL_STRANGE_UTILITY child budded from MEMORY_HISTORY's general subniche. Membership also hardly moves, because an artifact's subniche comes from its whole signal history.

**Owner decision (2026-10-27):** adopt the locks and inversion removal now. Option 2 is redone as a new pre-registered comparison with the subniche measured within the parent tag, with and without a recency-based membership. Forced displacement and soft migration stay in place until a corrected option 2 passes the health rules, so children keep getting populated in the meantime.
