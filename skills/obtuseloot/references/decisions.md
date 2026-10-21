# ObtuseLoot decision ledger

The project owner (falcoignis) made these decisions on 2026-10-19 after an ingest of all repo documentation at HEAD `fab9f2b`. They are authoritative. They override anything in the repo's own docs, CLAUDE.md, or historical reports. If a later conversation changes one, edit the entry and date the change. Don't leave two versions side by side.

## Core goal

**A truly emergent evolution engine.** "Emergent" counts as demonstrated only when all four of these hold:

1. **Survives ablation.** Niche and ability diversity persists with the top-down aids switched off. That means forced bifurcation, one-occupant reconciliation, top-3 compression and normalization floors.
2. **Lineage birth and extinction.** New lineages actually arise, and old ones actually die out. In every recorded run so far the lineage set never changed.
3. **Open-ended niches.** New niches can appear beyond the fixed `MechanicNicheTag` set and beyond binary A/B splits of existing parents.
4. **Driven by real outcomes.** What survives is decided by measured in-game outcomes of ability use, not by scoring weights.

**Owner emphasis, 2026-10-20:** the evolutionary algorithm is the main selling point. The owner wants a true functioning ecosystem. The four criteria above remain the acceptance standard; reproduction plumbing alone does not establish them.

**Second core goal (owner, 2026-10-19): a healthy ecosystem with a wide range of options and niches.** Open-ended, emergent evolution must not collapse into a few dominant templates or niches. Diversity is a goal in its own right. It should be **measured** (bounded per-window metrics over live niches) and **produced by emergent mechanisms**: selection from real outcomes, competition and niche partitioning. It should not be forced by quotas, floors or normalization. If emergent dynamics collapse diversity, that is a real defect to fix in the mechanisms, not something to accept or to paper over with a cap.

**Third requirement (owner, 2026-10-19): keep a random mutation chance that can disrupt otherwise deterministic outcomes.** This is designed randomness. Today `DriftEngine` already rolls `ThreadLocalRandom` (0.05 base chance). It is distinct from *accidental* nondeterminism, such as JVM-randomized `Map.copyOf`/`Set.copyOf` iteration order changing seeded genome mutation. Accidental nondeterminism is a bug (#401) and gets fixed. Designed mutation chance must be kept, and should be an explicit, measurable roll. Never remove or seed away the designed chance while fixing reproducibility. **Add a wild genome-mutation roll** (2026-10-19): a small true-random chance per evolution step to apply an extra, unseeded mutation, logged in telemetry so its effect is measurable. Simulations may inject the RNG to replay runs. Chance and magnitude are not decided yet; propose values and ask. **2026-10-20 follow-up:** the owner said "Hold wild mutation; do the test first". Wild mutation is deferred from PR #403; chance and magnitude remain undecided.

## Evolution design

| Topic | Decision |
|---|---|
| The ~20 corrective/top-down layers in ability selection and ecology (normalization, soft caps, top-3 compression, cold bootstrapping, applicability boosts, exposure rebalance, forced bifurcation, one-occupant reconciliation, etc.) | **Scaffolding to remove.** Replace with outcome-driven selection, then delete. **2026-10-21 follow-up:** after the top-three ablation, the owner approved removing gamma/mean score normalization next, with the soft cap evaluated separately afterward. |
| Distribution targets (top template share <= 50%, top-3 share near 0.50, every template reachable with minimum hit counts) | **Tuning artifacts, not design goals.** Tests should catch bugs, not shape outcomes. Templates may concentrate or go extinct. |
| Artifact identity | **Per-item.** Each physical item is its own evolving individual and keeps its history through trades. The old one-profile-per-player model is wrong. The local-inheritance follow-up introduces durable item keys, inherited genomes, item reputation and holder binding; see `docs/local-artifact-inheritance.md` for supported activation and validation limits. |
| Reproduction | **New loot inherits from successful local lineages, with variation** (owner approved the recommendation, 2026-10-20). Existing items retain identity/history and evolve through use. No arbitrary deletion of owned items to simulate extinction. |
| Local inheritance range | **Configure in chunks, default 20 chunks** (owner, 2026-10-20). Local means a range around the loot's actual generation position in the same world, not a whole map/server. Implementation uses a horizontal radius; only recent measured activity inside it qualifies. The 6000-game-tick evidence window is an implementation default, not a separately confirmed owner value. |
| Real in-world ability effects | **Top priority** within the engine work. Implement a small set of real, measurable effects and feed their outcomes back into selection. **Owner focus, 2026-10-20:** keep combat effects minimal; prioritize social, interaction and exploration effects. The owner approved **Gentle Harvest** (replant a mature crop using an existing planting item, respecting protection checks) and **Material Insight** (information about the inspected block) as the first slice. |
| Noncombat evolution timing | **Effects and feedback first; noncombat timing next** (owner, 2026-10-20). Effects landed in #405. The owner then requested the timing follow-up. Its implementation uses a configurable 100-game-tick window, measured utility earning existing-family reputation with durable fractional credit/debt, and ordinary stage evaluation/subscription refresh. These defaults are implementation choices proposed during the follow-up, not separately confirmed owner values. Existing progression/awakening/convergence gates and designed drift remain unchanged. |
| Ability extensibility | **Make abilities easy to add/remove** (owner, 2026-10-20). One catalog registration owns metadata and its optional live handler, shared by selection and execution. Timing consumes generic measured outcomes without ability IDs. Live selection includes implemented entries; the complete catalog remains available offline. Unsupported variants retain their executable base effect and record that fallback. Catalog code changes require restart. |
| Equipment and passive abilities | **Support passive abilities, off-hand use, secondary click, armor and elytra** (owner, 2026-10-21). **Speed first, then double jump**: the owner approved a configurable 5% default and strongest-copy default, with an **additive stacking option**, not multiplicative artifact stacking. The equipment follow-up implements six-slot binding, item-specific credit and owned passive-effect lifecycle; the next Double Jump follow-up is an opt-in native-input slice, with per-player charges and shared per-item feedback. The 0.42 strength / 60-tick cooldown are configurable implementation defaults, not separately confirmed owner values. The owner selected a disposable local Purpur 26.2 server for validation; no live deployment is authorized. Read `docs/equipment-ability-activation.md` and current validation status. |
| Integrations and standalone operation | **Support PlaceholderAPI and LuckPerms; core must also work standalone** (owner, 2026-10-21). These are required supported integrations, not required runtime installations. **AuraSkills support when found at boot** was added by the owner, who explicitly required checking the local skill and official docs and **preserving normal AuraSkills XP gain**. Whether to add extra skill XP is unresolved. **PacketEvents is optional**, only when needed. Keep plugin compatibility in mind for every future ability/system change; read `docs/plugin-compatibility.md`. No live installation/deployment authority is granted. |
| Environment pressure | **Co-evolve** with the artifact population, replacing the 4 fixed templates rotated by `Random(7331)`. |
| Ecology timing | **Game time, not wall clock**, so simulation matches live. **Persist** ecology state (niches, child niches, registry) across restarts. |
| Analytics PROPOSED -> ACCEPTED tuning loop | **Remove it.** No external tuning. The engine adapts from in-game outcomes. |
| Open-endedness verdict (currently string literals in `OpenEndednessTestRunner`) | **Compute it from data,** using the four emergence criteria above. |
| `diversityIndex`, `turnoverRate`, END, TNT | **Redefine** as bounded, per-window measures over live niches, defined in one place. Examples: Shannon/Simpson diversity over non-zero niches, per-window births and extinctions. |

## Correctness and operations

| Topic | Decision |
|---|---|
| Signed seeds | Any non-zero `long` is a valid artifact seed. `0` is the non-artifact sentinel. Never mask, `abs`, or remap seeds. Doing so merges distinct identities. |
| Storage load failure | **Fatal at startup only.** If storage fails at boot, disable the plugin. At runtime, refuse to load or save that one player's artifact, log loudly, and retry later. Never treat a failure as "no record" and never regenerate an identity. (2026-10-19: first answered "always disable", then revised after the risk of a brief DB outage taking the plugin down server-wide was flagged.) |
| `storage.fallbackToYamlOnFailure` | **Remove the option** (2026-10-19). A startup storage failure always disables the plugin; never silently switch to an empty store. |
| Reputation load failure | **Same treatment as artifacts** (2026-10-19): never treat a failure as "no record", never overwrite the real row. |
| Save failures | **Report them** (2026-10-19). A failed save leaves the entry dirty so it retries on the next autosave. Included in the baseline PR. |
| Loot stamping | **Equipment only:** weapons, tools, armor, elytra. |
| Root `obtuseloot.command` (op) gate | **Not intended.** Everyone can reach `help` and `info`. Every privileged subcommand keeps its own permission check. |
| `/ol reload` | **Full safe reload** of every section that can be reloaded safely. Merge `/ol debug reload` into it (confirmed). |
| Debug permission | Console may bypass. **OPs may not.** OPs must hold `obtuseloot.debug` like anyone else, since the server uses LuckPerms. |
| `analytics.ecology.*` config block | **Delete it** from `config.yml`. World-lab keeps its own settings. |
| Dashboard | **Remove the HTTP web server.** Static reports stay, fed by Java engine output rather than the Python pipeline (confirmed). |
| Existing player data | **None exists anywhere.** Break storage formats freely. No migration code. |

## Tooling and process

**Canonical skill location (owner, 2026-10-20):** `skills/obtuseloot/` in this GitHub repository. Installed Claude/Codex copies and external mirrors are noncanonical. Record decision changes here through repository commits and refresh installed copies after comparing them. This supersedes the earlier external-only placement decision.

| Topic | Decision |
|---|---|
| Python synthetic pipeline (`codex/run_internal_pipeline.py`, chaos/population/meta scripts, dashboard balance JSON) | **Rewire to Java** harness output. |
| Historical docs and committed `analytics/` | **History only.** Nothing counts as evidence unless it is re-run on current code and the run is recorded. |
| Stale repo docs (historical `CLAUDE.md` at `fab9f2b` with its sandbox Maven-proxy section, `NEWCOMER_GUIDE.md`, `EVOLUTION_FUSION_TABLE.md`, others listed in SKILL.md) | Treat as stale. Refresh or archive them via PR. |
| Generated `analytics/` (~299 MiB, 943 files) | **Untrack and gitignore** going forward. History keeps it. |
| Maven layout | **Two modules:** the plugin (including the engine) and offline tooling (simulation/analytics). Not three. |
| Sensing-probe `>= 18` floor (`IntraCategoryNormalizationProbeTest`) | **Remove that one assertion** in the baseline PR (2026-10-19). Leave the rest of the test for the scaffolding-removal work. |
| Probe-test randomness and reachability targets | **Both** (2026-10-19, after the PR #400 CI failure): probe tests derive owner UUIDs from the seed so results are repeatable, and `ColdTemplateBootstrappingProbeTest`'s reach / named-template / top-share targets are dropped; its novelty checks stay. |
| Distribution assertions in probe tests | **Drop all of them** (2026-10-19, after main went red on two after the #400 merge): every reach / top-share / top-3 / per-category-minimum target across the probe tests. Keep crash, wiring and novelty checks. Also drop the JSD / lineage-divergence floors. **Keep** checks of what a scaffolding layer does (they go when the layer goes) and static registry-composition counts. **Delete `LongHorizonSimulationTest`** (only targets, about 7 min). |
| Ecosystem health test | **Required before the probe-target drop PR is pushed.** Fixed-seed simulation, **relative-only** loss from early-run diversity; no absolute niche/template target. Owner ordered #401, metrics, health test, then push. PR #403 now contains this regression (32 artifacts, 12 windows, first 3 baseline); it uses the proposed **25% maximum relative loss** setting. Owner selected doing the test first while holding wild mutation (2026-10-20). No engine tuning to make the test pass. |
| Tests | **Fast and full tiers.** Tag long simulation tests so routine builds skip them, and run the full suite before each PR. Tests keep generating their reports, but **write them to `target/` or a temp dir**, never into tracked `docs/` files. |
| 10 unmerged remote branches (naming, convergence refactor, evolution-stability, old analytics) | **Review once,** salvage anything useful, then delete. |
| PRs | **Grouped PRs** (related fixes batched). The owner reviews and merges unless explicitly authorizing a particular merge. The 2026-10-20 authorization covered the effects batch and its prerequisites. **Scoped authorization, 2026-10-21:** during the local-inheritance continuation, the owner instructed "once you've finished make the merge". This authorizes merging the current completed, tested inheritance batch and its timing prerequisite #406; it is not standing authorization for future work. |
| Releases | **`v`-prefixed tags** through `release.yml`. The next release bumps the version and gets corrected release notes. Leave `0.9.50-beta` alone. |
| Nightly CI | **Leave disabled.** |
| Minecraft / Java target | **Target Minecraft 26.2** (owner, 2026-10-21), using the stable Purpur 26.2 API and **Java 25**. Align Maven, all Java CI jobs, plugin metadata and build instructions. This supersedes the old 1.21.1/Java 21 target; it does not authorize server upgrades or deployment. |

## Roadmap order

1. **Green build and safety fixes:** signed seeds, analytics fixtures, storage-failure handling (fatal at startup, per-player refusal at runtime), permissions.
2. **Real ability effects** with outcome feedback into selection.
3. **Per-item identity.**
4. **Remove scaffolding,** behind ablation tests.
5. **Tooling:** two-module split, Java-fed simulation and reports, redefined metrics, tracked-data cleanup.

## Still open (do not decide these yourself; ask)

- **Top-down mechanisms not covered by the scaffolding ruling.** These are: bifurcation caps, forced displacement/migration, niche locks, inversion multipliers, production safety guards / dominance suppression, progression tier gates, archetype inertia/switch margin, and the drift cap. For each one, ask: remove, replace, or keep as a game rule? (See engine-map.md section 6b.)
- **Heritable performance:** should genes change real effect behavior/reliability and tradeoffs, or only influence ability selection for now? Asked during the local-inheritance work; no answer yet. Existing effects remain fixed while this is unresolved.
- **Additional AuraSkills XP:** preserving normal XP is required. Whether artifact outcomes should grant extra skill XP remains undecided; no extra awards are implemented.
- **Branch naming convention** for new work. History uses `codex/<task>` and `claude/<task>-<id>`.

- **HomesteadCraft test and deploy path.** Testing backends versus a local disposable server first. Not decided.
