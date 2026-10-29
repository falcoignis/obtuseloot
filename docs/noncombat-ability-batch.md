# Four implemented utility abilities

Owner request, 2026-10-29: implement more abilities. This batch follows the existing
noncombat utility/exploration priority and uses the catalog's live-effect and
performance-model hooks. It builds on the replay fixture in PR #434; that PR remains
an owner-reviewed prerequisite, not an authorization to merge either change.

| Ability | Actual effect and input | Inherited benefit versus recovery |
|---|---|---|
| Footprint Memory | With the item in the interacting hand, sneak-right-click a block to remember your current position. Right-click a block normally to read a same-world return bearing, horizontal distance and elevation difference. | Mobility affinity increases recall range and committed recovery. |
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

All four effects use the item's existing shared game-tick recovery. A delivery
reservation blocks same-item reentrancy even at zero configured recovery. A failed
delivery restores owned state/recovery without overwriting unrelated callback changes.
Producing genome revisions stay attached to results if evolution occurs during
delivery; new untested genes cannot inherit the old revision's fitness.

Countdown updates alone keep the same weather/animal observation identity, retaining
repeat-use penalties. Real condition changes can create a different observation.
Recorded benefits are committed waypoint operations or delivered applicable facts;
delivery is **not proof of player usefulness**. No selection weights, diversity quotas,
fitness coefficients or wild-mutation settings are changed.

Walking sensing now recognizes actual chunk crossings, including ordinary sub-block
steps and negative coordinates. It previously required a four-block displacement in
one move event. Destination chunks must already be loaded. Deferred work retains the move event and rechecks cancellation/world;
cancelled work releases its pending chunk marker. Entity input uses the general
`PlayerInteractEntityEvent`, including its more-specific subclass, with existing
per-input duplicate suppression. Ordinary block right-clicks route the waypoint
gesture while preserving the previous door/campfire source labels and vanilla input.

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

Focused checks: **109 tests, zero failures/errors/skips**. Stateful Bukkit API doubles
exercise actual handlers, shared cooldowns, gene-dependent facts/range and recovery,
denials, visibility, failed-delivery rollback, reentrancy, profile corruption, trades,
new offspring, revision attribution and positive local reproductive evidence.
Existing equipment, integration, harvest, Material Insight and persistence checks pass.

The target is the pinned Purpur `26.2.build.2633-stable` API. Current online Bukkit
documentation has advanced to 26.3; the target JAR was inspected to verify its
`GameRule.DO_WEATHER_CYCLE` and animal/redstone interfaces. The weather test replaces
only the registry-backed game-rule read because a plain API-double JVM lacks Paper's
RegistryAccess bootstrap. Other world data/reporting remains exercised normally.
No new native server/client or actual protection/skill-plugin combination was tested.
Before deployment, validate gestures, equipment changes, denied claims/entities,
walking/chunk transitions, default weather-rule lookup and waypoint restart/trade
behavior on the owner-selected disposable server. Preserve normal AuraSkills XP.

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

## Results

Pending registered Actions execution. Owner review/merge and live deployment remain
separate; neither has been authorized by this implementation request.
