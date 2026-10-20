> Analysis of falcoignis/obtuseloot HEAD fab9f2b, produced 2026-10-19 by a doc-ingest agent. Read from git HEAD. Inferences are labeled "inference:". Historical snapshot: verify against current code before relying on any file:line.

# Slice A — core / player- and operator-facing docs (ObtuseLoot @ HEAD fab9f2b)

Source of every quote: `git show HEAD:<path>` in the audit checkout. HEAD = fab9f2b (2026-04-04, merge of PR #399). Working-tree modifications were ignored.
Marking convention: **[src: file:line]** = spot-checked against source at HEAD; **[doc-only]** = stated by docs, not verified; **inference:** = my reasoning, not verified.

Path correction: `docs/commands and permissions.md` does **not** exist at HEAD. The file lives at the repo root: `commands and permissions.md` (added 3b6bf4f, 2026-03-26). README links the root path. I ingested the root file.

---

## 1. Per-document findings

### README.md — last commit 2026-04-03
Purpose: operator-facing overview, build/install, runtime architecture.
Key facts:
- "pre-release Paper/Purpur plugin for persistent, identity-driven equipment progression"; status **0.9.50-beta**.
- Loop: player ↔ artifact interaction → usage/reputation/memory tracking → telemetry → evolution decisions (drift/awakening/convergence) → lore/identity update → persistence/analytics/dashboard → next sessions.
- Startup order: config/tuning → telemetry (archive, rollup snapshot store, rehydration) → persistence (yaml|sqlite|mysql) → engines → dashboard → commands (`/obtuseloot`, `/ol`) → periodic tasks. **[src: ObtuseLoot.java:78-113 — matches]**
- Bootstrap classes under `src/main/java/obtuseloot/bootstrap/`: TelemetryBootstrap, PersistenceBootstrap, EngineBootstrap, DashboardBootstrap, CommandBootstrap, PluginPathLayout (+ BootstrapContext, not listed). **[src: tree — matches]**
- Storage: `storage.backend` yaml (default) / sqlite / mysql; `storage.fallbackToYamlOnFailure`.
- `paths.analyticsRoot` default root `plugins/ObtuseLoot/analytics`; example outputs `analytics/telemetry/ecosystem-events.log`, `.../rollup-snapshot.properties`, `.../dashboard/ecosystem-dashboard.html`, `.../safety/ecosystem-safety-dump.json`. **[src: PluginPathLayout.java:24-99, DashboardService.java:40 — matches]**
- Build: Java 21, Maven 3.9+, Paper/Purpur 1.21 API. `mvn -B -ntp clean package` (tests enabled by default), output `target/ObtuseLoot-0.9.50-beta.jar`; fast profile `mvn -B -ntp -Pfast clean package` skips tests. **[src: pom.xml profile `fast` sets maven.test.skip — matches]**
- Ops notes: beta, schema drift expected; dashboard web server = internal admin endpoint; validate config with `/obtuseloot ecosystem ...`.
Status: **current** (most recent doc in slice; consistent with source on everything I checked).

### CLAUDE.md — last commit 2026-03-21
Purpose: AI/codebase guide (package map, lifecycle, listeners, tasks, commands, persistence, telemetry, offline tooling, Maven proxy workaround, CI, config, conventions, known inconsistencies).
Key facts that still hold: main class `obtuseloot.ObtuseLoot`, alias `/ol`; Purpur API `1.21.1-R0.1-SNAPSHOT` provided; JUnit 5 `junit-jupiter:5.11.4`; CI workflow list (build.yml push main [also pull_request], nightly 03:00 cron, benchmark/world-lab/open-endedness/dashboard dispatch, release on tag) **[src: .github/workflows — matches; build.yml also triggers on pull_request]**; tasks: environment pressure every 24,000 ticks sync **[src: EngineBootstrap.java:112-119]**, telemetry flush async at `telemetryFlushIntervalTicks` **[src: TelemetryBootstrap.java:53]**; "do not hand-edit `analytics/`"; tests are plain JUnit with no live server.
Offline tooling commands (doc-only): WorldSimulationRunner / OpenEndednessTestRunner / AnalyticsCliMain via `exec-maven-plugin:3.5.0:java`; AnalyticsCliMain subcommands `analyze`, `run-spec`, `decide`, `export-accepted`; scripts list (all exist at HEAD except the list omits `run-chaos-tests.sh`, `run-population-simulation.sh`, `run-meta-analysis.sh`, `build-and-publish.sh`, `publish.sh`, `mvn-via-mirror.sh`, `deep_authenticity_validation.py`).
Status: **partly superseded** (written at v0.9.28, before Phase 6.4+, command suite expansion, bootstrap refactor). Stale items in section 3.
Maven-proxy section: sandbox-specific to a different (Codex/cloud) environment — `/usr/local/bin/maven-proxy-server.py` is **absent** on this machine (checked with `test -e`); `scripts/start-maven-proxy.sh` is in the repo. I did not read the local Maven settings file (possible credentials).

### commands and permissions.md (repo root) — last commit 2026-03-26
Purpose: canonical command + permission reference. Full table reproduced in section 2.
Status: **current except one stale caveat**: it says `ecosystem environment` is missing from plugin.yml usage, but HEAD plugin.yml (changed 2026-04-03, newer than this doc) lists `ecosystem [health|dashboard|environment]`. Otherwise accurate against `ObtuseLootCommand.java`, `DashboardCommandExecutor.java`, `DebugCommand.java`, plugin.yml (spot-checked). Minor gaps in section 3, S10.

### docs/NEWCOMER_GUIDE.md — last commit 2026-03-19 (last content change was a rarity-terminology cleanup; structure dates from ≤2026-03-08)
Purpose: onboarding for an earlier architecture.
Describes `obtuseloot/engine/SoulEngine.java`, `obtuseloot/data/` (`SoulData`, `PlayerSoulState`), `ObtuseLoot#loadAllData/onLootPopulate/generateArtifact/applyArtifactMetaImpl`, "soul" powers + particles, loot injection on inventory open.
Status: **superseded**. `git ls-tree` at HEAD has no Soul*/`data/` files; CLAUDE.md "Known Inconsistencies" also says so.

### docs/EVOLUTION_FUSION_TABLE.md — last commit 2026-03-19 ("Draft v2")
Purpose: balancing reference for archetype/hybrid/fusion design.
Key facts:
- Archetype pair formulas: vanguard = survival+consistency; deadeye = precision+consistency; ravager = brutality+chaos; strider = mobility+precision; harbinger = chaos+survival; warden = survival+precision. **[src: ArchetypeResolver.java:17-24 — matches; effective stat = reputation + seedAffinity + driftBias + awakeningBias, lines 45-55]**
- Claims `paragon` is the fallback when #1 doesn't beat #2 by `archetype-switch-margin`; stage names `-initiate/-adept/-advanced`; per-archetype hybrid branches (deadeye-tempest, vanguard-aegis, ...); fusion recipes `stormglass-reaver`, `aegis-paragon`, `cataclyst-tyrant`, `horizon-keeper` "currently in config" plus candidates `windscar-phantom`, `voidhymn-oracle`, `crown-of-twins`; tuning keys `fusion.min-score`, `evolution.archetype-dominance-delta`, `min-boss-kills`.
Status: **superseded in most specifics** (see section 3). Only the six pair formulas still match.

### docs/ecosystem-dashboard-guide.md — last commit 2026-03-09
Purpose: how to read the generated HTML dashboard.
Panels: Health strip (Dominance Index, Branch Entropy, Trait Variance, Lineage Concentration, Collapse Risk), Rank-Abundance Curve, Trait Interaction Heatmap, Genome Trait Scatter Plot, Lineage Survival/Concentration. **[src: DashboardRenderer.java:69-84 — all panel and metric labels found]**
Review thresholds: Dominance Index > 0.40, Branch Entropy < 2.5, Lineage Concentration > 0.20, Collapse Risk MEDIUM/HIGH across multiple seasonal snapshots. **[src: DashboardService.java:160-166 uses `>= 0.40`, `<= 2.5`, `>= 0.20` — boundary inclusivity differs]**
Heatmap hints: bright `precisionAffinity × resonance` = possible generation bias; dim `mobilityAffinity × resonance` = missing interaction support; heavy `chaosAffinity × volatility` = collapse pressure. [doc-only]
Status: **mostly current** (older than the current dashboard; renderer also has END/TNT/NSER and niche metrics this guide doesn't mention).

### docs/ecology-alerts-guide.md — last commit 2026-03-11
Purpose: PNNC metric and CI ecology-alert gates.
- PNNC = Persistent Novel Niche Count. Defaults: min persistence seasons 3, min occupancy share 0.05, min species support 2, novelty distance 0.35. **[src: PersistentNovelNicheAnalyzer.java:16-18 — matches]**
- Interpretation: 0 none; 1-2 weak bounded novelty; 3-5 real expansion; >5 strong/possible open-ended. [doc-only]
- Alerts: FALSE_DIVERGENCE (warn), BOUNDED_RESHUFFLING (warn), EMERGENT_ECOLOGY (info), NOVELTY_REGRESSION (warn, or error if failIfNoveltyRegresses). **[src: EcologyAlertEngine.java:36-64 — matches; BOUNDED_RESHUFFLING fires on PNNC==0 && TNT >= 0.20, hard-coded, line 46]**
- JVM properties (world-lab/CI only): `analytics.ecologyAlerts.enabled=true`, `failOnError=true`, `minEND=2.5`, `maxTNT=0.60`, `minNSER=0.05`, `minPNNC=1`, `warnIfFalseDivergence=true`, `failIfNoveltyRegresses=false`. **[src: EcologyAlertEngine.java:22-23 defaults; read via System.getProperty in WorldSimulationHarness.java:1629-1636 — matches]**
- END/TNT/NSER are used but END/TNT never defined in the guide. Source: TNT = "Temporal Niche Turnover", NSER = "Novel Strategy Emergence Rate" **[src: DashboardRenderer.java:75-76]**; END = effective niche diversity **(inference from EcosystemHealthGaugeAnalyzer.java:25,46 `effectiveNicheDiversity` feeding `endTrend`)**.
Status: **current** for what it covers. Offline/CI only, not live-server behavior.

### src/main/resources/plugin.yml — last commit 2026-04-03
name ObtuseLoot, version 0.9.50-beta, main obtuseloot.ObtuseLoot, api-version "1.21", authors [falcoignis], website "". One command `obtuseloot`, aliases [ol], **command-level `permission: obtuseloot.command`** (default op). No `libraries:`, `depend`, or `softdepend`. 19 permission nodes, no `children:` relationships. Full table in section 2.
Usage block lists `inspect` twice (once as legacy, once in artifact suite).

### src/main/resources/config.yml — last commit 2026-04-03
Header says "Version: 0.9.50-beta", "Reload at runtime with: /ol reload". Full key table in section 2.

### pom.xml — last commit 2026-04-03
groupId `com.falcoignis`, artifactId `obtuseloot`, version `0.9.50-beta`, packaging jar, Java 21 (`release` 21). Repo: purpur snapshots `https://repo.purpurmc.org/snapshots`. Deps: `org.purpurmc.purpur:purpur-api:1.21.1-R0.1-SNAPSHOT` (provided), `org.xerial:sqlite-jdbc:3.46.1.3` (compile), `com.mysql:mysql-connector-j:8.4.0` (compile), `org.junit.jupiter:junit-jupiter:5.11.4` (test). Plugins: maven-compiler-plugin 3.13.0, maven-jar-plugin 3.4.2 (default impl entries), maven-surefire-plugin 3.5.2. **No shade/assembly plugin, no enforcer plugin, no distributionManagement.** finalName `ObtuseLoot-${project.version}`. Profile `fast` → `maven.test.skip=true`. Resource filtering off.
Header comment claims output `target/obtuseloot-<version>.jar` (wrong case vs finalName) and "Publish: mvn -B -ntp clean deploy" (no distributionManagement in pom; `scripts/publish.sh` supplies GitHub Packages settings, server id "github").

### releases/ (14 files)
| File | Date | Gist |
|---|---|---|
| v0.9.2/BUILD_INFO.md | 03-09 | `mvn clean package` → `ObtuseLoot-0.9.2.jar`; nightly workflow builds+sims+analytics |
| v0.9.2/CHANGELOG.md | 03-09 | deterministic `artifact-seed` lifecycle; debug `simulate`/`seed`; simulation harnesses; multi-run validation docs |
| v0.9.2/QA_NOTES.md | 03-09 | seed determinism, debug paths; scaffolds under simulation/*; outputs `analytics/{evolution,population,meta,world-lab}`; 5-run `analytics/meta/multirun-validation.*` |
| v0.9.3/* | 03-09 | version alignment only |
| v0.9.5/BUILD_INFO.md | 03-09 | jar not committed; CI uploads binaries |
| v0.9.5/CHANGELOG.md | 03-09 | procedural ability generation, mutation tuning, ability evolution tree, memory influence; removed committed build output |
| v0.9.5/QA_NOTES.md | 03-09 | smoke: evolution → drift/mutation → awakening → fusion; seed + simulate checks; "mutation events ... do not collapse into a single dominant outcome" |
| v0.9.6/BUILD_INFO.md | 03-09 | tag `v*` triggers release.yml, uploads `target/*.jar` |
| v0.9.6/CHANGELOG.md, QA_NOTES.md | 03-09 | release-ops record; persistence lifecycle join-load/autosave/quit-save/disable-save |
| v0.9.50-beta/BUILD_INFO.md | 03-24 | `ObtuseLoot-0.9.50-beta.jar`, SHA-256 `457e09e2f74c08113998b5a403ca14d44dbfeca5ac51152541e1d1f3ea3d652f`, built 2026-03-24T21:38:00Z, runtime Purpur/Bukkit 1.21.1 |
| v0.9.50-beta/RELEASE_NOTES.md | 03-26 | first public beta, dated 2026-03-24; core systems list; packaging changes; known considerations; operator checklist; "No migration steps required from 0.9.x" |
No release folders exist for v0.9.0/v0.9.7 (tags exist) or v0.9.28–v0.9.42 (version bumps in commit messages). `docs/release-v0.9.0.md`, `release-v0.9.3.md`, `release-v0.9.6.md`, `release-readiness-audit-v0.9.50-beta.md` exist outside this slice.
Status: v0.9.2–v0.9.6 are **historical**. v0.9.50-beta is **current by name but describes a different build than HEAD** (section 3, item S1).

### simulation/*/README.md (all 2026-03-09)
- chaos-tests: "1000 hits, 500 kills, 200 multikill chains, 100 boss fights, 200 chaos events", run by `scripts/run-chaos-tests.sh`.
- gameplay-simulator: "Simulates movement, combat, boss fights, kill chains, and low-health survival loops", run via `python3 codex/run_internal_pipeline.py`.
- population-simulator: "default 5000 seeds", run by `scripts/run-population-simulation.sh`.
- world-simulation-lab: `WorldSimulationRunner`; defaults player_count=120, artifacts_per_player=4, sessions_per_season=18, season_count=6, boss_frequency=0.18, encounter_density=7, chaos_event_rate=0.20, low_health_event_rate=0.15, mutation_pressure_multiplier=1.0, memory_event_multiplier=1.0 **[src: WorldSimulationConfig.java:35-66 — matches; also seed 90210, output `analytics/world-lab`, all 7 feature toggles true]**; overrides via `-Dworld.*` system properties (`world.players`, `world.seasonCount`, `world.fitnessSharing.*`, `world.scoringMode`, ...) **[src: WorldSimulationRunner.java:12-82]**. Outputs `analytics/world-lab/world-sim-{report.md,data.json,balance-findings.md,meta-shifts.md}`.
Status: world-lab README is **current**. The other three READMEs **misdescribe what runs** (section 3, item S5). Only world-lab runs Java plugin code.

---

## 2. Consolidated model

### What the plugin does for players (per docs + spot checks)
- Each player has a tracked **artifact profile**: `ArtifactManager.getOrCreate(UUID playerId)` loads or generates one artifact per player UUID **[src: ArtifactManager.java:33-36]**. Items are linked to it via PDC keys (storage key, owner, version, seed, mechanic fingerprint, memory pressure) **[src: ArtifactStateCodec.java:16-25]**.
- **Reputation** accrues from combat/movement/kills: precision, brutality, survival, mobility, chaos, consistency (+ counters kills, bossKills, recentKillChain, survivalStreak) **[src: ArtifactReputation.java:4-15]**. Bosses default ENDER_DRAGON, WITHER, WARDEN.
- **Evolution**: once total score ≥ `archetype-threshold` (10), an archetype is chosen from 6 pair sums with inertia/hysteresis. Then path = `<arch>-formed` (<25), `-tempered` (≥25), `-advanced` (≥45), hybrid (≥70) = `<arch>-awakened-hybrid` if awakened, `-tempest-hybrid` if lineage stormbound or drift tempest, `-void-hybrid` if lineage graveborn or chaos > consistency, else `-equilibrium-hybrid` **[src: EvolutionEngine.java:19-37; HybridEvolutionResolver.java:7-22]**.
- **Drift**: probabilistic, base 0.05, cap 0.40, raised by chaos, lowered by consistency; instability 600 s.
- **Awakening** (identity replacement), **convergence** (procedural identity transitions; formerly "fusion"), **memory**, **lore/naming** (seeded by owner UUID; name tiers known 3 / revealed 8 / storied 14 kills), **abilities** (trigger subscriptions, genome/projection), **lineage/species**, **environmental pressure** (season advance every 24,000 ticks, per-GenomeTrait multipliers).
- **Loot stamping** (post-tag, HEAD only): `LootGenerateEvent` where entity is a Player, plus `EnchantItemEvent`, stamp every non-air generated or enchanted item with the player's artifact identity **[src: LootInsertionListener.java:22-55]**. There is no equipment-eligibility check in this path, unlike `/ol convert`.

### Commands — declared vs implemented
Root `/obtuseloot` (alias `/ol`), Bukkit command permission `obtuseloot.command` (op). Routing: `DashboardCommandExecutor` intercepts `dashboard`, `ecosystem` (bare / `health` / `dashboard`, exactly 1-2 args), `ecosystem map` (exactly 2 args), `ecosystem dump`, `ecosystem reset-metrics`, `debug dashboard`, then delegates everything else to `ObtuseLootCommand` **[src: CommandBootstrap.java; DashboardCommandExecutor.java:52-92, 267-292]**.

| Subcommand | Perm checked in code | Sender | In plugin.yml usage? |
|---|---|---|---|
| (none) / help | obtuseloot.help | both | yes |
| info | obtuseloot.info | both | yes |
| dashboard | obtuseloot.info | both | yes |
| ecosystem [health\|dashboard] | obtuseloot.info | both | yes |
| ecosystem environment | obtuseloot.info | both | yes (HEAD plugin.yml lists `[health\|dashboard\|environment]`) |
| ecosystem dump | obtuseloot.info, cooldown `safety.dump.cooldownMs` | both | yes |
| ecosystem reset-metrics | obtuseloot.admin | both | yes |
| ecosystem map | obtuseloot.info (prints heatmap path) | both | yes |
| ecosystem map lineage\|species\|collapse \| genome <trait> \| off | obtuseloot.info → EcosystemMapRenderer | both (`off` direct only for players) | yes |
| refresh/reset [player] | obtuseloot.admin | both (console needs target) | yes |
| reload | obtuseloot.admin | both | yes |
| addname/removename <prefixes\|suffixes> <value> | obtuseloot.edit or obtuseloot.edit.<pool> | both | yes |
| give <player> | obtuseloot.command.give | both | yes |
| convert, reroll, inspect, force-awaken, force-converge, repair-state, debug-profile, dump-held | obtuseloot.command.<name without dash> | player | yes |
| give-specific <player> <archetype\|family> | obtuseloot.command.givespecific | both | yes |
| debug <...> | obtuseloot.debug; **players who are OP and all non-player senders bypass** **[src: DebugCommand.java:958-964]** | both | yes |
| debug dashboard | obtuseloot.debug (strict hasPermission, no OP bypass) **[src: DashboardCommandExecutor.java:90-93]** | both | yes |

Debug subcommands: inspect, rep set/add/reset (stats precision, brutality, survival, mobility, chaos, consistency, kills, bosskills, recentkillchain, survivalstreak), evolve, drift, awaken, fuse, lore, reset, save, reload, instability clear, archetype set (unformed, vanguard, deadeye, ravager, strider, harbinger, warden, paragon), path set, seed show/reroll/set/import/export, ability [show|refresh|explain|tree], memory, persistence backend/test/migrate (yaml-to-sqlite, yaml-to-mysql, nbt-artifacts), ecosystem [bias|balance], lineage, genome interactions, projection [cache|stats], subscriptions [stats|player], artifact [storage|resolve|cache|stats], simulate help/hit/move/lowhp/kill/multikill/bosses/chaos/cycle/resetcontext/path (precision, brutality, mobility, survival, chaos, boss, hybrid, awaken, drift) **[src: DebugCommand.java:35-45, 74-90]**.

### Permissions (plugin.yml)
| Node | Default | Used in code? |
|---|---|---|
| obtuseloot.help | true | yes |
| obtuseloot.info | true | yes |
| obtuseloot.inspect | op | **no** (dead node) |
| obtuseloot.admin | op | yes |
| obtuseloot.edit | op | yes (manual parent of edit.<pool>) |
| obtuseloot.edit.prefixes / .suffixes | op | yes (scoped) |
| obtuseloot.debug | op | yes |
| obtuseloot.command | op | Bukkit command gate only |
| obtuseloot.command.{give,convert,reroll,inspect,forceawaken,forceconverge,repairstate,debugprofile,givespecific,dumpheld} | op | yes |
inference: because `/obtuseloot` declares `permission: obtuseloot.command` (default op), standard Bukkit `PluginCommand` permission testing would block non-op players from every subcommand, including help/info, even though those default to `true`. Not verified on a server. The commands doc flags this only as "may require".

### Config (config.yml) — keys, defaults, and whether live code reads them
| Section | Keys = defaults | Live read site |
|---|---|---|
| storage | backend=yaml, fallbackToYamlOnFailure=false | PersistenceConfig (enable only) |
| sqlite | file=plugins/ObtuseLoot/data/obtuseloot.db (code default: dataFolder/data/obtuseloot.db) | PersistenceConfig.java:17 |
| mysql | host localhost, port 3306, database obtuseloot, username root, password change-me, useSsl false, connectionPool.enabled false, maxPoolSize 10 | PersistenceConfig / MySqlPersistenceProvider |
| persistence | autosave-interval-seconds=300 | RuntimeSettings |
| reputation | combat-window-ms 10000, low-health-threshold 6.0, mobility-distance-threshold 12.0, kill-chain-window-ms 8000, multi-target-chaos-threshold 3, boss-types [ENDER_DRAGON, WITHER, WARDEN], volatile-decay-interval-seconds 300, volatile-decay-factor 0.96, context-cleanup-seconds 120 | RuntimeSettings.java:19-27 |
| evolution | archetype-threshold 10, tempered-threshold 25, advanced-threshold 45, hybrid-threshold 70, archetype-switch-margin 4.0, current-archetype-inertia 2.0 | RuntimeSettings.java:29-34 |
| drift | base-chance 0.05, max-chance 0.40, chaos-multiplier 0.01, consistency-reduction 0.005, instability-duration-seconds 600 | RuntimeSettings.java:35-39 |
| combat | precision-threshold-damage 10.0 | RuntimeSettings.java:28 |
| naming | use-deterministic-owner-seed true; discovery-thresholds known 3 / revealed 8 / storied 14; compression.max-words 6; lexeme-pools (defensive, chaotic, precision, aggression, memory, mobility, ritual, support, control) | RuntimeSettings.java:41-70 |
| dashboard | webServerEnabled false, port 8085 | DashboardBootstrap.java:14-16 (enable only) |
| paths | analyticsRoot=analytics (relative → plugins/ObtuseLoot/; blank throws) | PluginPathLayout.java:24-33 |
| runtime | triggerSubscriptionIndexing true, activeArtifactCache true, activeArtifactCacheMaxEntries 2048, activeArtifactCacheIdleExpireMs 300000 | RuntimeSettings.java:55-58 |
| text.channels.*.max-words | name 4, lore 14, identify 16, awakening 14, lineage 16, memory 16, drift 10, convergence 14 | RuntimeSettings.java:47-54 |
| analytics.ecology | behavioralProjection(enabled, traitEcologyWeight 0.35, behaviorWeight 0.65), roleBasedRepulsion(beta 0.15, weights...), fitnessSharing(mode niche, alpha 0.10, maxPenalty 0.15, targetOccupancy 0.18, similarityRadius 0.20), adaptiveNicheCapacity(min 0.80, max 1.25, ..., maxSeasonDelta 0.05), opportunityWeightedMutation(maxBias 0.10, ...) | **No live read found.** grep of src/main/java for `analytics.ecology.` and the section names finds only world-lab code, which takes `world.*` system properties with defaults in Java |
| ecosystem.parameters | nicheSaturationSensitivity 0.90, lineageMomentumInfluence 1.00, mutationAmplitudeMin 0.70, mutationAmplitudeMax 1.65, driftWindowDurationTicks 5, competitionReinforcementCurve 1.00, telemetryFlushIntervalTicks 100, telemetryArchiveBatchSize 256, telemetryRollupIntervalMs 5000, telemetryRehydrateReplayWindowEvents 512 | EvolutionParameterRegistry (onEnable only, ObtuseLoot.java:84-85) |
| safety | guards.categoryDominanceThreshold 0.65, categorySuppressionFactor 0.85, templateDominanceThreshold 0.55, templateSuppressionFactor 0.90, candidatePoolCollapseThreshold 2, rollingWindowSize 100; telemetryVerbose false; snapshotIntervalEvents 500; periodicConsoleLog false; periodicConsoleLogIntervalTicks 1200; guard.minCombinedSuppression 0.75; dump.cooldownMs 3000 | ProductionSafetyConfig.from (EngineBootstrap.java:58, enable only) |

Reload scope: `/ol reload` = reloadConfig + RuntimeSettings + NamePoolManager only **[src: ObtuseLootCommand.java:163-172]**. `/ol debug reload` also resets trigger-subscription indexing and invalidates all cached artifacts **[src: DebugCommand.java:410-421]**. inference: changes to storage, sqlite/mysql, dashboard, paths, ecosystem.parameters and safety need a restart, because the only read sites found are enable-time.

### Build / release process
- Local: `mvn -B -ntp clean package` (tests run), `-Pfast` skips tests. `scripts/build.sh` probes Maven Central and falls back to `MAVEN_MIRROR_URL` via `mvn-via-mirror.sh`. `scripts/publish.sh` / `build-and-publish.sh` deploy to GitHub Packages (needs `GITHUB_TOKEN`).
- CI: build.yml (push main + PRs) builds and uploads `target/*.jar` and `analytics/`; nightly (03:00 UTC cron) builds, runs chaos/population/world sims plus meta-analysis, uploads `analytics/**` and the jar; release.yml on tag `v*` (or dispatch) builds and publishes the jar to a GitHub release.
- Release record convention: `releases/vX/BUILD_INFO.md` + `CHANGELOG.md` + `QA_NOTES.md` (0.9.50-beta uses RELEASE_NOTES.md instead of CHANGELOG/QA).
- Tags at HEAD: `v0.9.0` (070a37d, 03-09), `v0.9.6` (d433a46 "testing release workflow"), `v0.9.7` (84dcce0), `0.9.50-beta` (4c26d9c, 03-24, **no `v` prefix**).

### Version history highlights (pom.xml commit log)
v0.9.0–0.9.7 (03-09: seed lifecycle, simulation/analytics scaffolding, release workflow) → 0.9.29–0.9.32 (03-21, Phase 6.1–6.25 convergence engine audits: seed entropy, recipe shadowing, finalist window, lineage family mapping) → 0.9.33 awakening refinement → 0.9.34 command/permission integrity → 0.9.36 intra-category score distribution fix → 0.9.39 ability identity audit → 0.9.40 evolution stability → 0.9.41/0.9.42 production hardening + safeguards (guard stacking cap, dump throttle) → Phase 8.2 hot-path perf → 0.9.50-beta packaging (03-24) → post-tag on the same version string: tests skipped by default (03-25, later reverted to the `fast` profile), loot/enchant stamping (03-26), artifact command suite (03-26), bootstrap refactor + PluginPathLayout centralization (04-03), telemetry ingestion contracts (04-04).

---

## 3. Contradictions and staleness

S1. **Published 0.9.50-beta jar ≠ HEAD.** Tag `0.9.50-beta` = 4c26d9c (2026-03-24); HEAD is 46 commits later with the same version string. `LootInsertionListener.java` is absent at the tag. The tag's plugin.yml has 0 `obtuseloot.command.give` entries, so the artifact command suite is post-tag too. Yet RELEASE_NOTES.md (edited 03-26 by 55cfb7d) lists "Loot/enchantment identity stamping ... chests, trial vault rewards, enchanting-table outcomes" as a packaging change in this release. BUILD_INFO's SHA-256 457e09... was built 2026-03-24T21:38Z, so it cannot include those features. The tag lacks the `v` prefix that release.yml triggers on. inference: it was published via workflow_dispatch or manually. Unverified.

S2. **CLAUDE.md is v0.9.28-era.**
- Says Version `0.9.28`; pom/plugin.yml = 0.9.50-beta.
- Maps `inspect [player]` to `obtuseloot.inspect`. Code: `inspect` takes no player argument, is player-only, and checks `obtuseloot.command.inspect` (ObtuseLootCommand.java:42,113). `obtuseloot.inspect` is never checked.
- Its command table omits give/convert/reroll/force-*/repair-state/debug-profile/give-specific/dump-held, plus ecosystem dump/reset-metrics/environment.
- "Known Inconsistencies": scoped edit perms "not declared in plugin.yml". They are declared now (plugin.yml; RELEASE_NOTES says so). debug usage "does not enumerate subscriptions/artifact". It does now.
- Listener table omits `LootInsertionListener` (ObtuseEngine.java:30).
- "enforcer plugin validates this, though currently set `<skip>true`": no enforcer plugin in pom at HEAD.
- "SQLite JDBC and MySQL connector are compile-scoped and bundled": they are compile-scoped, but the pom has no shade/assembly plugin and plugin.yml has no `libraries:`. inference: Maven's default jar plugin does not bundle dependencies, so they are not in the jar. Whether the server supplies them is unknown (question Q4). The pom header comment also contradicts itself ("zero extra dependencies to shade" vs compile deps).
- "Adding a new engine: construct in `ObtuseLoot.onEnable()`". Engines are now built in `bootstrap/EngineBootstrap` and registered in `BootstrapContext` (ObtuseLoot.java:88-104).
- Package map lacks `bootstrap`, `significance`, `artifacts/cache`, `artifacts/eligibility`.

S3. **EVOLUTION_FUSION_TABLE.md vs source.**
- Paragon fallback: the doc says paragon is chosen when there's no clear lead. `ArchetypeResolver` (lines 26-38) never returns `paragon`. Its candidate map has 6 archetypes only. `archetype-switch-margin` is hysteresis that keeps the *current* archetype when the challenger's lead is < margin. paragon/unformed are reachable via `debug archetype set` (DebugCommand.java:38-40). inference: possibly also via other code paths not checked.
- Stage names: doc `-initiate/-adept/-advanced` vs source `-formed/-tempered/-advanced` (EvolutionEngine.java:30-36).
- Hybrid branches: the doc's per-archetype table (deadeye-tempest, vanguard-aegis, ...) vs source's 4 generic suffixes `-awakened/-tempest/-void/-equilibrium-hybrid` (HybridEvolutionResolver.java).
- Fusion: the doc says recipes are "currently in config". config.yml has no `fusion` section. grep for the recipe IDs, `fusion.min-score`, `archetype-dominance-delta` and `min-boss-kills` in src finds nothing. Fusion appears to have been replaced by `ConvergenceEngine` (commit history "Phase 6.x convergence ... recipe shadowing"; debug `fuse` = "attempts convergence/fusion transition").
- Release notes tier list "initial → tempered → advanced → hybrid" also differs from source (`formed`).

S4. **Config comments vs code.**
- config.yml header implies `/ol reload` reloads the config. It reloads only RuntimeSettings + name pools (see section 2).
- `analytics.ecology.*`: the config comment says these "drive niche modeling, fitness sharing, and mutation bias" for "offline and runtime" analytics. No live read site was found. World-lab uses Java defaults plus `-Dworld.*` props. The YAML mirrors the Java defaults for fitnessSharing (FitnessSharingConfig.java:12), adaptiveNicheCapacity (AdaptiveNicheCapacityConfig.java:13), opportunityWeightedMutation (OpportunityWeightedMutationConfig.java:11) and roleBasedRepulsion (SpeciesNicheAnalyticsEngine.java:20). It **does not** mirror behavioralProjection: YAML traitEcologyWeight 0.35 / behaviorWeight 0.65 vs code default 0.25 / 0.75 (SpeciesNicheAnalyticsEngine.java:44).
- `dashboard.webServerEnabled`: the log line says "Serving ... at http://localhost:<port>" (DashboardBootstrap.java:23), but the server binds `new InetSocketAddress(port)`, all interfaces, with no auth (DashboardWebServer.java:26). The handler serves any file under the dashboard dir, with a normalize/startsWith traversal guard (lines 46-60). The config comment does warn that it is a security risk without a firewall.

S5. **Simulation READMEs misdescribe the tooling.**
- `scripts/run-chaos-tests.sh` and `scripts/run-population-simulation.sh` both only run `python3 codex/run_internal_pipeline.py` and then `echo` the README numbers ("Chaos tests simulated: 1000 hits..."; "Population simulation complete (5000 seeds)."). The scripts' own comment says the message only "labels which simulation lens was requested".
- `codex/run_internal_pipeline.py` (287 lines) never invokes Java/Maven. `sim()` (lines 22-35) derives family/trigger/branch/mutation/memory/mechanic from a random 63-bit seed by modulo over hard-coded Python lists. It writes committed reports such as `analytics/ability-ecology-report.md`, `analytics/ecosystem-balance-{data.json,report.md,suggestions.md}`, `analytics/memory-system-report.md` and `analytics/procedural-generator-audit.md`, with "recommendations".
- The gameplay-simulator README ("Simulates movement, combat, boss fights...") describes this same script.
- Only world-lab (`WorldSimulationRunner`, Java) and other Java runners exercise plugin code.
- The v0.9.2/v0.9.3/v0.9.5 QA notes cite these scaffolds as validation tooling.

S6. **Release notes "seven behavioral axes"** vs six scored axes in `ArtifactReputation` (precision, brutality, survival, mobility, chaos, consistency) plus counters (ArtifactReputation.java:4-15; ArchetypeResolver.java:13).

S7. **Dashboard guide thresholds** use strict `>`/`<`; DashboardService.java:160-166 uses `>=`/`<=`. Minor.

S8. **NEWCOMER_GUIDE.md** describes the removed SoulEngine architecture (no Soul*/data files at HEAD).

S9. **pom header** says output `target/obtuseloot-<version>.jar` and "Publish: mvn deploy". finalName is `ObtuseLoot-${version}`, and there is no distributionManagement (publish.sh supplies it).

S10. **commands doc small gaps.** Its `ecosystem environment` entry and "Notes and Caveats" say the form is omitted from plugin.yml usage. HEAD plugin.yml now includes it, so that caveat is stale. The debug "OP bypass" also covers console, command blocks and every non-Player sender (no permission check at all, DebugCommand.java:959). `debug dashboard` has no OP bypass. Extra trailing args on `ecosystem health/dump/map` fall through to "Unknown subcommand" (arg-count-exact matchers, DashboardCommandExecutor.java:267-292).

S11. README's commands link is correct (root file). The brief's `docs/` location was wrong.

---

## 4. Emergence lens (catalogue only)

**Top-down / designer-imposed control described in this slice**
- Fixed archetype formulas: 6 hand-authored stat-pair sums (FUSION_TABLE §1; ArchetypeResolver).
- Fixed score gates for tiers: 10/25/45/70 (config `evolution`).
- Hysteresis: `current-archetype-inertia` 2.0, `archetype-switch-margin` 4.0 ("prevent thrashing").
- Hand-written hybrid branch rules (HybridEvolutionResolver: lineage name / drift alignment / chaos>consistency).
- Fusion recipes with hand-tuned stat and boss-kill gates (doc-only, apparently dead).
- Drift probability with a hard cap `max-chance` 0.40.
- Safety guards: category share > 0.65 → ×0.85; template share > 0.55 → ×0.90; candidate-pool collapse guard at < 2; rolling window 100; **floor** `minCombinedSuppression` 0.75. Described as "prevent any single ability category or trigger template from dominating ... self-reverting ... once distributions normalise". README: "safety checks monitor distribution drift", "prevent unhealthy loot distribution". RELEASE_NOTES: "automatic diversity maintenance".
- Fitness sharing with `targetOccupancy` 0.18 (an occupancy target), `maxPenalty` 0.15; adaptive niche capacity clamped to [0.80, 1.25] with `maxSeasonDelta` 0.05; opportunity-weighted mutation `maxBias` 0.10; role-based repulsion `beta` 0.15. All bounded; in source these apply to world-lab only (clamps in FitnessSharingConfig/AdaptiveNicheCapacityConfig).
- `mutationAmplitudeMin/Max` 0.70/1.65 bounds; `nicheSaturationSensitivity`, `competitionReinforcementCurve` tuning knobs.
- Exogenous environmental pressure: a season advance every 24,000 ticks that sets per-trait multipliers (not derived from player outcomes, per what's documented).
- Text word caps per channel; curated lexeme/name pools; deterministic owner-seeded names.
- Operator overrides: give-specific (archetype/family, "no generic fallback"), force-awaken, force-converge, reroll, debug archetype/path/seed set, rep set/add, simulate profiles.
- CI gates: ecology alerts can fail CI (`failOnError`), plus `NOVELTY_REGRESSION`. These measure novelty and steer development, not runtime.
- The release-notes QA goal "mutation events ... do not collapse into a single dominant outcome" is a design target checked by tests.

**Mechanisms described as driven by real outcomes / behavior**
- Reputation accrues from actual player combat, movement, kills, low-HP survival and kill chains. Archetype and tier follow from it. This is a deterministic mapping from behavior, not selection among variants.
- Drift chance is coupled to the player's chaos (up) and consistency (down).
- Memory events (first kill, boss kill, etc.) influence later progression (v0.9.5 changelog; README "memory").
- Lineage momentum (`lineageMomentumInfluence`) and niche saturation (`nicheSaturationSensitivity`) are frequency-dependent terms in the live tuning profile (source semantics not read).
- Awakening/convergence are triggered by milestones/history (details outside slice).
- Measurement only, not mechanisms: PNNC, END, TNT, NSER, Dominance Index, Branch Entropy and Lineage Concentration measure diversity and novelty. PNNC/alerts are world-lab/CI only. Dashboard metrics are rendered from analytics files.
- Not documented anywhere in this slice: any mechanism where artifact variants **reproduce or are retained in proportion to real in-game outcomes** (differential survival or inheritance). "Selection" language appears (environment "selection pressure modifiers" in help text, "fitness sharing") but the docs never say what is selected or from what population.

---

## 5. Open questions for the project owner

Q1. **Identity model: per player or per item?** Verified: `give`, `convert`, `reroll` and `give-specific` all *replace* the target player's single loaded artifact. They call `ArtifactManager.recreate(playerId)` or `recreateWithArchetype(playerId, ...)`, which `trackDiscard(existing)` and then install a fresh one (ObtuseLootCommand.java:416,460,477,640; ArtifactManager.java:89-116). Force-awaken/converge use `replaceIdentity(playerId, ...)` (lines 524,557). `getOrCreate(UUID playerId)` and `LootInsertionListener` (stamps all generated loot with that one key) point the same way. So the code at HEAD models one active artifact per player, and items are pointers to it. README ("a player acquires an artifact") and the `resolveByStorageKey` lookup leave the design intent ambiguous. Is one-artifact-per-player intended? What "population" is meant to evolve: players' current artifacts, the discard/create history (`trackDiscard`/`trackCreated`), or lineages?

Q2. **Should loot stamping be filtered?** `stampIfEligible` (LootInsertionListener.java:51-56) checks only null/AIR. All chest/vault loot, including non-equipment, gets the player's artifact identity. Intended?

Q3. **What exactly is the published 0.9.50-beta?** Tag 4c26d9c lacks loot stamping and the artifact command suite, but RELEASE_NOTES lists stamping. How was it published (the tag has no `v`, release.yml needs `v*`)? Should HEAD get a new version (e.g. 0.9.51-beta) before the next release, and should RELEASE_NOTES be corrected?

Q4. **JDBC drivers at runtime.** sqlite-jdbc and mysql-connector-j are compile-scoped with no shading and no plugin.yml `libraries:`. Are they expected to come from the server (Paper/Purpur), or is `sqlite`/`mysql` backend support broken in the jar? (CLAUDE.md says "bundled".)

Q5. **Is the `analytics.ecology.*` config block meant to affect the live server?** No live read site was found. Delete it, wire it, or document it as world-lab reference values? Its behavioralProjection values (0.35/0.65) also differ from the Java defaults (0.25/0.75). Which is canonical?

Q6. **Fusion vs convergence.** Is EVOLUTION_FUSION_TABLE.md dead (no `fusion.*` config, no recipe IDs in source)? Is `paragon` supposed to be reachable naturally? ArchetypeResolver never yields it. Is `unformed` the pre-threshold state?

Q7. **Root command permission.** Is `permission: obtuseloot.command` (op) on `/obtuseloot` intended to lock all non-op players out of `help`/`info` (both default true)? Or should the root gate be removed or set to default true?

Q8. **Debug gate.** Is it intended that any non-Player sender (console, command blocks) bypasses `obtuseloot.debug`, and that player OPs bypass it except for `debug dashboard`?

Q9. **Dead node `obtuseloot.inspect`.** Remove it from plugin.yml, or re-wire `inspect [player]` (the CLAUDE.md-era behavior)?

Q10. **Reload semantics.** Should `/ol reload` cover safety guards, ecosystem.parameters, dashboard and storage, or is "restart required" acceptable and should it be documented? Why do `/ol reload` and `/ol debug reload` differ?

Q11. **Python pipeline status.** Are `codex/run_internal_pipeline.py` and the chaos/population scripts (which never run plugin code) still considered validation? Should the `analytics/*` reports they produce (ability-ecology, ecosystem-balance suggestions, memory-system) be treated as non-evidence when judging emergence? Should the READMEs and nightly CI be corrected?

Q12. **Emergence target definition.** Given the goal of "a truly emergent evolution engine": which documented controls are considered acceptable scaffolding (safety guards with floor 0.75, fitness-sharing occupancy target 0.18, capacity clamps, archetype hysteresis, fixed tier gates, exogenous season pressure), and which are slated for replacement by outcome-driven selection? Is PNNC ≥ 3 in world-lab the agreed success criterion (ecology-alerts-guide says 3-5 = "real ecological expansion")?

Q13. **What is a "season"?** Live: one environmental-pressure advance every 24,000 ticks (20 min at 20 TPS; inference from tick math). World-lab: 18 sessions. PNNC "minimum persistence seasons 3" uses world-lab seasons. Is there a live-server equivalent of PNNC/ecology alerts, or is it intentionally offline-only?

Q14. **Dashboard web server exposure.** It binds all interfaces without auth while logging "localhost". Should it bind to loopback by default or get auth?

Q15. **CLAUDE.md maintenance.** CLAUDE.md is a v0.9.28 snapshot with a cloud-sandbox Maven-proxy section that doesn't apply on this machine (`/usr/local/bin/maven-proxy-server.py` absent). Should it be refreshed, and should the proxy section be marked environment-specific?

Q16. **Reputation axes count.** RELEASE_NOTES says seven; code scores six. Is a seventh axis planned, or is the note wrong?

---

## 6. Glossary (slice-specific)

| Term | Meaning | Source |
|---|---|---|
| Artifact | Long-lived tracked equipment identity with seed, name, lore, archetype/evolution path, memory, lineage; currently keyed by player UUID | README; ArtifactManager.java:33 |
| Artifact seed (`artifact-seed`) | Deterministic seed driving identity/naming; debug seed show/set/reroll/import/export | v0.9.2 changelog; commands doc |
| Storage key | PDC string linking an ItemStack to an artifact record | ArtifactStateCodec.java:18 |
| Reputation | Behavioral stats: precision, brutality, survival, mobility, chaos, consistency + kill/boss/chain/streak counters | ArtifactReputation.java |
| Volatile reputation decay | Periodic ×0.96 decay every 300 s of volatile stats | config.yml `reputation` |
| Archetype | vanguard, deadeye, ravager, strider, harbinger, warden (+ paragon, unformed via debug) chosen from stat-pair sums | ArchetypeResolver; DebugCommand.java:38 |
| Archetype path / evolution path | `<arch>` / `<arch>-formed\|-tempered\|-advanced\|-{awakened,tempest,void,equilibrium}-hybrid` | EvolutionEngine; HybridEvolutionResolver |
| Inertia / switch margin | Bonus to the current archetype, and the min lead needed to switch (anti-thrash) | config `evolution`; ArchetypeResolver |
| Drift | Probabilistic stat mutation; chaos raises it, consistency lowers it; causes instability | config `drift` |
| Instability | Timed state after drift (600 s), cleared every 100 ticks or via debug | config; CLAUDE.md |
| Awakening | Milestone identity replacement event; `awakeningPath` "dormant" until awakened | RELEASE_NOTES; HybridEvolutionResolver.java:12 |
| Convergence | Procedural identity transition (successor to "fusion") | README; RELEASE_NOTES |
| Fusion | Older recipe-gated identity merge; doc-only now | EVOLUTION_FUSION_TABLE |
| Memory | Recorded artifact events (first kill, boss kill, ...) influencing progression | v0.9.5 changelog |
| Lineage / species | Population-level families and speciation tracking | RELEASE_NOTES; CLAUDE.md |
| Latent lineage | Artifact lineage tag, e.g. stormbound, graveborn | HybridEvolutionResolver |
| Genome trait | Ability genome dimension; env multipliers are per GenomeTrait (e.g. precisionAffinity, mobilityAffinity, chaosAffinity, resonance, volatility) | ObtuseLootCommand.java:182; dashboard guide |
| Environmental pressure / event | Seasonal exogenous per-trait multipliers, advanced every 24,000 ticks | EngineBootstrap.java:112-119; `/ol ecosystem environment` |
| Safety guards | Category/template dominance suppression, pool-collapse guard, combined-suppression floor | config `safety` |
| Ecosystem safety dump | JSON snapshot `analytics/safety/ecosystem-safety-dump.json`, throttled by `dump.cooldownMs` | commands doc |
| Trigger subscription indexing | Index for fast ability-trigger dispatch | config `runtime` |
| Active artifact cache | In-memory artifact cache (2048 entries, 5 min idle) | config `runtime` |
| Name pools | Editable `prefixes`/`suffixes` pools via addname/removename | commands doc |
| Lexeme pools | Fallback per-axis word pools in config | config `naming` |
| Discovery thresholds | Kill counts unlocking name tiers known/revealed/storied | config `naming` |
| Telemetry archive / rollup / rehydration | Event log, periodic aggregates, startup replay (512 events) | README; config `ecosystem.parameters` |
| analyticsRoot | Base dir for all runtime outputs | config `paths` |
| World-lab | Java seasonal population simulator (`WorldSimulationRunner`), `-Dworld.*` props | simulation/world-simulation-lab/README |
| Season (world-lab) | 18 sessions; 6 seasons per default run | world-lab README |
| PNNC | Persistent Novel Niche Count: novel (distance ≥ 0.35) and durable (≥ 3 seasons, ≥ 0.05 occupancy, ≥ 2 species) niches | ecology-alerts-guide |
| NSER | Novel Strategy Emergence Rate | ecology-alerts-guide; DashboardRenderer.java:76 |
| TNT | Temporal Niche Turnover | DashboardRenderer.java:75 |
| END | Effective niche diversity (inference from EcosystemHealthGaugeAnalyzer) | ecology-alerts-guide (undefined there) |
| FALSE_DIVERGENCE / BOUNDED_RESHUFFLING / EMERGENT_ECOLOGY / NOVELTY_REGRESSION | Ecology alert codes | ecology-alerts-guide; EcologyAlertEngine |
| Dominance Index / Branch Entropy / Lineage Concentration / Collapse Risk | Dashboard health-strip metrics | ecosystem-dashboard-guide |
| Fitness sharing / adaptive niche capacity / opportunity-weighted mutation / role-based repulsion / behavioral projection | World-lab niche-ecology mechanisms (config block mirrors, not read live) | config `analytics.ecology`; worldlab sources |
| Equipment archetype / family | Item-kind archetype preserved on reroll; `give-specific` takes an archetype id or ability family | commands doc |
| `fast` profile | Maven profile that skips tests | pom.xml |
