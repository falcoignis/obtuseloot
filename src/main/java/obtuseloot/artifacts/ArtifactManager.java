package obtuseloot.artifacts;

import obtuseloot.names.ArtifactNameGenerator;
import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ArtifactManager {
    private final PlayerStateStore stateStore;
    private final ArtifactSeedFactory seedFactory;
    private final Map<UUID, Artifact> loadedArtifacts = new ConcurrentHashMap<>();

    public ArtifactManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
        this.seedFactory = new ArtifactSeedFactory();
    }

    public Artifact getOrCreate(UUID playerId) {
        return loadedArtifacts.computeIfAbsent(playerId, id -> {
            Artifact loaded = stateStore.loadArtifact(id);
            return loaded != null ? loaded : ArtifactGenerator.generateFor(id);
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
        loadedArtifacts.remove(playerId);
    }

    public Map<UUID, Artifact> getLoadedArtifacts() {
        return loadedArtifacts;
    }

    public Artifact recreate(UUID playerId) {
        Artifact fresh = ArtifactGenerator.generateFor(playerId);
        loadedArtifacts.put(playerId, fresh);
        return fresh;
    }

    public Artifact reseed(UUID playerId, long newSeed) {
        Artifact artifact = getOrCreate(playerId);
        artifact.resetMutableState();
        regenerateBaselineIdentity(artifact, newSeed);
        return artifact;
    }

    public void regenerateBaselineIdentity(Artifact artifact) {
        regenerateBaselineIdentity(artifact, artifact.getArtifactSeed());
    }

    public void regenerateBaselineIdentity(Artifact artifact, long seed) {
        seedFactory.regenerateFromSeed(artifact, seed);
        artifact.setGeneratedName(ArtifactNameGenerator.generateFromSeed(seed));
        artifact.setItemCategory(ArtifactGenerator.resolveCategory(seed));
    }

    public long rollSeed() {
        return ThreadLocalRandom.current().nextLong();
    }
}
