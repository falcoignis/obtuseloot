# ObtuseLoot v0.9.50-beta — Release Notes

**Release type:** Public Beta
**Date:** 2026-03-24
**Target:** Purpur / Bukkit API `1.21` / `1.21.1` · Java 21+

---

## What This Release Is

This is the first public beta release of ObtuseLoot. Core systems are complete and have been validated through extensive simulation and world-lab testing. This build is intended for beta operator testing on live servers.

This is **not** a feature-complete final release. Balance settings, config structure, and telemetry defaults may still evolve across beta iterations based on operator feedback.

---

## Core Systems (Complete)

- **Artifacts** — persistent per-player state with name, lore, trait scores, and progression history
- **Reputation engine** — seven behavioral axes tracked continuously from combat and movement events
- **Evolution and archetypes** — tier advancement (initial → tempered → advanced → hybrid) driven by dominant reputation axes
- **Drift** — probabilistic stat variation driven by chaos/consistency balance
- **Awakening** — identity replacement event on significant milestones
- **Convergence** — procedural identity transitions over time
- **Lore and name generation** — seeded, milestone-gated name and epithet generation
- **Abilities** — trigger subscription system with genome/projection matrices and non-combat dispatch
- **Lineage and species** — population-level tracking and ecological pressure
- **Ecosystem safety guards** — automatic diversity maintenance with self-reverting suppression
- **Telemetry pipeline** — archive, rollup, rehydration, and dashboard reporting
- **Persistence** — YAML, SQLite, and MySQL backends with migration support

---

## Packaging Changes in This Release

- Version bumped to `0.9.50-beta` across `pom.xml` and `plugin.yml`
- `plugin.yml` — debug command usage now includes all implemented subcommands (`subscriptions`, `artifact`)
- `plugin.yml` — scoped edit permissions `obtuseloot.edit.prefixes` and `obtuseloot.edit.suffixes` are now declared (previously code-checked but undocumented)
- `plugin.yml` — permission descriptions updated for clarity
- `config.yml` — all sections now have comments explaining purpose, tuning guidance, and safety notes
- `README.md` — full rewrite as beta-ready operator and tester documentation

---

## Known Beta Considerations

- Balance values for evolution thresholds, drift probability, and ecology parameters are validated by simulation but not yet tuned against live server populations.
- The dashboard web server is disabled by default. Enabling it on a live server requires appropriate firewall configuration.
- The `analytics/` directory accumulates telemetry reports on the server filesystem. Monitor disk usage on high-activity servers.
- Scoped permissions (`obtuseloot.edit.prefixes`, `obtuseloot.edit.suffixes`) are now declared in `plugin.yml` but permission plugins may need a server restart to pick them up.

---

## Operator Checklist

- [ ] Review `config.yml` — at minimum update MySQL credentials if using that backend
- [ ] Confirm `dashboard.webServerEnabled: false` unless intentionally exposing the HTTP dashboard
- [ ] Run `/ol ecosystem health` after the first hour of play to observe artifact distribution
- [ ] Check `/ol ecosystem dump` output periodically and review safety guard activation counts
- [ ] Report anomalies (unexpected archetype concentration, guard thrashing, lore generation issues) via the issue tracker

---

## Upgrade Notes

No migration steps are required when upgrading from `0.9.x` pre-beta builds. Existing YAML/SQLite/MySQL data is compatible.

---

## Build Artifact

```
ObtuseLoot-0.9.50-beta.jar
```

SHA-256 checksum will be published alongside the release artifact.
