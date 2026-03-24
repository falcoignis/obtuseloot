# ObtuseLoot

ObtuseLoot is a progression plugin for **Purpur / Paper-compatible Minecraft Java servers** where each player develops a persistent artifact identity over time. Player behavior (combat style, risk profile, movement, kill chains, and long-term patterns) drives reputation, evolution, drift, lore, and ecosystem outcomes.

> **Beta Status (v0.9.50-beta):** this is a **beta** release. Core systems are playable, but balancing, defaults, and operational tuning may continue to change during beta.

---

## Systems Overview

### Artifacts
- Each player is tracked as a persistent artifact profile (not just a random loot roll).
- Artifact state includes identity, progression paths, drift history, lineage/species metadata, and generated text.

### Reputation
- Reputation is built from observed behavior (precision, brutality, survival, mobility, chaos, consistency, kills/boss chains).
- These signals feed evolution and other progression systems.

### Evolution
- Evolution tiers advance using threshold-based progression and archetype resolution.
- Inertia and switch margins reduce rapid archetype thrashing near boundaries.

### Drift / Instability
- Drift applies probabilistic variation over time.
- Chaos and consistency influence drift chance; instability has a configurable duration window.

### Awakening
- Awakening can trigger identity replacement at milestone conditions.
- This updates artifact identity state while preserving longitudinal progression context.

### Convergence
- Convergence applies identity transitions based on accumulated progression context.
- It is surfaced in debug/admin tooling as part of progression diagnostics.

### Significance
- Artifact significance is computed from identity/progression metadata and surfaced in debug inspection.

### Lore / Epithets / Personality Feel
- Naming and lore generation are seeded and discovery-gated.
- Discovery thresholds and text channel limits control readability and reveal pacing.

### Ecosystem Health, Safety, and Dashboard Tooling
- Ecosystem telemetry tracks aggregate diversity and collapse risk indicators.
- Safety guards suppress dominant categories/templates and self-revert when distributions normalize.
- Operator-facing tools include health summaries, JSON dumps, map rendering modes, and dashboard generation.

---

## Requirements

| Component | Requirement |
|---|---|
| Minecraft Java | **1.21.11 target deployment** (plugin declares API `1.21`) |
| Server software | **Purpur** or **Paper-compatible** server implementing the 1.21 API |
| Java runtime | **Java 21** |
| Build tool (source builds) | **Maven 3.9+** recommended |

---

## Installation

1. Download or build `ObtuseLoot-0.9.50-beta.jar`.
2. Place the JAR in your server `plugins/` directory.
3. Start the server once to generate `plugins/ObtuseLoot/config.yml` and data folders.
4. Review configuration before production use (especially storage and safety settings).
5. Restart server after major storage/backend changes. For normal config edits, `/ol reload` is available.

### Storage setup during install
- Set `storage.backend` to `yaml`, `sqlite`, or `mysql`.
- If using MySQL, set real credentials before live deployment.
- Optional: set `storage.fallbackToYamlOnFailure` if you want automatic YAML fallback on backend init failure.

---

## Configuration Overview

`config.yml` is organized into operator-facing sections:

| Section | What it controls |
|---|---|
| `storage`, `sqlite`, `mysql` | Backend selection and connection settings |
| `persistence` | Autosave interval |
| `reputation` | Combat windows, decay, chain logic, boss types |
| `evolution` | Tier thresholds and archetype switching behavior |
| `drift` | Drift probability bounds and instability duration |
| `combat` | Precision damage threshold |
| `naming` | Deterministic naming, discovery thresholds, lexeme pools |
| `dashboard` | Embedded HTTP dashboard server toggle + port |
| `runtime` | Cache/index performance toggles |
| `text` | Word caps for generated channels (name/lore/awakening/etc.) |
| `analytics.ecology` | Advanced ecological weighting and niche model controls |
| `ecosystem.parameters` | Runtime ecosystem and telemetry-rollup parameters |
| `safety` | Dominance guards, suppression behavior, dump cooldown/logging |

**Dashboard note:** `dashboard.webServerEnabled` defaults to `false`. Keep it firewalled if enabled.

---

## Commands

Primary command: `/obtuseloot` (alias: `/ol`).

| Command | Description | Permission |
|---|---|---|
| `/ol help` | Show command help | `obtuseloot.help` |
| `/ol info` | Show runtime status | `obtuseloot.info` |
| `/ol inspect [player]` | Inspect a tracked player artifact profile | `obtuseloot.inspect` |
| `/ol refresh [player]` | Regenerate a player's artifact profile | `obtuseloot.admin` |
| `/ol reset [player]` | Clear tracked artifact + reputation state | `obtuseloot.admin` |
| `/ol reload` | Reload config/runtime settings and name pools | `obtuseloot.admin` |
| `/ol dashboard` | Show ecosystem dashboard/health summary | `obtuseloot.info` |
| `/ol ecosystem [health\|dashboard]` | Show ecosystem health summary | `obtuseloot.info` |
| `/ol ecosystem environment` | Show active environmental pressure multipliers | `obtuseloot.info` |
| `/ol ecosystem dump` | Write ecosystem safety JSON dump | `obtuseloot.info` |
| `/ol ecosystem reset-metrics` | Reset rolling ecosystem safety metrics | `obtuseloot.admin` |
| `/ol ecosystem map [lineage\|species\|collapse]` | Start live map visualization mode | `obtuseloot.info` |
| `/ol ecosystem map genome <trait>` | Render genome trait hotspot map | `obtuseloot.info` |
| `/ol ecosystem map off` | Disable ecosystem map rendering | `obtuseloot.info` |
| `/ol addname <prefixes\|suffixes> <value>` | Add entry to a name pool | `obtuseloot.edit` or scoped edit node |
| `/ol removename <prefixes\|suffixes> <value>` | Remove entry from a name pool | `obtuseloot.edit` or scoped edit node |
| `/ol debug help` | Show debug command surface | `obtuseloot.debug` |
| `/ol debug <subcommand>` | Debug/admin tooling (rep/evolve/drift/awaken/fuse/lore/seed/simulate/projection/ecosystem/etc.) | `obtuseloot.debug` |

---

## Permissions

| Permission | Default | Purpose |
|---|---|---|
| `obtuseloot.help` | `true` | Access command reference |
| `obtuseloot.info` | `true` | Runtime status, ecosystem health, dashboard summaries, map views |
| `obtuseloot.inspect` | `op` | Inspect tracked player state |
| `obtuseloot.admin` | `op` | Refresh/reset/reload and ecosystem metric reset |
| `obtuseloot.edit` | `op` | Full name pool editing (`addname` / `removename`) |
| `obtuseloot.edit.prefixes` | `op` | Edit prefixes pool only |
| `obtuseloot.edit.suffixes` | `op` | Edit suffixes pool only |
| `obtuseloot.debug` | `op` | Full debug/diagnostic command surface |

---

## Storage Backends

ObtuseLoot supports three persistence backends:

| Backend | Operator expectations |
|---|---|
| `yaml` | Default file-based storage (`plugins/ObtuseLoot/playerdata/`) |
| `sqlite` | Embedded single-file DB (configured path in `sqlite.file`) |
| `mysql` | External MySQL/MariaDB backend for shared or larger deployments |

Additional operational notes:
- Backend is selected by `storage.backend`.
- `storage.fallbackToYamlOnFailure` controls fallback behavior if chosen backend fails at startup.
- Debug migration paths exist for YAML to SQLite/MySQL (`/ol debug persistence migrate ...`).

---

## Beta Notes for Operators

- **This is a beta build (`0.9.50-beta`)**; expect tuning changes across beta versions.
- Use `/ol ecosystem health`, `/ol dashboard`, and `/ol ecosystem dump` to monitor ecosystem state.
- Review safety behavior and dominance trends before changing guard thresholds.
- Keep routine backups, especially before changing storage backend or running migration/debug operations.
- Validate updates in a staging world before production rollout.

---

## Building From Source

```bash
mvn -B -ntp clean package
```

Output artifact:

```text
target/ObtuseLoot-0.9.50-beta.jar
```

To include tests:

```bash
mvn -B -ntp clean test package
```

---

## License

This repository is licensed under the **MIT License**. See `LICENSE`.

## Contribution / Support

There is currently no dedicated `CONTRIBUTING.md` or support policy document in this repository. For beta feedback, use the repository issue tracker/process used by the project maintainers.
