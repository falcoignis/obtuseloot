package obtuseloot.artifacts;

import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactManager {
    private static ArtifactManager INSTANCE;

    private final PlayerStateStore stateStore;
    private final Map<UUID, Artifact> loadedArtifacts = new ConcurrentHashMap<>();

    public ArtifactManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public static void initialize(ArtifactManager manager) { INSTANCE = manager; }

    public Artifact getOrCreateArtifact(UUID playerId) {
        return loadedArtifacts.computeIfAbsent(playerId, id -> {
            Artifact loaded = stateStore.loadArtifact(id);
            return loaded != null ? loaded : ArtifactGenerator.generateFor(id);
        });
    }

    public void saveArtifact(UUID playerId) {
        Artifact artifact = loadedArtifacts.get(playerId);
        if (artifact != null) stateStore.saveArtifact(playerId, artifact);
    }

    public void saveAll() { loadedArtifacts.forEach(stateStore::saveArtifact); }

    public void unloadArtifact(UUID playerId) {
        saveArtifact(playerId);
        loadedArtifacts.remove(playerId);
    }

    public Map<UUID, Artifact> getLoadedArtifacts() { return loadedArtifacts; }

    public static Artifact getOrCreate(UUID playerId) { return INSTANCE.getOrCreateArtifact(playerId); }
    public static void remove(UUID playerId) { INSTANCE.unloadArtifact(playerId); }
}
