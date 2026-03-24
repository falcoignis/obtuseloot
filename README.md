# ObtuseLoot

**Version:** `0.9.50-beta`
**Status:** Public Beta — configurations and balance may still evolve.

ObtuseLoot is a Minecraft plugin for Purpur/Bukkit that drives persistent, behavior-based artifact progression. Each player develops a single artifact that evolves based on how they play — combat style, kill patterns, survival behavior, and movement all shape the artifact's identity, traits, and generated lore over time.

---

## Beta Notice

This is a **public beta release**. The core systems are complete and stable for testing, but:

- Config defaults and balance values may change between beta builds.
- Operators are encouraged to monitor ecosystem metrics using `/ol ecosystem health` and `/ol ecosystem dump`.
- Report issues and feedback at the project repository.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft server | Purpur or Bukkit-compatible `1.21` / `1.21.1` |
| Java | 21 or later |
| Maven (build only) | 3.9 or later |

---

## Installation

1. Build the plugin JAR:
   ```bash
   mvn clean package
   ```
2. Copy `target/ObtuseLoot-0.9.50-beta.jar` to your server's `plugins/` directory.
3. Start or restart the server. `plugins/ObtuseLoot/config.yml` will be generated on first run.
4. Review and adjust `config.yml` as needed, then run `/ol reload` to apply changes without restarting.

---

## What ObtuseLoot Does

Each player has exactly one **artifact** — a persistent entity that evolves with them.

### Artifacts

An artifact is not an item. It is a tracked state object associated with a player's UUID. It has a generated name, lore, trait scores, and a progression history. As the player accumulates reputation through combat and behavior, the artifact evolves.

### Reputation

Seven behavioral axes are tracked continuously:

| Axis | Driven by |
|---|---|
| Precision | High-damage single strikes |
| Brutality | Low-damage aggressive hits |
| Survival | Fighting at low health |
| Mobility | Movement during combat |
| Chaos | Fighting many targets at once |
| Consistency | Sustained, repeated combat patterns |
| Kill chains | Rapid successive kills, boss kills |

### Evolution and Archetypes

As reputation accumulates, the artifact advances through archetype tiers (initial → tempered → advanced → hybrid). The dominant behavioral axis shapes which archetype emerges. Inertia and switching margins prevent rapid thrashing near tier boundaries.

### Drift

Artifacts undergo probabilistic stat drift over time. Chaos and instability increase drift chance; consistency reduces it. Drift introduces natural variation and prevents static plateaus.

### Awakening

When an artifact reaches a significant milestone, it may **awaken** — undergoing an identity replacement event where its name, lore, and core identity are regenerated to reflect its evolved state.

### Convergence

The convergence system drives procedural identity transitions over time, gradually shifting an artifact's character as its behavioral history accumulates.

### Lore and Epithets

The lore engine generates artifact names and descriptive text from the artifact's history and reputation profile. Name fragments are drawn from seeded pools and unlock at discovery milestones (known → revealed → storied) as the player accumulates kills.

### Lineage and Species

Artifacts track lineage and speciation patterns. The ecosystem engine monitors the population-level distribution of artifact types and applies ecological pressure to maintain diversity.

### Ecosystem Safety Guards

Automatic guards prevent any single ability category or trigger template from dominating the artifact ecosystem. Guards are self-reverting — suppression lifts automatically once distributions normalize. Use `/ol ecosystem health` to observe guard state.

---

## Commands

All commands use `/obtuseloot` (alias `/ol`).

| Command | Permission | Description |
|---|---|---|
| `/ol help` | `obtuseloot.help` | Show command reference |
| `/ol info` | `obtuseloot.info` | Show plugin runtime status |
| `/ol inspect [player]` | `obtuseloot.inspect` | Inspect a player's artifact and reputation state |
| `/ol refresh [player]` | `obtuseloot.admin` | Regenerate a player's artifact profile |
| `/ol reset [player]` | `obtuseloot.admin` | Clear a player's tracked artifact and reputation state |
| `/ol reload` | `obtuseloot.admin` | Reload config and name pools at runtime |
| `/ol dashboard` | `obtuseloot.info` | Show ecosystem health and dashboard summary |
| `/ol ecosystem [health]` | `obtuseloot.info` | Ecosystem health and live safety metrics |
| `/ol ecosystem environment` | `obtuseloot.info` | Show active environmental selection pressure modifiers |
| `/ol ecosystem map [lineage\|species\|collapse]` | `obtuseloot.info` | Live ecosystem map visualization |
| `/ol ecosystem map genome <trait>` | `obtuseloot.info` | Genome trait intensity hotspot map |
| `/ol ecosystem map off` | `obtuseloot.info` | Stop map rendering |
| `/ol ecosystem dump` | `obtuseloot.info` | Write a JSON safety snapshot to `analytics/safety/` |
| `/ol ecosystem reset-metrics` | `obtuseloot.admin` | Clear rolling safety metrics |
| `/ol addname <pool> <value>` | `obtuseloot.edit` or scoped | Add an entry to a name pool (`prefixes` or `suffixes`) |
| `/ol removename <pool> <value>` | `obtuseloot.edit` or scoped | Remove an entry from a name pool |
| `/ol debug <subcommand>` | `obtuseloot.debug` | Full debug and diagnostics surface (see below) |

### Debug Subcommands

All require `obtuseloot.debug` (op by default).

```
/ol debug help
/ol debug inspect|rep|evolve|drift|awaken|fuse|lore|reset|save|reload
/ol debug instability|archetype|path|ability|memory|persistence
/ol debug ecosystem [bias|balance]
/ol debug lineage|genome interactions|projection [cache|stats]
/ol debug subscriptions [player]
/ol debug artifact [storage|resolve] [player]
/ol debug seed show|reroll|set|export|import
/ol debug simulate hit|move|lowhp|kill|multikill|bosses|chaos|cycle|resetcontext|path
```

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `obtuseloot.help` | everyone | View the command reference |
| `obtuseloot.info` | everyone | Runtime status, ecosystem health, map visualizations |
| `obtuseloot.inspect` | op | Inspect player artifact state |
| `obtuseloot.admin` | op | Refresh, reset, reload, clear ecosystem metrics |
| `obtuseloot.edit` | op | Edit all name pools (`addname` / `removename`) |
| `obtuseloot.edit.prefixes` | op | Edit the prefix name pool only |
| `obtuseloot.edit.suffixes` | op | Edit the suffix name pool only |
| `obtuseloot.debug` | op | Full debug and diagnostics surface |

**Scoped editing:** `obtuseloot.edit` grants full pool access. Alternatively, grant `obtuseloot.edit.prefixes` or `obtuseloot.edit.suffixes` independently to restrict per-pool editing.

---

## Storage

Three backends are available, selected in `config.yml` under `storage.backend`.

| Backend | When to use |
|---|---|
| `yaml` | Default. File-based. Good for small servers and testing. |
| `sqlite` | Embedded database file. Better for larger player counts with no external DB. |
| `mysql` | External MySQL/MariaDB. Required for multi-server or proxy setups. |

Change the backend in `config.yml` and restart. Migration utilities are included.

**Important:** If using MySQL, update `mysql.username` and `mysql.password` before going live.

---

## Configuration Overview

The main config is `plugins/ObtuseLoot/config.yml`. All keys are commented. Key sections:

| Section | Purpose |
|---|---|
| `storage` / `sqlite` / `mysql` | Backend selection and connection |
| `persistence` | Autosave interval |
| `reputation` | Behavioral tracking windows and decay |
| `evolution` | Archetype tier thresholds |
| `drift` | Stat drift probability and instability duration |
| `combat` | Precision strike threshold |
| `naming` | Name generation settings and lexeme pools |
| `dashboard` | Embedded HTTP server (disabled by default) |
| `runtime` | Performance feature flags (cache, indexing) |
| `text` | Per-channel word count caps for generated text |
| `analytics.ecology.*` | Advanced ecological modeling parameters |
| `ecosystem.parameters.*` | Telemetry pipeline tuning |
| `safety.*` | Ecosystem diversity guard thresholds and logging |

### Dashboard Web Server

The embedded HTTP dashboard is **disabled by default** (`dashboard.webServerEnabled: false`). If enabled, it serves ecosystem metrics on `dashboard.port` (default `8085`). Do not expose this port publicly without appropriate firewall rules.

---

## Operator Recommendations (Beta)

- Run `/ol ecosystem health` regularly to observe artifact distribution and guard activations.
- Use `/ol ecosystem dump` to capture a full JSON safety snapshot for analysis.
- Use `/ol ecosystem environment` to see active environmental selection pressure.
- The `analytics/` directory accumulates telemetry reports. Review `environment-pressure-report.md` for seasonal progression data.
- If artifacts are clustering into a single archetype, check safety guard logs and consider widening `evolution` thresholds.

---

## Building from Source

```bash
# Build (skip tests)
mvn -B -ntp clean package -DskipTests

# Build and run tests
mvn clean package

# Output
target/ObtuseLoot-0.9.50-beta.jar
```

Offline simulation and analytics tooling are also available:

```bash
# World simulation runner
./scripts/run-world-simulation.sh

# Open-endedness experiment
./scripts/run-open-endedness-test.sh

# Ecosystem analytics CLI
mvn -DskipTests -Dexec.mainClass=obtuseloot.analytics.ecosystem.AnalyticsCliMain \
    -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
    -Dexec.args="analyze --dataset <path> --output <path>"
```

---

## Repository Structure

```
obtuseloot/
├── pom.xml
├── src/
│   ├── main/java/obtuseloot/    # Plugin source (~360 files)
│   └── main/resources/
│       ├── plugin.yml
│       └── config.yml
├── src/test/java/obtuseloot/    # JUnit 5 tests
├── scripts/                     # Build, simulation, and validation helpers
├── analytics/                   # Generated reports and telemetry artifacts (do not edit)
├── simulation/                  # Scenario configs and world-lab readmes
├── docs/                        # Operational documents and audits
├── releases/                    # Release notes per version
└── .github/workflows/           # CI pipelines
```

---

## License

No explicit license file is present in this repository.
