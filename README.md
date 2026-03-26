# ObtuseLoot

Deterministic artifact progression for **Minecraft Java 1.21.11** servers running **Purpur / Paper-compatible APIs**.

> **Beta status:** `0.9.50-beta` is a **beta release**. Core systems are implemented, but balance and tuning are still being refined.

---

## What ObtuseLoot Is

ObtuseLoot tracks each player through a persistent artifact identity instead of disposable loot rolls.

At runtime, player behavior feeds deterministic progression systems:

- **Reputation** (precision, brutality, survival, mobility, chaos, consistency, kill patterns)
- **Evolution and archetype routing**
- **Drift and instability**
- **Awakening and convergence identity transitions**
- **Significance/lore expression** with epithets and personality feel
- **Ecosystem-level balancing and safeguards**

The plugin is built around repeatable identity state, not random one-off item rarity tiers.

---

## Core Systems Overview

### Artifacts
Persistent per-player artifact records include seeded identity, storage key, archetype/evolution paths, awakening/convergence state, drift history, lineage/species data, memory traces, and notable events.

### Reputation
Reputation is continuously updated from combat/movement context and kill-chain signals. These values are the primary input for progression and identity shifts.

### Evolution
`EvolutionEngine` resolves archetype + evolution tier from reputation thresholds and hybrid logic. Runtime thresholds are configurable.

### Drift / Instability
Drift chance is probabilistic and influenced by chaos vs consistency. Drift mutations modify bias maps and can apply temporary instability states.

### Awakening
Awakening performs deterministic identity replacement when memory + progression conditions are met (or via debug force), preserving continuity fields while reshaping expression traits.

### Convergence
Convergence evaluates bounded recipe paths and can shift artifact equipment identity/profile when prerequisites are met.

### Significance
Significance is derived from lineage traces, functional role, current state (steady/awakened/converged/unstable, etc.), distinctness context, and age/continuity.

### Lore / Epithets / Personality Feel
Lore lines combine significance formatting + generated fragments. Naming and text channels are controlled by deterministic seed behavior and per-channel text limits.

### Ecosystem Safeguards / Telemetry / Monitoring
The ecosystem engine includes category/template dominance guards, rolling safety windows, failure-signal detection, telemetry flush/rollups, snapshots, and safety dump tools.

### Dashboard Surfaces
ObtuseLoot generates analytics/dashboard files under `analytics/` and can optionally serve them via an embedded HTTP server (`dashboard.webServerEnabled`).

---

## Requirements

| Component | Requirement |
|---|---|
| Minecraft | Java Edition **1.21.11** target deployment |
| Server | **Purpur** or compatible Paper/Bukkit 1.21 API implementation |
| Java | **Java 21** |
| Build tool | **Maven** (for source builds) |

Notes:
- `plugin.yml` declares `api-version: "1.21"`.
- `pom.xml` uses Java 21 and Purpur API `1.21.1-R0.1-SNAPSHOT` as provided dependency.

---

## Installation

1. Build or obtain `ObtuseLoot-0.9.50-beta.jar`.
2. Place it in your server `plugins/` directory.
3. Start the server once (first-run generates `plugins/ObtuseLoot/config.yml` and plugin data directories).
4. Configure storage backend (`yaml`, `sqlite`, or `mysql`) and review safety/runtime settings.
5. Restart after backend changes. For normal config/name-pool updates, use `/ol reload`.

### First-run behavior
- Default config is written via `saveDefaultConfig()`.
- Runtime settings and evolution parameter profiles are loaded from config.
- Persistence backend initializes at startup; startup aborts if backend fails and fallback is disabled.
- Dashboard artifacts and telemetry files are generated under `analytics/`.

### Item discovery sources
- ObtuseLoot-enabled items can be discovered from naturally generated chest loot, trial vault rewards, and items enchanted through an enchanting table.

---

## Configuration Overview

`src/main/resources/config.yml` is organized as follows:

| Section | Purpose |
|---|---|
| `storage` | Select backend and fallback behavior (`fallbackToYamlOnFailure`) |
| `sqlite` / `mysql` | Backend-specific connection/file settings |
| `persistence` | Autosave cadence (`autosave-interval-seconds`) |
| `reputation` | Combat windows, chaos thresholds, decay, boss types, context cleanup |
| `evolution` | Archetype/tier thresholds and switch inertia |
| `drift` | Drift probability bounds and instability duration |
| `combat` | Precision-hit threshold tuning |
| `naming` | Deterministic name seeding, discovery thresholds, lexeme pools |
| `dashboard` | Optional embedded web server and port |
| `runtime` | Trigger subscription indexing + active artifact cache tuning |
| `text` | Word-count caps per generated text channel |
| `analytics.ecology` | Advanced ecological projection/fitness-sharing/adaptive niche tuning |
| `ecosystem.parameters` | Environment pressure and telemetry rollup/rehydration parameters |
| `safety` | Dominance thresholds, suppression bounds, snapshot cadence, dump cooldown |

---

## Commands

Base command: `/obtuseloot` (alias: `/ol`).

### Operator commands

| Syntax | Description | Permission |
|---|---|---|
| `/ol help` | Command reference | `obtuseloot.help` |
| `/ol info` | Runtime status summary | `obtuseloot.info` |
| `/ol inspect [player]` | Inspect tracked artifact/reputation state | `obtuseloot.inspect` |
| `/ol refresh [player]` | Regenerate a player artifact profile | `obtuseloot.admin` |
| `/ol reset [player]` | Clear tracked artifact + reputation state | `obtuseloot.admin` |
| `/ol reload` | Reload config/runtime settings and name pools | `obtuseloot.admin` |
| `/ol addname <prefixes\|suffixes> <value>` | Add name pool entry | `obtuseloot.edit` or scoped edit node |
| `/ol removename <prefixes\|suffixes> <value>` | Remove name pool entry | `obtuseloot.edit` or scoped edit node |

### Ecosystem / dashboard commands

| Syntax | Description | Permission |
|---|---|---|
| `/ol dashboard` | Dashboard/health summary output | `obtuseloot.info` |
| `/ol ecosystem` | Health summary output | `obtuseloot.info` |
| `/ol ecosystem health` | Health summary output | `obtuseloot.info` |
| `/ol ecosystem dashboard` | Health summary output | `obtuseloot.info` |
| `/ol ecosystem environment` | Active environmental pressure multipliers | `obtuseloot.info` |
| `/ol ecosystem dump` | Write safety snapshot JSON to `analytics/safety/ecosystem-safety-dump.json` | `obtuseloot.info` |
| `/ol ecosystem reset-metrics` | Reset rolling ecosystem safety metrics | `obtuseloot.admin` |
| `/ol ecosystem map` | Console hotspot summary (or player live map default mode) | `obtuseloot.info` |
| `/ol ecosystem map lineage\|species\|collapse` | Start live map mode | `obtuseloot.info` |
| `/ol ecosystem map genome <trait>` | Start genome-trait hotspot rendering | `obtuseloot.info` |
| `/ol ecosystem map off` | Stop live map rendering for player | `obtuseloot.info` |

### Debug suite

All debug subcommands require `obtuseloot.debug`.

| Syntax | Purpose |
|---|---|
| `/ol debug help` | Debug command index |
| `/ol debug inspect [player]` | Deep artifact state dump |
| `/ol debug rep set\|add\|reset ...` | Reputation manipulation |
| `/ol debug evolve\|drift\|awaken\|fuse\|lore [player]` | Force progression stages |
| `/ol debug seed show\|reroll\|set\|export\|import ...` | Deterministic seed controls |
| `/ol debug simulate ...` | Simulation inputs (`hit`, `move`, `lowhp`, `kill`, `multikill`, `bosses`, `chaos`, `cycle`, `path`, `resetcontext`) |
| `/ol debug ability ...` | Ability profile inspection/refresh/explain/tree |
| `/ol debug memory [player]` | Memory snapshot view |
| `/ol debug lineage [player]` | Lineage assignment/trace |
| `/ol debug genome interactions` | Trait interaction exports/heatmap |
| `/ol debug projection cache\|stats` | Trait projection cache and scoring stats |
| `/ol debug subscriptions [stats\|player]` | Trigger subscription index diagnostics |
| `/ol debug artifact [storage\|resolve\|cache\|stats] [player]` | Artifact storage/cache inspection |
| `/ol debug persistence [backend\|test\|migrate ...]` | Persistence status + migrations |
| `/ol debug ecosystem [bias\|balance]` | Ecosystem snapshot internals |
| `/ol debug dashboard` | Regenerate dashboard + heatmap artifacts |

---

## Permissions

| Permission | Default | Operator meaning |
|---|---|---|
| `obtuseloot.help` | `true` | Use `/ol help` |
| `obtuseloot.info` | `true` | View runtime info, ecosystem summaries, dashboard output, map tools |
| `obtuseloot.inspect` | `op` | Inspect tracked player artifact state |
| `obtuseloot.admin` | `op` | Refresh/reset/reload and reset ecosystem rolling metrics |
| `obtuseloot.edit` | `op` | Full name pool editing across all pools |
| `obtuseloot.edit.prefixes` | `op` | Edit prefixes pool only |
| `obtuseloot.edit.suffixes` | `op` | Edit suffixes pool only |
| `obtuseloot.debug` | `op` | Full debug/admin diagnostics surface |

---

## Storage / Persistence

ObtuseLoot supports three backends:

- **YAML** (`storage.backend: yaml`) — default file-based storage.
- **SQLite** (`storage.backend: sqlite`) — embedded DB file (`sqlite.file`).
- **MySQL** (`storage.backend: mysql`) — external database (`mysql.*`).

Operator notes:

- Backend initialization happens during plugin startup.
- If initialization fails:
  - plugin startup stops by default, or
  - falls back to YAML when `storage.fallbackToYamlOnFailure: true`.
- Debug migrations exist for YAML → SQLite/MySQL (`/ol debug persistence migrate yaml-to-sqlite|yaml-to-mysql`).
- Species snapshots are persisted/restored through the same player state store abstraction.

---

## Monitoring / Ecosystem Health

For beta operation, these are the primary monitoring surfaces:

- `/ol ecosystem health` (or `/ol dashboard`) for live summary metrics and collapse risk.
- `/ol ecosystem dump` for JSON safety snapshots with active guard/failure signals.
- `/ol ecosystem map ...` for in-world or console hotspot views.
- Dashboard artifacts under `analytics/dashboard/` (and optional web serving).
- Telemetry artifacts under `analytics/telemetry/`.

Safety guards are designed to be deterministic and self-reverting: suppression is automatically relaxed when rolling distributions normalize.

---

## Beta Guidance

`0.9.50-beta` should be treated as an actively tuned beta:

- Balance values and defaults may change between beta updates.
- Monitor ecosystem metrics regularly instead of relying on one-time checks.
- Keep backups before storage backend changes, migration commands, or major config retuning.
- Validate production settings in a staging environment first when possible.

---

## Building From Source

```bash
mvn -B -ntp clean package
```

Output JAR:

```text
target/ObtuseLoot-0.9.50-beta.jar
```

Optional test run:

```bash
mvn -B -ntp clean test package
```

---

## Known Limits / Notes

Grounded operational notes for the current codebase:

- The embedded dashboard web server is **disabled by default** and should be firewalled if enabled.
- Several diagnostics (map rendering, debug player targets) depend on online players and loaded artifacts.
- `analytics/` telemetry and report files can grow over time; monitor disk usage on busy servers.
- Some command usage text in `plugin.yml` is intentionally broad shorthand; in-code handlers/tabs are the exact behavior source.

---

## License

This project includes an MIT license (`LICENSE`).

## Contribution / Support

No dedicated `CONTRIBUTING.md` or formal support policy is present in this repository at this time.
