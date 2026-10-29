# Replaying item abilities with controlled identities

## Diagnosis

The item-variation study in #433 held birth seeds fixed but did not control item
identities. `ArtifactStorageKeys.create()` uses `UUID.randomUUID()`.
`LocalEcosystem.createEquipment` makes a founder's lineage ID from that storage key,
then offspring inherit it. `ArtifactLineage.registerDescendantBias` seeds its
ordinary lineage drift with `artifactSeed ^ lineageId.hashCode()`. That evolving
bias feeds later ability selection through `LineageInfluenceResolver`.

Thus the study did not supply identical inputs to the resolver. This is a harness
replay defect, not evidence that the ability sampler ignores its RNG seed. It does
not establish that all other replay defects are absent.

A database-free witness using the same birth/observation/resolver path reproduced
three different ability sets in 400 births across two JVMs on unchanged main
`5bba50883710eec1698b38a20b65ef485eb9db56`. Birth seeds, generations and candidate
pool sizes matched. With identities supplied from a separate seeded stream, three
independent JVMs matched all 400 rows (including abilities, genomes, affinities and
names). A small causal witness also holds descendant inputs constant and changes
only the lineage ID: the resulting drift differs.

## Correction

`LocalEcosystem` has a package-private storage-key supplier for test fixtures.
Existing public constructors still use `ArtifactStorageKeys::create`; live item
identities and lineage drift stay random in exactly the same places as before.
No engine weight, gate, quota, mutation chance or protection check changes.

The measurement derives canonical UUID-shaped keys from a separate
`Random(runSeed ^ 0x4944454E54495459L)` stream. This controls an existing input,
does not consume birth/parent-choice RNG draws, and is not a live identity scheme.
The CSV now records storage/lineage IDs, exact parent identity/seed/revision and
the supplied observation amount/legacy delta as well as the original columns.

## Registered confirmation, before the Actions run

Repeat the same five 3,000-birth arms twice on independent GitHub runners:
baseline, affinity-only, names-only, both, and both with the second study seed.
Seeds remain `0x554E495155` and `0x554E495156`. Both copies of an arm receive the
same controlled identity stream. Baseline restores the pre-#433 affinity-copy and
name-generation behavior while retaining the common identity-input correction.

Required checks, without changing any previous neutrality rule:

- Every column of every one of the five arms must match its independent repeat.
- Birth seeds, archetypes, generations and genomes match across the four paired arms.
- Identities, parentage and supplied outcomes match across those paired arms.
- Names-only preserves baseline abilities and affinity vectors; both preserves
  affinity-only abilities and affinity vectors. Names-only and both names match.
- Report the existing uniqueness and pair-collision metrics and full-suite result.
  Do not tune any distribution to achieve them.

Historical uncontrolled-identity ability rows cannot be used as an exact baseline
for the corrected study. Their failed neutrality result remains in
`docs/item-affinity-name-variation.md`. Compare their other recorded domains
separately; do not certify the old ability comparison retroactively.

## Confirmation result

Pending the registered Actions run. Wild mutation remains held. No merge or live
deployment authorization is granted by this task.
