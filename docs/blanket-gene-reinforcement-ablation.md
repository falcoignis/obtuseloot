# Blanket genetic reinforcement ablation

The owner selected this ablation after revision-specific fitness: remove the aggregate
fitness term that prescribed the direction of every trait, and compare selection when
local conditions change midway through a multi-generation run.

Previously, `ExperienceEvolutionEngine` normalized aggregate fitness by 8, clamped it to
[-1, 1], then multiplied every expressed trait by a fitness factor. Five traits received
the full 10% adjustment and four received half; positive fitness raised them all and
negative fitness lowered them all. The same factor also adjusted latent genes. This
assigned directions without establishing which genes caused a useful outcome.

The production transform now omits that factor and its fitness normalization. Ordinary
birth/transition mutation, latent activation, saved revision history, measured reputation
and local measured-outcome parent selection remain. Existing ecological/context scaling,
lineage support and seeded environmental modifiers remain for separate evaluation. In
particular, this does not make every genetic change directionless: the retained context
multiplier still scales all traits, and fixed environmental events still favor traits.
Public constructor and feedback signatures are preserved. Revision-specific snapshots
remain available at the transition API; aggregate fitness no longer directly sets genes.
There is no runtime switch restoring the removed term.

## Controlled comparison

The test-only `LegacyExperienceEvolutionEngine` restores the original transform from
merged baseline `c04a211e617c4d10d190f60a3abccb5f8fe0662f`. A regression against that
baseline failed in the two intended places: direct trait scaling by fitness and its
effect on real genetic transitions. The control also agrees with the retained transform
when the fitness term is neutral. The control is excluded from the production JAR.

`ReinforcementEnvironmentSwapTest` uses two areas in the same world, 2048 blocks apart,
with actual configured chunk-radius parent filtering. Each has 32 sampled items per
round for 12 rounds, using RNG seeds 722 and 723. Both current and legacy arms run a
steady-condition control and a condition-swap case. Open-space and 0.0784-block-headroom
response models exchange places at round 6. All arms share their initial conditions and
random streams. Swapped/steady ancestry is identical before the change; afterward both
arms produce different actual parentage. The ablated swap run replays exactly.
Replay compares parent/child seeds, parental revisions, founder ancestry and all
expressed/latent/activation values. Physical UUID keys and names remain freshly minted.
Direct comparison of parent seed/revision columns records 116 different post-swap
draws out of 384 in the ablated arm and 138 out of 384 in the legacy arm.

The fixture uses the catalog's Double Jump output/recovery expression, a six-tick modeled
response, real utility classification, stored producing-revision summaries, explicit
genetic transitions, ordinary inherited mutation and real local parent draws. Profiles
and ecology are serialized each round. Old positive revisions remain in the input so
current-only eligibility is exercised; offspring start with empty fitness. Old owned
profiles remain after evidence expires. No trait direction or distribution target is an
assertion. Fixed cohort sizes and scheduled expiry are test protocol, not production rules.

Unrelated macro telemetry is held at the same empty state in both arms, avoiding the
existing wall-clock bifurcation scheduler as a confounder. Default seeded environmental
modifiers remain fixed without advancing seasons; their full values are in the report.
With empty macro telemetry, neutral support and unit lineage influence, the retained
context formula is 0.95. Its kinetic environmental modifier is 1.0 in this fixture, so
that remaining context term applies a 5% downward kinetic adjustment at each transition,
alongside ordinary variation and subsequent measured selection. Both arms retain this
existing bias.
Measured fitness is submitted directly through the real revision-history classifier and
local evidence pool. This experiment isolates genetic/reproductive mechanisms; it is
not a replacement for live ability-manager or native gameplay validation.

At round 11, the ablated arm produced these values:

| Area | Steady condition: mean gene / recovery | Swapped condition: mean gene / recovery |
|---|---|---|
| 0 | Open: 0.35159 / 51.56 ticks | Low headroom: 0.14065 / 38.84 ticks |
| 1 | Low headroom: 0.13971 / 38.88 ticks | Open: 0.21796 / 43.66 ticks |

Selection responds to changed conditions relative to matched steady controls. Absolute
gene means do not consistently rise after the swap; retained context bias and sampling
also affect the trajectory. Both legacy and ablated arms respond, so this is not evidence
that removal improves overall balance.

Founder representation contracts in the sampled offspring. At the last swapped round,
offspring founder-ancestry Simpson diversity is 0.60742 / 0.77734 in the ablated areas
versus 0.68555 / 0.75781 in the legacy areas, down from roughly 0.93 / 0.94 after the
first draw. One area has lower diversity after ablation. These small single-ability
cohorts do not establish a healthy ecosystem, natural niche formation or permanent
lineage extinction; all four emergence criteria remain open. No coefficients were tuned
to improve these numbers.

Reports under `target/reinforcement-ablation/` contain full trajectories and canonical
parentage/expressed/latent/activation data. Gene/performance columns describe the current
cohort; diversity/founder/parent columns describe the offspring draw from that cohort.

## Validation

Final focused Docker validation passed **72 tests across 14 classes**. The complete clean
build passed **544 tests across 106 classes**, zero failures/errors/skips and Maven exit 0,
in **31:58**. All **528** frozen executable inputs and **563** production entries match
the focused-tested build. Both generation experiments match across focused/full JVMs.
Java 25/API 26.2, removal of the old transform from bytecode and exclusion of test controls/
optional API classes were verified. Only the experience engine and its enum helper differ
from the merged validated artifact; the other **561** production entries match.
No new ability, movement event, protection/permission hook,
storage format, optional-plugin integration or XP behavior changes. Wild mutation remains
held. This slice adds no server run or live deployment.
