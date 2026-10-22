# Fitness belongs to the producing genome

An ability invocation captures the item's signed seed and genetic revision in its event
context. Position and activation copies preserve that snapshot. Completed and deferred
observations carry it into utility telemetry, durable fitness history and local evidence.
A result arriving after evolution credits its producing revision, never the newer genes.
Unseeded items, offline classifications and unfinished effects do not earn genetic fitness.
Existing effect-owned eligibility and once-only receipt checks remain necessary.

Each physical item retains separate measured utility summaries for every observed
seed/revision and mechanic/trigger. The history includes counters, utility, costs, the last
outcome signature, repetition streak and eight-outcome no-op window. Saving and loading
therefore preserves the next penalty as well as accumulated fitness. Trading preserves
the history; identity replacement carries it forward under its original keys. Lifetime
item reputation and ability preferences remain separate and are not erased on evolution.

At an explicit genetic transition, the feedback API receives a snapshot built from the
producing revision's measured summaries, excluding lifetime activity and ancestor fitness.
The blanket-reinforcement ablation removes direct aggregate-fitness scaling of genes;
see `blanket-gene-reinforcement-ablation.md`. The subsequent ecological gene-multiplier
ablation removes blanket context scaling as well; see `ecological-gene-multiplier-ablation.md`.
Measured outcomes still drive reputation,
progression and local reproduction. Existing macro ecology, lineage support, pressure,
memory and ordinary mutation remain; other historical selection scaffolding remains.

Local reproductive evidence is partitioned by item key, seed and revision. The existing
same-world, configurable chunk radius and game-tick window apply before scoring. The draw
excludes evidence that does not match the parent's currently stored seed/revision. A newly
evolved or reseeded item must demonstrate fresh success before reproducing. Historical
evidence remains attributable until its normal expiry; durable item summaries remain after
that expiry. Missing or unreadable recorded parents still refuse birth. With no current
positive local evidence, the existing founder behavior applies, without a global fallback.

Children record the parent's item key, signed seed and revision, inherit its current genes
with the existing ordinary mutation, and start with empty personal fitness. No distribution
weights, quotas, diversity floors, forced extinction or extra mutation roll were added.
Wild mutation remains held.

Both per-item profile and ecology formats are version **3**. Counts, identities and fitness
state are validated; missing, malformed or old data is refused rather than silently reset.
There is no migration because the owner confirmed no player data exists. YAML and SQL use
the same per-item codec. Existing failed-save retention and autosave/crash limits remain.

## Controlled experiment

`RevisionFitnessGenerationsTest` runs eight generations of 32 test items under open-space
and low-ceiling response models. It uses the catalog's Double Jump expression, a six-tick
modeled response, measured utility/cost scoring, actual local parent draws, ordinary birth
mutation, explicit genetic transitions and profile/ecology serialization each generation.
Old positive revision evidence remains in the draw's input so current-revision filtering is
exercised. Cohort size and scheduled evidence expiry are test protocol, not production rules.

Fixed input seed 722 produced these observations; no threshold on gene distribution is an
acceptance condition and no engine coefficient was tuned to obtain them:

| Modeled condition | Initial mean kinetic gene / recovery | Generation 7 mean gene / recovery | Mean response at generation 7 |
|---|---|---|---|
| Open space | 0.5007 / 60.56 ticks | 0.5555 / 63.78 ticks | 1.3449 blocks |
| 0.0784-block headroom | 0.5007 / 60.56 ticks | 0.1355 / 38.63 ticks | 0.0784 blocks |

The open-space run replayed exactly, including parent seeds/revisions, child genes and
measurements. Changed responses produced different parentage. The assertions also check
current-only ancestry, fresh child fitness and retention of owned profiles after evidence
expiry. CSV and parentage reports are generated under `target/revision-fitness/`.
These are controlled modeled observations, not native physics, player usefulness, natural
niche formation or proof that all four emergence criteria have been met.

## Validation

The focused regression slice and `server-tests/revision-fitness/README.md` cover delayed
attribution, fresh-evidence eligibility, ancestry, repeat penalties, malformed storage,
reseed/replacement continuity, trade and restart. The original revision-fitness slice's official Java 25 Docker validation
passed **113 focused tests** and **540 full-suite tests across 104 classes**, with zero
failures/errors/skips and Maven exit 0. The full clean build took **30:33**. All **528**
frozen source/resource/build/fixture inputs and **563** production entries matched the
server-tested build; the generation experiment matched across focused and full JVMs.

Standalone Purpur 26.2 build 2633 passed all three process phases: captured delayed
results, refusal of unproven genes, current parent ancestry, preservation of the next
repeat penalty, two actual restarts and a holder change. The fixture submits controlled
API observations; it does not certify native movement or optional integrations. The
existing missing-dashboard-data startup warning remains. Purpur was verified against
its publisher-provided MD5; its SHA-256 was calculated locally, not publisher-verified.
