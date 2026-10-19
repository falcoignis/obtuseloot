package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

public class PersistenceManager {
    private final Plugin plugin;
    private final PersistenceConfig config;
    private PersistenceProvider provider;

    public PersistenceManager(Plugin plugin, PersistenceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        // No fallback to another backend: an empty substitute store would regenerate every identity
        // and later overwrite the real data once the configured backend is reachable again.
        try {
            provider = createProvider(config.backend());
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("[Persistence] Failed to initialize backend '" + config.backend() + "': " + ex.getMessage());
            throw ex;
        }
        plugin.getLogger().info("[Persistence] Using backend: " + provider.backendName());
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
