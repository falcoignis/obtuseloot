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

Pending registered execution. This fixes unearned scoring evidence, not every
remaining scaffold or all emergence criteria. Existing category/lineage weights,
recency and other aids remain. The controlled world still lacks harvest-use and
passive-speed measurements, retains fixed cohort replacement, bypasses wall-clock
budgets and native transport, and does not model natural item retirement or prove
open-ended native niches. No native server, live file, source image or external
service is changed. Wild mutation remains held. #434 and #435 remain prerequisites
for owner review; this request does not authorize merging them.
