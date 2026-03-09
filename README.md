# ObtuseLoot

ObtuseLoot is a pre-release Bukkit/Spigot plugin that tracks a persistent, behavior-driven artifact progression system per player.

Instead of treating progression as a short session buff, the plugin models artifact identity over time: combat behavior feeds multi-axis reputation, reputation drives evolution, drift mutates long-term bias, awakening adds trait/multiplier effects, and fusion acts as a higher-order milestone state.

The system is intentionally stateful and testable. Artifact and reputation data are persisted on disk per player, loaded on join/first access, and maintained with scheduler tasks (autosave, decay, cleanup). A dedicated debug suite under `/obtuseloot debug ...` is included for QA and tuning.

> **Status:** Active refactor/testing phase. Architecture is in place and still being iterated.

---

## Core Concept

At a high level, progression loops through:

```text
combat events
  -> reputation updates
  -> archetype/evolution evaluation
  -> drift mutation chance
  -> awakening evaluation
  -> fusion evaluation
  -> lore/history output
  -> persisted state
```

Player behavior shapes artifact identity via multiple dimensions (precision, brutality, survival, mobility, chaos, consistency), plus kill and boss progression.

---

## Major Systems

### Reputation
- `ArtifactReputation` tracks:
  - `precision`, `brutality`, `survival`, `mobility`, `chaos`, `consistency`
  - `kills`, `bossKills`
  - volatile context stats like `recentKillChain`, combat timestamps, survival streak
- Combat/kill listeners feed reputation context.
- Volatile dimensions decay on schedule; persistent dimensions remain progression anchors.

### Evolution
- `EvolutionEngine` evaluates path updates based on reputation score and archetype resolver output.
- `ArchetypeResolver` uses **effective weighted stats**:
  - raw reputation
  - hidden seed affinities
  - drift bias adjustments
  - awakening bias adjustments
- Includes inertia/switch margin behavior to reduce archetype thrashing.

### Drift
- `DriftEngine` uses drift chance tuning (`base + chaos*multiplier - consistency*reduction`, clamped).
- Drift is a **real mutation system**, not message-only:
  - applies drift bias changes
  - increments drift counters
  - updates drift alignment/state
  - can apply instability windows
  - records drift/lore/notable history
- `DriftProfile` and `DriftMutation` represent drift profile behavior and mutation results.

### Awakening
- `AwakeningEngine` evaluates and applies awakening paths.
- Awakening effects are represented by `AwakeningEffectProfile`:
  - bias adjustments
  - reputation gain multipliers
  - trait grants
- Awakening changes future progression weighting and gain behavior.

### Fusion
- `fusion.FusionEngine` evaluates fusion recipes and applies fusion/evolution milestone state when conditions match.
- Fusion outcomes are recorded in artifact history/notable events.

### Lore
- `LoreEngine` builds compact action-bar summaries and full lore lines.
- `LoreFragmentGenerator` + `LoreHistoryFormatter` compose lineage/drift/awakening/instability/history fragments.
- Lore history is transition-oriented and tied to progression state changes.

### Persistence
- `PlayerStateStore` abstraction with `YamlPlayerStateStore` implementation.
- Per-player YAML file model in `plugins/ObtuseLoot/playerdata/<uuid>.yml`.
- Stores both artifact state and reputation state.

### Debug / QA Tooling
- Integrated under `/obtuseloot debug ...` (permission-gated).
- Allows direct inspection, progression forcing, resets, lore dumps, and save/reload workflows against real systems.
- Current branch provides deterministic debug controls; dedicated `simulate` subcommands are not currently registered in this command tree.

---

## Architecture Overview

Primary runtime components:

- **Plugin bootstrap:** `obtuseloot.ObtuseLoot`
- **Managers:**
  - `ArtifactManager`
  - `ReputationManager`
  - `CombatContextManager`
- **Engines:**
  - `ArtifactProcessor`
  - `EvolutionEngine` (`ArchetypeResolver`, `HybridEvolutionResolver`)
  - `DriftEngine` (`DriftProfile`, `DriftMutation`)
  - `AwakeningEngine` (`AwakeningEffectProfile`)
  - `FusionEngine`
  - `LoreEngine`
- **Persistence:** `PlayerStateStore` / `YamlPlayerStateStore`
- **Scheduler:** `EngineScheduler`
- **Event listeners:** join/load, quit/cleanup, combat/death feed listeners

Flow sketch:

```text
Bukkit events
  -> ReputationFeedListener + CombatCore/EventCore
  -> ArtifactProcessor
      -> EvolutionEngine
      -> DriftEngine
      -> AwakeningEngine
      -> FusionEngine
      -> LoreEngine
  -> Managers (in-memory)
  -> PlayerStateStore (YAML persistence)
```

---

## Installation / Development

### Build
This project uses **Maven**.

```bash
mvn clean package
```

Compiled JAR output goes to:

```text
target/obtuseloot-<version>.jar
```

### Deploy
1. Stop server.
2. Copy the JAR into `plugins/`.
3. Start server.

On first run, plugin data is created under:

```text
plugins/ObtuseLoot/
  config.yml
  playerdata/
```

---

## Configuration Overview

Main config sections in `config.yml`:

- `reputation`
  - combat window sizing
  - low-health threshold
  - mobility threshold
  - kill-chain window
  - multi-target chaos threshold
  - boss entity types
  - volatile decay interval/factor
  - context cleanup cadence
- `evolution`
  - archetype/evolution/hybrid thresholds
  - switch margin
  - current archetype inertia
- `drift`
  - base/max drift chance
  - chaos multiplier
  - consistency reduction
  - instability duration
- `persistence`
  - autosave interval
- `combat`
  - precision threshold damage
- `naming`
  - name generation behavior knobs

Runtime settings are loaded into an in-memory snapshot (`RuntimeSettings`) and can be reloaded via command.

---

## Commands

Root command:

- `/obtuseloot` (alias: `/ol`)

### General/Admin
- `/ol help`
- `/ol info`
- `/ol inspect [player]`
- `/ol refresh [player]`
- `/ol reset [player]`
- `/ol reload`
- `/ol addname <prefixes|suffixes|generic> <value>`
- `/ol removename <prefixes|suffixes|generic> <value>`

### Debug Suite
- `/ol debug help`
- `/ol debug inspect [player]`
- `/ol debug rep set <stat> <value> [player]`
- `/ol debug rep add <stat> <value> [player]`
- `/ol debug rep reset [player]`
- `/ol debug evolve [player]`
- `/ol debug drift [player]`
- `/ol debug awaken [player]`
- `/ol debug fuse [player]`
- `/ol debug lore [player]`
- `/ol debug reset [player]`
- `/ol debug save [player]`
- `/ol debug reload`
- `/ol debug instability clear [player]`
- `/ol debug archetype set <archetype> [player]`
- `/ol debug path set <evolutionPath> [player]`

> `simulate` subcommands are **not currently registered** in this branch’s command implementation.

Permissions are defined in `plugin.yml`, including `obtuseloot.debug` for the debug suite.

---

## Debug Workflow (Practical QA)

Typical progression test flow:

```text
/ol debug inspect
/ol debug rep add chaos 20
/ol debug evolve
/ol debug drift
/ol debug awaken
/ol debug fuse
/ol debug lore
/ol debug save
```

For clean retests:

```text
/ol debug reset
```

Useful checks:
- Verify seed affinities, drift bias, awakening bias/multipliers in `inspect`.
- Use `rep set/add` to push controlled scenarios.
- Force evolution/drift/awakening/fusion through real engines.
- Use `lore` to inspect generated lore output without item inspection.
- Use `save` before restart to verify persistence behavior.

---

## Persistence Model

- Artifact + reputation are persisted per player in YAML:
  - `plugins/ObtuseLoot/playerdata/<uuid>.yml`
- Lifecycle behaviors:
  - load on join/first access
  - save on quit/unload paths
  - periodic autosave via scheduler
  - save-all on plugin disable
- State includes progression paths, drift history/lore history/notable events, bias maps, multipliers, traits, and reputation dimensions.

---

## Current Project Status

ObtuseLoot is currently **pre-release** and under active iteration.

The architecture is focused on internal coherence, tuning, and QA workflows rather than backward compatibility guarantees. Expect balancing changes, command expansion, and behavior refinements as testing continues.
