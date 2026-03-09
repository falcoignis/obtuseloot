package obtuseloot.persistence;

import org.bukkit.plugin.Plugin;

public class MySqlPlayerStateStore extends JdbcPlayerStateStore {
    public MySqlPlayerStateStore(DatabaseManager database, Plugin plugin) {
        super(database, plugin, Dialect.MYSQL);
    }
}
