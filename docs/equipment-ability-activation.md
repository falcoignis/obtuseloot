# Equipment and ability activation design

Owner requirements, 2026-10-21: support passive abilities, main-hand and off-hand
use, secondary click, armor and elytra. Speed increases and double jump are example
abilities. Support PlaceholderAPI and LuckPerms. PacketEvents is optional and should
be used only when a required feature cannot work adequately through the server API.

This is the design for the next implementation slice, not a claim that these features
are implemented. The local-inheritance batch still activates one main-hand artifact.
Its per-item profiles, genomes, reputation and ancestry are reusable across all slots;
the current single-active-item binding and player-indexed subscriptions must change
before multiple equipped artifacts can execute safely.

## Equipment is a set of individuals

Reconcile the player's equipped artifacts into bindings for main hand, off hand,
head, chest, legs and feet. Elytra occupies the chest slot; it is not another
simultaneous armor slot. Bindings carry the item storage key, seed and equipment slot.
Two items held by one player never share reputation, history or outcome windows.
One item identity cannot receive duplicate credit through two slot bindings.

Inventory contents are not equipped artifacts. Missing or corrupt tracked profiles
remain unavailable under the existing load guard; never replace them with a player
profile. Resolving equipment must not create items, change ancestry or invent success.
Equipping, removing or trading an item must not reset its identity or genome.

Reconcile on equipment and selected-slot changes and on join/respawn, with a bounded
game-tick reconciliation as a safety net. Include drag, shift click, hand swap,
breakage, drop, death, world changes and disconnect. Compare identities, not just
material names. Subscription refresh and pending-outcome flush must target the
affected individual, rather than replacing every equipped item's state.

## One catalog, explicit activation requirements

Extend the existing shared ability registration with activation metadata: eligible
equipment slots, activation mode and event/input requirements. Modes distinguish
passive state, passive event reactions and deliberate actions. Existing trigger
metadata continues to describe the event; a right click is input, not a substitute
for an ecological outcome. New abilities should need one registration and a handler,
without special cases in the scheduler or reproduction pool.

Dispatch context must identify the contributing item, slot, hand when applicable,
action and relevant world position. An interaction from the off hand must never be
credited to whichever item is currently in the main hand. Normalize server events
so two hand callbacks do not accidentally execute the same logical action twice.
If deliberate abilities may run for both hands, that must be explicit registration
behavior rather than an accidental consequence of duplicate callbacks.

Delayed work captures the original item identity and verifies it is still eligible
before executing. Switching an item between click and execution must not transfer
the old action or credit to the new item. Final cancellation/protection decisions,
item-use denial and vanilla interactions must be respected. Input handling must not
consume ordinary chest, door or item actions merely because an artifact is equipped.

## Passive effect ownership and measured results

Passive effects need an idempotent apply/update/remove lifecycle. Keep an owned
effect ledger keyed by player, item identity and ability registration. Reconciliation
does not repeatedly add the same speed modifier. Removing equipment, disabling an
ability or losing permission releases only the modifiers and resources this system
owns. Do not overwrite another plugin's movement state or remove its effects.

Use the existing game-time scheduler to reconcile registered passives; avoid one
task per item/ability and avoid rebuilding all profiles every tick. Attribute measured
benefits/costs to the responsible item. Merely being equipped, applying a modifier,
checking a condition or ticking must not generate successful outcomes each tick.
Shared outcomes need an explicit contribution rule before awarding multiple items
the full credit. Keep reproduction generic and item-scoped.

For speed and double jump, settle effect strength, stacking, cooldown/charges and
movement-state rules before enabling them. Double jump must coexist with creative
flight, spectator mode, elytra gliding and server movement/protection controls; do not
grant unrestricted flight as an implementation shortcut. PacketEvents is a possible
input adapter only after a server-API limitation is reproduced, not a requirement
for the core equipment model.

## Required integration support

PlaceholderAPI and LuckPerms are supported integration targets, not discretionary
feature ideas. Their implementation is pending; this batch adds no expansion or
integration dependency. Whether either plugin must be installed for the core plugin
to enable is not decided by the owner's requirement to support it.

- PlaceholderAPI: provide read-only, slot-qualified artifact and ability values.
  Querying display values must not activate gear, load or mutate profiles, award
  outcomes or select parents. Define unavailable/empty/offline results. Serve safe
  snapshots so asynchronous placeholder consumers do not read mutable Bukkit state.
- LuckPerms: honor effective permissions for commands and registered ability access,
  including explicit denials and context changes. Use the server permission bridge
  where sufficient; use the LuckPerms API only for features that require it. Permission
  loss must reconcile active passive effects. Do not write groups or grants, grant
  privileges through placeholder strings, or bypass checks for OPs automatically.
- PacketEvents: keep any packet handling in an adapter feeding the same activation
  context. Marshal execution to the server thread, validate input against current
  equipment/world state and retain protection checks. Core evolution and persistence
  must not depend on packet classes.

## Implementation acceptance checks

Before claiming support, test simultaneous independent main/off-hand and worn-item
state, elytra chest replacement, equipment change during deferred work, duplicate
hand callbacks, denied interactions, persistent item identity across trades/restarts,
and per-item measured credit. Test repeated reconciliation, removal, death, disconnect
and permission loss without modifier leakage. Exercise speed stacking and double-jump
movement-state rules once those effects are specified. Verify PlaceholderAPI lifecycle,
slot results and read-only asynchronous queries, plus LuckPerms allow/deny/context
changes. Run actual server compatibility checks before deployment; controlled API
doubles alone do not establish movement or protection-plugin compatibility.
