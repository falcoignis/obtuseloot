# First measured interaction effects

This slice implements two existing templates. It follows the owner's focus on social,
interaction and exploration, with minimal combat effects. It does not add combat effects
or wild mutation.

| Template | Effect | Recorded observation |
| --- | --- | --- |
| Gentle Harvest (`survival.gentle_harvest`) | Replants one mature wheat, carrot, potato, beetroot or nether-wart crop at age zero, consuming one matching planting item from that harvest's actual drops. | One committed replant, or zero if unavailable/denied. |
| Material Insight (`precision.material_insight`) | Sends the holder the inspected block's material and light level, plus growth stage for ageable blocks. Main hand only. | One delivered report, or zero if unavailable/denied. Delivery does not prove the player found the report useful. |

Effects are registered by template ID, so a localized display name does not change behavior.
Unsupported mechanic/trigger/family variants cannot execute under the mutated identity.
Live selection retains their executable original and records the fallback; directly dispatched
unsupported definitions return an unmeasured no-op. See
[noncombat timing and the ability catalog](noncombat-evolution-timing.md).
Their measurements enter the existing utility profile, niche telemetry and selection-history
snapshot immediately. The utility cache is invalidated after each observation; autosave is
no longer required before selection can read the new history. Existing fitness and selection
rules remain unchanged.

The follow-up [noncombat evolution schedule](noncombat-evolution-timing.md) evaluates normal
stages and refreshes subscriptions after measured effects, on a configurable game-tick
window. Join, kills and explicit command refreshes remain available. Progression gates
remain unchanged.

`AbilityExecutionResult.observedAmount` is nullable. A live observation has an amount
(including zero); a legacy offline classification or unimplemented effect has no amount. Ability telemetry includes
`measurement_basis` (`observed`, `classified`, or `not_observed`) and `observed_amount`
when present. Live dispatches of unfinished effects return `NO_OP`; offline classification
remains available to the existing simulation tests. Historical classification-based reports
are not evidence of gameplay success.

## Event and protection handling

Harvest dispatch waits until the `BlockDropItemEvent` has completed, retaining its original
block-state snapshot. Cancelled breaks and breaks with drops disabled do not produce this
event. The executor rechecks cancellation, survival mode, holder/world, mature crop, empty
replacement block, correct soil, and an available unmodified planting-item drop. It never
creates a seed or consumes a customized planting item.

A provisional age-zero crop is presented through a `BlockPlaceEvent`, including the air
snapshot it replaces and the planting item. Cancellation or `canBuild=false` rolls it back
without payment. The payment is revalidated after callbacks. Rollback preserves unrelated
changes made by another listener. A separate conservative spawn-radius check protects
normal-world spawn for non-operators, including servers where no protection plugin handles
spawn. Unlike vanilla's server-specific exemption rules, this check also applies when the
operator list is empty and in additional normal worlds.

Inspection also waits until interaction listeners finish and rechecks block-use denial.
It reads only the targeted block and changes no blocks or inventories.

The event semantics are documented by the Bukkit API:
[BlockDropItemEvent](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/block/BlockDropItemEvent.html)
and [BlockPlaceEvent](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/block/BlockPlaceEvent.html).

## Validation boundary

`LiveAbilityEffectsTest` uses real Bukkit event classes with stateful API doubles. It checks
all five crops, payment conservation, protection cancellation/build denial, rollback,
late changes and missing drops, spawn radius, game modes, inspection denial/offhand,
indexed/full-scan budget suppression, telemetry, immediate ecology/history updates,
mutation guards, and the effect of observations on fitness and template utility scores.

No server is started or deployed by these tests. The test/deploy destination remains an
owner decision. Before deployment, validate normal play, denied claims, cancelled drops,
spawn protection and seed conservation against the chosen server and its protection plugins.
This slice does not demonstrate emergent evolution, lineage birth/extinction, player benefit
from a report, or ablation resilience. Social and exploration effects remain future work.
