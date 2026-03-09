package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

public class YamlPersistenceProvider implements PersistenceProvider {
    private final YamlPlayerStateStore store;

    public YamlPersistenceProvider(Plugin plugin) {
        this.store = new YamlPlayerStateStore(plugin);
    }

    @Override
    public String backendName() {
        return "yaml";
    }

    @Override
    public PlayerStateStore playerStateStore() {
        return store;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String statusMessage() {
        return "YAML persistence active";
    }

    @Override
    public void close() {
        store.flushPendingWrites();
    }
}
