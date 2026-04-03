# ObtuseLoot

**ObtuseLoot** is a pre-release Paper/Purpur plugin for persistent, identity-driven equipment progression.
It is designed for servers that want long-lived artifacts instead of disposable rarity ladders.

> Current branch status: **0.9.50-beta** (pre-release).

---

## What it does

ObtuseLoot tracks artifact identity over time and applies progression through runtime systems, not fixed rarity tiers:

- **Artifact identity and memory** (item behavior reflects usage history).
- **Reputation-driven evolution** (player behavior influences archetype outcomes).
- **Awakening + convergence transitions** (major identity shifts under real pipeline rules).
- **Ecosystem balancing and production safety guards** (distribution pressure monitoring and guardrails).
- **Telemetry + rollups + dashboard generation** (runtime evidence, summaries, and diagnostics).

This repository also includes extensive offline simulation and analytics tooling under `simulation/` and `scripts/`.

---

## Runtime architecture (current codebase)

At plugin startup (`obtuseloot.ObtuseLoot`):

1. **Runtime config and tuning** are loaded.
2. **Telemetry pipeline** is bootstrapped (archive, rollup snapshot store, rehydration).
3. **Persistence backend** is initialized (`yaml`, `sqlite`, or `mysql`).
4. **Engine components** are composed (artifact, reputation, ecosystem, lineage, abilities, lore).
5. **Dashboard service/web server** is initialized.
6. **Command layer** is wired (`/obtuseloot`, `/ol`).
7. Periodic tasks start for environment pressure and telemetry flushing.

Core composition is split into dedicated bootstrap units under `src/main/java/obtuseloot/bootstrap/`:

- `TelemetryBootstrap`
- `PersistenceBootstrap`
- `EngineBootstrap`
- `DashboardBootstrap`
- `CommandBootstrap`
- `PluginPathLayout` (centralized runtime output paths)

---

## Persistence backends

Configured in `config.yml`:

- `storage.backend: yaml` — file-backed player state (default)
- `storage.backend: sqlite` — embedded DB file
- `storage.backend: mysql` — external DB for larger/shared environments

Fallback behavior is controlled by `storage.fallbackToYamlOnFailure`.

---

## Runtime outputs and telemetry paths

Runtime analytics/report output is centralized through `paths.analyticsRoot` in `config.yml`.

- Default: `plugins/ObtuseLoot/analytics`
- All runtime telemetry/dashboard/report files resolve under that root unless explicitly externalized.

Examples:

- `plugins/ObtuseLoot/analytics/telemetry/ecosystem-events.log`
- `plugins/ObtuseLoot/analytics/telemetry/rollup-snapshot.properties`
- `plugins/ObtuseLoot/analytics/dashboard/ecosystem-dashboard.html`
- `plugins/ObtuseLoot/analytics/safety/ecosystem-safety-dump.json`

---

## Commands and permissions

Command surface is intentionally broad (admin, debug, ecosystem, and artifact control flows).

Use the dedicated reference:

- [`commands and permissions.md`](./commands%20and%20permissions.md)

---

## Build and install

### Requirements

- Java **21**
- Maven **3.9+**
- Paper/Purpur **1.21 API line**

### Standard build (tests enabled by default)

```bash
mvn -B -ntp clean package
```

Output jar:

```text
target/ObtuseLoot-0.9.50-beta.jar
```

### Optional fast local build (explicit test skip)

```bash
mvn -B -ntp -Pfast clean package
```

### Server install

1. Build the jar.
2. Copy to your server `plugins/` directory.
3. Start/restart server.
4. Review generated `plugins/ObtuseLoot/config.yml` and tune storage/runtime settings.

---

## Operational notes

- This is a **beta** plugin; expect tuning and report schema drift between versions.
- If dashboard web serving is enabled, treat it as an internal admin endpoint.
- Prefer validating config changes with ecosystem diagnostics (`/obtuseloot ecosystem ...`) before production rollout.

---

## License

See [`LICENSE`](./LICENSE).
