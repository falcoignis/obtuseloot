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

**2026-10-29 follow-up:** the failed ability replay was traced to uncontrolled
random founder lineage identities, an input to ordinary lineage drift. A new
controlled-identity confirmation reproduces every recorded column across all five
independent arm pairs and passes all 16 checks (30,000 births). See
[`ability-replay-inputs.md`](ability-replay-inputs.md) for the correction, study and
remaining ability repetition. The historical results and failed neutrality below
remain unchanged.

Original comparison: run [40789517726](https://github.com/falcoignis/obtuseloot/actions/runs/40789517726),
source `76790c5`. All five measurement jobs passed; the superseded full-suite job
was canceled after the optimized head's run started. Every downloaded artifact
matches its published SHA-256 digest.

These are **descriptive sample counts**. The full registered neutrality condition
failed for ability sets (diagnostic amendment below); no causal ability-diversity
claim is made.

| Arm (3,000 births each) | Distinct names | Items sharing a name | Distinct affinity vectors | Name-pair collision probability | Complete profile duplicates |
|---|---:|---:|---:|---:|---:|
| baseline | 56 | 2,999 (99.97%) | 8 | 0.0283210 | 0 |
| affinities | 61 | 2,996 (99.87%) | 3,000 | 0.0281850 | 0 |
| names | 2,988 | 24 (0.80%) | 8 | 0.00000266756 | 0 |
| both | 2,988 | 24 (0.80%) | 3,000 | 0.00000266756 | 0 |
| both, second seed | 2,988 | 22 (0.73%) | 3,000 | 0.00000311215 | 0 |

There are 4,498,500 unordered pairs per sample. Names-only and both each have
12 equal-name pairs; the second seed has 14. Zero complete-profile collisions
were also observed in the **baseline**, because genomes were already distinct.
Unique affinity vectors and broader names improve different identity dimensions;
they do not prove unique gameplay effects. Finite name pools still collide.

The optimized composer reproduces all 3,000 names from the original combined
corpus exactly in a database-free Java check. The Maven image's entrypoint emitted
a default-cache permission warning; the Java checker itself exited 0.

Optimized rerun: [40792059771](https://github.com/falcoignis/obtuseloot/actions/runs/40792059771),
source `f14668c`: **all six jobs passed**. Full `clean package`: **612 tests,
zero failures/errors, 10 opt-in skipped, across 122 classes**. All six artifacts
match their published SHA-256 digests. The five measurement samples reproduce
the original implementation's seeds, archetypes, generations, names, genomes
and affinities exactly (15,000 births across revisions). Ability sets differ in
16 to 18 rows per arm; even the unchanged baseline differs in 16 of 3,000.
Thus the registered full neutrality condition remains **not met**. No ability
comparison is certified, and no engine weights were tuned to repair it.

The temporary workflow is removed before review. To reproduce the recorded run,
use the workflow and Java source at `f14668c`; the result summarizer is
`scripts/summarize-item-variation.py ARTIFACTS --prior-baseline PRIOR_ITEMS_CSV`.
It prints the domain-specific checks and deliberately exits nonzero when full
neutrality fails. The pre-registered random-pair probability is
`sum(count * (count - 1) / 2) / (births * (births - 1) / 2)`.

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
