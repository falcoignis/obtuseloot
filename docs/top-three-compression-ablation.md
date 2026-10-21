# Top-three compression ablation

The final within-category sampler now uses its supplied weights unchanged. The old
policy blended every weight toward the category mean whenever the largest three
exceeded 55% of the total, trying to reach a 50% share. That policy implemented a
preset distribution rather than preserving the computed selection signal.

The initial top-three ablation removed only this layer. Category choice, score
construction, nonlinear normalization, the soft top cap, cold balancing,
small-category flattening and unseen-template overrides still ran at that snapshot.
The [normalization follow-up](score-normalization-ablation.md) removes gamma/mean
blending separately. There is no live switch to restore the old compressor; each
later removal needs its own ablation.

## Controlled comparison

`TopThreeCompressionAblationTest` restores the exact former compressor **only in
test code**, from prerequisite `26c2f9816e1e1324ef3d176bcba8eead8974dd5e`.
Its test subclass intercepts the same final sampling boundary as production and
consumes exactly one original RNG draw. Everything else uses the current generator.

Both arms use the existing isolated fixed-seed ecosystem fixture: 32 artifacts,
12 windows, first three as the early baseline, real Java generation and utility
feedback, simulated outcomes. Each arm has its own population history. The
comparison requires the legacy arm to change actual weights and actual choices;
a dormant control cannot pass as an ablation. The treatment is replayed to verify
identical occupancy windows in an independent run.

The existing 25% maximum relative diversity-loss gate applies separately to each
arm. Results are observations, not target template counts or maximum shares.
There is no assertion that both arms produce the same distribution or that
removing compression must increase diversity. The original health test retains
its empty-population and monoculture negative controls.

A direct interval regression fails against the old production behavior: for
weights `[9, 1, 1, 1]`, a draw of `0.65` belongs to the first interval, but the
compressor makes the sampler choose the third. The replacement honors the supplied
intervals and leaves the input array unchanged. The test-only control also records
that a 50% top-three share is unattainable with four or five positive weights;
the old binary search therefore makes those categories uniform.

## Recorded paired result

At PR #411 (`6e886ec`), the paired focused and full-suite outputs matched exactly: maximum relative diversity loss was **9.82%** with
legacy compression and **8.02%** without it. The legacy control changed weights at
141 sampling points and changed nine immediate choices; treatment replay matched
exactly. These are one-seed observations, not a claim of general improvement.
The dated canonical status records the complete suite validation. Later ablations
change the surrounding pipeline; these figures describe that prerequisite snapshot,
not every future run of the same test.

## Scope and compatibility

The current live pool has at most two implemented entries in any category, so the
old compressor was dormant there. This removal affects the full offline catalog
and future categories with more than three eligible live entries. It introduces
no ability-ID special cases, permissions, storage changes, event interception,
movement, XP or optional-plugin API calls. All abilities continue through the
shared catalog and execution policies.

This is one ablation, not evidence of a complete emergent ecosystem. Remaining aids
still affect the comparison. These runs do not exercise Bukkit worlds, reproduction,
lineage birth/extinction, dynamic niche formation, or live protection/integration
behavior. Wild mutation remains held. See the canonical decision ledger for the
remaining emergence criteria and unresolved design choices.
