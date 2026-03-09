package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;

public class SqlitePersistenceProvider implements PersistenceProvider {
    private final DatabaseManager db;
    private final SqlitePlayerStateStore store;

    public SqlitePersistenceProvider(Plugin plugin, File dbFile) {
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        this.db = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath(), null, null);
        this.store = new SqlitePlayerStateStore(db, plugin);
        try (Connection connection = db.openConnection()) {
            new SqliteSchemaManager().ensureSchema(connection);
        } catch (Exception ex) {
            throw new IllegalStateException("SQLite schema initialization failed", ex);
        }
    }

    @Override
    public String backendName() { return "sqlite"; }
    @Override
    public PlayerStateStore playerStateStore() { return store; }
    @Override
    public boolean isHealthy() { return true; }
    @Override
    public String statusMessage() { return "SQLite connected: " + db.jdbcUrl(); }
    @Override
    public void close() { store.flushPendingWrites(); }
}
