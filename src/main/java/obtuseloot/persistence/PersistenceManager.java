package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

public class PersistenceManager {
    private final Plugin plugin;
    private final PersistenceConfig config;
    private PersistenceProvider provider;
    private boolean fallbackUsed;

    public PersistenceManager(Plugin plugin, PersistenceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        try {
            provider = createProvider(config.backend());
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("[Persistence] Failed to initialize backend '" + config.backend() + "': " + ex.getMessage());
            if (!config.fallbackToYamlOnFailure() || "yaml".equalsIgnoreCase(config.backend())) {
                throw ex;
            }
            plugin.getLogger().warning("[Persistence] Falling back to YAML because storage.fallbackToYamlOnFailure=true");
            provider = new YamlPersistenceProvider(plugin);
            fallbackUsed = true;
        }
        plugin.getLogger().info("[Persistence] Using backend: " + provider.backendName() + (fallbackUsed ? " (fallback)" : ""));
    }

    private PersistenceProvider createProvider(String backend) {
        return switch (backend.toLowerCase()) {
            case "yaml" -> new YamlPersistenceProvider(plugin);
            case "sqlite" -> new SqlitePersistenceProvider(plugin, config.sqliteFile());
            case "mysql" -> new MySqlPersistenceProvider(plugin, config.mySql());
            default -> throw new IllegalArgumentException("Unknown backend: " + backend);
        };
    }

    public PlayerStateStore stateStore() {
        return provider.playerStateStore();
    }

    public String backendName() {
        return provider.backendName();
    }

    public boolean fallbackUsed() {
        return fallbackUsed;
    }

    public String statusMessage() {
        return provider.statusMessage();
    }

    public PersistenceProvider provider() {
        return provider;
    }

    public void close() {
        if (provider != null) {
            provider.close();
        }
    }
}
