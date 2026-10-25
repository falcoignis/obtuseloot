# Holder target context for local parent selection

> On this branch a parent's fitness on a target is its total validated utility
> there, and recovery is counted once through fewer uses. The witness weights and
> experiment tables below were measured on b90a6f4 under per-attempt fitness. For
> current results see [holder target context on shared recovery](holder-target-shared-recovery-results.md).

The [independent-condition diagnostic](independent-ecosystem-conditions.md) showed
that column-only context pools crop and stone evidence measured under the same
ceiling height. The owner selected the holder-activity option on 2026-10-24
("go with option 1"). A newborn item's parents are now weighted by the action
targets its receiving holder has recently worked on.

## Why not a target descriptor at the birth origin

Births do not happen where abilities are measured. An enchanted item's origin is
one block above the enchanting table, so a column floor read there is the table.
Chest loot uses the loot-context location; what lies under it was not measured
for this change, but nothing ties it to the targets players later work on. An
origin material descriptor would therefore rarely match inspection evidence. Selection would then fall back to fresh founders
without any visible failure. The receiving holder is the measured link between
past evidence and where the new item is likely to be used.

## Rule

1. Each ecology sample records the measuring holder and its **action target**:
   the exact lower-case block material of a clicked block, a broken block's
   captured state, or a block event's block. Actions without a block target
   record `none`. Materials are never grouped and no adjacency is defined.
2. Habitat competition is unchanged. The nearest supported column depth inside
   the configured chunk radius and outcome window selects the competing current
   revisions first.
3. For each competing parent, target fitness is the existing pooled formula
   restricted to its samples measured on that target.
4. The receiving holder's recent attempt mix is counted from **all** of that
   holder's samples in the outcome window, at any position. Only targets that at
   least one competing parent was measured on are kept, then renormalized.
5. Parent fitness is the mix-weighted sum of its target fitnesses. A parent with
   no evidence on a kept target receives nothing for it. Non-positive results are
   ineligible, as before.
6. A null holder, or a holder with no recent attempts on any kept target, keeps
   the pooled fitness exactly. Unclaimed loot therefore keeps pooled selection.

There is no quota, floor, boost, target bonus or diversity rule. Ordinary
mutation and the 20-chunk parent radius are unchanged. Using the whole pool for
the holder's mix, exact materials and the zero contribution for unmeasured
targets are implementation choices, not owner-confirmed niche boundaries.

`/ol debug ecosystem health` remains holder-blind. Its competition census shows
pooled fitness.

## Storage

Ecology storage is **version 6**. Each sample requires `holder` (UUID) and
`target` (`[a-z0-9_]+`). Version 5 state is rejected rather than reset. No
player data exists, so no migration is provided.

## Validation

The two-item witness now keeps both behaviors. With no holder, pooled weights
at both same-column origins are still **1.2 / 0.954545** for precision 0/1. A
holder who only inspected crops, beyond the parent radius, receives exactly the
crop-only weights **1.2 / 1.363636** at the same origin. A holder who only
inspected stone receives exactly the stone-only weights **1.2 / 0.545455**.
Weights survive an encode/decode round trip. A holder without measured targets
receives the pooled list.

The independent-condition experiment runs in three selection modes with
identical inputs. The neutral control draws uniformly from the exact list the
production selector uses in each mode.

| Mode | Holder | Reports |
| --- | --- | --- |
| Pooled | none (the #423 selector) | `target/independent-conditions/` |
| Shared | one holder owns every site | `target/independent-conditions-holder-shared/` |
| Per-site | each site has its own holder | `target/independent-conditions-holder-site/` |

Pooled mode reproduces the archived #423 reports byte for byte, and its summary
matches.

**The realistic case is closer to Shared.** A player who both farms and mines has
a mix that spans targets, and Shared mode does not resolve the deficit: the same
weighting applies at every origin, stone/sky stays below neutral and stone/low
becomes worse. Per-site holders are the **best case**: each holder's mix tracks
its own site's target, and no modeled holder moves between sites.

Each mode's neutral control draws uniformly from that mode's own eligible list.
Holder weighting removes parents with no evidence on the holder's targets, so the
per-site neutral is not the #423 neutral. The tables give both baselines.

Final steady generation, varied-opportunity arm, Material Insight, four seeds
pooled. Each cell shows facts per observed item, then mean completed recovery in
ticks:

| Cell | Pooled measured | Shared measured | Per-site measured | #423 pooled neutral | Per-site neutral |
| --- | --- | --- | --- | --- | --- |
| Stone/sky | 2.8750, 68.42 | 3.4375, 50.13 | 3.9688, 43.03 | 3.9375, 44.31 | 3.5000, 52.42 |
| Stone/low | 3.3125, 42.78 | 2.5938, 65.14 | 3.5625, 40.88 | 2.8438, 55.10 | 2.8438, 53.63 |

Seed pairs where measured selection delivers more facts **and** shorter recovery:

| Cell | Pooled vs its neutral | Shared vs #423 neutral | Per-site vs #423 neutral | Per-site vs its neutral |
| --- | ---: | ---: | ---: | ---: |
| Stone/sky | 0/4 | 1/4 | 2/4 | 4/4 |
| Stone/low | 3/4 | 0/4 | 4/4 | 3/4 |

Against the #423 neutral, per-site stone/sky is at parity (3.97 versus 3.94 facts,
43.0 versus 44.3 ticks). The pooled deficit closes; it does not reverse. Crop
cells trade more facts against longer recovery, so neither direction is a deficit
by itself; their per-seed results are mixed in every mode.

Founder diversity still falls under measured selection. Last-generation
varied-opportunity newborn founder Simpson is 0.588379 / 0.608398 for measured
steady/changing per-site, versus 0.742676 for the per-site neutral and 0.713379
for the #423 pooled neutral. Measured steady cross-target parents fall from 128
(pooled) to 34 (per-site); neutral falls from 129 to 106. These are reported, not
corrected. No fresh-founder fallback births occur in any mode.

## Live action sources

Passive effects such as Fleetfoot speed contribute attribute modifiers and record
no pool samples. Double Jump reaches the pool through a deferred air-jump request,
so it and any other non-block action share the `none` target. A holder who jumps
far more often than they inspect blocks has a mix dominated by `none`, which
weights parents by their jump evidence. Real activity mixes are not yet measured.

This does not establish natural niches, extinction or ecosystem health. Assigned
loadouts, modeled jumps and API-double inspection remain the model's inputs.
Players who move between targets, real procedural profiles and native capture of
action targets remain future validation.

To reproduce the aggregates for any mode:

```sh
python3 scripts/summarize-independent-conditions.py target/independent-conditions-holder-site --output target/holder-site-summary.json
```
