package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

public class SqlitePlayerStateStore extends JdbcPlayerStateStore {
    public SqlitePlayerStateStore(DatabaseManager database, Plugin plugin) {
        super(database, plugin, Dialect.SQLITE);
    }
}
