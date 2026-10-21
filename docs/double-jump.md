# Native double jump

Double Jump is an optional equipped ability for Minecraft 26.2 / Java 25.
It uses `PlayerInputEvent` and the shared ability catalog; PacketEvents is not required.
It reacts to a new jump press while airborne, after a grounded frame has been observed.
Holding the key is not another press. Walking off an edge can also use the available charge.

```yaml
abilities:
  double-jump:
    enabled: false
    upward-velocity: 0.42
    cooldown-ticks: 60
```

Enable it and restart to include it in live selection. Disabled entries occupy no live
selection slots. The complete offline catalog still includes the template. Reloading can
stop execution immediately, but rebuilding the boot-time live catalog requires restart.
The strength/cooldown values are configurable implementation defaults, previously proposed
but not separately confirmed by the owner. Nonfinite/nonpositive strength and negative
cooldown are rejected without replacing the runtime settings snapshot.

## Input, identity and ownership

`mobility.double_jump` is registered once, with `ON_AIR_JUMP` and `MOVEMENT_ECHO` metadata.
The equipment service resolves an eligible main-hand, off-hand or armor contributor through
its ordinary catalog, effective permissions and captured per-item key/seed. It checks that
binding again after scheduling and after protection callbacks. A swap, reseed, world change,
death, disconnect or permission denial cannot transfer the input to another item.

The charge and game-tick cooldown belong to the player. Several equipped copies give one
extra jump, and swapping copies does not reset the airborne charge. A landing restores the
charge but does not bypass cooldown. A pre-impulse veto does not spend the charge; a distinct later
press can retry. A native velocity veto occurs after submission, so its cooldown is retained. Teleports and invalid movement states require a new grounded frame.

The effect changes only vertical velocity to the configured value and preserves horizontal
velocity. It declines to replace an already stronger upward launch or a velocity changed
by another callback. It never grants flight, sets flight speed, resets fall distance,
changes gravity, replaces potion effects or changes another plugin's configuration.

It declines while flying, allowed to fly, gliding, riding, swimming/in water or lava,
climbing, riptiding, sleeping, levitating, without gravity, dead, offline, or outside
survival/adventure. Wearing elytra reserves jump input for ordinary gliding even before
`isGliding()` becomes true; other armor/passive abilities continue to work.

## Protection and measurement

`ArtifactAirJumpEvent` exposes the player, artifact key, ability ID and a copy of the
proposed velocity. It is cancellable and runs before the velocity write. The effect
revalidates equipment, permissions and movement state afterward. Original move, input,
teleport, flight and velocity events are not replaced or uncancelled. Cancelled or redirected
movement invalidates the attempt; native velocity cancellation/modification earns no success.

Submitting velocity returns `PENDING`, a generic execution status excluded from utility
outcome history. It earns no reputation or reproductive evidence. A short receipt waits for
an accepted matching native velocity event and more than 0.02 blocks of actual upward
position change. It then records one completed observation through the shared feedback path;
no qualifying motion within six game ticks records a measured no-op. Invalidated or no-longer
bound identities discard the receipt. Idle frames generate no ability outcomes.

This measures the first observed motion response after an accepted impulse, not proof of
player benefit or strict causal isolation from every other movement source. Later corrections
beyond the observation window are outside the measurement. Permission/equipment invalidation
can discard credit for a physical effect whose confirmation was still pending.

Paper can retry a cancelled native velocity event on later ticks while its motion flag
remains set. A protection policy must keep denying that blocked action, not merely deny its
first high-velocity event. ObtuseLoot observes those events and never uncancels or re-emits them.

## Integrations

The existing global and per-ability permission gates apply, including explicit OP denials:
`obtuseloot.abilities` and `obtuseloot.ability.mobility.double_jump`. PlaceholderAPI's slot
`abilities` field includes eligible Double Jump definitions. Its `passives` field remains
for installed passive leases, rather than input-triggered movement.

Standalone operation and all existing optional adapters remain. No AuraSkills XP event,
mana, stat, placed-block tracking or extra XP award is introduced. Actual AuraSkills,
claim-plugin and external-anticheat compatibility still needs the intended plugin combination.
The effect stays disabled by default. See [plugin compatibility](plugin-compatibility.md).

## Verification

The owner selected a disposable local Purpur 26.2 server. Both Java and protocol-client
execution use official Docker images. Server data is temporary; the offline test listener
binds only `127.0.0.1:25598`, with RCON/query disabled. Nothing was deployed to an existing server.

The fixtures in [server-tests/double-jump](../server-tests/double-jump/README.md) are excluded
from the production JAR. The protocol client is 26.1 through official ViaVersion/ViaBackwards
5.12.1 because the reviewed client library lacks 26.2 support. This does not certify a
graphical native 26.2 client or older client versions.

Final focused run: **58 tests in six classes**, zero failures/errors/skips, Maven exit 0.
The pending-history regression failed before the fix and passed afterward. Standalone boot
of the corrected build passed with every optional integration absent and a clean exit.

All **eight** Docker server/protocol cases passed. On the platform at Y=64, the ordinary
jump peaked at Y=65.2522 and the extra jump at Y=66.5014. Two copies and a third press
still produced one impulse, one completed attempt and one success. Both veto policies,
permission denial, deferred swap, foreign flight and elytra reservation produced zero
successes. These fixtures are controlled policies, not the actual production integrations.

A fresh boot logged the existing missing-dashboard-data warning; the core still enabled,
completed its checks and shut down cleanly. The initial disposable flat-world configuration
error was corrected before the movement run. Those logs are retained with the evidence.

Final full `clean package`: **504 tests across 97 classes**, zero failures/errors/skips,
Maven exit 0, **20:29**. All **512** source/resource/build-file hashes matched the frozen
manifest. All **556** production class/resource entries in the final JAR matched the
server-tested build; ZIP timestamps differ. Java 25 bytecode and API 26.2 metadata were
verified. Optional API classes and the test probe are excluded from the production JAR.
The five server fixture files also match their tested manifest.
