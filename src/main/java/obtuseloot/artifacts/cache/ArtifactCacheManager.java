package obtuseloot.artifacts.cache;

import obtuseloot.artifacts.Artifact;
import obtuseloot.persistence.PlayerStateStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public final class ArtifactCacheManager {
    private final PlayerStateStore store;
    private final boolean enabled;
    private final int maxEntries;
    private final long idleExpiryMs;
    private final Map<ArtifactCacheKey, ArtifactCacheEntry> byKey = new ConcurrentHashMap<>();
    private final Map<UUID, ArtifactCacheKey> ownerToKey = new ConcurrentHashMap<>();
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder missCount = new LongAdder();
    private final LongAdder loadCount = new LongAdder();
    private final LongAdder saveCount = new LongAdder();
    private volatile long lastFlushAtMs;

    public ArtifactCacheManager(PlayerStateStore store, boolean enabled, int maxEntries, long idleExpiryMs) {
        this.store = store;
        this.enabled = enabled;
        this.maxEntries = Math.max(16, maxEntries);
        this.idleExpiryMs = Math.max(0L, idleExpiryMs);
    }

    public ActiveArtifactHandle resolve(UUID ownerId, Supplier<Artifact> loader) {
        ArtifactCacheKey key = new ArtifactCacheKey(Artifact.buildDefaultStorageKey(ownerId));
        if (!enabled) {
            missCount.increment();
            return new ActiveArtifactHandle(key, loader.get(), false);
        }
        ArtifactCacheEntry existing = byKey.get(key);
        long now = System.currentTimeMillis();
        if (existing != null) {
            existing.touch(now);
            existing.setOnlinePinned(true);
            ownerToKey.put(ownerId, key);
            hitCount.increment();
            return new ActiveArtifactHandle(key, existing.artifact(), true);
        }
        missCount.increment();
        Artifact loaded = loader.get();
        loadCount.increment();
        put(ownerId, loaded, false, true);
        return new ActiveArtifactHandle(key, loaded, false);
    }

    public Artifact resolveByStorageKey(String storageKey, Supplier<Artifact> loader) {
        if (storageKey == null || storageKey.isBlank()) return null;
        ArtifactCacheKey key = new ArtifactCacheKey(storageKey);
        if (!enabled) {
            missCount.increment();
            return loader.get();
        }
        ArtifactCacheEntry existing = byKey.get(key);
        if (existing != null) {
            existing.touch(System.currentTimeMillis());
            hitCount.increment();
            return existing.artifact();
        }
        missCount.increment();
        Artifact loaded = loader.get();
        if (loaded != null) {
            loadCount.increment();
            put(loaded.getOwnerId(), loaded, false, true);
        }
        return loaded;
    }

    public void put(UUID ownerId, Artifact artifact, boolean dirty, boolean onlinePinned) {
        if (!enabled || artifact == null || ownerId == null) return;
        long now = System.currentTimeMillis();
        ArtifactCacheKey key = new ArtifactCacheKey(artifact.getArtifactStorageKey());
        ArtifactCacheEntry entry = new ArtifactCacheEntry(key, ownerId, artifact, now, dirty);
        entry.setOnlinePinned(onlinePinned);
        byKey.put(key, entry);
        ownerToKey.put(ownerId, key);
        evictIfNeeded(now);
    }

    public void pinSubscription(UUID ownerId, boolean pinned) {
        ArtifactCacheEntry entry = ownerEntry(ownerId);
        if (entry != null) {
            entry.setSubscriptionPinned(pinned);
            entry.touch(System.currentTimeMillis());
        }
    }

    public void markDirtyByOwner(UUID ownerId) {
        ArtifactCacheEntry entry = ownerEntry(ownerId);
        if (entry != null) entry.markDirty(System.currentTimeMillis());
    }

    public void markDirty(Artifact artifact) {
        if (artifact == null) return;
        ArtifactCacheEntry entry = byKey.get(new ArtifactCacheKey(artifact.getArtifactStorageKey()));
        if (entry != null) entry.markDirty(System.currentTimeMillis());
    }

    public boolean saveOwner(UUID ownerId) {
        return saveEntry(ownerEntry(ownerId));
    }

    public int saveAllDirty() {
        int saved = 0;
        for (ArtifactCacheEntry entry : byKey.values()) {
            if (saveEntry(entry)) saved++;
        }
        if (saved > 0) lastFlushAtMs = System.currentTimeMillis();
        return saved;
    }

    private boolean saveEntry(ArtifactCacheEntry entry) {
        if (entry == null || !entry.dirty()) return false;
        store.saveArtifact(entry.ownerId(), entry.artifact());
        entry.markSaved(System.currentTimeMillis());
        saveCount.increment();
        return true;
    }

    public void releaseOwner(UUID ownerId) {
        ArtifactCacheEntry entry = ownerEntry(ownerId);
        if (entry == null) return;
        saveEntry(entry);
        entry.setOnlinePinned(false);
        entry.setSubscriptionPinned(false);
        if (entry.evictable(System.currentTimeMillis(), idleExpiryMs)) {
            byKey.remove(entry.key());
            ownerToKey.remove(ownerId);
        }
    }

    public void invalidateAll() {
        saveAllDirty();
        byKey.clear();
        ownerToKey.clear();
    }

    public ArtifactCacheEntry ownerEntry(UUID ownerId) {
        ArtifactCacheKey key = ownerToKey.get(ownerId);
        return key == null ? null : byKey.get(key);
    }

    private void evictIfNeeded(long now) {
        if (byKey.size() <= maxEntries) return;
        List<ArtifactCacheEntry> candidates = new ArrayList<>(byKey.values());
        candidates.sort(Comparator.comparingLong(ArtifactCacheEntry::lastAccessMs));
        for (ArtifactCacheEntry candidate : candidates) {
            if (byKey.size() <= maxEntries) break;
            if (!candidate.evictable(now, idleExpiryMs)) continue;
            saveEntry(candidate);
            byKey.remove(candidate.key());
            ownerToKey.remove(candidate.ownerId());
        }
    }

    public ArtifactCacheStats stats() {
        long entries = byKey.size();
        long dirty = byKey.values().stream().filter(ArtifactCacheEntry::dirty).count();
        long hits = hitCount.sum();
        long misses = missCount.sum();
        long total = hits + misses;
        double hitRate = total == 0 ? 0.0D : (double) hits / total;
        long online = byKey.values().stream().filter(ArtifactCacheEntry::onlinePinned).count();
        long subs = byKey.values().stream().filter(ArtifactCacheEntry::subscriptionPinned).count();
        return new ArtifactCacheStats(enabled, entries, dirty, maxEntries, idleExpiryMs, online, subs, hits, misses, hitRate,
                loadCount.sum(), saveCount.sum(), lastFlushAtMs);
    }

    public List<ArtifactCacheEntry> snapshotEntries() {
        return List.copyOf(byKey.values());
    }

    public record ArtifactCacheStats(boolean enabled, long entries, long dirtyEntries, int maxEntries, long idleExpiryMs,
                                     long onlinePinnedEntries, long subscriptionPinnedEntries,
                                     long hits, long misses, double hitRate,
                                     long backendLoads, long backendSaves, long lastFlushAtMs) {}
}
