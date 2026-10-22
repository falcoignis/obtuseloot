# Ecological gene multiplier ablation

The owner approved removing the remaining blanket ecological/context gene multiplier
after the aggregate-fitness ablation in #417. This is a dependent follow-up to #417;
it does not authorize merging either PR.

## Production change

`ExperienceEvolutionEngine.applyExperienceFeedback` previously multiplied every active
trait by `ecoTraitBias`, then by a separate trait-specific environmental modifier.
The same context multiplier also nudged every latent trait toward its direction:

```text
expressed = clamp01(gene * context * environmental)
latent = clamp01(latent * (1 + (context - 1) * 0.35))
```

The removed context formula combined retention, specialization, repulsion, lineage
influence and adaptive support into one number clamped to [0.90, 1.12]. With neutral
pressure/support/influence it was 0.95: active genes declined 5% and latent genes
declined 1.75% at each transition even without evidence for a direction.

The new feedback transform is:

```text
expressed = clamp01(gene * environmental)
latent = unchanged
```

This applies at subsequent explicit genome transitions. Profile lookup still does not
reroll or rewrite saved genes; existing genomes are not retroactively restored.

Seed and latent activation flags are preserved. Ordinary mutation and latent activation
still run elsewhere in the transition pipeline. Public constructor
and feedback signatures remain compatible. The removed formula exists only in a test
control frozen from #417 head `db1928072f7c861426ecab0abdd29a211ce2140f`.
The feedback method no longer requests an adaptive-support allocation just to calculate
this factor, so it also stops emitting that redundant allocation telemetry event. The
transition's existing mutation-support calls and their telemetry remain.

This removes one genetic transform, not all ecological scaffolding. Existing ecological
mutation amplitude/support, lineage mutation tendencies, template weighting and the
four fixed environmental events remain. The environment still directly alters specific
active traits and can clip them at the existing genome boundary. Co-evolving environmental
pressure remains future work. Ordinary mutation, revision-specific fitness, measured
reputation and measured local reproduction are retained. Wild mutation stays held.

## Controlled comparison

The new arm in `ReinforcementEnvironmentSwapTest` compares production against the frozen
#417 transform. It shares the same protocol as the earlier experiment: 32 artifacts per
area, 12 rounds, two same-world areas 2048 blocks apart, actual configured 20-chunk local
parent filtering, RNG seeds 722/723, and a swap between open/low-headroom conditions at
round six. Each arm also has a steady-condition control and the ablated swap is replayed
with independent state. Real genetic transitions, parent draws, profile/ecology codecs
and the measured-utility classifier execute. Offspring inherit parent seed/revision and
start with empty fitness; evidence expires without deleting owned profiles.

The six-tick Double Jump response is modeled using the catalog-owned benefit/recovery
tradeoff, not measured from a server. Unrelated wall-clock macro telemetry is held empty
in both arms; the same initial seeded environmental event is retained without advancing
seasons. Thus the multi-generation experiment isolates the neutral 0.95 term. A separate
regression exercises non-neutral pressure, support and lineage influence; another checks
the retained environmental transform across 24 seasons and preservation of latent genes.

The earlier aggregate-fitness comparison now uses the frozen #417 control as its treatment,
so its historical output remains reproducible rather than silently acquiring this second
ablation. Both experiments share the harness and emit separate reports. Reports include
all active/latent genes, activation flags, parent seed/revision, founder ancestry, response,
recovery, utility and variance. Gene/performance rows describe the current cohort;
founder diversity and distinct-parent columns describe the offspring draw.

No preferred mean, distribution, founder count or diversity threshold is imposed here.
Fixed cohorts and evidence expiry are experiment protocol, not new production quotas.
Assertions check actual parent-identity response, locality, revision integrity and exact
replay. Founder representation in these sampled descendants is not permanent lineage
extinction; all owned profiles remain. These single-ability modeled cohorts do not
establish natural niches or ecosystem health.

## Results and validation

Reports are written under `target/ecological-gene-ablation/`.
The new regression first ran against unchanged #417 production: three tests, two intended
failures (neutral 0.4 became 0.38; the non-neutral fixture produced 0.36).

All 384 pre-swap ancestry records match their respective steady controls. After swapping,
125 of 384 parent seed/revision draws differ from steady conditions with the context term
removed, versus 116 with it retained. Both arms respond; removal is not required to make
selection react. The ablated swap replays exactly, including parentage, active/latent genes
and activation flags. Random physical storage UUIDs are excluded from replay identity.

At round 11, the ablated cohorts have these means:

| Area | Steady condition: gene / recovery | Swapped condition: gene / recovery |
|---|---|---|
| 0 | Open: 0.52495 / 62.00 ticks | Low headroom: 0.27848 / 47.16 ticks |
| 1 | Low headroom: 0.16075 / 40.16 ticks | Open: 0.23554 / 44.59 ticks |

The same response cap in the low area yields 0.0784 blocks with either transform, while
the ablated swapped cohort's longer recovery lowers mean utility (0.04391 versus 0.04881).
In the swapped open area, mean modeled response is 0.98515 versus 0.96750 blocks and mean
utility is 0.54775 versus 0.54598. These are observations from one seed pair, not balance
claims. Variance remains nonzero; no variance target was added.

Last-round swapped offspring founder Simpson diversity is **0.55664 / 0.75781** after
ablation versus **0.60742 / 0.77734** with the context term retained: both areas are lower
in this run. Founder diversity still contracts. This is not evidence of a healthier
ecosystem, and no weight, floor, quota or coefficient was added to improve it. The earlier
#417 generation and parentage CSVs match its archived focused/full results exactly.

Focused Docker validation passed **49 tests across 12 classes**, zero failures/errors/skips,
Maven exit 0, in **10:53**. All **530** executable inputs match the frozen snapshot. Of
**563** production entries, only the experience engine and its generated enum helper
differ from the validated #417 JAR. The helper differs only in its debug source-line
reference (166 to 159); instructions and local-variable metadata match.

The full clean Docker build passed **548 tests across 107 classes**, zero failures/errors/
skips, Maven exit 0, in **30:31**. All 530 frozen inputs and 563 production entries match
the focused-tested build. The three generation-report sets and both earlier selection-
ablation reports match across focused/full JVMs. Java 25 class major 69, API 26.2,
removal of the blanket transform from production bytecode and exclusion of test controls/
optional API classes were verified. Generated tracked reports were restored.

This slice changes no movement event, permission/protection callback, equipment binding,
storage format, optional-plugin API or XP behavior. It adds no server run or live deployment.
