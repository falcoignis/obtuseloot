# Item affinity and name variation

Owner instruction, 2026-10-28: implement both proposed supporting changes:
small inherited-affinity variation at birth, and wider name generation. The standing
requirement is for every item to be unique, or duplication to be extremely unlikely.
No new ability effects or wild mutation are included.

## Pre-registered comparison

Baseline: main `c7357f7cc159efc05617d5cf57dee0ada362aaed` (#432 merged).
Four arms: baseline, affinity-only, names-only, both. Each runs the same live
`LocalEcosystem.createEquipment` and implemented-only `SeededAbilityResolver` paths
as the prior measurement, at 3,000 births with seed `0x554E495155`, eight separated
local areas, unchanged measured synthetic outcomes, and no extra selectable abilities.
An additional combined-arm replicate uses seed `0x554E495156`.

Report distinct values, fraction of items sharing a value, largest group, matching
unordered pairs and empirical random-pair collision probability for seeds, names,
ability sets, archetype+abilities, name+abilities, affinities, genome and combined
identity (including a combination excluding the genome). Do not interpret zero
observed collisions as a proof of zero population-wide probability.

Before interpreting results, verify baseline seed/name/ability/genome/affinity
columns against run 40628288495's baseline. Verify that paired seeds, archetypes,
generation, genome and outcomes stay identical across arms; names-only must also
retain baseline affinity vectors. Ability profiles can change under affinity drift:
that is part of the measured effect, not a reason to retune selection.

Mechanism expectations, fixed before the runs:

- Affinity-only: parent vectors cease being copied exactly; siblings differ while
  staying near their parent. Names can change indirectly at affinity tag thresholds.
- Names-only: more distinct names; seeded naming must not alter reproduction or
  abilities. Names remain readable, include the equipment root and obey existing
  word/character limits. Use existing operator prefix/suffix pools, with their bundled
  defaults available before plugin initialization. Choose complete entries that fit;
  do not improve collision numbers with UUID text or truncation.
- Both: report all collision rates, including any remaining name/ability repetition.
  No acceptance quota, resampling-until-unique, diversity floor or tuning follows
  from the measurement. Full suite and existing ecology-health checks must pass.

## Implementation choices

Each inherited affinity gets an independent centered uniform delta using a separate
full-width seeded stream. Its amplitude is the existing stage-two genome mutation
amplitude scaled from the genome's unit interval to the three-unit founder affinity
span: `0.02 + 0.08 * sensitivity + 0.06 * volatility` (at most 0.16 per birth).
This is a proposed implementation scale, not an owner-confirmed balance constant.
It consumes no birth/parent-choice RNG draws. Affinities remain unbounded, as in the
existing setters; there is no new clipping or pull toward a preferred distribution.
Only offspring change, and the existing profile codec persists their values.

Names choose a prefix, the actual equipment root and a fitting suffix using the
stored naming seed. Existing text sanitization and limits remain. This increases
the name space but cannot make a finite word catalog collision-free at arbitrary
population sizes. Real gameplay/effect diversity and long-horizon affinity drift
are not established by this birth-only synthetic measurement.

## Results

RESULT_PENDING

### Diagnostic amendment before the optimized rerun

The first run (40789517726, implementation `76790c5`) reproduces the prior
baseline's 3,000 seeds, archetypes, generations, names, genomes and affinity
vectors. It does **not** reproduce all ability sets: 13 differ. Two standalone
400-birth replays on the combined implementation also differ in three ability
sets while matching every other recorded column. The cause is unconfirmed.
The registered full neutrality condition has therefore not passed. Do not
attribute small ability-count differences to this change or silently relax
the check; exact name/affinity reproduction is a separate result.

Before rerunning, the composer caches pool-entry lengths and word counts against
the immutable pool snapshot. Filtering and seeded choices retain the same order;
pool replacement invalidates the cache. This removes repeated candidate-string
construction and regex work from name refreshes without tuning a distribution.
Focused checks on the optimized source: 36 tests, zero failures/errors/skips.
