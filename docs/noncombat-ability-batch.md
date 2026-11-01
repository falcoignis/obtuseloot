# Four implemented utility abilities

Owner request, 2026-10-29: implement more abilities. This batch follows the existing
noncombat utility/exploration priority and uses the catalog's live-effect and
performance-model hooks. It builds on the replay fixture in PR #434; that PR remains
an owner-reviewed prerequisite, not an authorization to merge either change.

| Ability | Actual effect and input | Inherited benefit versus recovery |
|---|---|---|
| Footprint Memory | With the item in the interacting hand, sneak-right-click a block top to remember your current position. Sneak-right-click a side face to read a same-world return bearing, horizontal distance and elevation difference. | Mobility affinity increases recall range and committed recovery. |
| Weather Omen | While equipped, crossing a chunk boundary reports the holder world's actual weather state and applicable server timers. Paused/unknown natural weather cycling does not produce a timed forecast. | Survival instinct increases information depth and recovery. |
| Herd Instinct | Right-click the animal being inspected to read its actual type, health, age/growth and breeding condition. | Resonance increases information depth and recovery. |
| Redstone Sympathy | Right-click an inspected circuit block for applicable power, delay, lock, comparator mode, piston/hopper state, facing or wire-connection data. | Precision affinity increases information depth and recovery. |

The default implemented/selectable catalog grows from three to seven. Double Jump
retains its existing opt-in setting. New effects have no combat damage, item/XP
creation, inventory edits, flight/attribute ownership or optional-plugin dependency.
They preserve final event denials and effective root/per-ability permissions, with no
OP bypass. Diagnostics do not inspect container inventories or neighboring machines;
an unloaded clicked chunk is rejected before reading block data. Animal inspection
rejects invisible, invalid, dead or other-world targets. Waypoint reads load no worlds
or chunks and never teleport the holder.

## State, timing and compatibility

Waypoints belong to the physical item. **Giving the item away shares its saved return
point with its recipient.** They persist through save/load, trades and same-item
genome replacement. New offspring have no inherited waypoint, recovery or fitness.
Unknown/removed ability IDs retain their saved waypoint just as recovery state does.

The shared SQL/YAML per-item codec moves from format 4 to **5** and requires complete
validated waypoint payloads. It rejects old/incomplete/nonfinite data instead of
silently losing an anchor. No migration is included under the existing owner decision
that there is no real player data. Deploy readers together; older builds cannot read
new profiles. No live server or existing operator files are changed here.

All four effects use the item's existing shared game-tick recovery. Multiple eligible
effects competing for one input can be limited by dispatch order and that recovery;
a distinct birth ability set does not guarantee a useful combination. A delivery
reservation blocks same-item reentrancy even at zero configured recovery. A failed
delivery restores owned state/recovery without overwriting unrelated callback changes.
Producing genome revisions stay attached to results if evolution occurs during
delivery; new untested genes cannot inherit the old revision's fitness.

Countdown updates alone keep the same weather/animal observation identity, retaining
repeat-use penalties. Real condition changes can create a different observation.
Weather readings describe world-wide flags and timers, not biome-specific precipitation
or a guarantee against later world/plugin changes. Recorded benefits are committed
waypoint operations or delivered applicable facts;
delivery is **not proof of player usefulness**. No selection weights, diversity quotas,
fitness coefficients or wild-mutation settings are changed.

Walking sensing now recognizes actual chunk crossings, including ordinary sub-block
steps and negative coordinates. It previously required a four-block displacement in
one move event. Destination chunks must already be loaded. Deferred work retains the move event and rechecks cancellation/world;
cancelled work releases its pending chunk marker. Entity input uses the general
`PlayerInteractEntityEvent`, including its more-specific subclass, with existing
per-input duplicate suppression. Block right-clicks route the explicit waypoint
gesture; ordinary non-sneaking clicks remain available for inspection while preserving the previous door/campfire source labels and vanilla input.

## Proposed configurable defaults

These are implementation defaults for review, not owner-confirmed balance values.
Configuration is under `abilities.utility.<ability-name>` with `base-output` and
`recovery-ticks`; generic tuning does not enumerate catalog IDs. All four use the
existing trait tradeoff model with 0.5 output/recovery spreads.

| Setting key | Baseline output | Baseline recovery |
|---|---:|---:|
| footprint-memory | 256 blocks of recall range | 60 game ticks |
| weather-omen | 4 applicable facts | 200 game ticks |
| herd-instinct | 4 applicable facts | 80 game ticks |
| redstone-sympathy | 4 applicable facts | 80 game ticks |

More detail is delivered only when available. A high information gene cannot invent
extra facts on a sparse target. Partial overrides are allowed; malformed/nonfinite
outputs and negative/fractional recovery are rejected before replacing the active
settings snapshot.

## Validation boundary

Focused checks: **115 tests, zero failures/errors/skips**. Stateful Bukkit API doubles
exercise actual handlers, shared cooldowns, gene-dependent facts/range and recovery,
denials, visibility, failed-delivery rollback, reentrancy, profile corruption, trades,
new offspring, revision attribution and positive local reproductive evidence.
Existing equipment, integration, harvest, Material Insight and persistence checks pass.

The target is the pinned Purpur `26.2.build.2633-stable` API. Current online Bukkit
documentation has advanced to 26.3; the target JAR was inspected to verify its
`GameRule.DO_WEATHER_CYCLE` and animal/redstone interfaces. The weather test replaces
only the registry-backed game-rule read because a plain API-double JVM lacks Paper's
RegistryAccess bootstrap. Other world data/reporting remains exercised normally.
No actual protection/skill-plugin combination was tested. Preserve normal AuraSkills XP.

## Native disposable-server validation

The fixture in [server-tests/utility-abilities](../server-tests/utility-abilities/README.md)
ran against `83d5753` on Purpur `26.2` build 2633 (Java 25), with a controlled 26.1
protocol client through ViaVersion/ViaBackwards 5.12.1. Expectations were registered
before the first run. The final run passed all three stages (boot-only, client, restart)
with every exit zero: **22/22 cases, 18 delivered reports**.

| Ability | Native result |
|---|---|
| Footprint Memory | Server saw sneaking; unsneaked click did nothing. Mark, off-hand click during recovery blocked, recall "west, 200 blocks; 0 blocks up" (gene 1, recovery 90). Gene 0: recall beyond 128 blocks delivered nothing and reserved no recovery; recall at 100 blocks delivered (recovery 30). Waypoint and genes survived restart and a codec round trip. |
| Herd Instinct | One report per click, second click recovering. Facts/recovery 2/40 and 5/120. Adult "breeding ready true"; baby "young; growth in 23988 ticks". Cancelled entity event and ability permission off (OP retained) delivered nothing. |
| Redstone Sympathy | Piston "extended false" (gene 0, recovery 40) and "extended false \| facing up" (gene 1, recovery 120). Stone, cancelled block/item interaction and permission off delivered nothing. |
| Weather Omen | One report per chunk crossing: clear (2 facts, recovery 100), running cycle, forced clear window, storm and thunder (5 facts, recovery 300), paused cycle (3 facts). With permission off the crossing reached the server as a move event and delivered nothing; after restoring the permission on the same item, the next crossing delivered one five-fact report. |

Observations for review:

- Redstone Sympathy reports the block after vanilla's right-click change, because the
  effect runs one tick later. A lever placed unpowered read "powered true", a repeater
  placed at delay 1 read "delay 2 redstone ticks", and an isolated wire placed in its
  default state read all four sides. Inspecting a lever or repeater therefore also
  changes it; comparators were not tested. This is a design question; no production
  behavior was changed.
- The 26.1 client's entity click produced `PlayerInteractAtEntityEvent`, which
  `PlayerInteractEntityEvent` handlers also receive; exactly one report was delivered.
- `GameRule.DO_WEATHER_CYCLE` is deprecated for removal in the 26.2 API but reads
  correctly on build 2633.
- Every start logs a pre-existing dashboard warning for the missing
  `analytics/ecosystem-balance-data.json`; it predates this batch.

Not covered: a two-player hand-over (trade is covered only by the codec round trip),
recovery shared between two abilities on one item, walking-scan rejection of unloaded
destinations, a graphical 26.2 client, LuckPerms/PlaceholderAPI/AuraSkills, claim
plugins and anticheat. No live deployment is authorized.

API references: [weather timers](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/World.html),
[entity interaction hierarchy](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/player/PlayerInteractAtEntityEvent.html),
[redstone block data](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/block/data/type/RedstoneWire.html).
Those rolling pages must be checked against the pinned target API.

## Registered birth comparison, before Actions execution

Compare the three-ability control registry at `3030d74609033d70e8f3a3e217ca9dca9350827a`
with the expanded registry. Both arms retain the new codec and identical current
birth code, separate identity stream and original fixed synthetic observations.
Use study seeds `0x554E495155` and `0x554E495156`, 3,000 births each, and two independent
repeats of every arm/seed combination: **eight measurements, 24,000 births**.

Required controls before interpreting differences:

- Every recorded column reproduces across independent repeats within each arm/seed.
- Every paired column except abilities matches between control and expanded arms,
  including exact identities/parentage, genomes, affinities, names and supplied outcomes.
- Control rows match the corresponding combined/second-seed outputs from replay run
  40831895982, retaining the established baseline.
- The full suite passes. Report ability-set frequencies, distinct selectable IDs,
  matching unordered pairs/probabilities, and the existing other identity metrics.
  Do not impose a minimum reach/share target or tune weights to improve the result.

This is a **birth-profile** comparison. Fixed synthetic observations do not simulate
natural use/selection of the new effects, changing player opportunities, live usefulness
or a healthy emergent ecology. Actual new-effect feedback is checked separately by
the handler/inheritance tests above. No complete-profile uniqueness or global
collision-free claim follows from the sample.

## Input-combination correction before final confirmation

A failing same-item witness showed that an automatic waypoint recall on ordinary
right-click spent the shared recovery before an inspection could run. Waypoint use
now requires the explicit sneak/top-or-side gesture. Catalog-owned input applicability
is checked before attempts and budgets, so unrelated clicks create no waypoint attempt
or biological outcome. Genuine attempted recalls can still fail and retain their
normal measured no-op/failure handling. This changes input routing, not selection weights.
The original study and its rerun precede this correction and are superseded.

## Results

Actions run [40931547378](https://github.com/falcoignis/obtuseloot/actions/runs/40931547378)
on `3cee188de32bd575ad7cc079d8750186c1f51b58` passed all eight measurements and all
eight registered replay/input controls. Each arm/seed repeated every column; paired
non-ability columns and both historical control samples matched exactly. All nine
measurement/comparison ZIPs matched their published SHA-256 digests; local
recomputation reproduced the Actions summary. The study full suite ran 637 tests with one failure, zero errors and ten opt-in skips: an
older catalog test assumed these four effects had fixed performance. Its correction
explicitly checks all six declared inherited models, and the final 115 focused tests pass.
Production code is unchanged; the full-suite recheck runs through the normal PR build.
Consult its checks for the current result.

| Seed | Catalog | Distinct sets | Observed IDs | Largest set | Equal unordered pairs / 4,498,500 | Pair-match probability |
|---|---|---:|---:|---:|---:|---:|
| First | Control | 2 | 3 | 1,726 / 3,000 | 2,299,576 | 51.12% |
| First | Expanded | 17 | 7 | 2,646 / 3,000 | 3,506,193 | 77.94% |
| Second | Control | 3 | 3 | 1,696 / 3,000 | 2,285,613 | 50.81% |
| Second | Expanded | 18 | 7 | 2,667 / 3,000 | 3,561,511 | 79.17% |

The expanded largest set is Fleetfoot + Footprint Memory (88.2% / 88.9%). Catalog
reach increased, but concentration and ability-pair repetition worsened. No weights
were tuned to hide that result. Complete per-set counts are in the run's
`noncombat-catalog-comparison/catalog-summary.json` artifact. Dispatch/recovery and
actual player usefulness remain separate from birth-set reach.

All four samples have 2,988 distinct names, 3,000 genomes and 3,000 affinity vectors.
Full-profile duplicates were absent in control too, so that is not an improvement
attributable to this batch. Expanded name-plus-ability distinct counts are
2,992 / 2,989 versus 2,992 / 2,992 in control. These are finite samples, not universal
uniqueness guarantees or evidence of natural niche formation.

Superseded runs 40924823737 and 40925770963 were deliberately cancelled for the
loaded-chunk guard and reproduced input-combination correction, respectively. They
do not certify the final source. Owner review/merge and live deployment remain
separate; neither has been authorized by this implementation request.
