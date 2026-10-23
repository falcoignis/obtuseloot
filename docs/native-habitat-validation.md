# Native habitat capture validation

The owner requested the next step after the #417–#420 merge: exercise habitat
selection on a disposable Purpur 26.2 Docker server. This validation found a real
capture-frame defect, rather than establishing that the first model was ready.

## Native counterexample and correction

Three controlled areas share a world and the configured 20-chunk inheritance
radius: x=0 has a ceiling at Y=67, x=160 at Y=68, and x=240 is open sky. Their
support platforms are at Y=63. Controlled Double Jump items use actual native
input/velocity handling. Vanilla `END_CITY_TREASURE` is assigned to a chest and
unpacked through its native inventory; the resulting `LootGenerateEvent` must
have `isPlugin() == false`. The fixture never fabricates that event.

In the unmodified #420 build, both low-ceiling items produced 0.0784-block measured
rise with 30/90-tick recovery. Their airborne contexts recorded one overhead block;
the chest at Y=64.5 recorded two, matching the neighboring room's airborne context.
The actual stamped equipment inherited from `near-costly`, although same-room
measured parents existed. Its restart preserved that evidence and the genes.
Two focused regressions then failed against unchanged production code: the same
room differed by actor height, and different rooms aliased to the same value.

The correction measures the **vertical span between observed lower and upper
passability boundaries**, excluding the reference cell itself. An actor or loot
source can occupy that cell. The low area now records height 3 at Y=64, 64.5 and
65.1; the neighboring area records 4 at all those elevations. Translating both
boundaries and the observer equally leaves the descriptor unchanged.

Block events reference the space above the target block. Roofed columns without
an observed lower boundary are explicit unknown; unloaded columns remain unknown
and are checked not to load their chunk. Open sky retains its explicit category.
The descriptor is still one coarse column, not exact collision clearance or a
room-wide traversability model. The origin cell's occupancy is deliberately ignored;
neighboring shapes, poses and furniture can still limit physical motion differently.

Current ecology format is **version 5**, using `habitat.column-height-blocks`.
Version 4 and incomplete fields fail closed. There is no migration because no player
data exists. Item/genome profile formats and producing-revision attribution remain.
Parent competition still uses the nearest supported descriptor, current seed/revision,
configured chunk radius/window and unchanged measured utility/repeat penalties.
No gene multiplier, distribution target, diversity quota or extra mutation was added.

## Validation boundaries

`HabitatCaptureFrameTest` covers actor/loot frame invariance, altitude translation,
occupied origin cells, distinguishable neighboring ceilings, unknown missing support
and ground-block reference alignment. The server fixture is under
`server-tests/habitat-selection/`; it is compiled separately and never bundled.

The native protocol uses client 26.1 with official ViaVersion/ViaBackwards 5.12.1
because the pinned protocol library lacks 26.2 support. A separate boot/restart run
has all optional plugins absent. This is a real Purpur 26.2 server with controlled
client physics, not certification of a graphical 26.2 client or every plugin combination.

An initial corrected run selected the right parents and switched to matching evidence
when the chest column's roof was raised, but a fixture assertion incorrectly assumed
equal activation heights. Recorded positions were Y=64.73584064 and 65.1216, leaving
0.46415936 and 0.0784 blocks of headroom. The fixture now checks each measured rise
against its own actual headroom and verifies the inherited impulse/recovery separately.
It compares recovery-only utility ordering only when responses actually match.
The failed assumption and complete first-run evidence are retained as superseded data.

Sampling timings are recorded from 1000 captures per area after 100 warm-up captures,
on the server thread. They are microbenchmarks of these columns at startup, not a
production TPS/latency guarantee or a before/after performance conclusion. Full raw
results, dependency digests, exit codes and server/client logs belong with the reviewed
artifact. Purpur publishes an MD5 for build 2633; a local SHA-256 is additionally
recorded. Both protocol adapters are checked against GitHub release SHA-256 digests.

The existing paired modeled generation/ancestry controls should reproduce unchanged:
only native environmental capture and its durable units changed. Genuine emergence,
whole-ecosystem health and multiple heritable ability families remain separate claims.
No new ability effect, permission bypass, XP interception, optional dependency or live
deployment is introduced. Wild mutation remains held.

## Final native findings
The final client/server processes exited 0, with four successful native cases, three
vanilla-loot controls and a subsequent successful standalone restart.
| Role | Captured column | Impulse | Measured rise | Recovery ticks |
|---|---:|---:|---:|---:|
| low-cheap | 3 | 0.315 | 0.07840000 | 30 |
| low-costly | 3 | 0.525 | 0.07840000 | 90 |
| near-costly | 4 | 0.525 | 0.95081291 | 90 |
| sky-costly | sky | 0.525 | 1.75358092 | 90 |

Native loot used `low-costly` with low-only evidence and again with the neighboring
room's evidence present; after raising the birth column's roof, it used `near-costly`.
These are three observed draws, not required ancestry shares. The equal-response
low-room weights were 0.03725568 for 30-tick recovery and 0.01693440 for 90-tick
recovery, a 2.2 ratio. Higher weight is a chance advantage, not guaranteed parenthood.

The restarted pool clock advanced from 2912 to 2952 while captured evidence and
parent genes stayed exact; each newborn retained its producing parent key/seed/revision.
The restart's positive plugin inventory contained only core and probe. No optional
integration was needed to reload the data. The unloaded-column check also passed.

Sampling distributions are retained as raw JSON for each boot. They are observations
under those startup conditions; no optimization or production-load claim is inferred.

Final startup sampling observations (microseconds per capture):

| Area | Median | P95 | Maximum |
|---|---:|---:|---:|
| Low ceiling | 2.638 | 5.511 | 44.408 |
| Neighbor ceiling | 1.692 | 3.015 | 15.055 |
| Open sky | 38.995 | 61.369 | 888.463 |

The Maven full suite was running concurrently during this final boot. These are
1000-capture startup samples at each point; they do not isolate a before/after
performance effect or establish sustained multi-player capacity.
