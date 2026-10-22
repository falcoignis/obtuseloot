# Durable genome evolution

The owner approved making live genetic changes permanent. Previously, profile lookup
mutated a temporary genome, substituted lineage genes, applied experience feedback and
activated latent genes. Selection could see those temporary genes while actual effects
read the stored genome. Repeated lookup could also advance lineage generation bookkeeping.

## Transition boundary

`GenomeEvolutionEngine` commits ordinary mutation, existing experience feedback and latent
activation to the item at explicit path evaluation, drift, awakening and convergence
boundaries. Live bootstrap and world-lab inject the same mechanism with their own catalog,
memory, lineage and experience dependencies. Coefficients and activation thresholds remain
unchanged. There are no added weights, quotas or probability rolls. The separate wild
mutation roll remains held; ordinary designed random drift remains intact.

A persisted revision and length-prefixed state marker cover archetype/evolution paths,
awakening, convergence and drift count/level/alignment. Owner, seed, timestamps and profile
diagnostics are excluded. The marker prevents replay after refresh failure, reload or trade.
Formed and tempered are separate transitions even though both resolve to coarse stage 2.
Ordinary mutation draws incorporate the saved revision, so different transitions at the
same stage have separate reproducible variation. Birth mutation keeps its existing draw.
Explicit administrative baseline recreation/reseeding retains the item's genes and revision
but starts the marker at that baseline. A later return to the same evolution path is a new
transition, rather than an incorrectly suppressed replay.

Genetic computation finishes before genome/revision/marker assignment under the item's
lock. A retry after a profile-build failure sees the committed marker and skips mutation.
The existing dirty-state retry buffer retains failed saves. Awakening and convergence copy
the old genes, activation flags, revision, ancestry and item reputation before their new
transition; the manager retains that newly evolved genome instead of overwriting it with
the discarded identity's genes. Identity replay is rejected before changing loaded state.
Genetic feedback during replacement reads the old identity's measured experience and role
signals before the usage tracker transfers them to the new seed.
Lineage summaries observe committed item genes without incrementing ancestry on profile
reads. Trait correlation reports and spatial trait hotspots also read the stored genome.

## Reads, inheritance and storage

Profile resolution reads `Artifact.getGenome()`. It no longer mutates, activates latent
genes or substitutes lineage genes for the item's genome. Parent inheritance still happens
at local birth, from the actual persisted parent genome with ordinary small variation.
Subscription caches compare the stored immutable genome reference and revision as well
as item identity. Performance models already read that same stored genome; pending
Double Jump observations reject a changed genome without resetting player-owned recovery.

Join/load and administrative refresh rebuild profiles without evaluating evolution.
Measured gameplay windows, ordinary kill processing and explicit debug evolution remain
write paths. Profile resolution still updates existing diagnostics and population/lineage
selection bookkeeping; this change guarantees genetic read stability, not complete purity
of the entire ability resolver.

Per-item profile format is now version 3 for both YAML and SQL. It stores the expressed
and latent values, activation flags, revision and transition marker together. Missing receipts, invalid revisions and non-string/blank state markers fail closed. The owner confirmed no player data exists, so no migration
or fallback generation is supplied. Revision-specific fitness history and parental genome
attribution are saved alongside these values; ecology evidence is also version 3.

Double Jump is still the first real benefit/recovery expression. Other effects remain
fixed/configurable. This does not change permissions, optional integration hooks, XP, flight
ownership or movement protection. Revision-specific fitness now separates measured genetic
experience and local reproductive evidence; see `revision-specific-fitness.md`.
Durability alone does not demonstrate an emergent ecosystem.

## Validation

The regression slice covers real path changes, same-stage transitions, experience applied
only at transitions, persisted latent activation, read/refresh stability, restart continuity
of the next mutation draw, YAML/SQLite saves/trades, failed-save retry, rejected identity
replay, convergence continuity, subscription invalidation and refresh failure after commit.
The disposable server fixture is documented in `server-tests/durable-evolution/README.md`.
The original durable-evolution slice's official Java 25 Docker validation passed 93 focused tests and all 532 full-suite
tests (102 classes), with zero failures/errors/skips and Maven exit 0. The full build took
31:39. All 521 frozen source hashes and 560 server-tested production entries matched.
The standalone Purpur 26.2 build 2633 probe passed all three process phases, including
baseline resets, two actual restarts, trade and repeated refresh stability. This is
controlled transition/storage validation, not native player-input or integration certification.
The existing seeded health test exercises profile selection; genetic transition durability
is covered separately by the new transition/storage/scheduler tests and real-server probe.
