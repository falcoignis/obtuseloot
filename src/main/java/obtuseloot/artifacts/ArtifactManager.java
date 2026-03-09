package obtuseloot.artifacts;

import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactManager {
    private final PlayerStateStore stateStore;
    private final Map<UUID, Artifact> loadedArtifacts = new ConcurrentHashMap<>();

    public ArtifactManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
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
        unload(playerId);
        Artifact fresh = ArtifactGenerator.generateFor(playerId);
        loadedArtifacts.put(playerId, fresh);
        return fresh;
    }
}
