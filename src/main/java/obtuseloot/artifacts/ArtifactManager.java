package obtuseloot.artifacts;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.cache.ArtifactCacheEntry;
import obtuseloot.artifacts.cache.ArtifactCacheManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ArtifactManager {
    private final PlayerStateStore stateStore;
    private final ArtifactSeedFactory seedFactory;
    private final Map<UUID, Artifact> loadedArtifacts = new ConcurrentHashMap<>();
    private final Map<String, UUID> storageToOwner = new ConcurrentHashMap<>();
    private final ArtifactCacheManager cache;

    public ArtifactManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
        this.seedFactory = new ArtifactSeedFactory();
        this.cache = new ArtifactCacheManager(
                stateStore,
                RuntimeSettings.get().activeArtifactCache(),
                RuntimeSettings.get().activeArtifactCacheMaxEntries(),
                RuntimeSettings.get().activeArtifactCacheIdleExpireMs()
        );
    }

    public Artifact getOrCreate(UUID playerId) {
        Artifact artifact = cache.resolve(playerId, () -> {
            Artifact loaded = stateStore.loadArtifact(playerId);
            Artifact resolved = loaded != null ? loaded : ArtifactGenerator.generateFor(playerId);
            if (loaded != null && ObtuseLoot.get() != null) {
                ObtuseLoot.get().getArtifactUsageTracker().hydrateFromArtifact(resolved);
            }
            if (resolved.getNaming() == null) {
                resolved.setNaming(ArtifactNameResolver.initialize(resolved));
            }
            if (resolved.getArtifactStorageKey() == null || resolved.getArtifactStorageKey().isBlank()) {
                resolved.setArtifactStorageKey(Artifact.buildDefaultStorageKey(playerId));
            }
            if (loaded == null && ObtuseLoot.get() != null) {
                ObtuseLoot.get().getArtifactUsageTracker().trackCreated(resolved);
            }
            return resolved;
        }).artifact();
        loadedArtifacts.put(playerId, artifact);
        storageToOwner.put(artifact.getArtifactStorageKey(), playerId);
        return artifact;
    }

    public void save(UUID playerId) {
        Artifact artifact = loadedArtifacts.get(playerId);
        if (artifact != null) {
            if (ObtuseLoot.get() != null) {
                artifact.setLastUtilityHistory(ObtuseLoot.get().getArtifactUsageTracker().utilityHistoryFor(artifact).encode());
            }
            stateStore.saveArtifact(playerId, artifact);
        }
        cache.saveOwner(playerId);
    }

    public void saveAll() {
        if (ObtuseLoot.get() != null) {
            loadedArtifacts.values().forEach(artifact ->
                    artifact.setLastUtilityHistory(ObtuseLoot.get().getArtifactUsageTracker().utilityHistoryFor(artifact).encode()));
        }
        loadedArtifacts.forEach(stateStore::saveArtifact);
        cache.saveAllDirty();
    }

    public void unload(UUID playerId) {
        cache.releaseOwner(playerId);
        Artifact removed = loadedArtifacts.remove(playerId);
        if (removed != null) {
            storageToOwner.remove(removed.getArtifactStorageKey());
        }
    }

    public Map<UUID, Artifact> getLoadedArtifacts() {
        return loadedArtifacts;
    }

    public Artifact recreate(UUID playerId) {
        Artifact existing = loadedArtifacts.get(playerId);
        if (existing != null && ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackDiscard(existing);
        }
        Artifact fresh = ArtifactGenerator.generateFor(playerId);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(fresh);
        }
        replaceLoadedArtifact(playerId, ArtifactIdentityTransition.recreate(existing, fresh), true);
        return fresh;
    }

    public Artifact resolveByStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        UUID owner = storageToOwner.get(storageKey);
        if (owner != null) {
            return getOrCreate(owner);
        }
        Artifact fromCacheOrStore = cache.resolveByStorageKey(storageKey, () -> {
            if (storageKey.startsWith("player:")) {
                try {
                    UUID parsed = UUID.fromString(storageKey.substring("player:".length()));
                    return getOrCreate(parsed);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        });
        if (fromCacheOrStore != null) {
            storageToOwner.put(fromCacheOrStore.getArtifactStorageKey(), fromCacheOrStore.getOwnerId());
            loadedArtifacts.put(fromCacheOrStore.getOwnerId(), fromCacheOrStore);
            return fromCacheOrStore;
        }
        if (storageKey.startsWith("player:")) {
            try {
                UUID parsed = UUID.fromString(storageKey.substring("player:".length()));
                return getOrCreate(parsed);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public Artifact reseed(UUID playerId, long newSeed) {
        Artifact artifact = getOrCreate(playerId);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackDiscard(artifact);
        }
        Artifact replacement = createRegeneratedArtifact(artifact, newSeed);
        replaceLoadedArtifact(playerId, ArtifactIdentityTransition.regenerate(artifact, replacement), true);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(replacement);
        }
        return replacement;
    }

    public Artifact replaceIdentity(UUID playerId, ArtifactIdentityTransition transition) {
        replaceLoadedArtifact(playerId, transition, true);
        return transition.replacement();
    }

    public void markDirty(UUID playerId) {
        cache.markDirtyByOwner(playerId);
    }

    public void markDirty(Artifact artifact) {
        cache.markDirty(artifact);
    }

    public void pinSubscriptions(UUID playerId, boolean pinned) {
        cache.pinSubscription(playerId, pinned);
    }

    public ArtifactCacheManager.ArtifactCacheStats cacheStats() {
        return cache.stats();
    }

    public Map<String, ArtifactCacheEntry> cacheSnapshotByStorageKey() {
        Map<String, ArtifactCacheEntry> copy = new ConcurrentHashMap<>();
        for (ArtifactCacheEntry entry : cache.snapshotEntries()) {
            copy.put(entry.key().storageKey(), entry);
        }
        return copy;
    }

    public void invalidateAll(String reason) {
        cache.invalidateAll();
        loadedArtifacts.clear();
        storageToOwner.clear();
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getLogger().info("[ArtifactCache] Invalidated all entries: " + reason);
        }
    }

    public long rollSeed() {
        return ThreadLocalRandom.current().nextLong();
    }

    private Artifact createRegeneratedArtifact(Artifact previous, long seed) {
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValidArchetype(
                ArtifactGenerator.resolveCategory(seed),
                "artifact baseline identity regeneration");
        Artifact replacement = new Artifact(previous.getOwnerId(), archetype);
        replacement.setPersistenceOriginTimestamp(previous.getPersistenceOriginTimestamp());
        replacement.setIdentityBirthTimestamp(System.currentTimeMillis());
        replacement.setArtifactStorageKey(previous.getArtifactStorageKey());
        replacement.setOwnerId(previous.getOwnerId());
        replacement.resetMutableState();
        seedFactory.regenerateFromSeed(replacement, seed);
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        return replacement;
    }

    private void replaceLoadedArtifact(UUID playerId, Artifact artifact, boolean dirty) {
        replaceLoadedArtifact(playerId, new ArtifactIdentityTransition(null, artifact, "artifact-load"), dirty);
    }

    private void replaceLoadedArtifact(UUID playerId, ArtifactIdentityTransition transition, boolean dirty) {
        Artifact artifact = transition.replacement();
        Artifact previous = loadedArtifacts.put(playerId, artifact);
        if (transition.discarded() != null && previous != null && previous != transition.discarded()) {
            throw new IllegalStateException("Artifact transition discarded instance does not match loaded artifact for " + playerId);
        }
        if (previous != null) {
            storageToOwner.remove(previous.getArtifactStorageKey(), playerId);
        }
        storageToOwner.put(artifact.getArtifactStorageKey(), playerId);
        cache.put(playerId, artifact, dirty, true);
    }
}
