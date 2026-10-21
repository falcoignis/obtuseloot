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

    public static boolean initialize(BootstrapContext context) {
        ObtuseLoot plugin = context.require(ObtuseLoot.class);
        FileConfiguration config = context.require(FileConfiguration.class);
        PersistenceManager persistenceManager = new PersistenceManager(plugin, PersistenceConfig.from(config, plugin.getDataFolder()));
        obtuseloot.ecology.LocalInheritancePool inheritancePool;
        try {
            persistenceManager.initialize();
            inheritancePool = obtuseloot.ecology.LocalInheritancePool.decode(persistenceManager.stateStore().loadEcologyState());
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("[Persistence] Startup aborted: " + ex.getMessage());
            persistenceManager.close();
            return false;
        }

        PlayerStateStore playerStateStore = persistenceManager.stateStore();
        ArtifactManager artifactManager = new ArtifactManager(playerStateStore);
        ArtifactItemStorage artifactItemStorage = new ArtifactItemStorage(plugin);
        ReputationManager reputationManager = new ReputationManager(playerStateStore);

        context.register(obtuseloot.ecology.LocalInheritancePool.class, inheritancePool);
        context.register(PersistenceManager.class, persistenceManager);
        context.register(PlayerStateStore.class, playerStateStore);
        context.register(ArtifactManager.class, artifactManager);
        context.register(ArtifactItemStorage.class, artifactItemStorage);
        context.register(ReputationManager.class, reputationManager);
        return true;
    }
}
