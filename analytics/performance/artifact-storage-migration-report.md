# Artifact Storage Migration Report

## 1) Old storage model summary
- Legacy artifact items could carry heavy embedded state blobs in item `PersistentDataContainer` fields.
- This made item metadata a de-facto source of truth and increased per-item metadata overhead.

## 2) New storage model summary
- Artifact state remains in plugin-managed persistence (YAML / SQLite / MySQL through configured `PlayerStateStore`).
- Item metadata now stores only a minimal identity marker (`artifact-storage-key`, owner id, and storage version).

## 3) What remains in NBT/PDC
- `artifact-storage-key`
- `artifact-owner-id`
- `artifact-storage-version`

## 4) What moved to plugin storage
- All mutable artifact runtime state (drift, awakening/fusion pathing, history streams, memory profile counters, evolution/archetype transitions, biases, mutation history) remains persisted via backend stores and not item payloads.

## 5) Backend compatibility
- YAML backend: stores `artifact.storage-key` alongside artifact record.
- SQLite backend: unchanged record flow through `JdbcPlayerStateStore` abstraction.
- MySQL backend: unchanged record flow through `JdbcPlayerStateStore` abstraction.

## 6) Cache strategy
- ArtifactManager keeps active artifacts in memory and adds a storage-key-to-owner resolution map for fast item identity lookup.
- Storage key lookup resolves loaded entries first and can recover player-bound keys (`player:<uuid>`) deterministically.

## 7) Expected runtime benefits
- Reduced item metadata pressure from large payloads.
- Faster inventory/event item handling due to minimal identity tags.
- Cleaner persistence responsibilities (backend as source-of-truth).

## 8) Risks / caveats
- Current architecture is player-bound artifact identity (`player:<uuid>`), so one active artifact identity is expected per owner.
- Missing/stale item keys resolve safely via owner fallback paths and debug diagnostics.

## 9) Migration notes
- Added inventory migration path for legacy payload key removal and minimal tag rewrite:
  - join-time migration listener
  - interaction-time lazy migration
  - command: `/obtuseloot debug persistence migrate nbt-artifacts`
- Migration logs each converted item set and preserves artifacts by resolving current persisted owner artifact.
