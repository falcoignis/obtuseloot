# ObtuseLoot Newcomer Guide

## What this project is
ObtuseLoot is a Java 21 Minecraft plugin for Purpur/Paper-style servers that generates custom artifacts with procedural names/lore and optional "soul" powers and particle behaviors.

## High-level structure
- `src/main/java/com/gemini/obtuseloot/ObtuseLoot.java`: plugin entrypoint, config/data loading, artifact generation, event hooks, and command handling.
- `src/main/java/com/gemini/obtuseloot/engine/SoulEngine.java`: runtime soul effect engine (particles, ability behaviors, scheduled tasks, and combat/movement hooks).
- `src/main/java/com/gemini/obtuseloot/data/`: immutable records (`SoulData`, `PlayerSoulState`) shared between loader and runtime engine.
- `src/main/java/com/gemini/obtuseloot/lore/` and `.../names/`: curated default text pools used when first creating data files.
- `src/main/resources/plugin.yml`: Bukkit metadata (main class, command registration, permissions).
- `pom.xml`: Maven build config targeting Java 21 and `purpur-api` as a provided dependency.

## Runtime flow in plain English
1. Server enables plugin (`onEnable`).
2. Plugin creates folders/default config/list files if needed and loads all data.
3. Plugin wires listeners and commands.
4. When lootable inventories are opened, plugin may inject generated artifacts.
5. Generated items get rarity/name/lore and optional soul metadata.
6. `SoulEngine` watches player equipment/projectiles/events and drives particles + abilities from soul metadata.

## Important concepts
- **Main-thread model:** Bukkit calls and mutable plugin state are designed around synchronous server-thread access.
- **Hot-reload model:** `/ol reload` rebuilds loaded dictionaries/souls/ability snapshots and refreshes the soul engine.
- **Data-driven content:** names/lore/categories/souls are loaded from YAML and can be edited at runtime with commands.
- **Ability gating:** souls may declare ability IDs; runtime checks both slot compatibility and config toggles.

## Suggested learning path
1. Read `plugin.yml` to understand operator surface area (commands + permissions).
2. Read `ObtuseLoot#onEnable`, `loadAllData`, and `onLootPopulate` for startup + generation flow.
3. Read `generateArtifact` + `applyArtifactMetaImpl` to understand naming/lore/soul stamping.
4. Read `SoulData` and `PlayerSoulState` so engine payload/state shapes are clear.
5. Read `SoulEngine#reload`, cache loop, and key handlers (`onEntityDamageByEntity`, projectile handlers, movement/block hooks).
6. Finally, explore curated dictionaries in `names/` and `lore/` to tune content quality.
