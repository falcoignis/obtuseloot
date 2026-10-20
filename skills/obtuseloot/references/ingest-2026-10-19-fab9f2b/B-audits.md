> Analysis of falcoignis/obtuseloot HEAD fab9f2b, produced 2026-10-19 by a doc-ingest agent. Read from git HEAD. Inferences are labeled "inference:". Historical snapshot: verify against current code before relying on any file:line.

# Slice B: audits, operations, telemetry, harness readiness, process

Repo: the audit checkout, read from git HEAD fab9f2b (0.9.50-beta). Every repo read used `git show HEAD:<path>`. External sources: the private 2026-10-08 review.md and test-results.txt. build.log was NOT read. GitHub state was checked with read-only `gh run list`, `gh run view`, `gh workflow list` and `gh release list/view` on 2026-10-19.

Severity labels are the docs' own words. "not stated" means the doc gave no severity.

---

## 0. Headline findings (verified unless labeled)

1. **CI on main is red. Verified via gh.**
   - The last green "Build ObtuseLoot" run on main was the PR #397 merge (2026-04-04T03:39Z).
   - Every run since PR #398 ("Harden telemetry ingestion contracts", 2026-04-04T13:46Z) has failed, including PR #399 (the release-readiness audit, which is HEAD's merge).
   - Nightly Build failed on every run listed, 2026-05-28 to 2026-06-04. I inspected only the 2026-06-04 run (26936807839), which failed at the "Build plugin" step; the other runs' failing steps were not checked. GitHub now reports that workflow as `disabled_inactivity`.
   - This matches the Oct-8 external baseline: 379 tests, 370 pass, 1 failure, 8 errors.
   - Inference: the CI failure has the same cause as that baseline. I did not read the CI logs.
2. **Release 0.9.50-beta has no assets. Verified via gh.**
   - The GitHub release has tag `0.9.50-beta`, no `v` prefix. It is non-prerelease, marked "Latest", published 2026-03-24, with `assets: []`.
   - `release.yml` triggers on tag pushes matching `v*`. It attaches assets only `if: startsWith(github.ref, 'refs/tags/v')`.
   - Both workflow_dispatch release runs on 2026-03-24 failed. Both ran on ref `main`. A dispatch from `main`, or a run on the un-prefixed `0.9.50-beta` tag, cannot attach assets even when it succeeds, because that step is gated on a `refs/tags/v*` ref. A dispatch on a `v*` tag ref would pass the gate.
3. **The open-endedness verdict is a hardcoded string, not a computed result.**
   - `src/main/java/obtuseloot/simulation/worldlab/OpenEndednessTestRunner.java` (around lines 220-224 at HEAD) writes these literal strings into its report:
     - "Strongest divergence contributors: ecosystem controls (World C) and trait interaction layer (World D), then EDE (World B)."
     - "Designer-controlled classification remains: mostly designer-controlled with moderate emergent divergence in full system."
   - The confidence lines (around 193, 241, 319, 361) are also literals.
   - `docs/open-endedness-review.md` repeats the same verdict and ranking.
4. **Three CI "simulation" scripts, and part of the dashboard, run on synthetic Python data, not the Java engine.**
   - `scripts/run-chaos-tests.sh`, `run-population-simulation.sh` and `run-meta-analysis.sh` each run `python3 codex/run_internal_pipeline.py`, then echo a canned sentence such as "Chaos tests simulated: 1000 hits, 500 kills...".
   - That Python script builds 5000 rows from `random.Random(1337)` by modular indexing into hardcoded lists, using the old fusion-era taxonomy. It overwrites several `analytics/*.md` and `*.json` files.
   - The only tracked writer of `analytics/ecosystem-balance-data.json` is `codex/run_internal_pipeline.py:220`.
   - Java only reads that file: `dashboard/DashboardService.java:44`, `dashboard/DashboardDataAssembler.java:19`, `analytics/VisualizationExporter.java:26`.
   - The dashboard has other sources too. `DashboardService.buildDataSourceDescriptors` (`:108-120`) also lists `ecosystem-health-gauge.json`, `ecology-diagnostic-state.json` and `world-lab/world-sim-data.json`, and the service reads `ecology-truth-snapshot.json` (`:211`). Only the "ecosystem-balance" source (labeled `SIMULATION_SNAPSHOT`) and `VisualizationExporter` depend on the Python-written file.
   - `benchmark.yml` runs only Python. `nightly-build.yml` (`run-world-simulation.sh`), `dashboard.yml` and `world-lab.yml` (`run-world-lab-validation.sh`) also run the Java harness, so their artifacts mix Java output with Python-overwritten files.
   - The Oct-8 review independently states this for the wrapper scripts.
5. **The world-lab.yml inputs and the open-endedness.yml matrix mode do nothing.**
   - `world-lab.yml` exports `WORLD_PLAYERS`, `WORLD_SEASONS`, `WORLD_SIMULATION_MODE` and `WORLD_SCORING_MODE`. `open-endedness.yml` exports `OPEN_ENDEDNESS_MODE` per matrix leg.
   - `git grep getenv` over `src/` and `scripts/` at HEAD returns zero hits. `pom.xml` has no `env.` or exec `systemProperties` passthrough.
   - `run-world-lab-validation.sh` hardcodes its `-D` values (players 40/600, seeds 90211-90216).
   - `OpenEndednessTestRunner` reads only `world.players`, `world.seasonCount` and `world.sessionsPerSeason` system properties, and always runs worlds A-D.
   - Result: each of the 4 matrix legs runs the same full A-D experiment.
6. **The canonical "truth source" is stale.**
   - `analytics/ecology-truth-snapshot.json` was last committed 2026-03-11. It reads END=1.0, TNT_latest=0.0, nicheCount 2, speciesCount 0, dominantNicheShare 1.0, diagnosticState STAGNANT_ATTRACTOR.
   - `analytics/validation-suite/latest-run.properties` was last committed 2026-03-19. It points at `<historical-checkout>/analytics/validation-suite-rerun/deep-long-horizon-post-ability-fresh`, an absolute path from another machine.

---

## 1. Per-document summaries

Format: path | last commit | purpose | key durable facts | status with evidence.

### docs/AUDIT.md | 2026-03-24
- **Purpose:** an append-only log of several audit passes:
  - original configurability/lag audit (undated)
  - 2026-03-08 CI audit
  - 2026-03-08 build/publish
  - 2026-03-24 build/publish for 0.9.50-beta
  - 2026-03-09 command/perms
  - 2026-03-09 JDK root cause
  - 2026-03-20 ability-generation distribution audit (the bulk of the file)
- **Durable facts:**
  - `RuntimeSettings` is the central config snapshot. `LoreEngine` throttles via `lore.min-update-interval-ms`.
  - Commands added: `/ol addname|removename <pool>`, with `obtuseloot.edit.<pool>` and umbrella `obtuseloot.edit`.
  - `/ol reload` hot-reloads config, runtime settings and name pools.
  - Java 21 is required.
  - Publishing needs `GITHUB_TOKEN` and `GITHUB_REPOSITORY`. Both the 2026-03-08 and 2026-03-24 publishes were blocked for lack of a token.
  - Ability-audit pipeline: `scoreTemplate -> baseCompositeTemplateScore -> compositeTemplateScores -> weightedTemplateSelection -> normalizeCategoryTemplateScores`.
  - `triggerSaturationPenalty = clamp(1 - n*0.045*w, 0.72, 1.02)`. `TRIGGER_SATURATION_WEIGHTS` holds ON_WORLD_SCAN x1.22 and ON_RITUAL_INTERACT x1.15.
  - The input was `analytics/post-expansion-probe-20260319.txt`, 180 forced-probe selections. Each category got 9-26 selections.
- **Status:**
  - **Ability repairs: fix applied; outcome not re-measured.** The reachability test (sensing 16 vs 18) fails at HEAD. Checked at `ProceduralAbilityGenerator.java:1227-1240`. All recommended adjacency entries are present:
    - ON_BLOCK_INSPECT gets +ON_WITNESS_EVENT.
    - ON_STRUCTURE_PROXIMITY gets +ON_MOVEMENT and +ON_WEATHER_CHANGE.
    - ON_STRUCTURE_SENSE gets {ON_STRUCTURE_PROXIMITY, ON_MEMORY_EVENT}.
    - ON_WORLD_SCAN gets {ON_STRUCTURE_PROXIMITY}.
    - ON_SOCIAL_INTERACT gets +ON_WITNESS_EVENT.
  - **Context gate: implemented** as `isHighFrequencyContextMisaligned` (`:1242`, used at `:627` as `HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING`).
  - **Recategorizations** (checked in `AbilityRegistry.java`):
    - `consistency.path_thread` moved to TRAVERSAL_MOBILITY (line 46).
    - `evolution.lineage_fortification` moved to SURVIVAL_ADAPTATION (line 114).
    - `survival.last_light_cache` moved to SURVIVAL_ADAPTATION (line 169).
    - `consistency.structure_echo` moved to **SENSING_INFORMATION** with mechanic SENSE_PING (line 58). The audit proposed neither of these.
  - **Enforcer: superseded.** It was added in 76adaf6 (2026-03-08) and removed in 327657a (2026-03-21, "was already skip=true").
  - **CI workflows: superseded.** `ci.yml` and `maven-publish.yml` were deleted in 0d0cdbe (2026-03-09) and replaced by `build.yml` and `release.yml`.
  - **Check script: superseded.** `scripts/diagnose-maven-access.sh` was deleted in d8ee5d5 (2026-03-09).
  - **Still open:**
    - The "Recommended next steps" (branch protection, signing credentials) are not verifiable from the repo.
    - The 0.9.50-beta publish is still unexecuted. The release has 0 assets.

### docs/full-repo-audit.md | 2026-03-09
- **Purpose:** "Pass 1" whole-repo audit at v0.9.6.
- **Durable facts:**
  - The repo is split into modular domains.
  - The YAML/SQLite/MySQL persistence abstraction exists.
  - Single-run summaries should be treated as "directional".
- **Findings and status:**
  - **Nightly JAR path pinned to 0.9.5: resolved.** It is now `target/ObtuseLoot-*.jar`.
  - **`ecosystem environment` missing from plugin.yml usage: resolved.** plugin.yml line 27 now lists `ecosystem [health|dashboard|environment]`.
  - **`BalanceSignal` unreferenced: resolved/removed.** No such file at HEAD.
  - **`scripts/mvn-via-mirror.sh` missing: resolved.** It exists at HEAD (last commit 2026-03-09). The Oct-8 review confirms.
  - **Committed generated analytics: open.** The Oct-8 review counts about 299 MiB and 943 tracked files.

### docs/efficiency-and-regression-audit.md | 2026-03-09
- **Purpose:** "Pass 2" efficiency and regression audit.
- **Durable facts:**
  - Startup regenerates the dashboard and analytics.
  - Periodic environmental-pressure report writes are scheduled.
  - Config is read from in-memory `RuntimeSettings`, not repeated file reads.
- **Status:**
  - **Nightly path: resolved.** Same fix as above.
  - **Command doc drift: resolved.**
  - **Mirror script: resolved.**
  - **"Document which analytics files are authoritative": partially addressed** by `analytics-truth-source-guide.md`.
  - **"CI check for stale explicit versions in workflow paths": not implemented.** No such check exists in the workflows.
  - **Risk stated:** "medium for CI/ops clarity".

### docs/persistence-audit.md | 2026-03-09
- **Purpose:** YAML/SQLite/MySQL parity audit.
- **Durable facts:**
  - All backends persist through `PlayerStateStore`.
  - Migrations: `yaml-to-sqlite`, `yaml-to-mysql`.
  - `storage.fallbackToYamlOnFailure`. With fallback off, a failed init aborts startup.
  - Schema DDL is idempotent.
  - YAML is the default backend.
- **Status:**
  - **"MySQL pooling flag config-present but non-pooled": open.** Restated in the 2026-04-04 release-readiness audit and the Oct-8 review (`DatabaseManager.java:18` uses DriverManager).
  - **"Synchronous writes": open.** The Oct-8 review says SQL is synchronous from join, commands and autosave.
  - **HikariCP, SQLite integration tests, parity matrix: open.** Oct-8 counts 6 persistence test annotations across 2 files.
  - **Doc omission:** this audit says nothing about load failure returning null. That later became the blocker.

### docs/repo-hygiene-audit.md | 2026-03-09
- **Purpose:** v0.9.5 hygiene pass.
- **Durable facts:**
  - `target/` is never tracked.
  - `analytics/README.md` was added.
  - `releases/v0.9.x/` folders hold CHANGELOG, QA_NOTES and BUILD_INFO.
- **Status:**
  - Mostly historical.
  - **Normalizing `world-sim` vs `large-world-sim` report names: not stated as done. Open.**

### docs/release-readiness-audit-v0.9.50-beta.md | 2026-04-04 (newest in-repo audit)
- **Purpose:** RC readiness audit. Verdict "NOT READY / BLOCKED".
- **Blockers:**
  1. Silent SQL load failure leads to replacement identity.
  2. The root `obtuseloot.command` (default op) gates help/info.
  3. `/ol reload` is partial.
- **High-priority non-blockers:**
  - MySQL pool is inert.
  - Dashboard binds all interfaces with no auth or TLS.
  - Startup error messages are generic.
  - Compatibility claim is too broad.
- **Polish items:** `inspect` repeated in usage, minimal `/ol info`, no first-hour triage doc.
- **Status:** all three blockers are still open at HEAD.
  - **Gating: verified.** `plugin.yml` line 47 has `permission: obtuseloot.command`, defined default op at line 74. help and info default true (lines 50-55).
  - **SQL load failure: verified.** `JdbcPlayerStateStore.loadArtifact` logs and returns null in its catch-all, around line 154.
  - **Reload:** the Oct-8 review confirms at `ObtuseLootCommand.java:163`.
  - **Duplicate `inspect`: still present** (plugin.yml lines 15 and 39).
  - **Compatibility claim: still present.** `releases/v0.9.50-beta/RELEASE_NOTES.md` says "No migration steps are required when upgrading from 0.9.x pre-beta builds."
  - **No fix commit follows.** HEAD is this audit's merge (PR #399).

### docs/telemetry-ingestion-contract.md | 2026-04-04
- **Purpose:** typed telemetry boundary contract.
- **Durable facts:**
  - Boundaries:
    - `TelemetryEventFactory#create`
    - `TelemetryAggregationService#record`
    - `EcosystemHistoryArchive#append/read*`
    - `RollupStateHydrator`
    - `TelemetryRollupSnapshotStore#readLatest`
  - Event invariants: `timestampMs > 0`, non-null type, **`artifactSeed >= 0` (0 reserved for non-artifact events)**, non-null attributes.
  - `TelemetryFieldContract.normalize/validateEvent` require per-type fields and `schema_version`.
  - Failures are reported with batch-index, line-number and property-level context.
- **Status:**
  - **The doc is accurate to the code, but the contract itself caused the current regression.** Java longs used as seeds are signed.
  - The Oct-8 review reproduces 4 `WorldSimulationHarnessValidationProfileTest` errors ("Telemetry artifactSeed must be >= 0 but was -658..."). It also reproduces 4 analytics fixture errors ("'mechanic' is required for ABILITY_EXECUTION").
  - **Open.**

### docs/phase6-6-analytics-operations.md | 2026-03-12
- **Purpose:** operator guide for offline analytics.
- **Durable facts:**
  - Entry point: `obtuseloot.analytics.ecosystem.AnalyticsCliMain`, with `analyze`, `run-spec`, `decide` and `export-accepted`.
  - `TelemetryDatasetContract` runtime layout: `telemetry/ecosystem-events.log` plus `telemetry/rollup-snapshot.properties` or `rollups/*.properties`.
  - Harness layout: `telemetry/ecosystem-events.log` plus `telemetry/rollup-snapshot.properties`.
  - `scenario-metadata.properties` is optional.
  - The legacy `telemetry-events.log` is not ingested.
  - Each job writes 5 files: report, recommendation-history, job-record, run-metadata, output-manifest.
  - Governance is human-reviewed: PROPOSED, then `decide`, then accepted recommendations are exported as tuning profiles.
  - Wrapper: `scripts/run-ecosystem-analysis.sh <spec>`.
- **Status:**
  - **Current, but its tests are red.** The 3 `AnalyticsOperationalCliTest` cases and `EcosystemAnalyticsRunnerTest` error on the strict archive boundary, per the Oct-8 review.

### docs/phase6-6-forensic-audit.md | 2026-03-12
- **Purpose:** execution-path check of Phase 6.6. Verdict PARTIAL.
- **Durable facts:**
  - Governance states: PROPOSED, ACCEPTED, REJECTED, SUPERSEDED, APPLIED.
  - `TuningProfileExport` exports only ACCEPTED and APPLIED, and maps analytics keys to runtime config keys.
  - The analytics runner is not called from the plugin runtime.
- **Gaps:**
  - Legacy datasets are rejected, by design.
  - There is no embedded scheduler.
- **Status:** gaps still stand as designed. Not re-verified in source.

### docs/reporting-hardening-audit.md | 2026-03-19
- **Purpose:** records an upgrade of generated reports to a structured format: scope, method, findings, dominant/low-frequency/suspicious systems, confidence, next steps.
- **Status:**
  - **"Evidence still weak: multi-seed rerun coverage limited": open.** No later doc in this slice reports multi-seed open-endedness reruns.
  - **Inference: "confidence framing" overstates the guarantee.** In `OpenEndednessTestRunner` the confidence lines are literal strings, not derived from variance.

### docs/analytics-truth-source-guide.md | 2026-03-11
- **Purpose:** declares the canonical metrics file.
- **Durable facts:**
  - The canonical file is `analytics/ecology-truth-snapshot.json`.
  - END, TNT, NSER and PNNC propagate from one run-level snapshot into gauge, diagnostic, novelty, dashboard and impact reports.
  - Impact reviews answer yes, no or inconclusive.
  - Consistency checks enforce trend-tail alignment.
- **Status:**
  - **The mechanism is described. The file instance is stale** (STAGNANT_ATTRACTOR, 2026-03-11).
  - There is a second, parallel "latest dataset" mechanism: `analytics/validation-suite/latest-run.properties`. The guide does not mention it.

### docs/open-endedness-review.md | 2026-03-09
- **Purpose:** conclusion on emergence.
- **Durable facts:** "Current status remains mostly designer-controlled, with measurable but still moderate ecosystem divergence."
  - Ranked contributors: ecosystem bias/diversity/self-balancing controls, then trait interaction layer, then EDE.
  - "Divergence strength emerges from progression feedback loops, not from generator starvation."
  - Next step: 3-seed reruns, with turnover/entropy/concentration as pass/fail gates.
- **Status:**
  - **The conclusion text matches strings hardcoded in `OpenEndednessTestRunner.java`** (see Section 4).
  - The 3-seed rerun step is not reported done in this slice. **Open.**

### docs/pr-split-plan.md | 2026-03-08
- **Purpose:** split the monolithic commit 8a8ebc8 (base c03664f) into stacked branches `split/pr1-foundation` through `split/pr4-core`.
- **Status: superseded/historical.** `scripts/execute-pr-split.sh` was added in 5f8888d and deleted in e32d4b9, both on 2026-03-08. The doc's one-command flow no longer works.

### docs/release-v0.9.0.md | 2026-03-08
- First public release. `obtuseloot-0.9.0.jar`, built with `mvn -B clean package`. Historical.
- The GitHub release "ObtuseLoot v0.9.0" exists, published 2026-03-09.

### docs/release-v0.9.3.md | 2026-03-09
- Version-alignment note pointing at `releases/v0.9.3/`. Historical.

### docs/release-v0.9.6.md | 2026-03-09
- Says `v*` tag pushes run `release.yml`. Historical.
- The release tag push runs for v0.9.6 and v0.9.7 on 2026-03-10 **failed** (gh run list), yet the GitHub release v0.9.6 exists, published 00:31Z.
- Inference: the v0.9.6 release was created by some other path. Unverified.

### docs/obtuseloot_codex_analytics_review_pack.md | 2026-03-19
- **Purpose:** a reviewer checklist and 8-category 1-5 rubric for Codex-generated analytics.
- **Its standard:** "thousands of seeds". Low-frequency paths should not be auto-labeled bad, nor dominant paths auto-nerfed. Review evidence first, recommendations second.
- **Review order:** BUILD_INFO, population-report, meta-report, balance-suggestions, meta-analysis.json, evolution-report.
- **Status:** process guidance, still applicable.
- Several of the files it names come from the Python synthetic pipeline. That pipeline's recommendations use fixed-ratio rules such as "Increase {low_f} template exposure by 5-8%", which is exactly the pattern the pack warns against.

### docs/harness-pre-execution-analysis.md | 2026-03-13
- **Purpose:** verifies the `WorldSimulationRunner` system properties.
- **Durable facts:**
  - Properties: `world.validationProfile`, `world.players`, `world.artifactsPerPlayer`, `world.sessionsPerSeason`, `world.seasonCount`, `world.encounterDensity`, and `world.telemetrySamplingRate` (clamped).
  - `world.branchLifecycleCoalescing` and `world.branchLifecycleEvaluationInterval` do not exist. Still absent at HEAD (git grep, 0 hits).
  - Recommended bounded config: 18 players / 3 artifacts / 2 sessions / 3 seasons / density 5 / sampling 0.25.
  - Verdict READY.
- **Status:** the properties remain. The READY verdict relied on `WorldSimulationHarnessValidationProfileTest`, which now errors. **Superseded by the current red state.**

### docs/validation-harness-readiness-report.md | 2026-03-13
- **Purpose:** harness entrypoints and validation-profile output check.
- **Durable facts:**
  - Entrypoints: `WorldSimulationRunner#main`, `OpenEndednessTestRunner#main`, `scripts/run-world-lab-validation.sh`.
  - `world.scenarioConfigPath` is loaded via `EvolutionExperimentConfig.load`.
  - In validation profile the harness:
    - writes a reduced JSON
    - omits `seasonal_snapshots` and `phase6_experiment_outputs`
    - writes placeholder `world-sim-meta-shifts.md` and `world-sim-balance-findings.md`
    - returns early
  - Lifecycle telemetry fields: `lifecycle_state`, `survival_score`, `maintenance_cost`, `event=branch-collapsed`, `collapse_reason`.
- **Status:** READY is contradicted by the current test failures (same tests). **Superseded.**

### docs/a41b-preflight-analysis.md | 2026-03-14
- **Purpose:** preflight for "A4.1b". Verdict PARTIAL.
- **Findings:**
  - There was no `.dataset-complete.properties` or `latest-run.properties` writer in `run-world-lab-validation.sh`.
  - Only 4 open-endedness scenarios (A-D) exist, against a 5-scenario requirement.
- **Status: partially resolved, elsewhere.**
  - The completion marker and pointer now exist in `run-validation-suite-rerun.sh` (around lines 260 and 670-700, tmp-then-mv write) and `run-deep-validation.sh` (around lines 386-405).
  - `run-world-lab-validation.sh`, the script that CI calls, still has no marker (grep, 0 hits).
  - The 5-scenario set (explorer-heavy, ritualist-heavy, gatherer-heavy, mixed, random-baseline) lives in the validation-suite scripts. `OpenEndednessTestRunner` still has 4 worlds.

### docs/codex-validation-phase-report-20260315.md | 2026-03-15
- **Purpose:** report on a constrained 5-scenario validation run. Verdict MOSTLY READY.
- **Durable facts:**
  - Runner: `scripts/run-validation-suite-rerun.sh`. Pointer: `analytics/validation-suite/latest-run.properties`.
  - Half-life keys: `branch_survival_half_life`, `cohorts_measured`, `branch_survival_half_life_censored` and `estimate_status`, in `EcosystemAnalyticsRunner`.
  - Fusion adoption: 0.35 (random) to 0.50 (explorer).
  - Births 69-74 and collapses 62-71 per scenario.
  - Dominant family rate 0.40-0.50.
  - Half-life is 1.000 with `cohorts_measured=1` in every scenario, so there is no differentiation.
  - Niche attribution: a large GENERALIST share plus "niche-1".
- **Status:**
  - The half-life depth limitation was the stated next action. A later pointer run id `deep-long-horizon-post-ability-fresh` (2026-03-19) suggests deeper runs happened. Inference only. Their results are in docs outside this slice.
  - It uses "fusion" terminology. The engine was later renamed to convergence (Oct-8 review: "convergence ... rather than a two-item crafting transaction").

### codex/ (git ls-files codex)
- **There are zero `*.md` files under `codex/`.** The only tracked file is `codex/run_internal_pipeline.py` (2026-03-09).
- Repo `CLAUDE.md` labels `codex/` "Internal pipeline scripts and analytics review docs". The review pack lives in `docs/` instead.
- **What `run_internal_pipeline.py` does:**
  - It is a synthetic, Java-independent generator.
  - Old taxonomy: 6 families, 13 mechanics, 12 triggers (including `onFusion`), 18 branches (`*.fusion-*`), 10 memory events.
  - `behavioral` is a rule over that synthetic mechanic/trigger.
  - It writes:
    - `analytics/ability-authenticity-report.md` (the ">=80% gate PASS/FAIL")
    - `analytics/procedural-generator-audit.md` and `analytics/review/procedural-generator-audit.md`
    - `ability-ecology-report.md`
    - `memory-system-report.md`
    - `ability-visualization-data.json`
    - `ecosystem-balance-data.json`
    - `ecosystem-balance-report.md`
    - `ecosystem-balance-suggestions.md`
  - The report prose asserts things like "Deterministic seed composition validated" and "Verified mutation outputs are generated from active profiles", but it never runs Java.

### .github/workflows/*.yml (all last committed 2026-03-24; all set FORCE_JAVASCRIPT_ACTIONS_TO_NODE24, temurin 21)

| Workflow | Trigger | What it does |
|---|---|---|
| build.yml | push to main, PRs | `mvn -B -ntp clean package` (tests on). Uploads `target/*.jar` and all of `analytics/`. Red on main since 2026-04-04. |
| nightly-build.yml | cron 03:00 UTC, dispatch | Package with tests. Runs chaos, population (both Python) and `run-world-simulation.sh` (Java), then `run-meta-analysis.sh` (Python). Uploads `analytics/**` and the jar. **disabled_inactivity**. The last 8 runs failed; the one inspected failed at the build step. |
| benchmark.yml | dispatch (`chaos`, `population`, `both`) | Package without tests, then runs only the Python wrappers. Uploads `analytics/performance`, `meta` and `population`. |
| dashboard.yml | dispatch | Package without tests. Runs `run-world-lab-validation.sh` (Java, 1 small run + 5 large seeded runs + Python aggregation), then `run-meta-analysis.sh` (Python; overwrites `ecosystem-balance-data.json`). Prints `ecology-alerts-report.md`. Uploads `analytics/dashboard` and `visualizations`. |
| world-lab.yml | dispatch (`player_count`, `season_count`, `simulation_mode`, `scoring_mode`) | Same scripts as the dashboard workflow. The inputs are exported as env vars that nothing reads. Prints the alerts and `world-lab/ecology-regression-gate-review.md`. Uploads world-lab, PNNC and alerts files. |
| open-endedness.yml | dispatch | Matrix over full-system, no-ede, no-bias-diversity and no-trait-interactions. Each leg runs `run-open-endedness-test.sh` (all 4 worlds; the mode env is ignored) and stashes its output. An aggregate job merges the artifacts. |
| release.yml | `v*` tag push, dispatch | Package with tests. Picks the first finalized jar. Uploads the artifact. Creates a GitHub release with jar plus world-lab and dashboard `*.md` only when the ref is `refs/tags/v*`. |

### scripts/ (header-level purposes)

| Script | Last commit | Purpose |
|---|---|---|
| build.sh | 03-08 | Probes Maven Central with curl. On success, `mvn -B -ntp $@` (default `clean package`). Otherwise falls back to `mvn-via-mirror.sh` if `MAVEN_MIRROR_URL` is set, else exits 2 with instructions. |
| mvn-via-mirror.sh | 03-09 | Writes a temp `settings.xml` with mirrorOf `*` set to `MAVEN_MIRROR_URL` and optional `MAVEN_PROXY_USER`/`PASS`, then runs mvn. |
| publish.sh | 03-08 | Deploys to GitHub Packages (server id `github`). Needs `GITHUB_TOKEN`. Resolves the slug from `GITHUB_REPOSITORY` or `origin`. Uses temp settings and supports mirror and proxy env. |
| build-and-publish.sh | 03-08 | Runs build.sh then publish.sh. Args after `--` go to publish. |
| start-maven-proxy.sh | 03-21 | Starts `/usr/local/bin/maven-proxy-server.py` on 127.0.0.1:18080. Environment-specific; see Section 3. |
| run-chaos-tests.sh | 03-09 | Runs the Python synthetic pipeline and echoes canned "1000 hits..." text. |
| run-population-simulation.sh | 03-09 | Same Python pipeline. Echoes "5000 seeds". |
| run-meta-analysis.sh | 03-09 | Same Python pipeline. Echoes "Meta analysis + confidence scoring + multi-run validation complete." |
| run-world-simulation.sh | 03-09 | Compiles, then runs `WorldSimulationRunner` via exec-maven-plugin 3.5.0 with defaults. |
| run-open-endedness-test.sh | 03-09 | Compiles, then runs `OpenEndednessTestRunner` (worlds A-D). Output goes to `analytics/world-lab/open-endedness`. |
| run-world-lab-validation.sh | 03-10 | One small run (seed 90211, 40 players) plus 5 large runs (seeds 90212-90216, 600 players, 4 artifacts, 24 sessions x 4 seasons, density 8). Python aggregates `multirun-world-sim-data.json` and reports. No completion marker. |
| run-validation-suite-rerun.sh | 03-15 | The 5-scenario constrained suite. Staged manifest; `.dataset-complete.properties` written atomically; pointer guard ("never point latest-run.properties at analysis-only directories"); `--test-mode` stubs. |
| run-deep-validation.sh | 03-18 | Deep suite: 8 seasons x 4 sessions = 32 windows, 4 players, 2 artifacts, population 6, density 3, sampling 0.25, 5 scenarios. Tries `mvn --offline` first, then `-s /tmp/maven-local-proxy.xml` "local proxy on :18081". |
| run-ecosystem-analysis.sh | 03-12 | Compiles, then runs `AnalyticsCliMain run-spec --spec <file>`. |
| deep_authenticity_validation.py | 03-19 | Parses `AbilityRegistry.java` by regex and baselines against `analytics/post-expansion-probe-20260319.txt`. Writes `docs/deep-authenticity-validation.md`, which self-describes as a "synthetic high-volume authenticity audit". Scenarios are explorer, ritualist, **warden**-heavy, mixed and random; 1250 per scenario; 100 forced per category. |

### External: ObtuseLoot-Reviews/2026-10-08T042712Z/review.md and test-results.txt (most recent assessment)

**Baseline:**
- fab9f2b, built in the official `maven:3.9-eclipse-temurin-21` image with `mvn -B -ntp clean package`.
- 377 main and 74 test sources compile.
- 379 tests: 370 pass, 1 failure, 8 errors. BUILD FAILURE, no jar.

**Confirms in-repo audits:**
- SQL null leads to regeneration (release-readiness blocker 1), plus saves that do not propagate failure.
- Root gate (blocker 2).
- Partial reload (blocker 3).
- Inert MySQL pool (persistence-audit and release-readiness).
- Dashboard bound to all interfaces with no auth. It adds that the startup log says "localhost".
- Overbroad 0.9.x compatibility claim.
- Partial config validation.

**New beyond in-repo audits:**
1. Signed seeds are rejected by the telemetry contract. The live path (join, then subscription rebuild, then `LineageRegistry`, then emit) is at risk. Fix without `abs`/masking, to preserve identity.
2. Analytics test fixtures bypass normalization (missing `mechanic`).
3. Sensing reachability is 16 against a minimum of 18 in `IntraCategoryNormalizationProbeTest`. Cause not isolated.
4. Persistence is per player (`player:<uuid>`), not per item.
5. The legacy migration rebinds modern items to whoever interacts with them (`ArtifactItemStorage.java:62`).
6. `LoreEngine` does not update item metadata.
7. Convert/reroll replace the item with a fresh `ItemStack`, so enchantments and durability are lost.
8. `AbilityExecutor.execute` performs no world effect.
9. The harvest replant callback reads the post-break block.
10. Movement sensing needs more than 4 blocks of displacement in a single event.
11. The self-balancing `evaluate` is fed only by the world harness.
12. There are two environment-pressure engine instances.
13. The analytics and simulation classes ship in the plugin JAR.
14. Ten remote branches are not ancestors of main. Confirmed: `git branch -r --no-merged origin/main` returns 10 in this clone.

**Contradicts or updates in-repo docs:**
- The mirror helper now exists.
- CLAUDE.md still says 0.9.28.
- The 0.9.50-beta release has no assets. Confirmed via gh.

---

## 2. Consolidated open-issue ledger (de-duplicated)

| # | Issue | Sources | Stated severity |
|---|---|---|---|
| 1 | The build and test baseline is red: 1 failure and 8 errors. CI main has been red since PR #398 (2026-04-04). Release packaging produces no JAR. | Oct-8 review, test-results.txt; gh run list | Oct-8: "Restore a passing baseline" is next-iteration item 1 |
| 2 | Telemetry `artifactSeed >= 0` conflicts with signed seed generation (generator, rollSeed, awakening, convergence). The live join path is at risk. | telemetry-ingestion-contract.md (introduces the invariant); Oct-8 review section 1 | Oct-8: "New blocker" |
| 3 | Analytics test fixtures emit ABILITY_EXECUTION without `mechanic`, which the strict archive rejects. | Oct-8 review; test-results.txt | Oct-8: "separate integration mismatch" (part of the baseline) |
| 4 | `IntraCategoryNormalizationProbeTest`: sensing reach is 16, below the minimum of 18. | Oct-8 review; test-results.txt | not stated (must fix before "green baseline") |
| 5 | A SQL load exception returns null, which `getOrCreate` treats as a missing record and replaces the identity. Failed SQL saves are marked as saved. | release-readiness Blocker 1; Oct-8 section 2; verified at JdbcPlayerStateStore around line 154 | Release Blocker |
| 6 | Root `permission: obtuseloot.command` (op) makes help/info (default true) unreachable for non-ops. | release-readiness Blocker 2; Oct-8 section 3; verified plugin.yml:47,74 | Release Blocker |
| 7 | `/ol reload` is partial: no ecology registry, persistence, dashboard, safety, scheduler-period or cache refresh. | release-readiness Blocker 3; Oct-8 section 3 | Release Blocker |
| 8 | `mysql.connectionPool.*` is parsed but does nothing. Uses DriverManager per call. | persistence-audit; release-readiness; Oct-8 | High-priority non-blocker |
| 9 | SQL I/O is synchronous on join, commands and autosave. Some paths double-save. | persistence-audit ("Risks"); Oct-8 section 2 | not stated |
| 10 | The dashboard HTTP server binds all interfaces with no auth, TLS or allowlist, and logs "localhost". Disabled by default. | release-readiness; Oct-8 section 6 | High-priority non-blocker |
| 11 | Config validation is partial: no bounds on telemetry, safety or cache values. | release-readiness section 5; Oct-8 section 6 | "Should fix before serious RC" |
| 12 | The release notes' 0.9.x "no migration steps" claim is broader than the tested migration paths. | release-readiness; Oct-8; verified in RELEASE_NOTES.md | High-priority non-blocker |
| 13 | SQL schema add-column swallows exceptions, which can hide non-duplicate DDL errors. | release-readiness section 6 | not stated |
| 14 | Startup failure messages are generic (for example a blank `paths.analyticsRoot` throws). | release-readiness | High-priority non-blocker |
| 15 | plugin.yml usage is dense, repeats `inspect` and mixes debug, admin and operator commands. No role-layered help. `/ol info` is minimal. | release-readiness sections 4, 7, 8; verified | Medium/Low polish |
| 16 | Modern items are rebound to the interacting player through the "legacy" migration path. | Oct-8 section 4 | not stated (needs integration test) |
| 17 | Lore and item rendering are not updated on progression. Only the action bar changes. | Oct-8 section 4 | not stated |
| 18 | Convert and reroll replace the ItemStack and lose enchantments, durability and metadata. | Oct-8 section 4 | not stated |
| 19 | `AbilityExecutor` performs no world effect. Utility and telemetry may overstate applied gameplay. | Oct-8 section 5 | not stated (next-iteration item 4) |
| 20 | The harvest relay reads the post-break block (Oct-8 labels replant failure an inference). Move sensing requires more than 4 blocks in one event. | Oct-8 section 5 | not stated |
| 21 | The self-balancing engine is fed by the world harness only. Two environment-pressure instances exist. Simulations may not represent the live balance path. | Oct-8, "Progression, abilities, and ecology" section | not stated ("deserves explicit runtime verification") |
| 22 | Persistence is keyed per player, not per item. This is a product decision still pending. | Oct-8 | not stated (next-iteration item 3) |
| 23 | Benchmark output is purely from the synthetic Python pipeline. Nightly, dashboard and world-lab artifacts mix Java harness output with Python-overwritten files. The dashboard's "ecosystem-balance" source and VisualizationExporter read a JSON written only by Python. | This review (Section 0.4); Oct-8 Tooling section | not stated |
| 24 | The workflow inputs (`WORLD_*`) and the open-endedness matrix `OPEN_ENDEDNESS_MODE` are ignored. | This review (Section 0.5) | not stated |
| 25 | Open-endedness report conclusions and confidence lines are hardcoded. | This review (Section 0.3) | not stated |
| 26 | The truth snapshot is stale (2026-03-11, STAGNANT_ATTRACTOR). The `latest-run.properties` pointer is an absolute foreign path. | This review | not stated |
| 27 | `run-world-lab-validation.sh` (the script CI calls) has no completion marker or pointer safeguard. | a41b-preflight; verified at HEAD | a41b: readiness PARTIAL |
| 28 | Multi-seed reruns of the open-endedness suite are limited or absent. Half-life ran with `cohorts_measured=1`. | reporting-hardening-audit; open-endedness-review; codex-validation-phase-report | "Evidence still weak" / "Regression watch" |
| 29 | Generated analytics are committed (about 299 MiB, 943 files), so stale reports may be mistaken for current. | full-repo-audit section 8; efficiency-audit section 6.3; Oct-8 | not stated |
| 30 | The 0.9.50-beta GitHub release has zero assets. The tag lacks `v`, so release.yml cannot attach assets. | Oct-8; verified via gh and release.yml | not stated |
| 31 | Nightly Build is disabled by GitHub for inactivity. All workflows use actions@v4 on Node 20 forced to Node 24 (deprecation annotations). | gh workflow list, gh run view | not stated |
| 32 | 10 remote branches are not merged into main, with unmatched patches (naming, convergence refactor, evolution-stability). | Oct-8; verified count | not stated ("needs targeted comparison") |
| 33 | No live Paper/Purpur test exists. Command tests do not exercise Bukkit root-permission routing. | Oct-8 Tooling section | not stated |
| 34 | Branch-lifecycle coalescing and evaluation-interval controls are not exposed. | harness-pre-execution-analysis | "does not block" |
| 35 | The analytics and simulation code ships inside the plugin JAR. | Oct-8 | not stated |

---

## 3. Process model

### Development flow (from docs and git)
- The work is organized in numbered phases. Commit subjects mention Phase 1, 1.5, 2-5, 6, 6.1, 6.1A, 6.25, 6.4, 6.5, 6.6, 7.1-7.6, 8, 8.1 and 8.4.
- There are lettered sub-tasks such as "A4.1b".
- Many docs are "pass N" reports, for example deep-extinction-turnover-tuning-pass2, pass3 and pass3_5 (seen in grep, outside this slice).
- **Agent branches** (origin has 304 remote heads):
  - 247 `codex/<kebab-task-description>[-<6char>]`, plus a few prefixed variants like `rclflc-codex/...`, `kscviy-codex/...`.
  - 49 `claude/<verb-topic>-<5char>`, for example `claude/audit-ability-generation-7upXk` and `claude/fix-fusion-adoption-lHPVl`.
  - 8 flat branches: Claude-test, Generation-test, Naming, Newnames, audit, ...
- Merges go to main through GitHub PRs ("Merge pull request #NNN from falcoignis/codex/..."). The highest is #399.
  - Merge-subject source branches: 286 codex, 51 claude.
- Commit authors: falcoignis 735, Claude 79, Codex 12.
- Branch-name verbs are typically analyze-, audit-, fix-, add-, adjust-. Inference: a common pattern is an analysis-only branch followed by a separate fix branch. There are many `analyze-harness-output-directories-*` variants.

### Validation and harness flow (as documented)
1. Build: `mvn -B -ntp clean package` (tests on). There is a `fast` profile in pom.xml at line 143.
2. **World lab** (`WorldSimulationRunner`, `-Dworld.*` properties):
   - `run-world-lab-validation.sh`: 1 small run plus 5 seeded large runs, aggregated by Python into `analytics/world-lab/` (multirun JSON, confidence report).
3. **Open-endedness ablation** (`OpenEndednessTestRunner`), worlds:
   - A: full system
   - B: no EDE
   - C: no ecosystem bias, diversity preservation, self-balancing or environment pressure
   - D: no trait interactions
   - Output goes to `analytics/world-lab/open-endedness/`.
4. **Constrained validation suite** (`run-validation-suite-rerun.sh`), 5 scenarios: explorer-heavy, ritualist-heavy, gatherer-heavy, mixed, random-baseline.
   - Output: `analytics/validation-suite-rerun/<run-id>/`, with per-scenario `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`, `rollup-snapshots.json` and `scenario-metadata.properties`.
   - Then a staged manifest, an atomic `.dataset-complete.properties`, and an update to the `analytics/validation-suite/latest-run.properties` pointer.
5. **Deep validation** (`run-deep-validation.sh`): the same 5 scenarios over 32 windows.
6. **Offline analytics** (`AnalyticsCliMain analyze|run-spec`) over a dataset. Output: report, recommendation-history, job-record, run-metadata, output-manifest.
   - Human governance (`decide`) moves recommendations from PROPOSED to ACCEPTED or REJECTED.
   - `export-accepted` writes runtime tuning profiles.
7. **Truth reconciliation:** `analytics/ecology-truth-snapshot.json` feeds gauge, diagnostic, novelty, dashboard and impact reviews.
   - `EcologyDiagnosticEngine` classifies the state as COLLAPSED_MONOCULTURE, STAGNANT_ATTRACTOR, FALSE_DIVERGENCE, TURBULENT_THRASH, EMERGENT_ECOLOGY or others, with fixed thresholds. Example: EMERGENT needs END >= 3.5, TNT in [0.20, 0.55), NSER in [0.10, 0.50), and PNNC >= 3.
8. **Review:** `docs/obtuseloot_codex_analytics_review_pack.md` checklist and rubric.
9. **Phase reports** use a "SECTION N" format with READY, MOSTLY READY, PARTIAL or NOT READY verdicts.

### CI and release steps
- **CI:** `build.yml` on push and PR.
- **Manual analytics workflows:** benchmark, dashboard, world-lab, open-endedness.
- **Nightly:** now disabled.
- **Release (documented):**
  1. Align the version in `pom.xml` and `plugin.yml`.
  2. Add `releases/vX/` (CHANGELOG, QA_NOTES, BUILD_INFO, RELEASE_NOTES) and `docs/release-vX.md`.
  3. Push a `v*` tag, which triggers `release.yml`.
  4. Optionally publish to GitHub Packages with `scripts/publish.sh` and a token.
- **Release (observed):**
  - The 0.9.50-beta tag has no `v` prefix.
  - The 03-24 dispatch runs failed.
  - The release exists with no assets.
  - The publish step was blocked for lack of a token.

### Portable vs environment-specific tooling
**Portable:**
- `mvn -B -ntp clean package` (the Oct-8 review used the official Maven/Temurin 21 image)
- GitHub workflows
- `build.sh` (Central probe)
- `mvn-via-mirror.sh` (generic mirror env)
- `publish.sh`
- `run-world-simulation.sh`, `run-open-endedness-test.sh`, `run-world-lab-validation.sh`, `run-validation-suite-rerun.sh`, `run-ecosystem-analysis.sh`
- `AnalyticsCliMain`

**Environment-specific (do not assume on falco-servo):**
- `scripts/start-maven-proxy.sh` needs `/usr/local/bin/maven-proxy-server.py` on 127.0.0.1:18080.
- Repo CLAUDE.md's "Maven Dependencies (IMPORTANT)" section describes an egress proxy at an environment-specific authenticated proxy that needs preemptive auth, a the local Maven settings file mirror to localhost:18080, and that proxy script. Inference: this describes an earlier cloud agent sandbox.
- `run-deep-validation.sh` uses `-s /tmp/maven-local-proxy.xml` and "port 18081". This disagrees with the 18080 used in `start-maven-proxy.sh` and CLAUDE.md.
- `latest-run.properties` uses `dataset_root=<historical-checkout>/...`.
- AUDIT.md cites JDK paths `/root/.local/share/mise/installs/java/{17.0.2,21.0.2}`.

**Synthetic, not engine-backed:**
- `codex/run_internal_pipeline.py`
- `run-chaos-tests.sh`, `run-population-simulation.sh`, `run-meta-analysis.sh`
- `deep_authenticity_validation.py`, which is regex plus synthetic text scoring over the registry source

---

## 4. Contradictions and staleness

1. **Open-endedness conclusion.** `docs/open-endedness-review.md` says "mostly designer-controlled ... moderate ecosystem divergence" and ranks bias/diversity, then trait interaction, then EDE. `OpenEndednessTestRunner.java` (around 220-224) hardcodes the same verdict and ranking as literal strings, independent of computed data.
2. **Workflow inputs and matrix.** `world-lab.yml` declares player, season, simulation and scoring inputs, and `open-endedness.yml` runs a 4-mode matrix. Against that: zero `getenv` in src/scripts, no pom passthrough, hardcoded `-D` values in `run-world-lab-validation.sh`, and `OpenEndednessTestRunner` always running A-D.
3. **CI and benchmark naming vs reality.** The workflows call steps "Run simulations", "Benchmark", "meta-analysis and confidence scoring". Three of those scripts run only the Python synthetic pipeline (`scripts/run-chaos-tests.sh` and others).
4. **Dashboard truth (partial).** `analytics-truth-source-guide.md` says END, TNT, NSER and PNNC propagate into the dashboard, and `DashboardService` does read `ecology-truth-snapshot.json` (`:211`), gauge and diagnostic JSONs. But `DashboardService.calculateMetrics` (`:44`) reads `familyDistribution`, `branchDistribution`, `triggerDiversity` and `mechanicDiversity` from `ecosystem-balance-data.json`, and the only tracked writer of that file is `codex/run_internal_pipeline.py:220`.
   - Inference: the rank-abundance and ecosystem-distribution metrics reflect synthetic data unless an operator supplies the file. Not verified at runtime.
5. **Readiness verdicts vs current tests.** `harness-pre-execution-analysis.md` and `validation-harness-readiness-report.md` both say READY based on `WorldSimulationHarnessValidationProfileTest`. That test class has 4 errors at HEAD (Oct-8 test-results.txt).
6. **Telemetry contract vs seeds.** `telemetry-ingestion-contract.md` requires `artifactSeed >= 0`. The Oct-8 review documents signed seeds from `ArtifactGenerator.generateFor`, `ArtifactManager.rollSeed`, awakening and convergence. Not re-verified here beyond the test output.
7. **AUDIT.md enforcer.** AUDIT.md: "Added Maven Enforcer rule to fail fast". Commit 327657a (2026-03-21) removed `maven-enforcer-plugin` ("was already skip=true").
8. **AUDIT.md CI files.** AUDIT.md describes `ci.yml` and `maven-publish.yml`. Both were deleted in 0d0cdbe (2026-03-09).
9. **AUDIT.md check steps.** They reference `./scripts/diagnose-maven-access.sh`, deleted in d8ee5d5. `pr-split-plan.md` references `scripts/execute-pr-split.sh`, deleted in e32d4b9.
10. **AUDIT.md "No scoring constants need adjustment".** AUDIT.md offered a context gate in `categoryWeight` as an alternative fix for `lineage_fortification`. At HEAD both the recategorization and a gate were done (`isHighFrequencyContextMisaligned`, with a new constant `HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING`). The gate adds a scoring constant despite the audit's statement. This is a divergence, not a flat contradiction. Separately, `structure_echo` went to SENSING_INFORMATION, which neither recommendation proposed.
11. **full-repo and efficiency audits** say `mvn-via-mirror.sh` is missing. It exists at HEAD (added 2026-03-09).
12. **Repo CLAUDE.md (2026-03-21) staleness:**
    - Says version 0.9.28. pom.xml and plugin.yml are 0.9.50-beta.
    - Says `codex/` holds "analytics review docs". It contains 0 .md files.
13. **Proxy port.** `start-maven-proxy.sh` and CLAUDE.md use 18080. `run-deep-validation.sh:53-54` says ":18081" with `/tmp/maven-local-proxy.xml`.
14. **Scenario naming.** `run-validation-suite-rerun.sh` and `run-deep-validation.sh` use `gatherer-heavy`. `deep_authenticity_validation.py:12` uses `warden-heavy`.
15. **a41b "five scenarios"** vs OpenEndednessTestRunner's four worlds. The 5-scenario set exists only in the validation-suite scripts. The two sets are different experiments that share no names.
16. **Terminology drift (fusion vs convergence):**
    - plugin.yml still lists `debug ... fuse` alongside `force-converge`.
    - The codex pipeline uses `onFusion` and `*.fusion-*`.
    - The 2026-03-15 report measures "fusion adoption".
    - The Oct-8 review describes a ConvergenceEngine.
17. **Release docs vs CI.** `release-v0.9.6.md` says `v*` tag pushes run release.yml. The v0.9.6 and v0.9.7 tag-push runs failed on 2026-03-10, yet a v0.9.6 GitHub release exists. 0.9.50-beta was tagged without `v`.
18. **Release-readiness "Build/install instructions are clear and modernized (... tests-on-default ...)"** vs reality: the default build with tests fails at HEAD.
19. **Truth snapshot.** It is "canonical" but dated 2026-03-11 and reads STAGNANT_ATTRACTOR. Later docs (validation reports, 03-15 to 03-19) report collapses, births and non-zero niches. Inference: it was not regenerated after those runs.
20. **Evidence scale.** The review pack demands "thousands of seeds". The AUDIT.md 2026-03-20 category conclusions rest on 9-26 selections per category (180 in total).

---

## 5. Emergence lens (catalogue only, no judgment)

### Top-down control mechanisms described in docs, or seen in spot-checked source

**Selection scoring:**
- **Trigger saturation penalty:** `clamp(1 - n*0.045*w, 0.72, 1.02)` with hand-set per-trigger weights (ON_WORLD_SCAN 1.22, ON_RITUAL_INTERACT 1.15). [AUDIT.md]
- **Intra-category normalization:** `normalizeCategoryTemplateScores` (`ProceduralAbilityGenerator.java:831`, used at 749). The Oct-8 review lists "category/trigger normalization".
- **Small-category handling:** `SMALL_CATEGORY_TEMPLATE_LIMIT = 6` (`:56`).
- **Cold-template and under-sampled boosts:** `underSampledApplicabilityBoost`, `underSampledTriggerRelief`, "cold-template boosts". [AUDIT.md]
- **Hand-authored adjacency map:** `adjacentTriggerFamily` (`:1227`), edited specifically to make zero-hit templates reachable. [AUDIT.md]
- **Context-misalignment dampening:** applied to ON_LOW_HEALTH and ON_TIME_OF_DAY_TRANSITION templates (`:1242`, `:627`).
- **Manual recategorization** of templates to rebalance category shares. [AUDIT.md]
- **AUDIT goal statement:** "bring all categories to full reachability".
- **Test-enforced reachability floor:** `IntraCategoryNormalizationProbeTest` requires sensing at least 18 and stealth reachable "without runaway leader". [Oct-8]

**Safety guards** (`config.yml` safety section, described as "prevent any single ability category or trigger template from dominating"):
- category dominance 0.65 gets x0.85 suppression
- template dominance 0.55 gets x0.90
- candidate-pool collapse threshold 2
- rolling window 100
- `minCombinedSuppression` floor 0.75
- Described as "self-reverting".

**Ecology controls** (`config.yml analytics.ecology`):
- fitness sharing (alpha 0.10, maxPenalty 0.15, targetOccupancy 0.18)
- adaptive niche capacity bounded [0.80, 1.25], maxSeasonDelta 0.05
- opportunity-weighted mutation maxBias 0.10
- role-based repulsion beta 0.15
- `ecosystem.parameters`: mutation amplitude bounded [0.70, 1.65], nicheSaturationSensitivity, lineageMomentumInfluence

**Ecosystem self-balancing:**
- ecosystem bias, diversity preservation, self-balancing, environment pressure (the World C ablation set) [open-endedness-review; OpenEndednessTestRunner]
- `ArtifactEcosystemSelfBalancingEngine.evaluate`, fed by harness metrics [Oct-8]

**Progression gates:**
- evolution thresholds at reputation 10/25/45/70
- drift chance capped at 0.40
- awakening gates: memory >= 5, history >= 10, kills >= 6
- convergence recipe gates
- [Oct-8]

**Human governance loop:** analytics produce PROPOSED recommendations. An operator accepts them and exports tuning profiles into runtime config. [phase6-6 docs]

**Synthetic recommendation rules:** fixed ratios such as "Increase {low_f} exposure by 5-8%" when the low family is under 60% of the top. [codex/run_internal_pipeline.py]

**Classification thresholds:** fixed END, TNT, NSER and PNNC bands decide EMERGENT_ECOLOGY vs STAGNANT, and so on. [EcologyDiagnosticEngine]

**Proposed acceptance gates:**
- "per-scenario half-life delta guardrails" [03-15 report]
- turnover, entropy and concentration "pass/fail gates" [open-endedness-review]

**Hardcoded verdict text:** "designer-controlled" classification strings. [OpenEndednessTestRunner]

**Validation-profile output shaping:** the report output is reduced, with placeholder reports. [validation-harness-readiness-report]

**From docs outside this slice, glimpsed in grep only:** the deep-extinction-turnover pass3 doc says "Deterministic end-of-window reconciliation is now the primary anti-gap mechanism" and refers to an "added continuity bias".

### Emergent and bottom-up mechanisms described
- **Experience-Driven Evolution (EDE):** usage and experience feedback into future selection. [open-endedness-review; Oct-8]
- **Lineage inheritance and lineage momentum,** with species formation and snapshots. [Oct-8; config]
- **Seeded genome mutation,** latent trait activation and regulatory eligibility. [Oct-8]
- **Trait interaction layer.** Its removal produced "largest entropy suppression". [open-endedness-review]
- **Niche populations, competition and bifurcation (child niches),** driven by live usage utility. [Oct-8]
- **Branch lifecycle:** births and collapses, with survival_score and maintenance_cost. [validation-harness-readiness-report; 03-15 report]
- **Population and recency effects on generation:** "A seed alone is not the entire selection context". [Oct-8]
- **Utility feedback loops that suppress weakly activated templates over generations,** for example `tactical.feint_window`. [AUDIT.md]
- **Saturation as a density-dependent penalty** that "self-corrects" when more templates share a trigger. [AUDIT.md]
- **Drift influenced by chaos and consistency reputation.** [Oct-8]
- **"Divergence strength emerges from progression feedback loops, not from generator starvation."** [open-endedness-review]
- **Measurement side (detection, not control):** NSER novelty, PNNC persistent novel niches, TNT turnover, END diversity, FALSE_DIVERGENCE detection (relabeling noise, sticky attractors). [EcologyDiagnosticEngine; truth guide]

---

## 6. Open questions for the project owner

1. **Should the open-endedness verdict be computed?** It is currently fixed text in `OpenEndednessTestRunner` and is repeated in `open-endedness-review.md`. Given the goal of "a truly emergent evolution engine", should the verdict, ranking and confidence be derived from the data? (Prompted by Section 0.3.)
2. **Keep, rewire or retire the Python synthetic pipeline?** It feeds the dashboard JSON and three CI "simulation" workflows. Should those be rewired to Java harness output? (Section 0.4; Oct-8 Tooling section.)
3. **Were the world-lab inputs and open-endedness matrix modes meant to work?** If so, should each matrix leg run one world? (Section 0.5.)
4. **Which truth source is authoritative now?** Options: `ecology-truth-snapshot.json` (stale, 03-11), `latest-run.properties` (03-19, foreign path), or regenerated output. Should either be regenerated or untracked? (Section 0.6.)
5. **Signed-seed fix direction.** Allow signed seeds in telemetry, or change seed generation? Oct-8 warns against `abs`/masking. What is the sentinel for non-artifact events if 0 stops being reserved? (telemetry-ingestion-contract.md; Oct-8 item 1.)
6. **Per-player profile or independent per-item artifact identity?** (Oct-8 item 3; affects trade and rebinding.)
7. **Integrity vs continuity on SQL load failure.** Fail-fast, read-only, or degraded mode? (release-readiness, section 6, "biases toward continuity over integrity".)
8. **Are reachability floors and anti-dominance guards intended design or temporary scaffolding?** These are the guards, normalization, test minimums and hand adjacency edits. Should a template that the ecology never selects be allowed to go extinct? (Section 5; AUDIT.md "full reachability" goal; IntraCategoryNormalizationProbeTest.)
   - Inference, unverified: the 03-20 structure_echo move into SENSING_INFORMATION may relate to the sensing reachability shortfall (16 vs 18). The Oct-8 review says the cause was not isolated.
9. **Is the human PROPOSED/ACCEPTED governance loop part of the intended end state, or should accepted tuning eventually close automatically?** (phase6-6 docs.)
10. **What is the official release tag convention?** It should determine whether 0.9.50-beta gets re-tagged as `v0.9.50-beta` so release.yml attaches the JAR. Should 0.9.50-beta stay marked non-prerelease "Latest" with no assets? (gh release view.)
11. **Do you want Nightly Build re-enabled** (GitHub disabled it for inactivity), and the Node 20 actions upgraded? (gh workflow list.)
12. **What do END and TNT officially stand for?** Not expanded in any doc in this slice. Source method names suggest END = `effectiveNicheDiversity` and TNT = `temporalNicheTurnover` (`EcosystemHealthGaugeAnalyzer.java:25,30`). That is an inference from method names.
13. **Should the stale or misleading docs be corrected or archived?** That covers repo CLAUDE.md (0.9.28, the `codex/` docs claim, sandbox proxy instructions), `pr-split-plan.md` (deleted script), and AUDIT.md's enforcer and CI claims.
14. **The 10 unmerged remote branches.** Which, if any, hold work still wanted: naming, convergence refactor, evolution-stability? (Oct-8.)
15. **Should the analytics and simulation packages move out of the plugin JAR** into a separate module? (Oct-8.)
16. **Is the `gatherer-heavy` vs `warden-heavy` scenario name split intended?** (`deep_authenticity_validation.py:12` vs the suite scripts.)
17. **Should the ~299 MiB of committed analytics stay tracked?** (full-repo-audit section 8; Oct-8.)

---

## 7. Glossary (terms in this slice)

- **Artifact:** the per-player evolving item profile. Stored as `player:<uuid>`; the item PDC v3 holds the storage key, owner, seed, mechanic fingerprint and memory pressure.
- **Archetype / tempered / advanced / hybrid:** evolution stages gated by total reputation of 10/25/45/70.
- **Awakening:** an identity transformation gated on memory, history and kills. It derives a new seed.
- **Convergence (formerly "fusion"):** a single-artifact identity transformation after awakening. "Fusion" survives in plugin.yml `debug fuse`, the codex pipeline and the 03-15 report.
- **Drift:** a probabilistic bias and instability change, capped at 0.40.
- **Reputation axes:** precision, brutality, survival, mobility, chaos, consistency (verified as getters in `reputation/ArtifactReputation.java:67-77`), plus kills, bossKills, recentKillChain, survivalStreak.
- **AbilityCategory (10 categories):** SENSING_INFORMATION, TRAVERSAL_MOBILITY, SURVIVAL_ADAPTATION, RITUAL_STRANGE_UTILITY, DEFENSE_WARDING, RESOURCE_FARMING_LOGISTICS, SOCIAL_SUPPORT_COORDINATION, COMBAT_TACTICAL_CONTROL, CRAFTING_ENGINEERING_AUTOMATION, STEALTH_TRICKERY_DISRUPTION. Template ids look like `stealth.trace_fold`.
- **Trigger saturation penalty:** a score multiplier that lowers templates sharing a trigger.
- **adjacentTriggerFamily:** a hand-authored map of related triggers, used for secondary applicability and under-sampled relief.
- **TriggerBudgetPolicy (STRICT, SOFT, ...):** trigger cost and cooldown profiles that affect efficiency in `ecologicalYieldScore`.
- **Niche / MechanicNicheTag / dominantNiche / GENERALIST / niche-1 / child niche / bifurcation:** ecological occupancy classes and their splits.
- **Lineage / species / branch:** inheritance groupings.
- **Branch lifecycle:** birth, collapse (`branch-collapsed`), `survival_score`, `maintenance_cost`.
- **Branch survival half-life:** the metric with `cohorts_measured` and censoring.
- **EDE:** Experience-Driven Evolution. Expanded in `OpenEndednessTestRunner` World B title.
- **Worlds A-D:** the open-endedness ablations: full, no-EDE, no-bias/diversity, no-trait-interactions.
- **Scenarios:** explorer-heavy, ritualist-heavy, gatherer-heavy (warden-heavy in one script), mixed, random-baseline. Used by the validation suite.
- **Validation profile:** `world.validationProfile`, a reduced-output harness mode.
- **Constrained suite / deep validation (32-window) / long-horizon:** validation run tiers.
- **Completion marker:** `.dataset-complete.properties`.
- **Pointer:** `analytics/validation-suite/latest-run.properties`.
- **Dataset contract:** `TelemetryDatasetContract`, which defines the runtime vs harness layout.
- **Archive codec:** the `ecosystem-events.log` format. `telemetry-events.log` is legacy and rejected.
- **Rollup snapshot:** `rollup-snapshot.properties`, used by `RollupStateHydrator` and rehydration.
- **schema_version:** a required telemetry field.
- **Truth snapshot:** `analytics/ecology-truth-snapshot.json`.
- **END:** inference: effective niche diversity. TNT: inference: temporal niche turnover. NSER: Novel Strategy Emergence ratio, from `NovelStrategyEmergenceAnalyzer`; "R" = rate or ratio is an inference. PNNC: Persistent Novel Niche Count, from `PersistentNovelNicheAnalyzer`.
- **Diagnostic states:** COLLAPSED_MONOCULTURE, STAGNANT_ATTRACTOR, FALSE_DIVERGENCE, TURBULENT_THRASH, EMERGENT_ECOLOGY, and others.
- **Impact review:** a yes/no/inconclusive evidence check against truth metrics.
- **Recommendation governance:** PROPOSED, ACCEPTED, REJECTED, SUPERSEDED, APPLIED. `export-accepted` turns accepted ones into tuning profiles.
- **Safety guards:** category and template dominance suppression and the candidate-pool collapse guard (config `safety.guards`).
- **RuntimeSettings:** the in-memory config snapshot.
- **Name pools:** prefixes, suffixes and generic, edited with `/ol addname|removename`.
- **Phase N.N / A4.1b:** work-unit labels for the agent-driven iteration stages.
- **codex/... and claude/... branches:** agent-authored PR branches.
