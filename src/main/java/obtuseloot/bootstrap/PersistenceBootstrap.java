package obtuseloot.bootstrap;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.ArtifactItemStorage;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.persistence.PersistenceConfig;
import obtuseloot.persistence.PersistenceManager;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ReputationManager;
import org.bukkit.configuration.file.FileConfiguration;

public final class PersistenceBootstrap {
    private PersistenceBootstrap() {
    }

    public static Result initialize(ObtuseLoot plugin, FileConfiguration config) {
        PersistenceManager persistenceManager = new PersistenceManager(plugin, PersistenceConfig.from(config, plugin.getDataFolder()));
        try {
            persistenceManager.initialize();
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("[Persistence] Startup aborted: " + ex.getMessage());
            return null;
        }

        PlayerStateStore playerStateStore = persistenceManager.stateStore();
        ArtifactManager artifactManager = new ArtifactManager(playerStateStore);
        ArtifactItemStorage artifactItemStorage = new ArtifactItemStorage(plugin);
        ReputationManager reputationManager = new ReputationManager(playerStateStore);

        return new Result(persistenceManager, playerStateStore, artifactManager, artifactItemStorage, reputationManager);
    }

    public record Result(PersistenceManager persistenceManager,
                         PlayerStateStore playerStateStore,
                         ArtifactManager artifactManager,
                         ArtifactItemStorage artifactItemStorage,
                         ReputationManager reputationManager) {
    }
}
