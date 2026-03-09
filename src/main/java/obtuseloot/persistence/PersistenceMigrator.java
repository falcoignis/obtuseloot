package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.UUID;

public class PersistenceMigrator {
    private final Plugin plugin;

    public PersistenceMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    public int migrateYamlTo(PlayerStateStore targetStore) {
        File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            return 0;
        }
        YamlPlayerStateStore yamlStore = new YamlPlayerStateStore(plugin);
        int migrated = 0;
        File[] files = playerDataDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return 0;
        for (File file : files) {
            try {
                UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));
                Artifact artifact = yamlStore.loadArtifact(playerId);
                ArtifactReputation rep = yamlStore.loadReputation(playerId);
                if (artifact != null) targetStore.saveArtifact(playerId, artifact);
                if (rep != null) targetStore.saveReputation(playerId, rep);
                migrated++;
            } catch (Exception ex) {
                plugin.getLogger().warning("[Persistence] Skipping migration file " + file.getName() + ": " + ex.getMessage());
            }
        }
        targetStore.flushPendingWrites();
        return migrated;
    }
}
