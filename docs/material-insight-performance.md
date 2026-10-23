# Material Insight: inherited depth and recovery

Material Insight now expresses `PRECISION_AFFINITY` through the existing catalog-owned
benefit/recovery model. The implementation defaults are four facts and 60 game ticks:

| Precision gene | Fact budget | Recovery |
| --- | --- | --- |
| 0 | 2 | 30 ticks |
| 0.5 | 4 | 60 ticks |
| 1 | 6 | 90 ticks |

The owner approved more information versus longer recovery. The baseline and spreads
are implementation defaults, not an owner-selected balance target. Configure
`abilities.material-insight.base-facts` (positive integer) and `recovery-ticks`
(nonnegative game ticks). The model uses a 0.5 output/recovery spread, floors the
expressed fact budget to an integer (at least the material itself), and rounds
recovery upward. Baseline 1 can therefore deliver one material fact at every gene.

Reports contain actual applicable properties, in stable order: material, light,
then crop growth, supporting soil and farmland moisture, or applicable moisture,
waterlogging, power, facing and open state. A crop on farmland has five available
facts in the fixture; plain stone has two. Unused budget creates no filler.
The observation records the number of properties delivered, **not** player learning
or usefulness. An unchanged report on the same block retains the same repeat key;
recovery time is excluded from that key.

The generic executor passes each registration's expressed performance to its effect.
Other handlers retain their existing functional-interface overload. Adding/removing
an ability needs no ability-ID branch in timing or persistence. Recovery is keyed
by ability ID in the item's profile, rather than by player, slot or revision.
Moving/trading/copying the same identity, changing genes or reloading configuration
cannot erase an already committed deadline. Newborn identities inherit genes and
ancestry, with empty recovery and fitness histories. A per-item execution lease
prevents reentrant duplicate delivery even when recovery is zero. A same-storage-identity
replacement continues the same deadline map and active lease; its delivery completion
releases the lease for the replacement too. Failed delivery
rolls back its reservation; denied/unavailable/stale-revision actions send nothing.

Action contexts capture the existing persistent local-ecosystem game clock.
Unknown clocks fail closed; no wall-clock cooldown is introduced. Per-item profile
format **4** requires complete recovery entries and rejects version 3 or malformed
state. Unknown ability IDs survive a round trip, so removing/readding a catalog
entry does not reset its deadline. Ecology stays at format 5. No data migration is
provided because the project has no real player data. Dirty recovery state uses the
existing profile autosave/shutdown path; these tests cover graceful restarts, not
transactional crash recovery across separate item/ecology files.

The normal six-slot activation, effective ability permissions and protection-denial
checks remain. This is read-only block inspection: it does not cancel a native
event, change the block/inventory, intercept XP, or require an optional plugin.
Standalone operation and normal AuraSkills XP coexistence remain requirements;
boot detection is not cross-plugin certification. Double Jump stays opt-in.
The equipment catalog currently has axes but no pickaxe archetypes; this change
does not add pickaxes.

## Mixed-generation protocol

`MixedAbilityEvolutionTest` compares four seed pairs, measured and neutral parent
sampling, steady conditions and a swap after generation 4. Each site starts with
16 items spanning a 4x4 precision/kinetic grid; all other genes start at 0.5.
Eight generations supply both ability definitions to every item, irrespective of
its generated loadout, to isolate the two performance tradeoffs. This does not
exercise evolving ability availability or armor/hand compatibility in the mixed
model. They execute the real Material Insight handler against Bukkit API doubles
plus the established six-tick jump-response model. Rich crop/three-block
column and plain stone/open sky sites are 160 blocks apart, inside the unchanged
configurable 20-chunk radius. Jump response caps are 0.0784 and 20 meters.

Every item supplies one observation per ability before an explicit evolution
transition and three afterwards, 100 ticks apart. Measured revision histories,
experience feedback, habitat-conditioned current-genome parent selection, ordinary
mutation, profile/ecology round trips and evidence expiry use production code.
Neutral sampling maps the same random draw into equal chances among the production
selector's eligible current parents; it does not replace the selector. All parent
seeds/revisions and full expressed/latent/activated newborn genes are recorded.
Independent replay must match exactly. The test asserts lifecycle/wiring invariants,
without a gene direction, diversity threshold or prescribed distribution.

Generated CSVs report precision and kinetic variation, actual delivered facts,
modeled jump meters, each recovery cost, existing pool utility, reproductive
concentration and founder ancestry separately. The existing utility pipeline
combines different benefit units; these experiments do not establish that one
fact and one meter have equal player value. Fixed cohorts and scheduled opportunities
also do not establish naturally emerged niches, actual item extinction or ecosystem
health. Target richness covaries with column structure in this protocol; it does
not isolate each ability's marginal contribution or demonstrate information niches
between blocks sharing the same column descriptor. No fitness coefficient, mutation policy, quota or variance floor is changed.

Native validation uses the separate disposable-server fixtures in
`server-tests/material-insight/`. Test results and limitations are recorded in the
canonical skill status after validation completes.

## Recorded validation

The final mixed protocol, including producing-revision experience feedback, emits
256 generation/site rows and 4,096 births. In each selection mode, all 512 pre-swap parent choices match
the paired steady controls. After the swap, 130/512 measured-parent choices differ;
the neutral control changes none. This shows context-sensitive measured selection
in the prescribed model, not that a particular gene direction is required.

Four-pair final-generation means (performance describes the observed parent
cohort; founder Simpson describes its resulting newborn cohort):

| Conditions | Selection | Facts | Insight recovery ticks | Jump meters | Jump recovery ticks | Founder Simpson |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Steady crop/low column | Measured | 2.9844 | 51.3281 | 0.0784 | 59.5469 | 0.5430 |
| Steady crop/low column | Neutral | 2.8750 | 51.0625 | 0.0784 | 56.0469 | 0.5840 |
| Steady stone/sky | Measured | 2.0000 | 39.5469 | 1.3268 | 63.2656 | 0.3848 |
| Steady stone/sky | Neutral | 2.0000 | 61.8125 | 1.2621 | 59.5469 | 0.5312 |
| Swapped to crop/low column | Measured | 3.0781 | 52.2031 | 0.0784 | 60.5156 | 0.3809 |
| Swapped to crop/low column | Neutral | 3.5781 | 61.8125 | 0.0784 | 59.5469 | 0.5312 |
| Swapped to stone/sky | Measured | 2.0000 | 46.0781 | 1.2079 | 56.3281 | 0.5117 |
| Swapped to stone/sky | Neutral | 2.0000 | 51.0625 | 1.1995 | 56.0469 | 0.5840 |

An earlier prototype omitted the experience-feedback engine and had slightly
different means. The table and frozen final reports use the complete feedback path.
Measured ancestry is lower than the neutral control in several cells; no healthier
ecosystem verdict follows from these measurements.

Native Purpur 26.2 build 2633 booted with only core/probe, then received actual
right-click packets through the controlled 26.1 client and official adapters.
Low/high crop cases delivered 2/5 facts at 30/90 ticks; sparse stone delivered
2 facts at 90 ticks. Moving the same identity to the offhand could not bypass
recovery and succeeded again after its deadline. Both interaction-denial flags
and an effective permission denial for OP produced zero measurements/deadlines.
Crop age stayed 4; raw client chat contains exactly five reports (2/5/5/2/5 facts).
A final high-precision inspection remained in recovery through standalone restart:
clock 245 -> 246, unchanged ready-at tick 329 and producing genes. All final
server/client processes exited 0, without failure files. Purpur matched its
published MD5; its API provides no published SHA-256. Both adapters matched
published GitHub SHA-256 digests; client dependencies used the recorded SRI lock.

The first corrected API/equipment/profile package passed 60 tests. Later lifecycle
review found same-item replacement lost recovery: a new regression reproduced
190 -> 0. The corrected final package passed 62 effect/mixed tests, followed by
25 genome/convergence/subscription lifecycle tests (87 distinct cases). The final
full-feedback mixed test passed separately with exact independent replay. The
initial deliberate red test failed on unchanged #421. The earlier incomplete full
run was stopped and archived at exit 143 before restarting final clean validation.
Superseded fixture failures
are retained with review evidence and documented in the native fixture README.
Full clean-suite and final package verification are recorded in canonical status.

The complete local clean run executed 583 cases across 115 classes: 582 passed,
with one pre-existing assertion still requiring Material Insight to have fixed
performance. That expectation alone was corrected; its five-case class/package
rerun passed. No production input changed. The final single-run GitHub full suite
is required before the owner-authorized merge. Full original and corrected reports
are retained separately rather than presenting the original run as green.
All five earlier generation-report sets and the new mixed reports reproduce exactly;
all 572 packaged production entries match the native-tested build. The packaging
verifier initially assumed literal YAML quote style; it now checks API metadata
structurally. Native fixture source was recorded after compilation and independently
recompiled to the exact native-tested probe class.
