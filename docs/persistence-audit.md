# Persistence Audit — YAML / SQLite / MySQL

## Scope
Audit of persistence correctness after introducing multi-backend support.

## Backends covered
- YAML (existing file persistence)
- SQLite (`jdbc:sqlite` embedded)
- MySQL (`jdbc:mysql` external)

## Findings

### 1) Backend parity
- Artifact state, reputation counters, memory events, mutation history, and branch path are persisted by all backends through `PlayerStateStore`.
- SQL schema includes dedicated tables for players/artifacts/memory/mutations/reputation.

### 2) Data completeness checks
Validated fields persisted and loaded:
- Artifact seed, owner UUID, name/category, archetype/evolution/drift/awakening/fusion paths.
- Drift counters/timestamps, instability state, latent lineage.
- Bias maps, awakening gain maps, drift/lore/notable history.
- Last ability branch path, last mutation history, last memory influence.
- Memory event counts (`artifact_memories`).
- Reputation axes + kill/boss/chain/streak/timestamps.

### 3) Migration safety
- Explicit migration commands implemented:
  - `yaml-to-sqlite`
  - `yaml-to-mysql`
- Migration reads YAML records and writes through target backend APIs.
- Migration operations are logged and count processed records.

### 4) Backend switching safety
- Switching backends is config-driven and startup-initialized.
- Startup fallback behavior controlled by `storage.fallbackToYamlOnFailure`.
- If fallback is disabled and backend init fails, plugin startup aborts (no silent fallback).

### 5) Failure scenarios reviewed
- Invalid MySQL credentials / unreachable host: provider init fails, startup fallback/abort policy applies.
- SQLite file/path errors: provider init fails, startup fallback/abort policy applies.
- Schema init failures: caught at provider creation and bubbled with clear error.

### 6) Simulation compatibility
- YAML remains default backend.
- Simulation does not require MySQL unless explicitly configured.
- SQLite can be used in simulation environments when configured.

### 7) Performance notes
- SQL store uses indexed lookup fields and targeted table structure.
- Risks:
  - Write-heavy operations remain synchronous per call; high throughput may benefit from dedicated async queue or batching.
  - MySQL connection pooling flag is config-present but currently non-pooled implementation.

## Validation log
- Maven build passes with SQL dependencies and new persistence classes.
- Schema creation is idempotent (`CREATE TABLE/INDEX IF NOT EXISTS`).

## Recommendation follow-ups
1. Add true MySQL connection pooling (HikariCP) behind config toggle.
2. Add integration tests using temporary SQLite DB and mocked artifact/reputation samples.
3. Add parity regression test matrix comparing YAML vs SQL roundtrip snapshots.
