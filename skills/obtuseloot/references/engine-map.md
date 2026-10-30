# ObtuseLoot engine map (baseline verified at HEAD fab9f2b, 2026-10-19)

**2026-10-30 outcome evidence:** unobserved items retain the classifier's GENERALIST
profile; the memory/lineage-inferred niche fallback is removed. Empty memory formerly
broke ties as NAVIGATION, favoring mobility before any use. Unseen-template
qualification now receives the same outcome-derived profile. Existing memory,
lineage and utility scoring remain; coefficients and regulatory gates do not change.
The generated-cohort diagnostic also reports complete ability-set Simpson beside
the legacy dominant-tag value. See `docs/outcome-evidence-niche-fix.md` and its run.

**2026-10-29 utility batch:** four existing templates gain live catalog handlers and
genome-owned output/recovery models. Per-item format 5 adds validated ID-keyed
waypoints; shared recovery and current-revision fitness remain. Generic utility
delivery reserves owned state/recovery before sending and rolls back failed delivery.
Chunk sensing accepts ordinary boundary crossings and retains final cancellation;
entity inspection uses the general interaction event. Read
[`docs/noncombat-ability-batch.md`](../../../docs/noncombat-ability-batch.md).

**2026-10-29 replay inputs:** founder lineage IDs include a random storage UUID;
ordinary lineage drift seeds on the lineage ID as well as the artifact seed.
Item measurements now control identity on a separate input stream through a
package-private test constructor; public constructors keep random UUIDs. Read
[`docs/ability-replay-inputs.md`](../../../docs/ability-replay-inputs.md).

**2026-10-28 item variation follow-up:** inherited seed affinities receive small
birth-only deltas on a separate seeded stream; parent choice and ordinary genome
mutation are unchanged. The name composer uses the stored naming seed with the
existing operator prefix/suffix pools and complete-entry length checks. Read
[`docs/item-affinity-name-variation.md`](../../../docs/item-affinity-name-variation.md)
for the measured collision rates and limitations. Exact hidden identity and
distinct visible names/abilities are different measurements.

**2026-10-24 holder target context:** ecology samples (storage version 6) record
holder and exact action-target material. `LocalInheritancePool.selectionCandidates`
weights each competing parent's per-target fitness by the receiving holder's recent
target mix, after the unchanged nearest-column filter; no holder or no overlap keeps
pooled fitness. Read [`docs/holder-target-context.md`](../../../docs/holder-target-context.md).

**2026-10-23 independent-condition diagnostic:** four crossed target/ceiling sites
and varied assigned loadouts/opportunities retain the production sampler. An
inspection-only witness shows crop/stone evidence at the same column height
receives identical pooled weights despite different target-specific tradeoffs.
The population experiment records a stone/sky output/cost deficit and keeps
other outcomes separate; it adopts no context rule or health verdict. Read
[`docs/independent-ecosystem-conditions.md`](../../../docs/independent-ecosystem-conditions.md).

**2026-10-23 native habitat validation:** the first overhead-relative model selected
neighboring-room parents for native low-ceiling loot because airborne and chest
reference heights differed. The correction captures lower/upper column boundaries,
uses their span and stores explicit version 5 units. No sampler weight or genetic
transform changes. Read [`docs/native-habitat-validation.md`](../../../docs/native-habitat-validation.md).


**2026-10-22 habitat-aware competition follow-up:** captured overhead-column evidence
conditions current-genome parent competition inside the unchanged chunk radius/window.
Nearest supported depths retain their original measured utility, including failure and
repeat penalties. Read [`docs/habitat-aware-competition.md`](../../../docs/habitat-aware-competition.md)
for version 4 ecology storage, the broader census versus conditional breeding diagnostics,
and limitations. This is a first measured contextual mechanism, not proof of emergence.


**2026-10-22 local health follow-up:** a read-only census and staff command expose
current measured item/revision eligibility, observed lineage/gene variation and actual
benefit/recovery data inside the existing chunk radius/window. Shared scoring preserves
the parent sampler; the new multi-seed neutral and overlapping-habitat controls identify
modeled habitat mixing while distinguishing ancestry contraction from useful specialization.
Read [`docs/local-ecosystem-health.md`](../../../docs/local-ecosystem-health.md).

**2026-10-22 ecological gene-multiplier follow-up:** the blanket context transform
retained by #417 is removed from active and latent genes. Fixed trait-specific
environmental modifiers and existing ecological mutation/support controls remain.
Read [`docs/ecological-gene-multiplier-ablation.md`](../../../docs/ecological-gene-multiplier-ablation.md).
The earlier comparison below is now reproduced with frozen test controls.

**2026-10-22 genetic reinforcement ablation:** aggregate fitness no longer directly
multiplies every expressed/latent trait at genetic transitions. Per-revision measured
history, ordinary variation and local measured parent selection remain. Other existing
context/ecological and fixed environmental biases still alter genes. The paired
mid-run condition-swap experiment and its limits are documented in
[`docs/blanket-gene-reinforcement-ablation.md`](../../../docs/blanket-gene-reinforcement-ablation.md).
This removes one term, not every historical aid described below.

**2026-10-20 follow-up:** the first effects slice implements Gentle Harvest and Material Insight with
observed outcomes, protection checks and immediate utility-history updates. It also replaces the old
post-break replant callback. Unfinished live effects return no-op. The baseline descriptions below
remain historical; see `status.md` and the repository's `docs/measured-ability-effects.md` for the new
implementation and its validation limits. Movement sensing and noncombat evolution timing remain
separate follow-ups.

**Local-inheritance follow-up:** the timing branch now has game-tick measured-outcome progression. The subsequent local-inheritance work adds per-item profiles, persistent genomes and parent selection from recent outcomes in a configurable 20-chunk range. Read `docs/local-artifact-inheritance.md` and `status.md` for its review state and limits; it does not establish all emergence criteria. The one-player-profile and seed-only genome descriptions below are historical baseline behavior.

**2026-10-21 compression ablation:** the follow-up removes the final top-three
weight compressor from production. Its historical policy survives only as a test
control. The rest of the selection pipeline remained at that snapshot; the next
normalization follow-up is described below. Read
[`docs/top-three-compression-ablation.md`](../../../docs/top-three-compression-ablation.md)
for the paired fixture and limits. The current live catalog has at most two
implemented templates per category, so this layer was dormant there.

**2026-10-21 normalization ablation:** the next follow-up removes gamma and mean
blending from within-category score preparation. The mean-relative soft-cap formula,
constant and original-mean reference remain for a separate ablation. The old transform
exists only in a paired test control that calls the same production cap. Read
[`docs/score-normalization-ablation.md`](../../../docs/score-normalization-ablation.md)
for the generation-trace comparison, multi-entry live-category scope and limits.

This file condenses what the 2026-10-19 doc-ingest agents verified against source. The full evidence, with file:line citations, is in `ingest-2026-10-19-fab9f2b/`. Items marked **inference:** were not directly verified. "What confirms it" says how to settle each one.

Line numbers drift. Re-grep before citing any of them in a PR.

**Heritable-performance follow-up, 2026-10-21:** Double Jump expresses persisted kinetic
bias through a catalog-owned output/recovery model. Peak observed rise and committed
recovery feed item utility and local reproductive fitness; unchanged registrations retain
fixed performance. Local evidence format is version 2. Read
[`docs/heritable-ability-performance.md`](../../../docs/heritable-ability-performance.md)
and dated validation status; this is not proof of natural niche formation.

## Contents
1. Runtime wiring and lifecycle
2. Identity and persistence
3. Ability generation pipeline
4. Ecology: niches, bifurcation, lineage, pressure
5. Metrics as actually computed
6. Scaffolding inventory (the removal list)
7. Tooling: what runs the real engine and what doesn't
8. Unverified inferences to settle

---

## 1. Runtime wiring and lifecycle

- **Bootstrap.** `ObtuseLoot.onEnable` runs in this order: config/runtime settings and paths, then telemetry (archive, buffer, service, `RollupStateHydrator`), then persistence (a failure disables the plugin), then `EngineBootstrap` engines, then commands, then reports and scheduled tasks, then `ObtuseEngine` listeners, then `EngineScheduler`.
- **Engines.** `EngineBootstrap.initialize` builds these live:
  - `EvolutionEngine`
  - `ArtifactUsageTracker`, which owns `NichePopulationTracker` and `NicheBifurcationRegistry`
  - `ArtifactEcosystemSelfBalancingEngine`, which owns `EnvironmentPressureEngine`
  - `ExperienceEvolutionEngine`
  - `DriftEngine`, `AwakeningEngine`, `ConvergenceEngine`, `ArtifactMemoryEngine`
  - `LineageRegistry`, which owns `LineageSpeciationEngine`. Species snapshots are restored from the store.
- **The niche ecology runs live, not only in simulation.** Live feed: `ItemAbilityManager` -> `ArtifactUsageTracker.trackAbilityExecution` -> `NichePopulationTracker.recordTelemetry` -> throttled `evaluateBifurcations`. The simulation harness (`WorldSimulationHarness`) calls the same entry point.
- **Kill path.** `ArtifactProcessor` handles kills: reputation, evolution, ability dispatch, drift, awakening, convergence, subscription rebuild, history, action bar. It is the highest-coupling class.
- **Abilities have no in-world effect.** `AbilityExecutor.execute` returns a status, an outcome classification and presentation text, and performs **no Bukkit world operation**. Ability "success" telemetry therefore measures classification, not gameplay. Fixing this is roadmap item 2.
- **Known listener bugs:**
  - `NonCombatAbilityListener` movement sensing checks a single move-event displacement against a squared distance of 16. It does not accumulate small steps.
  - The HARVEST_RELAY replant callback reads the block after it has been broken. The expected replant failure is inferred from the code, not reproduced.
- **Progression thresholds.**
  - Evolution stages are reached by total reputation: 10, 25, 45 and 70 (`config.yml`, `EvolutionEngine`).
  - Drift chance is 0.05 + chaos x 0.01 - consistency x 0.005, capped at 0.40.
  - Awakening requires memory pressure >= 5, history >= 10 and kills >= 6, then an archetype-specific check.
  - Convergence is a single-artifact identity transformation.
  - The real stage names are `-formed`, `-tempered`, `-advanced` and four hybrid names. `EVOLUTION_FUSION_TABLE.md` is mostly dead. `paragon` is never returned.

## 2. Identity and persistence

- **One artifact per player.**
  - `ArtifactManager` caches by player UUID, and storage keys are `player:<uuid>`.
  - `give`, `convert`, `reroll` and `give-specific` replace the player's single artifact.
  - `LootInsertionListener.stampIfEligible` stamps every non-air item from generated loot or enchanting with that player's key; it has no equipment filter.
  - The decided target is per-item identity with equipment-only stamping (see decisions.md).
- **Item PDC v3** holds the storage key, owner UUID, seed, mechanic fingerprint and memory pressure. The durable profile lives in the store, not on the item.
- **Load failure is treated as a missing record.** `JdbcPlayerStateStore.loadArtifact` catches non-`IllegalStateException` exceptions, logs them, and returns `null`. `ArtifactManager.getOrCreate` treats `null` as absent and generates a new identity, which later saves over the real row. The decided behavior is fatal at startup, and at runtime a per-player refusal with retry.
- **Saves.** SQL saves log failures without reporting success or failure to the caller. The cache marks the entry saved regardless. Some save paths save the same profile twice.
- **SQL I/O.** It is synchronous `DriverManager` I/O from join, commands and autosave. The MySQL pool settings are parsed but never used.
- **YAML** uses a single-executor, revision-aware write queue.
- **Item migration.** `ArtifactItemStorage` migration also matches modern items (the current storage key) and restamps them to whoever interacts with them.
- **Convert/reroll** replace the main-hand item with `new ItemStack(material)`, so enchantments and durability are lost.
- **Ecology state is not persisted.** No persistence was found for `NicheBifurcationRegistry` or `NichePopulationTracker`. **inference:** child niches and the forced-bifurcation flag reset on restart. What confirms it: read `RollupStateHydrator` and the registry/tracker constructors.

## 3. Ability generation pipeline

`ProceduralAbilityGenerator.generate(artifact, stage, memoryProfile)` runs these steps:

1. **Context.** Parse utility history, assign a lineage, and get support/ecology pressure from `ExperienceEvolutionEngine`.
2. **Genome.** `GenomeResolver.resolve(seed)` -> `GenomeMutationEngine.mutate` -> `LineageGenomeInheritance.inherit` -> `applyExperienceFeedback` -> latent trait activation.
3. **Regulatory gating.** `RegulatoryEligibilityFilter` keeps templates with gate weight >= `MIN_ELIGIBILITY = 0.18`. If nothing survives, the full registry is used. **inference:** this is the only place the genome affects template choice.
4. **Niche profile and variant.** The niche profile comes from utility history, or from hand-written formulas over the memory profile when there is none. The variant is ALPHA/BETA, the active pool comes from `AbilityDiversityIndex`, and a motif anchor is chosen.
5. **RNG.** `new Random(seed ^ path hashCodes ^ memory pressure)`.
6. **Picks.** 2 before awakening, 3 at stages 4-5. Selection has two stages, category then template (`selectNextTemplate`):
   - The composite template score multiplies about 15 factors: ecological yield (clamped), hand bonuses, a utility-history blend, trigger and niche saturation penalties, lineage and mechanic weights, `nicheWeight^1.58`, category weight, recency, lineage combination, motif anchor, novelty bonus, novelty floor, similarity penalty, tail preservation, cold boost, context dampening and an applicability boost.
   - Within a category, the scores then go through nonlinear normalization (gamma 2.50, mean-anchor blend 0.35), a soft top cap (1.28x mean), cold balancing, small-category flattening, **top-3 compression** (if top-3 share > 0.55, weights are pulled toward the mean until it is 0.50) and a 12% unseen-template override.
7. **Recording.** Recent-selection windows (template 96, category 144) and the diversity index are updated, and lineage descendant bias is recorded.

**Registry:** 88 templates in 10 categories: Traversal 9, Sensing 10, Survival 14, Combat 8, Defense 5, Resource 8, Crafting 5, Social 8, Ritual 12, Stealth 9. There are six families: PRECISION, MOBILITY, SURVIVAL, CHAOS, CONSISTENCY, BRUTALITY.

**Dead or by-construction checks:**
- The similarity weights sum to 0.82, so novelty is always >= 0.18. As a result:
  - `NOVELTY_FLOOR = 0.15` never fires;
  - the novelty gates on the tail, low-volume and unseen-override layers always pass;
  - the tests' `novelty >= 0.17` and `similarity < 0.83` are true by construction.
- **No runtime authenticity gate exists.** The "authenticity validation" is `scripts/deep_authenticity_validation.py`, which matches keywords and never runs the generator.
- `ColdCompletionDeepValidationTest` asserts only that its report file exists. Its strict criteria hard-code defense == 8 and ritual == 13, which the registry can never meet.

**Forced-probe results.** **inference:** these are explained by correlated `java.util.Random` first rolls across 500 sequential seeds, not by the scoring. Agent C reproduced the bands with `ingest-2026-10-19-fab9f2b/scripts/jrand*.py`. What confirms it: a JVM rerun with decorrelated seeds. This is separate from the `IntraCategoryNormalizationProbeTest` sensing flake (16 vs 27 hits on identical code, cause not isolated).

## 4. Ecology: niches, bifurcation, lineage, pressure

- **Parent niches are fixed.** They are values of the `MechanicNicheTag` enum. Only child niches (`<PARENT>_A<n>`/`_B<n>`) are created at runtime.
- **Bifurcation guards** (`NicheBifurcationRegistry.evaluateBifurcation`):
  - a pressure gate, with sustained pressure. The niche caps (8 global, 4 per parent) and the 60 s split cooldowns were **removed on 2026-10-26** (owner: splitting must be organic). A parent's split share and saturation exclude occupancy already absorbed into its children (relief). See `docs/organic-niche-splitting.md`.
  - **inference:** the effective gate is parent share >= 0.13 with 1 sustained window, and the specialization gate (>= 0.00) is vacuous.
  - **Share (2026-10-26):** every share reader divides rollup `occupancy` by total occupancy. Occupancy splits each artifact across its niches by utility mass (sums to one), and each signal's outcomes count only toward its own niches. Before this, an artifact counted fully in every niche it touched, which diluted share until no niche could split a second time (`docs/niche-split-gate-diagnosis.md`, `docs/niche-share-at-source.md`).
- **Evaluation timing.** Evaluation is throttled to 5,000 ms of wall-clock time. Each evaluation is one "window" for grace, establishment, collapse and age.
- **Forced bifurcation (removed 2026-10-25).** `FORCED_BIFURCATION_WINDOW = 3` guaranteed one bifurcation per tracker instance whatever the pressure. It was removed with one-occupant reconciliation; see `docs/niche-scaffolding-ablation.md`. Evaluation, cooldown and age now read the registry's supplied clock (wall time by default).
- **Child-niche aids:**
  - forced displacement and migration (seeded `0xC0FFEE1234ABCDEFL`);
  - locks (x1.30 utility);
  - inversion multipliers (child x1.30, parent x0.88);
  - lineage affinity (+0.05 per outcome, x0.93 decay per window, max 0.90);
  - continuity reconciliation that restores exactly one occupant to protected children before collapse is counted. This satisfies the zero-share success metric by construction.
- **"Extinction" in the docs** means a child niche going vacant. **In no recorded run did a lineage arise or die, and the artifact population was constant.**
- **Environment pressure.** `EnvironmentPressureEngine` uses 4 hard-coded events (DriftStorm, PrecisionAge, SurvivalWinter, MobilityBloom) rotated by `Random(7331)`, lasting 2-5 seasons each, with no feedback from the ecosystem. It advances live every 24,000 ticks.
- **Self-balancing.** `ArtifactEcosystemSelfBalancingEngine.evaluate` consumes simulation metric types. Live code reads the guard multipliers. **inference:** the full evaluation is driven only by the harness.
- **Two pressure-engine instances.** Experience feedback advances one, and regulatory gating reads the self-balancing engine's instance.
- **Speciation constants.** `LineageSpeciationEngine` has compatibility 0.66 / strict 0.74, persistence 5 generations and 8 minimum observations. No doc covers it.

## 5. Metrics as actually computed

These come from `telemetry/ScheduledEcosystemRollups` and the aggregation buffer:

- `diversityIndex` = number of niche keys / active artifacts. The keys include dead and zero-population children, so the index is unbounded; 1.67 has been observed.
- `turnoverRate` = **cumulative** branch births plus collapses / active artifacts. It grows with run length.
- Child share is a share of 29-32 artifacts in the current harness runs, so one artifact is about 3.4%. Before 2026-03-18 it was computed over cumulative counts, so values across the docs are not comparable.
- `dynamicNichePopulation` holds event counts, not artifact counts.
- END and TNT are never defined in the docs. **inference:** from method names, `effectiveNicheDiversity` and `temporalNicheTurnover` in `EcosystemHealthGaugeAnalyzer`.
- The open-endedness verdict and its confidence lines are **string literals** in `OpenEndednessTestRunner`.

All of these are to be redefined, and the verdict computed from data (decisions.md).

## 6. Scaffolding inventory (the removal list)

Agent C counted about 20 corrective layers in ability selection, added 2026-03-18 to 03-24, mostly to make top-share, top-3 or reachability numbers pass. Three of those passes ended FAILED and their layers were kept anyway.

### 6a. Owner-ruled: remove (2026-10-19), each behind an ablation test

| Area | Mechanism |
|---|---|
| Ability selection | Top-3 compression (`applyConditionalTopThreeCompression`, 0.55 -> 0.50); nonlinear normalization (gamma 2.50, mean anchor 0.35); soft top cap 1.28x; category-local cold balancing; small-category flattening (<= 6 templates); 12% unseen-template override; under-sampled applicability boost (<= 1.16); tail preservation (<= +10%); cold template boost (<= +22%); context-align dampening; novelty floor (dead); similarity penalty bands; recency bias; trigger and niche saturation penalties from static template counts; ecological-yield clamp; low-volume category boost; category exposure bias; niche weight applied twice |
| Ecology | Forced bifurcation (`FORCED_BIFURCATION_WINDOW=3`); one-occupant continuity reconciliation. **Both removed 2026-10-25** (`docs/niche-scaffolding-ablation.md`) |
| Process | Analytics PROPOSED -> ACCEPTED human tuning loop |
| Pressure | Fixed 4-template environment pressure (`Random(7331)`). This one is **replaced** by co-evolving pressure, not simply removed. |
| Config | `analytics.ecology.*` block (never read live). Delete it. |

### 6b. Classified top-down by the ingest agents, NOT ruled by the owner: ask before removing

| Area | Mechanism |
|---|---|
| Ecology | ~~Bifurcation caps (8 global / 4 per parent)~~ and ~~split cooldowns~~ removed 2026-10-26 (owner ruling); forced displacement and soft migration (removal pending a corrected observed-membership design); ~~niche locks (x1.30)~~ and ~~inversion multipliers (child x1.30 / parent x0.88)~~ removed 2026-10-27. Note the owner did rule that timing moves to **game time** and state **persists**. |
| Safety | Production safety guards / dominance suppression (`safety.*`, combined floor 0.75). These came from the v0.9.41/42 "production hardening" work. |
| Progression | Fixed tier gates 10/25/45/70; archetype inertia 2.0 and switch margin 4.0; drift cap 0.40 |

**Outcome-driven inputs that exist today:**
- the utility-history blend;
- `ExperienceEvolutionEngine` feedback;
- lineage affinity from child-niche outcomes;
- reputation from real play.

These are the hooks to build on. Note that the acceptance probes never wire the experience engine or set utility history, so the existing distribution tests measure only the top-down layers.

## 7. Tooling: what runs the real engine and what doesn't

| Tool | Runs Java engine? |
|---|---|
| `WorldSimulationRunner` / `WorldSimulationHarness` (world-lab) | Yes |
| `OpenEndednessTestRunner` | Yes, but the verdict text is hard-coded |
| `AnalyticsCliMain` (analyze / run-spec / decide / export-accepted) | Reads Java telemetry datasets |
| `scripts/run-chaos-tests.sh`, `run-population-simulation.sh`, `run-meta-analysis.sh` -> `codex/run_internal_pipeline.py` | **No.** Python with hard-coded lists and modulo arithmetic; the only writer of `analytics/ecosystem-balance-data.json`, which the dashboard reads |
| `scripts/deep_authenticity_validation.py` | **No.** Keyword matching over registry text |
| `benchmark.yml` workflow | **No.** Pure Python |
| world-lab workflow inputs (players/seasons/mode) and the open-endedness matrix modes | Exported as env vars that no code reads |

The decision is to rewire the Python tooling to Java harness output (decisions.md).

## 8. Unverified inferences to settle

| Claim | Source | What settles it |
|---|---|---|
| **SETTLED 2026-10-19:** JDBC drivers (sqlite-jdbc, mysql-connector-j) are NOT in the jar. A `-DskipTests package` jar had zero `org/sqlite` or `com/mysql` entries. Whether the server supplies them is still unverified (inference: Paper bundles both). Fix options: plugin.yml `libraries:` or shading | A, verified | Check the target server's bundled libraries |
| Root `obtuseloot.command` (op) blocks non-ops from help/info | A, B | Bukkit behavior on a test server, or read Paper's command permission handling |
| Child niches and forced-bifurcation state reset on restart | D | Read `RollupStateHydrator` and the registry/tracker init |
| Effective bifurcation gate is share >= 0.13 with one window; specialization gate vacuous | D | Unit test against `NicheBifurcationRegistry`; check whether `specializationScore` can go negative |
| Forced-probe shares come from correlated sequential-seed first rolls | C | JVM rerun with decorrelated seeds |
| Genome influences template choice only through the regulatory filter | C | Trace `genome` usage in `ProceduralAbilityGenerator` |
| Self-balancing `evaluate` is harness-only | D | Find all callers of `evaluate` |
| Sensing-probe flake source (16 vs 27 hits, same code) | baseline runs | Not needed if the floor is dropped (ask owner); otherwise run the class alone repeatedly with fixed UUIDs |

**2026-10-25 recovery and shared recovery:** parent fitness and per-target fitness are
total validated utility in the window; `AbilityPerformanceMeasurement.adjustUtility`
no longer divides by recovery. `Artifact.abilitiesReadyAt` is the latest entry in the
per-ability ready-time map; Material Insight, Gentle Harvest and native/modeled Double
Jump check it, so abilities on one item share recovery. Passive effects record no
outcomes and are not gated. Read [`docs/recovery-selection-research.md`](../../../docs/recovery-selection-research.md).
