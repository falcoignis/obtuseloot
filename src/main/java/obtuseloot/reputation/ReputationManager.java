package obtuseloot.reputation;

import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReputationManager {
    private static ReputationManager INSTANCE;

    private final PlayerStateStore stateStore;
    private final Map<UUID, ArtifactReputation> loadedReputations = new ConcurrentHashMap<>();

    public ReputationManager(PlayerStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public static void initialize(ReputationManager manager) { INSTANCE = manager; }

    public ArtifactReputation getReputation(UUID playerId) {
        return loadedReputations.computeIfAbsent(playerId, id -> {
            ArtifactReputation loaded = stateStore.loadReputation(id);
            return loaded != null ? loaded : new ArtifactReputation();
        });
    }

    public void saveReputation(UUID playerId) {
        ArtifactReputation rep = loadedReputations.get(playerId);
        if (rep != null) stateStore.saveReputation(playerId, rep);
    }

    public void saveAll() { loadedReputations.forEach(stateStore::saveReputation); }

    public void unloadReputation(UUID playerId) {
        saveReputation(playerId);
        loadedReputations.remove(playerId);
    }

    public Map<UUID, ArtifactReputation> getLoadedReputations() { return loadedReputations; }

    public static ArtifactReputation get(UUID playerId) { return INSTANCE.getReputation(playerId); }
    public static void remove(UUID playerId) { INSTANCE.unloadReputation(playerId); }
}
