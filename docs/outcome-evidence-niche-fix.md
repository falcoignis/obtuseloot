# Remove the invented niche of unobserved items

Owner request, 2026-10-30: "we gotta fix that", after the additional-ability check
found Fleetfoot + Footprint Memory at 92.2%/90.6% of final births and a blind spot
in the single-dominant-niche metric. This is a mechanism correction, not an approved
population share, new quota or merge/deployment authorization.

## Reproduced cause and correction

`MemoryInfluenceResolver` gives an item without memories eight zero inputs.
`inferDominantNiche` broke the resulting tie as NAVIGATION. The generator replaced
the classifier's honest GENERALIST result with that inferred niche even though no
ability had been used. `nicheWeight` favored mobility (witness: 2.2 for Fleetfoot,
0.693 for Redstone Sympathy), then category selection applied niche weight again.
An ancestral exploration tendency could also assign NAVIGATION without this item's
outcomes. The unseen-template qualification separately re-inferred that role.

The failing witness ran five tests: three failed, two passed. The correction keeps
`EcosystemRoleClassifier`'s outcome-derived profile, including GENERALIST when
unobserved, and passes the same profile into unseen-template qualification. The
inference helpers are removed. Existing memory/category scoring, lineage influence,
regulatory gates, actual utility-role classification, fitness, recovery, mutation,
permissions and every numerical selection coefficient remain unchanged.

Focused validation: 22 tests, zero failures/errors, one opt-in research skip. Tests
preserve measured roles and independent memory influence, check unobserved niche
neutrality, exercise generated cohorts, and replay through independent JVMs. A
compile check caught the second inference call site; the corrected source passes.

The measurement witness also shows why one dominant tag is insufficient: the
profiles Fleetfoot + Material Insight and Fleetfoot + Gentle Harvest both reduce
to NAVIGATION under the old tie-break, while their ability-set Simpson is 0.5.
The harness now adds `ability_set_simpson` over complete observed profiles. The old
`simpson` column and its registered rule remain visible; the new field neither
changes reproduction nor supplies a replacement passing verdict.

## Registered source comparison, before Actions execution

Baseline: generator source at `de8ccaf4943e575cfa19a1ef0d060110e7f50b71`.
Correction: this branch. Both arms use the expanded seven-entry catalog, identical
current harnesses and per-item codec. Only the generator source differs.

- Eight cohort jobs: baseline/corrected x measured/equal-chance eligible-parent
  selection x two independent repeats. Each uses the four seeds, steady/changed
  worlds, four sites, eight items/site, 16 generations and prescribed opportunities
  in `docs/additional-ability-function-check.md`: 32,768 descendant births plus
  2,048 founders. Same handlers, item identity streams, ecology and mutation.
- Eight birth-only jobs: baseline/corrected x seeds `0x554E495155` and
  `0x554E495156` x two repeats, 3,000 births/job: 24,000 births. Synthetic outcomes
  are unchanged and are not evidence of player usefulness.
- Full `clean package` on the corrected source, including the long cold probes.
  Long tests run on Actions, not on the operator's machine.

Required controls: every within-arm repeat reproduces every CSV column; all
prescribed cohort inputs and initial founder identities/genes align; steady/changed
prefixes match before the change. Baseline cohort common columns must reproduce
run 40999403843 exactly. The added profile metric must independently equal diversity
reconstructed from the corresponding recorded birth profiles. Both baseline birth
samples must reproduce run 40931547378's expanded-catalog samples; paired birth
columns other than abilities must match. No distribution/trait direction is a
passing criterion. Controls and Maven results must be evaluated before interpreting
differences. Record all per-ability outcomes, recovery, ancestry, complete sets,
concentration, gene means, existing niche-loss signals and new profile-loss signals.
For profile diversity, retain the same three-window baseline and report the same
25% loss signal descriptively, including zero-baseline non-assessability. Do not
hide old failures or treat the new measurement as a biology fix.

## Boundaries and results

The registered run is [41010363869](https://github.com/falcoignis/obtuseloot/actions/runs/41010363869)
on source `c62378386a5cd3be7e362cf05a4c2c1dd5355577`. Terminal numerical results are
recorded with PR #435 and the versioned report on `fix/generated-ability-concentration`;
the temporary study workflow is preserved in that source commit. This fixes unearned scoring evidence, not every
remaining scaffold or all emergence criteria. Existing category/lineage weights,
recency and other aids remain. The controlled world still lacks harvest-use and
passive-speed measurements, retains fixed cohort replacement, bypasses wall-clock
budgets and native transport, and does not model natural item retirement or prove
open-ended native niches. No native server, live file, source image or external
service is changed. Wild mutation remains held. #434 and #435 remain prerequisites
for owner review; this request does not authorize merging them.


## Registered results

[Study 41010363869](https://github.com/falcoignis/obtuseloot/actions/runs/41010363869)
passed all 18 jobs on `c62378386a5cd3be7e362cf05a4c2c1dd5355577`.
All **108 controls** passed, including exact historical baseline reproduction,
independent replays, prescribed input/founder alignment, pre-change prefixes,
complete-profile metric reconstruction and non-ability birth-column neutrality.
The separately capped local recomputation reproduced every summary field.
All 18 study artifact ZIPs matched their published SHA-256 digests.

The study full package passed **645 tests across 127 classes, zero failures/errors,
11 opt-in skips**. [PR build 41011108568](https://github.com/falcoignis/obtuseloot/actions/runs/41011108568)
also passed 645 tests, zero failures/errors, 11 skips on
`900147503cad800b66b44f80ed29ada9d90eb942`. The production source, tests, scripts and
manifest match the measured source exactly; only documentation and workflow removal
follow it. The downloaded plugin artifact also matched its published digest.

### Generated-use cohorts

Final offspring contain 128 items per row, after generation 15's uses. Distinct
ability sets and largest-group shares describe those unused newborns; parent and
observed-cohort metrics describe the generation that produced them.

| Source / selector | World | Final distinct sets | Largest set share | Mean effective parents | Complete-profile loss signal within 25% |
|---|---|---:|---:|---:|---:|
| Baseline / measured | Steady | 7 | 92.19% | 10.60 | 0/16 |
| Corrected / measured | Steady | 20 | 11.72% | 20.21 | 16/16 |
| Baseline / measured | Changing | 8 | 90.63% | 14.44 | 0/16 |
| Corrected / measured | Changing | 19 | 14.06% | 17.90 | 16/16 |
| Baseline / eligible-neutral | Steady | 8 | 91.41% | 11.59 | 0/16 |
| Corrected / eligible-neutral | Steady | 21 | 14.06% | 20.11 | 16/16 |
| Baseline / eligible-neutral | Changing | 8 | 91.41% | 10.40 | 0/16 |
| Corrected / eligible-neutral | Changing | 21 | 11.72% | 18.25 | 16/16 |

The baseline largest pair was Fleetfoot + Footprint Memory. The corrected measured
largest pair is Redstone Sympathy + Material Insight (15/128 steady, 18/128 changed).
No population-share requirement selected this result. Equal-chance controls remain
conditional on current positive evidence and habitat/holder filters.

All added handlers continue to deliver actual measured outcomes on generated
loadouts. Corrected measured-arm successes, aggregated over four seeds and all
observed generations:

| Added effect | Steady successes | Changed-world successes |
|---|---:|---:|
| Footprint Memory | 931 | 922 |
| Weather Omen | 323 | 346 |
| Herd Instinct | 929 | 933 |
| Redstone Sympathy | 727 | 801 |

The original single-dominant-tag signal is **not cleared**: the corrected measured
scenario is within its 25% loss signal in only 2/16 steady cells and 1/16 changed
cells (eligible-neutral: 1/16 in each). Unlike the baseline's 12 zero-baseline cells,
all corrected cells are assessable. Complete ability-set diversity has positive
baselines and stays within its independently registered descriptive signal in all
16 cells. The two measurements answer different questions; complete-profile variety
does not erase niche-level warnings or certify native ecological health.

### Birth-only uniqueness control

Each sample contains 3,000 births. Paired identities, seeds, names, parentage,
genomes, affinities and supplied synthetic outcomes match; only abilities differ.

| Seed | Source | Distinct ability sets | Largest group | Matching unordered-pair probability |
|---|---|---:|---:|---:|
| First | Baseline | 17 | 2,646 | 77.94% |
| First | Corrected | 21 | 369 | 6.39% |
| Second | Baseline | 18 | 2,667 | 79.17% |
| Second | Corrected | 21 | 369 | 6.25% |

This is the empirical probability that two sampled items share their **ability set**,
not whole-item duplication probability. Baseline full-profile duplicates were absent
already; this correction does not claim to improve invisible-identity uniqueness.

**Result:** removing the invented role removes the demonstrated dominant-pair
concentration under the registered conditions, while preserving measured roles,
local reproductive wiring and other identity inputs. This does not establish all
four emergence criteria, replace remaining scaffolding, explain every niche-loss
signal or validate native play. No coefficient was tuned to improve these tables.
The code correction is in PR #435 for owner review; neither it nor #434 is merged
by this task. The numerical report remains on the versioned research branch, while
PR #435's tested code head is preserved.
