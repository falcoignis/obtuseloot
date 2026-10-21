# Equipment abilities and integrations

The equipment follow-up implements independent activation for main hand, off hand,
head, chest, legs and feet. Elytra uses the chest slot. Double jump is the next
movement slice; it is not implemented here. This extends the per-item inheritance
foundation without changing ecology weights, progression gates or wild mutation.

## Equipment identity and evolution

`EquipmentAbilityService` reconciles actual equipped items, never the rest of the
inventory. Event callbacks reconcile after inventory changes, with a shared five-game-tick
safety pass. Join, slot changes, swaps, clicks/drags, breakage, drops, world changes,
death, respawn and disconnect participate in the lifecycle. Only tracked item profiles
are resolved; unreadable slots remain unavailable while healthy gear keeps working.

Bindings capture storage key and seed. Duplicate bindings for one identity are rejected,
including simultaneous claims by different holders. Normal trades work after the old
holder releases the item. A traded item retains its genome, reputation and ancestry.
Equipped profiles are pinned in the existing cache; the memory bound does not delete gear.

Subscriptions and measured-outcome windows are now keyed by holder **and item**, so
one item's outcome cannot overwrite another equipped item's window. Unequipping flushes
that individual's earned state and preserves retryable saves. Main-hand targeting remains
for the existing administrative commands and combat reputation paths. Armor does not
receive copies of main-hand kill credit. Scheduled reputation decay and instability cleanup
include equipped profiles without processing a shared instance twice.

## Catalog and input

A registration owns its metadata, activation policy, eligible slots and either a live
event handler or a passive handler. Add/remove an ability in the shared catalog; no
ability-specific branch is needed in equipment scheduling, outcome windows or reproduction.
Catalog code changes require restart. Indexed and unindexed live dispatch reuse the same
selected equipment profile; changing dispatch mode does not trigger another selection roll. Unsupported procedural variants retain their executable
base definition as before.

Modes are deliberate action, event reaction and passive state. A secondary-click input
records the actual hand. Delayed interaction/harvest/sensing work captures equipment
identity before scheduling and validates it again before execution, including world changes.
A swap, replacement or reseed cannot transfer old action credit to the newly held item.

For a secondary action, duplicate callbacks for the same ability, game tick and target
share an execution record across hands. Only a handler that actually executes claims it;
a denied main-hand action does not consume the off-hand opportunity. Block targets include
all three coordinates. This deduplicates callbacks rather than awarding every equipped
copy full credit for one world change. Distinct registered abilities can still react.

Dispatch respects final event cancellation and interaction/item-use denial. It does not
uncancel an event, consume ordinary input or replace vanilla break/harvest events.
Gentle Harvest still fires its protection placement event and pays one existing planting
item. Material Insight can now execute from an explicitly bound off-hand artifact.

## Owned passive effects

A passive handler proposes a contribution with a stacking group, strength, revision and
an apply callback returning a release lease. The controller applies a contribution once,
updates it only when its identity or behavior revision changes, and removes only its own
resources on removal, permission loss, death, world transition or shutdown. Failed removal
retains ownership and retries before installing a replacement. Handlers must include all
behavior-changing inputs in their revision and undo partial setup if applying throws.

Fleetfoot is the first real passive: eligible equipped gear adds a walking movement-speed
modifier. It uses a transient, namespaced additive-base attribute modifier and never writes
base speed, flight flags, potion effects or another plugin's modifier. It is inactive while
dead, flying, gliding, riding, or outside survival/adventure. No double-jump or flight grant
is included.

```yaml
abilities:
  passive-speed:
    bonus: 0.05
    stacking: strongest # strongest or additive
```

The owner approved the 5% default, strongest-copy default, and an additive option.
`strongest` applies one contributor per group. `additive` applies the sum of the equipped
contributions against base movement speed: two 5% artifact bonuses give +10%, never
`1.05 * 1.05`. Other plugins retain control of their own speed changes; this option does
not redefine their calculations. Zero disables the bonus. Invalid modes, negative amounts
and nonfinite amounts are rejected without replacing the runtime settings snapshot.

Merely wearing an item, checking its conditions, or installing a modifier generates no
successful outcome, reputation or local reproductive evidence. Measurement of passive
benefit remains future work; this slice does not invent fitness from idle ticks.

## Standalone operation and supported integrations

The core has no required plugin dependency. PlaceholderAPI, LuckPerms and AuraSkills are
soft dependencies, detected at normal boot. PacketEvents is not required or installed.
Integration code remains outside core evolution and persistence. Other plugins' data,
permissions and configuration are never rewritten by these hooks.

- **PlaceholderAPI:** an internal expansion registers when present, persists through a PAPI
  expansion reload, and unregisters on disable. Requests read immutable snapshots, including
  from asynchronous consumers; they cannot load profiles, activate gear or award outcomes.
  An incompatible/failed optional expansion is logged without disabling the core.
- **LuckPerms:** effective server permission checks gate actions and passives. The global
  node is `obtuseloot.abilities`; individual nodes are `obtuseloot.ability.<catalog-id>`,
  for example `obtuseloot.ability.mobility.fleetfoot`. Nodes default to allowed, with
  explicit denials honored even for OPs. Changed permission contexts reconcile through
  the next equipment pass, at most five game ticks later. No users/groups/grants are written.
- **AuraSkills:** boot detection and ordinary-event coexistence are included. This slice
  never listens to/cancels XP gain events, adds skill XP, changes mana, writes stats or
  removes placed-block tracking. Its skill-data API is not queried, so it cannot block
  standalone startup or read AuraSkills before readiness. This is a coexistence baseline,
  not a full skills/stat integration. See [the compatibility review](plugin-compatibility.md).

Use `%obtuseloot_<slot>_<field>%`. Slots: `mainhand`, `offhand`, `head`, `chest`, `legs`,
`feet`. Fields: `present`, `name`, `key`, `seed`, `lineage`, `generation`, `evolution`,
`reputation`, `abilities`, `passives`. Ability names reflect eligible permitted definitions;
`passives` lists active catalog IDs. `%obtuseloot_equipped_count%` gives the snapshot count.
`%obtuseloot_integration_placeholderapi%`, `%obtuseloot_integration_luckperms%` and
`%obtuseloot_integration_auraskills%` report **boot detection**, not compatibility certification.
Missing/offline slots return empty text, numeric zero, or `false`; unknown fields return
`null` so PlaceholderAPI can leave them unresolved. Snapshots may lag by five ticks.

## Verification boundary

Focused verification covers six independent slots, chest/elytra replacement, duplicate
identities, deferred swaps/reseeds, off-hand input, duplicate callbacks, cancellation,
permission denial/context changes, independent evolution, passive removal/retry, additive
speed, asynchronous read-only placeholders and optional APIs deliberately absent from a
class loader. Final full `clean package`: 493 tests across 96 classes, zero failures/errors/skips,
Maven exit 0, 29:46. Focused verification: 72 tests across 11 classes passed. All 505
source/resource/build-file hashes matched the verified manifest. Packaged plugin metadata
contains only soft plugin dependencies, and optional API classes are not bundled.

These tests use controlled API doubles and actual Bukkit event/attribute objects. No live
server was started. Full plugin startup on a real server, live movement/anticheat behavior,
AuraSkills equipment restoration/global requirement interactions and simultaneous crop
replanting still need compatibility testing. Do not claim full cross-plugin certification
from boot detection or these unit/integration tests.
