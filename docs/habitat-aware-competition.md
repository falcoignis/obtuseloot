# Habitat-aware local competition

> Historical #420 model. Native validation subsequently exposed actor-height
> aliasing. The [capture-frame correction](native-habitat-validation.md) measures
> a column span and uses ecology format 5; the findings below remain the first
> controlled model's results and limitations.


The owner approved implementing habitat-aware competition after the #419 health
baseline identified mixing between overlapping environments. This is a dependent
follow-up, not authorization to merge the pending stack.

## Mechanism

Each measured attempt retains its producing seed/revision, position and an immutable
habitat descriptor. Loot generation, enchanting, commands and live equipment effects
use the same position capture path. Capture happens when the event context is built,
before effect/protection callbacks, including delayed Double Jump observations.

The first descriptor is **consecutive passable blocks overhead**, starting one block
above the location's integer Y coordinate, in the same x/z column. It stops at the
first non-passable block. Reaching world height is explicit **open sky**, rather than
an arbitrary ceiling distance. An unloaded chunk is explicit **unknown**; inspection
never requests a chunk load. No blocks or native events are modified.

This uses Bukkit's [passability definition](https://purpurmc.org/javadoc/org/bukkit/block/Block.html#isPassable())
and [loaded-chunk query](https://purpurmc.org/javadoc/org/bukkit/World.html#isChunkLoaded(int,int)).
The published Javadoc currently follows 26.3; compilation/tests use this project's
26.2 dependency. Passability is deliberately coarse: an open door still has collision
parts, and this descriptor does not measure exact player-sized clearance.

At reproduction:

1. Restrict observations to the configured same-world horizontal chunk radius and
   recent game-tick window, as before (default 20 chunks / 6000 ticks).
2. Exclude evidence whose producing seed/revision is no longer the item's current
   genome. Failed current attempts remain evidence of the habitat.
3. Among supported roofed contexts, find the smallest absolute difference in observed
   overhead depth from the birth location. Retain all observations tied at that
   distance. Open sky matches open sky; unknown matches only explicit unknown.
4. Rebuild each parent's existing measured utility using only those relevant attempts,
   retaining repeat/no-op penalties. Positive finite means compete with their original
   utility weights. There is no fitness multiplier, diversity quota, rare-habitat bonus,
   minimum founder count or forced gene direction.
5. With no supported positive evidence, create a fresh individual/lineage through the
   existing path. Never import a distant world's parent or borrow success from an
   unsupported habitat to hide failure in the nearest supported context.

The environmental partition is inferred from the available measurements, rather than
fixed A/B niche names or a list of niche adjacency rules. Finite depths can supply
additional contexts without catalog changes. Equidistant depths compete together with
unequal measured chances; they are not allocated equal shares. Ordinary inherited
mutation remains. Extra wild mutation remains held.

`WorldPosition` carries the descriptor independently of ability IDs and equipment
slots. Adding/removing an ability does not require changing reproduction routing.
Future contextual dimensions need measured meaning and independent validation; this
one-dimensional nearest-observation model is not a complete habitat representation.

## Diagnostics and persistence

`/ol debug ecosystem health [player]` keeps the existing effective debug permission
check. Gene/lineage/activity census fields cover all current measured items inside the
radius. Breeding eligibility and concentration instead use the actual conditioned
competition. The command identifies the queried overhead context and explains those
scopes and counts the supporting items/attempts; it has no overall healthy verdict.

Ecology storage is **version 4**. Each sample requires an explicit habitat kind and,
for roofed observations, a nonnegative integer depth. Old/missing/malformed context
fails closed. No migration or silent unknown-context default is supplied: there is no
player data. Existing genotype-revision evidence and item-profile formats are retained.

## Validation protocol

`HabitatCompetitionTest` checks conditional raw weights, nearest-depth ties, failures
versus success elsewhere, unknown/sky separation, locality, expiry, old revision
exclusion, restart integrity, read-only diagnostics and loaded-column sampling.

`HabitatCompetitionBenchmarkTest` pairs the #419 eight-seed fixture with measured
habitat descriptors: open site depth 20, low site depth 0. These are fixture inputs,
not tuned boundaries or prescribed gene values. The response model remains exactly
20 / 0.0784 blocks of headroom. The two sites remain 160 or 2048 blocks apart, and
the radius remains 20 chunks. All existing inheritance, mutation, revision transitions,
codecs, evidence expiry and measured utility execute.

The unconditioned control and new experiment use identical seeds and fixed cohorts.
The control's raw parentage must reproduce archived #419 results. Neutral controls
sample uniformly among the relevant eligible parents using the real sampler; their
reported probabilities remain the nominal measured weights, not the neutral mapping.
Outputs include every parent/child genotype, local response/recovery, founder ancestry,
variance and cross-site parentage. No distribution threshold is imposed.

## Modeled findings

Mean last-round low-site results across eight seed pairs:

| Scenario | Prior recovery ticks | Conditioned recovery ticks | Prior local utility | Conditioned local utility |
|---|---:|---:|---:|---:|
| Measured, overlapping | 63.683875 | 37.035250 | 0.035531 | 0.050155 |
| Measured, separate | 37.035250 | 37.035250 | 0.050155 | 0.050155 |
| Neutral, overlapping | 58.004125 | 59.742250 | 0.038391 | 0.037894 |
| Neutral, separate | 59.742250 | 59.742250 | 0.037894 | 0.037894 |

The modeled benefit stays **0.0784 blocks**. Measured overlapping recovery improves
in all eight pairs; separate-control rows and ancestry reproduce exactly. The measured
overlap now reproduces the separated result because the environments no longer share
reproductive evidence. It is an expected effect of this partitioning mechanism, not
evidence that niches emerged naturally. Neutral recovery does not improve, so the
measured tradeoff selection, rather than separation alone, matters in this fixture.

Measured overlapping founder Simpson falls from **0.643555 to 0.496582** while
local utility improves. Last-round cross-site parentage falls from 30.25/32 to 0/32.
Those are observations, not enforced acceptance thresholds. The unconditioned control
reproduces all **24,576** archived #419 parentage records exactly. Neither control
establishes real gameplay usefulness or long-term ecosystem health.

## Limits

This measures overhead structure, not crops, social opportunities, biome ecology,
player demand, exact collision clearance or obstacle completion. Different conditions
with the same descriptor still mix. An unseen roofed depth uses nearest observed
depths, which is extrapolation rather than proof of suitability. Sparse observations
can define a small competition, including a single item; support must be inspected
rather than treated as a reliable health verdict. Habitat changes can alter which
samples compete, but do not rewrite their captured producing context or genes.

One-column capture ignores neighboring collision shapes and player height/pose, and
uses the event's location: a clicked block, loot source, enchant block or player's
position. Those origins can have different overhead descriptors even in the same room.
It costs at most the remaining world height in read-only block checks per capture;
this slice adds no cache that could silently retain a changed ceiling. Native server
geometry, practical overhead and multi-plugin combinations require separate validation.

Finite-cohort ancestry can contract under either control. Losing founders is not by
itself a defect if useful performance improves, and retaining more founders does not
prove a healthy ecosystem. This slice does not establish open-ended niches, natural
lineage extinction or all four emergence criteria. Existing macro ecology aids and
fixed trait-specific environmental transforms remain separate work.

Core remains standalone; no optional plugin dependency, permission bypass, movement
write, event cancellation, XP award or AuraSkills XP interception is introduced.
