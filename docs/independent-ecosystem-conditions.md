# Independent conditions and opportunity baseline

This diagnostic uncouples target richness from ceiling height and varies access
to the two inherited effects. It changes no production code, coefficients,
mutation policy, storage format, permission, activation or integration behavior.

## Protocol

Four study seeds (722, 1722, 2722, 3722) each initialize four site breeding
streams at seed offsets 0..3 and run three input arms with measured and neutral parent sampling, in steady and changing
conditions: **48 reported runs**, plus one independent replay. Each run starts with 16 items at each of four sites,
with the same 4x4 precision/kinetic founder grid and other genes at 0.5.

| Site | Initial target | Initial ceiling |
| --- | --- | --- |
| 0 | Crop on farmland | Three-block column |
| 1 | Stone | Three-block column |
| 2 | Crop on farmland | Open sky |
| 3 | Stone | Open sky |

Sites are 80 blocks apart in one world, inside the unchanged default **20-chunk**
inheritance radius. Changing runs exchange crop/stone targets at generation 4,
then low/sky ceilings at generation 6. All four combinations remain present.
The steady counterpart shares the same initial state and random streams.

| Input arm | Ability availability | Opportunities |
| --- | --- | --- |
| Both/regular | Both effects on every item | Both offered each round |
| Varied/regular | Inspection, jump, or both | Both offered each round |
| Varied/opportunities | Inspection, jump, or both | Item input profiles offer inspection/jump with 25%/75%, 75%/25%, or 50%/50% probabilities |

The loadout and opportunity choices are **assigned test inputs**, not production
procedural profiles. Their separate stream is keyed to study seed, site,
generation and birth slot, keeping paired opportunities aligned as genes and
parentage change. Production breeding randomness is not consumed to assign them.
An initial prototype used item seeds for those inputs; its source and reports are
kept separately. The final results below use the separate input stream.

Each generation has 12 rounds spaced 20 game ticks apart: one before an explicit
genome state transition, then 11 afterwards. Inspection uses the actual Material
Insight handler with Bukkit API doubles, including committed recovery and existing
repeat-report penalties. Some opportunities revisit the same target; others inspect
new coordinates. Jump displacement uses the existing six-tick physics model with
0.0784/20-meter response caps. Its cooldown is also modeled, not an invocation of
the native movement coordinator. Real armor/hand activation, procedural ability
availability, progression opportunities and live plugin combinations are outside
this model.

Missing abilities, unoffered actions, cooldown suppressions and measured executions
are distinct. Suppressions do not become measured fitness. Per-item activity rows
report offered opportunities, unavailable abilities, attempts, suppressions,
successes, delivered output, committed recovery, unique report keys and current
revision utility. Missing means/utility are empty fields, with explicit counts.
Facts and modeled meters remain separate; delivered facts do not prove learning.

The production revision history, experience-feedback path, current-genome local
selector, ordinary inheritance/mutation, profile/ecology round trips and evidence
expiry execute. The experiment injects modeled observations through those APIs;
these rows are not native gameplay telemetry. Empty breeding pools retain the
production fresh-founder fallback. Owned profiles are retained after evidence
expires. Cohort replacement is an experiment schedule, not production item death.
The macro ecology is not advanced as a whole server simulation.

Neutral controls map the same selector draw to equal chances among its current
eligible parents. The before-change parentage prefix must match the steady run;
one complete scenario independently replays every output row. The test imposes
no preferred gene direction, population distribution or diversity threshold.

## Context counterexample

A two-item inspection-only witness isolates what column-only context loses:

| Evidence queried | Precision 0 weight | Precision 1 weight |
| --- | ---: | ---: |
| Crop only | 1.200000 | 1.363636 |
| Stone only | 1.200000 | 0.545455 |
| Pooled, query at crop origin | 1.200000 | 0.954545 |
| Pooled, query at stone origin | 1.200000 | 0.954545 |

Both origins have the same three-block column, 80 blocks apart. The high-precision
item sends five crop facts versus two for low precision; both send two stone facts.
Their recovery is 90/30 ticks. The unchanged selector pools the two targets into
identical weights at both origins. Its captured context contains column geometry,
not target properties. The equality assertion records this baseline limitation;
a future context change should intentionally replace that witness, not preserve
aliasing as a health requirement. These weights are the existing classifier's
units, not a calibrated human-value scale.

Frequent-opportunity control: over 12 offered rounds at ticks 0..220, the real
inspection handler succeeds six times for precision 0 and three times for
precision 1. Longer recovery is therefore exercised as actual unavailability,
rather than only a number added to a receipt.

## Generation measurements

The final protocol writes **1,536 generation/site rows**, **49,152 per-item
activity rows**, and **24,576 births**. All ancestry records preserve producing
parent seed/revision, parent/child loadouts, founder, inheritance generation and
full expressed/latent/activated newborn genes. Activity rows also preserve the
independent input seed. Physical UUIDs, names and wall-clock timestamps are excluded
from replay traces.

All pre-change parent choices match the paired steady run. Counts below pool the
four seeds; later columns compare with the original steady run, so the final column
includes both changes and their history, not an isolated ceiling treatment.

| Measured selection arm | Before changes | After target change, before ceiling change | After both changes |
| --- | ---: | ---: | ---: |
| Both/regular | 0/1024 | 262/512 | 295/512 |
| Varied/regular | 0/1024 | 316/512 | 421/512 |
| Varied/opportunities | 0/1024 | 329/512 | 406/512 |

Every neutral counterpart has zero changed choices in every phase. This is
consistent with outcome-dependent selection affecting parentage; it does not
establish naturally emerged niches.

A concrete performance deficit appears in the final steady **stone/sky** cell of
the varied/opportunities arm:

| Metric | Measured selection | Neutral control |
| --- | ---: | ---: |
| Facts delivered per observed item over 11 post-transition rounds | 2.8750 | 3.9375 |
| Mean recovery of completed inspections | 68.4239 ticks | 44.3095 ticks |
| Distinct report keys summed across the 64 observed items | 81 | 99 |
| Offered inspection opportunities | 354 | 354 |
| Opportunities missing the ability | 108 | 108 |

Each of the four pairs delivers fewer stone facts with longer recovery under
measured selection. Unique report count is lower in three pairs and equal in one.
The paired schedules and loadouts align. Other cells differ: measured selection
in the steady stone/low cell delivers 3.3125 versus 2.8438 facts per item with
42.7830 versus 55.0989 ticks mean recovery. These are prescribed model outcomes,
not a production benchmark or proof that each cross-target parent is unsuitable.

In the last varied/opportunities generation, measured birth cohorts have mean
founder Simpson 0.547363 (steady) / 0.580078 (changing), versus neutral 0.713379
in both. There are no fresh-founder fallback births in this run set. Current
unobserved items remain explicit rather than gaining artificial fitness. Gene
variance, ancestry diversity, ability availability and output are distinct; no
overall healthy verdict follows from any one of them.

## What this establishes and leaves open

The column descriptor cannot express the inspection witness's target-dependent
weights, and measured selection can coexist with worse delivered output/cost in
one cell. The multi-ability result does not isolate the contributions of target
aliasing, aggregation of different benefit units, repeat penalties and genetic
linkage. Fixing it requires a measured context/fitness design, not a quota or a
coefficient chosen to make this table improve. It also remains necessary to test
actual generated profiles and player opportunities before claiming a functioning,
open-ended ecosystem. No engine change is adopted by this diagnostic slice.

All reports are generated below `target/independent-conditions/`; raw and superseded
runs are preserved with the private review archive. The focused Docker package passed 18 cases across four classes; the complete clean
Docker package passed all **586 cases across 116 classes** in 37:14 min, with zero
failures/errors/skips and process exit 0. New reports match across JVMs, all five
historical report sets and the earlier mixed experiment reproduce, and all 575
packaged entry contents match merged #422. Java 25/API 26.2 and exclusion of test
classes were verified. The canonical status records the publication snapshot.

To reproduce the aggregate tables, run:

```sh
python3 scripts/summarize-independent-conditions.py target/independent-conditions --output target/independent-summary.json
```

The standard-library postprocessor reads the Java-generated CSVs, checks field
counts, activity accounting, missing-value semantics and paired input alignment,
then reports every final condition/activity cell. Simulation and selection remain
in Java. Post-treatment child seeds may differ if a future run reaches fresh-founder
fallbacks; the analyzer does not require those outcome seeds to match.
