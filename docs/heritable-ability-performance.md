# Heritable ability performance

The owner requires inherited genes to change actual ability performance and tradeoffs.
This first slice expresses the existing item genome through a catalog-owned performance
model. It does not add a second genome, random performance rolls, or selection weights.

## First expression: Double Jump

`AbilityRegistration.withPerformanceModel` declares expression next to the live handler.
Unchanged registrations use `AbilityPerformanceModel.FIXED`; adding or removing a model
requires no timing, feedback, permission or inheritance switch on an ability ID. The same
registration builders preserve the model for input effects and passive implementations.
Fleetfoot still uses its existing configured speed and strongest/additive policy: its own
benefit/cost expression is separate future work.

Double Jump expresses `KINETIC_BIAS` (g, between 0 and 1):

- Output multiplier: `1 + (2g - 1) * 0.25`.
- Recovery multiplier: `1 + (2g - 1) * 0.50`.
- Velocity is configured baseline times output multiplier; recovery is configured baseline
  times recovery multiplier, rounded upward to whole game ticks.

With existing baselines 0.42 and 60 ticks, g=0 yields 0.315 / 30 ticks, g=0.5 yields
0.42 / 60 ticks, and g=1 yields 0.525 / 90 ticks. These spreads are implementation defaults
in the catalog, not separately owner-approved balance values or diversity targets. Configured
velocity and cooldown remain the baseline controls. Setting baseline recovery to zero also
removes the recovery cost; this is an operator choice, not the default tradeoff.

Higher output costs more availability. There is no universally rewarded high-gene bonus.
Birth applies ordinary inherited mutation before storing the child's genome; the extra
wild mutation roll remains held. The model uses the item's persisted expressed genes,
including inherited values, rather than reconstructing a genome from its seed. It rolls
no randomness. The durable-evolution follow-up commits ordinary mutation, experience feedback
and latent activation at explicit state transitions. Selection and real performance now read
that same stored genome; see `durable-genome-evolution.md`.

## Physical ownership and integrations

The activation captures one immutable genome reference, one settings snapshot and the
expressed recovery before protection callbacks. The cancellable `ArtifactAirJumpEvent`
exposes the proposed velocity and `getRecoveryTicks()` for integration policies. Callback
changes to item identity, genome, permission, movement eligibility or current velocity veto
submission. A settings reload cannot shorten a cost already captured for that impulse.

One player charge and deadline still apply across copies and swaps. The existing slot
priority chooses the contributor; the effect does not combine the most powerful output
from one copy with the cheapest recovery from another. A new genome cannot shorten a
recovery already spent. No flight, fall-distance, potion, XP, AuraSkills stat/mana or optional
plugin state is written. Existing LuckPerms checks and PlaceholderAPI snapshots remain.
PacketEvents remains unnecessary; core initialization remains standalone.

## Measured response and cost

The motion receipt now samples peak upward displacement during the existing six-game-tick
window instead of completing on the first small rise. It requires an accepted matching
native velocity event and more than 0.02 blocks of observed rise. A native veto or no
qualifying movement produces a measured no-op. A changed genome or equipment identity
before confirmation discards the receipt. Submission is still `PENDING` and earns no credit.

`AbilityPerformanceMeasurement` carries actual response and committed recovery ticks.
For Double Jump the response unit is upward blocks in that short window. The generic utility
profile adjusts positive classified utility by `response / (1 + recoveryTicks / 20)`;
zero/negative failure penalties remain unchanged. This expresses response per recovery
second with a one-second action term. It does not use predicted strength as evidence.
Effects without this measurement retain their previous utility behavior. Cross-ability
measurement units and additional costs must be designed for each later effect.

Both item utility history and local reproductive fitness consume this same measurement.
The local pool still recomputes its own fitness from spatially eligible measured attempts,
without importing artificial niche bonuses from the older utility delta. Evidence serializes
response/recovery under local ecology format version 2 and refuses invalid or partial
measurements. No migration is added: the owner reports no existing player data.
Telemetry records `performance_benefit`, `recovery_ticks` and the existing `utility_delta`.

This measures a short physical response, not obstacle completion or player usefulness.
It does not strictly isolate displacement caused by every other physics source, and a later
correction outside the window is not observed. Fitness comparisons under controlled response
fixtures are mechanism tests, not proof of natural niche emergence or long-term balance.
Local fitness still summarizes recent per-item attempts, which can span successive genomes
after an item evolves; this is not an isolated success trial for every genetic revision.

## Validation

Final full Docker `clean package` passed **522 tests across 100 classes**, zero failures,
errors or skips, Maven exit 0, in **33:09**. All 518 executable input hashes matched the
frozen source; all 559 production class/resource entries matched the corrected server-tested
build. Generated tracked reports were restored. See canonical dated status for publication. Corrected
focused Docker verification passed 79 tests across eight classes. A new regression first
failed (expected first-tick rise 0.1, got 0.0); sampling now starts on tick one and still
waits through the existing six-tick receipt window. Unit/integration coverage includes endpoint expression, fixed-model defaults, latent activation, deterministic
mutation/reproduction, persisted inheritance, item utility propagation, local cost-sensitive
fitness and restart, permission/veto/swap ownership, shared recovery and snapshot consistency.
The corrected build passed standalone boot without optional integrations and all twelve
real Purpur 26.2 protocol cases, including low/high expressed motion, shared recovery,
low-ceiling responses and the existing denial cases. Both ceiling traits recorded 0.0784
blocks of rise while retaining 30/90-tick costs; the observed item utility favored the
cheaper response there. Open-ground low/high rises were approximately 0.759/1.754 blocks
in the receipt window. These are controlled fixture observations, not a population study.

The initial protocol assertion incorrectly required exactly one native velocity callback;
Paper can retry a cancelled event. The corrected fixture requires one artifact proposal,
at least one native veto callback, and zero received impulses/successes. A subsequent
ceiling check exposed the first-tick sampling gap above. Both failed runs are retained
with the review evidence. The first focused run also had an incorrect assertion that a
suppressed classification meant no record at all; it now checks absence of measured credit.

Java and client execution both use official Docker images. The protocol client is 26.1
through SHA-256-verified ViaVersion/ViaBackwards 5.12.1; this does not certify a graphical
native 26.2 client or actual AuraSkills/claims/anticheat combinations. Purpur matched its
published MD5 (no published SHA-256 at that endpoint). The existing missing-dashboard-data
startup warning remains recorded. Live deployment is not authorized and Double Jump
remains disabled by default pending the intended plugin combination tests.
