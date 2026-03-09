package obtuseloot.persistence;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Locale;

public record PersistenceConfig(
        String backend,
        boolean fallbackToYamlOnFailure,
        File sqliteFile,
        MySqlConfig mySql
) {
    public static PersistenceConfig from(FileConfiguration config, File dataFolder) {
        String backend = config.getString("storage.backend", "yaml").toLowerCase(Locale.ROOT);
        boolean fallback = config.getBoolean("storage.fallbackToYamlOnFailure", false);
        String sqlitePath = config.getString("sqlite.file", new File(dataFolder, "data/obtuseloot.db").getPath());
        File sqliteFile = resolvePath(dataFolder, sqlitePath);
        MySqlConfig mysql = new MySqlConfig(
                config.getString("mysql.host", "localhost"),
                config.getInt("mysql.port", 3306),
                config.getString("mysql.database", "obtuseloot"),
                config.getString("mysql.username", "root"),
                config.getString("mysql.password", "change-me"),
                config.getBoolean("mysql.useSsl", false),
                config.getBoolean("mysql.connectionPool.enabled", false),
                config.getInt("mysql.connectionPool.maxPoolSize", 10)
        );
        return new PersistenceConfig(backend, fallback, sqliteFile, mysql);
    }

    private static File resolvePath(File dataFolder, String configuredPath) {
        File path = new File(configuredPath);
        return path.isAbsolute() ? path : new File(dataFolder.getParentFile(), configuredPath);
    }

    public record MySqlConfig(String host, int port, String database, String username,
                              String password, boolean useSsl, boolean poolEnabled, int maxPoolSize) {
    }
}
