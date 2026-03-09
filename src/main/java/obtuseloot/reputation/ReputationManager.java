package obtuseloot.reputation;

import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReputationManager {
    private final PlayerStateStore stateStore;
    private final Map<UUID, ArtifactReputation> loadedReputations = new ConcurrentHashMap<>();

    public ReputationManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public ArtifactReputation get(UUID playerId) {
        return loadedReputations.computeIfAbsent(playerId, id -> {
            ArtifactReputation loaded = stateStore.loadReputation(id);
            return loaded != null ? loaded : new ArtifactReputation();
        });
    }

    public void save(UUID playerId) {
        ArtifactReputation rep = loadedReputations.get(playerId);
        if (rep != null) {
            stateStore.saveReputation(playerId, rep);
        }
    }

    public void saveAll() {
        loadedReputations.forEach(stateStore::saveReputation);
    }

    public void unload(UUID playerId) {
        save(playerId);
        loadedReputations.remove(playerId);
    }

    public Map<UUID, ArtifactReputation> getLoadedReputations() {
        return loadedReputations;
    }

    public ArtifactReputation reset(UUID playerId) {
        unload(playerId);
        ArtifactReputation fresh = new ArtifactReputation();
        loadedReputations.put(playerId, fresh);
        return fresh;
    }
}
