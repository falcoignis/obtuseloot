# Active Artifact Cache Report

## 1) Cache model summary
- Added a backend-agnostic active cache layer (`ArtifactCacheManager`) keyed by stable storage identity.
- `ArtifactManager` now routes runtime artifact resolution through this cache before persistence lookup.
- Cache entries track dirty state, generation, last access, and pinning state (online/subscription).

## 2) What is cached
- Active player artifacts resolved by owner UUID and storage key.
- Artifacts involved in trigger subscription indexing are subscription-pinned.
- Online player artifacts are online-pinned to avoid accidental eviction during active sessions.

## 3) Population rules
- Populated on player join/load (`getOrCreate`).
- Populated on artifact item resolve by storage key.
- Populated on recreation/reseed flows.
- Populated when trigger subscription rebuild occurs.

## 4) Invalidation rules
- Dirty+save on player unload/quit, then release cache pinning.
- Full invalidation on debug reload (`invalidateAll("debug reload")`).
- Mutation flows mark entries dirty (combat/kill cycle, resolver dispatch updates, instability cleanup).

## 5) Dirty-state strategy
- Mutations mark cache entries dirty instead of immediate backend flush.
- Dirty entries are flushed on autosave, explicit save, player quit, and plugin disable.
- Persistence remains source of truth; cache is runtime acceleration only.

## 6) Cache hit/miss expectations
- Expected high hit rate for online players due to owner+subscription pinning.
- Misses should mainly occur on first load, post-reload invalidation, or eviction/idle expiry.

## 7) Expected runtime benefits
- Hot event dispatch (`resolveEffects`) avoids repeated backend artifact loads.
- Item storage resolution by storage key now checks cache first.
- Trigger subscription rebuild and dispatch operate on resident artifact state.

## 8) Remaining hot-path risks
- If cache is disabled via runtime toggle, behavior falls back to direct load path.
- Very high churn with low `activeArtifactCacheMaxEntries` may increase misses.

## 9) Backend compatibility notes
- Cache uses `PlayerStateStore` abstraction only.
- No gameplay-path coupling to YAML/SQLite/MySQL implementations.
- Same runtime behavior across all configured persistence backends.

## Lookup impact estimate (coarse)
- Before: repeated hot-path artifact resolution could re-hit persistence when not strongly resident.
- After: owner/storage-key lookups are in-memory first; backend reads become first-touch/recovery path.
