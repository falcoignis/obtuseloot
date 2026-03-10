package obtuseloot.artifacts;

import obtuseloot.ObtuseLoot;
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

    public ArtifactManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
        this.seedFactory = new ArtifactSeedFactory();
    }

    public Artifact getOrCreate(UUID playerId) {
        return loadedArtifacts.computeIfAbsent(playerId, id -> {
            Artifact loaded = stateStore.loadArtifact(id);
            Artifact artifact = loaded != null ? loaded : ArtifactGenerator.generateFor(id);
            if (artifact.getNaming() == null) {
                artifact.setNaming(ArtifactNameResolver.initialize(artifact));
            }
            if (artifact.getArtifactStorageKey() == null || artifact.getArtifactStorageKey().isBlank()) {
                artifact.setArtifactStorageKey(Artifact.buildDefaultStorageKey(id));
            }
            storageToOwner.put(artifact.getArtifactStorageKey(), id);
            if (ObtuseLoot.get() != null) {
                ObtuseLoot.get().getArtifactUsageTracker().trackCreated(artifact);
            }
            return artifact;
        });
    }

    public void save(UUID playerId) {
        Artifact artifact = loadedArtifacts.get(playerId);
        if (artifact != null) {
            stateStore.saveArtifact(playerId, artifact);
        }
    }

    public void saveAll() {
        loadedArtifacts.forEach(stateStore::saveArtifact);
    }

    public void unload(UUID playerId) {
        save(playerId);
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
        loadedArtifacts.put(playerId, fresh);
        storageToOwner.put(fresh.getArtifactStorageKey(), playerId);
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
        artifact.resetMutableState();
        regenerateBaselineIdentity(artifact, newSeed);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(artifact);
        }
        return artifact;
    }

    public void regenerateBaselineIdentity(Artifact artifact) {
        regenerateBaselineIdentity(artifact, artifact.getArtifactSeed());
    }

    public void regenerateBaselineIdentity(Artifact artifact, long seed) {
        seedFactory.regenerateFromSeed(artifact, seed);
        artifact.setItemCategory(ArtifactGenerator.resolveCategory(seed));
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
    }

    public long rollSeed() {
        return ThreadLocalRandom.current().nextLong();
    }
}
