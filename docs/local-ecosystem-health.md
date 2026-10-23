# Local ecosystem health diagnostics

> Historical #419 baseline and diagnostic behavior. The subsequent
> [habitat-aware competition](habitat-aware-competition.md) follow-up changes breeding
> context and advances ecology storage to version 4. The findings below remain the
> unconditioned control, not results from the new mechanism.

This first health slice supplies local measurements and a controlled multi-seed
baseline. It changes no reproductive weight, genetic transform, diversity target or
population quota. Habitat-aware competition remains a separate design change.

## Live diagnostics

Use `/ol debug ecosystem health [player]`. Players require `obtuseloot.debug`, including
OPs; console must name an online player. The command reads around that player's actual
location, using the existing inheritance radius (default **20 chunks**, same-world
horizontal circle) and recent-outcome game-tick window (default **6000 ticks**).
It adds no new settings, scheduler, plugin dependency or persistence format.

`LocalEcosystem.healthSnapshot` reports:

- Items with recent measured evidence for their **current seed/revision**, including
  unsuccessful items; positive finite fitness determines breeding eligibility separately.
- Excluded old seed/revision records, non-finite fitness and unassigned lineage counts.
- Observed assigned-lineage richness and Gini-Simpson diversity. Unassigned items are
  reported separately and excluded from that diversity denominator.
- Fitness-weighted effective parent count, `1 / sum(p_i^2)`, and largest individual parent
  probability. These describe the current sampler's probabilities, not a desired count.
  A non-finite raw sampler total is flagged and its probability readout is unavailable.
- Mean and population variance of every expressed and latent gene, without a variance
  target or any claim that every genetic difference changes gameplay.
- Attempts, meaningful outcomes and mean validated utility by existing mechanic/trigger;
  observed benefit/recovery means and their sample count when performance was measured.

Activities remain separate so incompatible benefit units are not averaged together.
An empty window supplies empty gene/activity maps, not invented zero measurements or
a healthy verdict. Non-finite utility is unavailable, not zero. A profile read failure
propagates; it cannot silently become an empty census.

The read-only evidence query filters expired samples without pruning stored state. It
does not advance time, save profiles/ecology, mutate genes/reputation or draw randomness.
Normal reproduction still prunes on its existing schedule/query. The shared scoring
path uses the same per-mechanic repeat/no-op penalties as before; recording, sampling,
pruning and encode/decode source bodies match #418 exactly. Version 3 storage is retained.

This reports **recently observed items and lineages**, not a census of all owned gear.
Unobserved lineages can return; a missing lineage in a window is not permanent extinction.
The activity readout still combines habitats inside its radius and cannot establish
habitat-specific adaptation. The older aggregate dashboard/health-gauge classifications
are a separate pipeline and are not replaced by this staff command.

## Multi-seed baseline

`LocalEcosystemHealthBaselineTest` compares four scenarios across eight seed pairs
(722/723 through 7722/7723, increasing by 1000): fitness-weighted or neutral reproduction,
with habitats either **160 blocks (10 chunks)** or **2048 blocks (128 chunks)** apart.
The actual reproduction radius remains 20 chunks in every scenario. There are 32 items
per site and 12 rounds. The fixture starts with a deliberate kinetic-gene range; that
is experimental input, not a production distribution target.

One site has open headroom, the other a 0.0784-block response cap. Observations use the
existing catalog-owned Double Jump output/recovery model and six-tick modeled motion.
These are **modeled observations**, not new native gameplay, obstacle completion or
player-usefulness measurements. Macro telemetry and seeded environmental pressure are
held fixed as in the preceding ablation experiment.
The fixture directly assigns Double Jump observations; it does not simulate players'
ability choices or competition between generated ability profiles.

The experiment executes real reproduction, ordinary mutation, genetic transitions,
revision-specific measured-utility classification, profile/ecology reload and evidence
expiry. Offspring start with empty fitness and preserve producing parent seed/revision.
Parent locality is checked against each donor's actual site, not assumed from its ID.
Owned profiles remain after evidence expiry. Fixed cohort replacement is an experimental
protocol, not a production population cap or lineage-extinction mechanism.

The neutral control uses one RNG draw to choose an eligible current parent uniformly,
then maps that draw into its interval in the unchanged production sampler. Child seed
draws, inheritance and mutation still use production code. A deterministic quantile-grid
test verifies equal parent chances despite unequal measured fitness. The neutral arm
is a counterfactual, not a proposed production rule. Its `effective_parents` and
`max_parent_share` CSV columns describe the **nominal production fitness weights**;
they are not the mapped neutral control's actual equal probabilities.

The overlapping measured scenario is independently replayed. All 24,576 recorded birth
records match the initial pre-diagnostics baseline exactly, and the three older generation
experiments match #418's archived output. This guards against changing selection while
adding measurements. Reports under `target/local-ecosystem-health/` include raw parentage,
all expressed/latent genes and activation flags, per-site performance, genetic variance,
founder ancestry, nominal parent concentration and cross-site parent draws.

## Findings

Mean last-round low-headroom results over the eight seed pairs:

| Reproduction and layout | Recovery ticks | Local utility | Founder Simpson | Parents from other site / 32 |
|---|---:|---:|---:|---:|
| Measured, overlapping | 63.68 | 0.03553 | 0.64355 | 30.25 |
| Neutral, overlapping | 58.00 | 0.03839 | 0.81714 | 15.63 |
| Measured, separate | 37.04 | 0.05016 | 0.49658 | 0 |
| Neutral, separate | 59.74 | 0.03789 | 0.65356 | 0 |

The modeled low-ceiling benefit is **0.0784 blocks** in all four scenarios. Overlapping
measured pools have longer recovery than separate measured pools in **all eight seed
pairs**. At the last round, 30.25/32 low-site births on average select parents whose
recent observations came from the open site. Raw mean utility at the open site is
larger and outweighs successful low-cost constrained responses inside the shared radius.
This identifies a habitat-mixing problem in this model, not a server-wide locality bug.

Founder ancestry contracts even under neutral reproduction. The separate measured
low-site cohort has fewer represented founders and lower genetic variance, yet lower
recovery and higher local utility. Adding a founder or gene-variance floor would confuse
specialization with failure. The overlapping case also shows that more founder diversity
can accompany worse local performance. These findings motivate measuring local functional
tradeoffs alongside ancestry, before changing competition rules.

No engine coefficient, quota, floor, distribution threshold or health pass mark was added
to improve these results. They do not establish natural niches, permanent lineage birth/
extinction or the four emergence criteria. More abilities with heritable performance,
measured habitat/opportunity context and appropriate competition remain future work.

## Validation

Initial baseline: one test, Maven exit 0, 1:17. Initial local evidence/command checks:
28 tests, zero failures/errors/skips. Final focused Docker package: **49 tests across
9 classes**, zero failures/errors/skips, exit 0, **1:43**. All **533** executable inputs
match the frozen snapshot. The JAR contains **571** production entries; all 563 #418
entries remain, with eight new diagnostics classes. Eight existing class files change
within the pool, local ecosystem and command layer, including nested record/evidence
classes. No movement, ability-performance or genetic-transform class changes.
Full clean Docker validation passed **557 tests across 109 classes**, zero failures/
errors/skips, Maven exit 0, **34:40**. All 533 frozen inputs and 571 production entries
match the focused-tested build. All four generation-report sets match across focused/
full JVMs; the unchanged #418 control also matches every new birth record. Java 25
class major 69/API 26.2 and exclusion of optional APIs/test controls were verified.
Generated tracked reports were restored.

The initial baseline omitted a frozen source manifest. An independent control replay
closed that gap: **531** frozen inputs, including all **530** existing source/build
blobs verified against unchanged #418 and a preserved test-only benchmark. All **24,576**
births and **768** shared generation rows match. The two diagnostics-only fields are
explicitly unavailable in that control, whose production code has no health-snapshot API.

No new movement effect, protection/permission policy, integration/XP behavior, server run
or live deployment. Standalone operation and PlaceholderAPI/LuckPerms/AuraSkills coexistence
paths are unchanged. Wild mutation remains held. The new command shares the existing
effective debug permission gate; there is no OP bypass.
