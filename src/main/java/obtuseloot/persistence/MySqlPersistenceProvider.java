package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;

public class MySqlPersistenceProvider implements PersistenceProvider {
    private final DatabaseManager db;
    private final MySqlPlayerStateStore store;

    public MySqlPersistenceProvider(Plugin plugin, PersistenceConfig.MySqlConfig config) {
        String jdbcUrl = "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database()
                + "?useSSL=" + config.useSsl() + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.db = new DatabaseManager(jdbcUrl, config.username(), config.password());
        this.store = new MySqlPlayerStateStore(db, plugin);
        try (Connection connection = db.openConnection()) {
            new MySqlSchemaManager().ensureSchema(connection);
        } catch (Exception ex) {
            throw new IllegalStateException("MySQL schema initialization failed", ex);
        }
    }

    @Override
    public String backendName() { return "mysql"; }
    @Override
    public PlayerStateStore playerStateStore() { return store; }
    @Override
    public boolean isHealthy() { return true; }
    @Override
    public String statusMessage() { return "MySQL connected: " + db.jdbcUrl(); }
    @Override
    public void close() { store.flushPendingWrites(); }
}
