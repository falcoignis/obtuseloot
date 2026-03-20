# CLAUDE.md — ObtuseLoot Codebase Guide

## Project Overview

ObtuseLoot is a Java 21 Minecraft plugin (Purpur/Bukkit API 1.21) focused on persistent, behavior-driven artifact progression. It combines a live server plugin with an extensive offline analytics and simulation toolchain.

- **Version:** `0.9.26` (`pom.xml` / `plugin.yml`)
- **Build system:** Single-module Maven JAR project
- **Runtime target:** Purpur API `1.21.1-R0.1-SNAPSHOT` (declared `provided`)
- **Plugin main class:** `obtuseloot.ObtuseLoot`
- **Plugin alias:** `/ol`

---

## Repository Layout

```
obtuseloot/
├── pom.xml                          # Maven build (Java 21, no shading)
├── src/
│   ├── main/
│   │   ├── java/obtuseloot/         # All production Java sources (~360 files)
│   │   └── resources/
│   │       ├── plugin.yml           # Bukkit metadata: commands, permissions, api-version
│   │       └── config.yml           # Runtime configuration template
│   └── test/
│       └── java/obtuseloot/         # JUnit 5 tests mirroring main package structure
├── scripts/                         # Shell helpers: build, simulation, validation, analysis
├── analytics/                       # Generated reports, datasets, JSON artifacts (DO NOT edit by hand)
├── simulation/                      # Scenario configs and world-lab readme folders
├── codex/                           # Internal pipeline scripts and analytics review docs
├── docs/                            # Operational documents, audits, guides
├── releases/                        # Release notes and QA docs (v0.9.x)
└── .github/workflows/               # CI pipelines: build, nightly, benchmark, world-lab, release
```

---

## Package Map

All production code lives under `src/main/java/obtuseloot/`.

| Package | Role |
|---|---|
| `obtuseloot` | Plugin bootstrap (`ObtuseLoot extends JavaPlugin`) |
| `obtuseengine` | Listener wiring (`ObtuseEngine`) and scheduler (`EngineScheduler`) |
| `commands` | Command executors and tab completers |
| `artifacts` | `ArtifactManager`, `ArtifactItemStorage` — core artifact state |
| `reputation` | `ReputationManager`, `ArtifactReputation` |
| `combat` | `CombatCore`, `CombatContextManager`, `CombatContext` |
| `events` | Kill/death event listeners |
| `evolution` | `EvolutionEngine`, `ExperienceEvolutionEngine`, `ArtifactFitnessEvaluator`, `ArchetypeResolver`, `params/` |
| `drift` | `DriftEngine` — stat drift system |
| `awakening` | `AwakeningEngine` — identity replacement on awakening |
| `convergence` | `ConvergenceEngine` — procedural convergence/identity transitions |
| `memory` | `ArtifactMemoryEngine` |
| `lore` | `LoreEngine` |
| `abilities` | `ItemAbilityManager`, `AbilityRegistry`, `SeededAbilityResolver`, genome/projection/trigger budget/indexing |
| `ecosystem` | `ArtifactEcosystemSelfBalancingEngine`, `EcosystemMapRenderer`, environment pressure |
| `lineage` | `LineageRegistry`, `LineageInfluenceResolver` |
| `species` | Speciation, population signatures, compatibility |
| `telemetry` | `EcosystemTelemetryEmitter`, aggregation buffer/service, rollups, archive, hydration |
| `dashboard` | `DashboardService`, `DashboardWebServer`, report writers |
| `persistence` | YAML/SQLite/MySQL providers, migration, `PersistenceManager` |
| `config` | `RuntimeSettings` |
| `text` | Artifact text resolver, composer, tone validator, safety filter |
| `names` | Name pool management |
| `analytics` | `AnalyticsCliMain`, ecological analyzers, `EnvironmentalPressureReporter` |
| `simulation/worldlab` | `WorldSimulationRunner`, `OpenEndednessTestRunner`, harness, scenario models |
| `debug` | `ArtifactDebugger` |

---

## Plugin Lifecycle

### `onEnable` sequence (in order)

1. Load `config.yml` → `RuntimeSettings` + `EvolutionParameterRegistry`
2. Initialize telemetry: `EcosystemHistoryArchive` → `TelemetryAggregationBuffer` → `TelemetryAggregationService` → `RollupStateHydrator` (replays archive)
3. Initialize persistence (`PersistenceManager`, `PlayerStateStore`)
4. Construct all engines: `EvolutionEngine`, `DriftEngine`, `AwakeningEngine`, `LoreEngine`, `ArtifactMemoryEngine`, `ItemAbilityManager`, `LineageRegistry`, `ExperienceEvolutionEngine`, `ArtifactEcosystemSelfBalancingEngine`, `DashboardService`
5. Bind `/obtuseloot` executor and tab completer
6. Write startup analytics reports
7. Start scheduled tasks: environment pressure (every 24,000 ticks), telemetry flush (`telemetryFlushIntervalTicks`)
8. Initialize `ObtuseEngine` (registers all event listeners)
9. Start `EngineScheduler` tasks (autosave, decay, combat cleanup, instability cleanup)

### `onDisable` sequence

- Stop scheduler/tasks
- Flush and save all stores
- Shutdown dashboard web server and engine

---

## Registered Event Listeners

All registered by `ObtuseEngine.initialize()`:

| Class | Purpose |
|---|---|
| `ReputationFeedListener` | Combat/movement/kill-style reputation signals |
| `CombatCore` | Combat event forwarding |
| `EventCore` | Kill/death progression updates |
| `PlayerJoinLoadListener` | Load player state on join |
| `PlayerStateCleanupListener` | Save/unload on disconnect |
| `ArtifactItemStorageListener` | Artifact item metadata sync |
| `NonCombatAbilityListener` | Non-combat trigger dispatch (movement/chunk/context) |

---

## Scheduled Tasks

| Task | Frequency | Purpose |
|---|---|---|
| Environment pressure update | Every 24,000 ticks (sync) | Advance seasons, write `analytics/environment-pressure-report.md` |
| Telemetry flush/rollup | Configured `telemetryFlushIntervalTicks` (async) | Flush `TelemetryAggregationBuffer` |
| Autosave | `autosaveIntervalSeconds * 20` ticks | Save artifact + reputation stores |
| Volatile reputation decay | `volatileDecayIntervalSeconds * 20` ticks | Decay volatile stats |
| Combat context cleanup | `contextCleanupSeconds * 20` ticks | Remove stale contexts |
| Instability cleanup | Every 100 ticks | Clear expired instability |
| Ecosystem map render | Every 60 ticks (command-driven only) | Push map visualization updates |

---

## Commands and Permissions

Root command: `/obtuseloot` (alias `/ol`)

| Subcommand | Permission | Default |
|---|---|---|
| `help` | `obtuseloot.help` | everyone |
| `info` | `obtuseloot.info` | everyone |
| `inspect [player]` | `obtuseloot.inspect` | op |
| `refresh [player]` | `obtuseloot.admin` | op |
| `reset [player]` | `obtuseloot.admin` | op |
| `reload` | `obtuseloot.admin` | op |
| `dashboard` | `obtuseloot.info` | everyone |
| `ecosystem [health\|dashboard\|map ...]` | `obtuseloot.info` | everyone |
| `addname <pool> <value>` | `obtuseloot.edit` (or `obtuseloot.edit.<pool>`) | op |
| `removename <pool> <value>` | `obtuseloot.edit` (or `obtuseloot.edit.<pool>`) | op |
| `debug <subcommand>` | `obtuseloot.debug` | op |

**Note:** Scoped permissions `obtuseloot.edit.<pool>` (e.g. `.prefixes`, `.suffixes`) are enforced in code but not declared in `plugin.yml`.

---

## Persistence

Three backends, selected via config:

| Backend | Classes | Notes |
|---|---|---|
| YAML | `YamlPersistenceProvider`, `YamlPlayerStateStore` | Default for small deployments |
| SQLite | `SqlitePersistenceProvider`, `SqliteArtifactStore`, `SqlitePlayerStateStore`, `SqliteReputationStore` | Embedded, no server needed |
| MySQL | `MySqlPersistenceProvider`, `MySqlArtifactStore`, `MySqlPlayerStateStore`, `MySqlReputationStore` | Production multi-server |

Migration is handled by `PersistenceMigrator` and `PlayerDataMigrator`. Schema managed by `SqliteSchemaManager` / `MySqlSchemaManager`.

---

## Telemetry and Analytics Pipeline

```
Runtime events → EcosystemTelemetryEmitter
  → TelemetryAggregationBuffer
  → ScheduledEcosystemRollups
    → analytics/telemetry/ecosystem-events.log   (EcosystemHistoryArchive)
    → rollup-snapshot.properties                  (TelemetryRollupSnapshotStore)
    → Dashboard + analytics reports

On startup: RollupStateHydrator replays archive to restore rollup state.
```

---

## Offline Tooling

### Simulation runner
```bash
mvn -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner \
    -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Open-endedness experiment
```bash
mvn -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.OpenEndednessTestRunner \
    -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Analytics CLI
```bash
mvn -DskipTests -Dexec.mainClass=obtuseloot.analytics.ecosystem.AnalyticsCliMain \
    -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
    -Dexec.args="analyze --dataset <path> --output <path>"
```
CLI commands: `analyze`, `run-spec`, `decide`, `export-accepted`

### Script helpers
```bash
./scripts/build.sh            # Smart build (falls back to mirror if Central unreachable)
./scripts/run-world-simulation.sh
./scripts/run-open-endedness-test.sh
./scripts/run-validation-suite-rerun.sh
./scripts/run-world-lab-validation.sh
./scripts/run-ecosystem-analysis.sh
./scripts/run-deep-validation.sh
```

---

## Build and Test

```bash
# Standard build
mvn clean package

# Build and skip tests
mvn -B -ntp clean package -DskipTests

# Run tests only
mvn test

# Output jar
target/ObtuseLoot-<version>.jar
```

**Requirements:**
- JDK 21+ (enforcer plugin validates this, though currently set `<skip>true`)
- Maven 3.9+

**Test framework:** JUnit 5 (`junit-jupiter:5.11.4`, test scope only)

Test packages mirror main source packages under `src/test/java/obtuseloot/`.

---

## CI Workflows (`.github/workflows/`)

| File | Trigger | Purpose |
|---|---|---|
| `build.yml` | Push to `main` | Standard build |
| `nightly-build.yml` | Daily cron 03:00 | Nightly build |
| `benchmark.yml` | `workflow_dispatch` | Performance benchmarks |
| `world-lab.yml` | `workflow_dispatch` | World simulation |
| `open-endedness.yml` | `workflow_dispatch` | Open-endedness experiments |
| `dashboard.yml` | `workflow_dispatch` | Dashboard regeneration |
| `release.yml` | Tag push | Release build and publish |

---

## Configuration Reference

Primary config: `src/main/resources/config.yml` (deployed to `plugins/ObtuseLoot/config.yml`)

Key config sections:
- `storage`, `sqlite`, `mysql`, `persistence` — backend selection and connection
- `reputation`, `evolution`, `drift`, `combat` — gameplay tuning
- `runtime` — feature toggles (trigger subscription indexing, active artifact cache)
- `dashboard` — `webServerEnabled` toggle (disabled by default)
- `analytics.ecology.*` — ecology analysis parameters
- `ecosystem.parameters.*` — telemetry flush/rollup/rehydration tuning

---

## Key Architectural Notes

### `ObtuseLoot` as wiring hub
`ObtuseLoot.java` constructs and holds all subsystem singletons and exposes `get*()` accessors. This centralizes startup but means many subsystems depend on accessing the plugin instance.

### `ArtifactProcessor` as integration point
`ArtifactProcessor` orchestrates evolution, drift, awakening, fusion, lore, memory, ability, and telemetry updates together. It is the highest-coupling class in the runtime.

### Thread model
Bukkit lifecycle callbacks and mutable plugin state run on the server main thread. The telemetry flush scheduler uses an async repeating task; all other scheduled tasks are sync unless explicitly noted.

### No shading
The plugin JAR contains only ObtuseLoot classes plus `plugin.yml`. SQLite JDBC and MySQL connector are compile-scoped and bundled; Purpur API is `provided`.

### `analytics/` directory
Contains generated reports and datasets from simulation and telemetry runs. Do **not** manually edit files here — they are produced by the analytics/simulation pipeline and may be overwritten.

---

## Development Conventions

- **Java style:** Java 21 features are used and expected (records, sealed classes, switch expressions).
- **Adding a new engine/subsystem:** Construct in `ObtuseLoot.onEnable()`, wire into `ObtuseEngine` or `EngineScheduler` as appropriate, expose a getter from `ObtuseLoot`.
- **Adding a new listener:** Register in `ObtuseEngine.initialize()`.
- **Adding a new command:** Add executor/completer in `commands/`, register in `ObtuseLootCommand` routing, update `plugin.yml` usage text and permissions.
- **Persistence changes:** Add schema migration logic in `PersistenceMigrator`/`PlayerDataMigrator` before changing store implementations.
- **Testing:** Tests do not run against a live server. Use plain JUnit 5 with model/logic classes directly. Avoid Bukkit API calls in tests.
- **Generated output:** Never commit hand-edited files to `analytics/` — those are pipeline artifacts.

---

## Known Inconsistencies

- `plugin.yml` usage text does not enumerate all implemented `debug` subcommands (`subscriptions`, `artifact/storage`, etc.).
- Scoped permissions (`obtuseloot.edit.<pool>`) are code-enforced but not declared in `plugin.yml`.
- `docs/NEWCOMER_GUIDE.md` describes an earlier architecture (`SoulEngine`, `data/` package) that has been superseded by the current design.
- `analytics/` folder contains historical reruns; not all artifacts represent current runtime defaults.
