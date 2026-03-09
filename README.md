# ObtuseLoot

`pre-release` `Bukkit/Purpur plugin` `Java 21` `persistent progression`

ObtuseLoot is a Java plugin for Bukkit-compatible servers (targeting Purpur 1.21 API) that tracks per-player artifact progression through combat behavior.

Each player has a persistent artifact profile with hidden seed affinities and evolving paths. Reputation is not just a flat XP bar: combat patterns (damage style, mobility, low-health survival, kill chains, boss kills, and chaos signals) feed multiple dimensions that drive evolution outcomes.

The progression stack is behavior-driven and stateful: evolution resolves from **effective weighted stats** (raw reputation + seed affinity + drift bias + awakening bias), drift applies mutation over time, awakening introduces additional bias/multiplier effects, and fusion unlocks higher-order progression states.

This repository is in active pre-release development. It includes practical debug tooling for rapid QA (inspection, reputation edits, forced evolution/drift/awakening/fusion, persistence controls, and reset paths).

## Key Features

- Behavior-driven, multi-axis reputation (`precision`, `brutality`, `survival`, `mobility`, `chaos`, `consistency` + kill/boss/chain state)
- Hidden seed affinities and latent lineage generated per artifact
- Evolution pathing based on effective weighted stats, with archetype inertia and switch margins
- Drift mutation system with profile-based bias adjustments and timed instability states
- Awakening profiles that apply bias adjustments, gain multipliers, and traits
- Fusion recipes as higher-order progression milestones
- Lore/history tracking for archetype/evolution/drift/awakening/fusion transitions
- Per-player YAML persistence with autosave, join-load, quit-save-unload, and disable-save
- Integrated admin/debug command suite for QA and balancing

## Gameplay Loop

```text
combat behavior
  -> reputation dimensions
    -> archetype + evolution evaluation
      -> drift mutation chance
        -> awakening gate
          -> fusion recipe check
            -> lore/history updates
              -> persisted player state
```

## System Overview

### Reputation
- Reputation is fed by combat events and context, not a single score source.
- Signals include combat hits, movement during combat windows, low-health survival, kill chains, boss kills, and multi-target chaos.
- Volatile stats decay on a scheduler to keep progression responsive to current behavior.

### Evolution
- Evolution starts after threshold gates and resolves archetypes from effective stat values.
- Effective stat formula includes raw rep + seed affinity + drift bias + awakening bias.
- Archetype inertia and switch margin reduce noisy flapping between paths.

### Drift
- Drift chance scales from base chance with chaos and consistency modifiers.
- Drift applies profile-specific bias mutations, increments drift counters, and writes drift/lore history.
- Drift can set temporary instability, later cleaned up by scheduled expiry.

### Awakening
- Awakening resolves profile-based outcomes from archetype + reputation criteria.
- Applied awakening adds stat bias adjustments, reputation gain multipliers, and traits.

### Fusion
- Fusion evaluates recipes once awakening is active and score/boss conditions are met.
- Successful fusion sets a fusion path and can override evolution pathing.

### Lore
- Lore lines are generated from lineage, drift, awakening, instability, reputation snapshot, and recent history.
- State transitions are recorded into lore/notable event history.

### Persistence
- Artifact and reputation state are stored per player UUID in YAML files.
- Save paths include autosave scheduler, explicit debug save, manager unload, and plugin disable.

### Debug
- `/obtuseloot debug ...` provides deterministic control over progression state for QA.
- Current codebase includes forced progression operations, stat editing, path overrides, and persistence/reload controls.

## Architecture Overview

Primary runtime components:

- `ArtifactManager` — in-memory artifact cache + lifecycle/save operations
- `ReputationManager` — in-memory reputation cache + lifecycle/save operations
- `CombatContext` / `CombatContextManager` — transient combat behavior context
- `PlayerStateStore` / `YamlPlayerStateStore` — persistence abstraction + YAML storage backend
- `EvolutionEngine` — archetype/evolution resolution
- `DriftEngine` — drift profile resolution + mutation application
- `AwakeningEngine` — awakening profile resolution/application
- `FusionEngine` — recipe matching and fusion transitions
- `LoreEngine` — action bar summary + lore/history formatting
- `EngineScheduler` — autosave, rep decay, context cleanup, instability cleanup

```text
Bukkit Events
  -> ReputationFeedListener / CombatCore / EventCore
    -> ArtifactProcessor
      -> EvolutionEngine -> DriftEngine -> AwakeningEngine -> FusionEngine
        -> LoreEngine
          -> ArtifactManager/ReputationManager
            -> YamlPlayerStateStore (playerdata/<uuid>.yml)
```

## Installation & Local Development

### Requirements
- Java 21
- Maven
- Bukkit-compatible server (project targets Purpur API `1.21.1-R0.1-SNAPSHOT`)

### Build
```bash
mvn -B -ntp clean package
```

Output jar:
```text
target/ObtuseLoot-0.9.3.jar
```

### Build Troubleshooting
If you see this Maven error:
```text
Fatal error compiling: error: release version 21 not supported
```
Your `JAVA_HOME` points to an older JDK (commonly 17). Switch to JDK 21+ and rebuild:
```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -B -ntp clean package
```

### Install on Server
1. Copy the built jar into your server `plugins/` directory.
2. Start the server once to generate plugin data/config files.

Runtime data locations:
- Config: `plugins/ObtuseLoot/config.yml`
- Per-player state: `plugins/ObtuseLoot/playerdata/<player-uuid>.yml`

## Configuration Overview

Current `config.yml` sections:

- `reputation`
  - Combat windowing, low-health threshold, mobility threshold
  - Kill-chain and chaos thresholds
  - Boss entity list
  - Volatile stat decay interval/factor
  - Combat context cleanup interval
- `combat`
  - Precision-hit damage threshold used in combat reputation logic
- `evolution`
  - Archetype/evolution score thresholds
  - Archetype switch margin + inertia tuning
- `drift`
  - Base/max drift chance and chaos/consistency modifiers
  - Instability duration
- `persistence`
  - Autosave interval
- `naming`
  - Prefix/suffix roll chance and deterministic owner seed toggle

## Commands

Root command: `/obtuseloot` (alias: `/ol`)

### Core Commands

| Command | Description |
|---|---|
| `/obtuseloot help` | Show top-level command help. |
| `/obtuseloot info` | Show plugin runtime status. |
| `/obtuseloot inspect [player]` | Inspect tracked artifact state summary. |
| `/obtuseloot refresh [player]` | Unload and regenerate the player's artifact profile. |
| `/obtuseloot reset [player]` | Clear tracked artifact + reputation state. |
| `/obtuseloot reload` | Reload config/runtime settings and name pools. |
| `/obtuseloot addname <prefixes\|suffixes\|generic> <value>` | Add an entry to a naming pool. |
| `/obtuseloot removename <prefixes\|suffixes\|generic> <value>` | Remove an entry from a naming pool. |

### Debug Commands

| Command | Description |
|---|---|
| `/obtuseloot debug inspect [player]` | Full debug dump of artifact/reputation state. |
| `/obtuseloot debug rep set <stat> <value> [player]` | Set one reputation stat value. |
| `/obtuseloot debug rep add <stat> <value> [player]` | Add (or subtract) from a reputation stat. |
| `/obtuseloot debug rep reset [player]` | Reset reputation object. |
| `/obtuseloot debug evolve [player]` | Force evolution evaluation. |
| `/obtuseloot debug drift [player]` | Force one drift mutation and reevaluate evolution. |
| `/obtuseloot debug awaken [player]` | Force awakening application if dormant. |
| `/obtuseloot debug fuse [player]` | Force fusion recipe evaluation. |
| `/obtuseloot debug lore [player]` | Print actionbar/lore lines to chat for inspection. |
| `/obtuseloot debug reset [player]` | Recreate artifact, reset reputation, clear combat context. |
| `/obtuseloot debug save [player]` | Save artifact + reputation to disk. |
| `/obtuseloot debug reload` | Reload config and runtime name pools. |
| `/obtuseloot debug instability clear [player]` | Clear active instability state. |
| `/obtuseloot debug archetype set <archetype> [player]` | Override archetype path. |
| `/obtuseloot debug path set <evolutionPath> [player]` | Override evolution path. |

> The debug surface includes `/obtuseloot debug`, `/obtuseloot debug simulate`, and `/obtuseloot debug seed` for deterministic QA and progression simulation.

## Debug / QA Workflow

Quick manual QA cycle:

```bash
/ol debug inspect
/ol debug rep set precision 20
/ol debug rep add chaos 8
/ol debug evolve
/ol debug drift
/ol debug awaken
/ol debug fuse
/ol debug lore
/ol debug save
/ol debug reset
/ol debug reload
```

Suggested approach:
1. Use `inspect` before/after each step.
2. Shape rep with `rep set/add` to hit intended thresholds.
3. Force `evolve`, then `drift`, then `awaken`, then `fuse`.
4. Verify lore/history output with `debug lore`.
5. Persist using `debug save`; validate reload behavior with `debug reload`.

## Persistence Model

- Storage backend: `YamlPlayerStateStore`
- File model: one YAML file per player UUID under `playerdata/`
- Loaded lazily on access/join via managers
- Save behavior:
  - scheduled autosave (`EngineScheduler`)
  - explicit debug save command
  - manager `unload(...)` (used on player quit/reset flows)
  - plugin disable (`saveAll` for artifacts + reputations)

## Project Status

ObtuseLoot is pre-release and under active iteration. Systems and thresholds are still being tuned for behavior quality and progression balance. Expect ongoing changes to formulas, thresholds, and command ergonomics during development.

## License

This project includes a repository `LICENSE` file.


### Simulation debug commands

- `/obtuseloot debug simulate hit <damage> [player]`
- `/obtuseloot debug simulate move <distance> [player]`
- `/obtuseloot debug simulate lowhp [player]`
- `/obtuseloot debug simulate kill [player]`
- `/obtuseloot debug simulate multikill <count> [player]`
- `/obtuseloot debug simulate bosses <count> [player]`
- `/obtuseloot debug simulate chaos [player]`
- `/obtuseloot debug simulate cycle [player]`
- `/obtuseloot debug simulate resetcontext [player]`
- `/obtuseloot debug seed show|reroll|set|export|import [args]`


## Internal Testing Repository Layout

- `analytics/` generated reports (`evolution/`, `population/`, `meta/`, `review/`, `world-lab/`, `failure-reports/`).
- `simulation/` harness scaffolds (`gameplay-simulator/`, `chaos-tests/`, `population-simulator/`, `world-simulation-lab/`).
- `releases/` release records (`v0.9.3/`, `nightly/`).
- `scripts/` automation entrypoints for build and analytics.
- `codex/` local orchestration utilities used by scripts.

## Analytics + Simulation Pipeline

Run in order:

```bash
./scripts/build.sh clean package
./scripts/run-chaos-tests.sh
./scripts/run-population-simulation.sh
./scripts/run-meta-analysis.sh
./scripts/run-world-simulation.sh
```

The pipeline writes deterministic report artifacts under `analytics/` and keeps binary plugin JAR output in `target/` only.

## Commentation Audit Notes

- Script wrappers in `scripts/` are expected to include a short header comment explaining purpose and output intent.
- Internal generators (for example `codex/run_internal_pipeline.py`) should keep function docstrings focused on report intent rather than implementation detail.
- When touching existing files, prefer clarifying or tightening comments over adding noisy line-by-line narration.
- Keep operational docs in this README synchronized with script responsibilities whenever tooling comments are updated.
