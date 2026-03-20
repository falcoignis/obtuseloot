package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.ArtifactNameResolver;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JdbcArtifactNamingPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void jdbcReloadPreservesPersistedNamingSeedAndNameForSameIdentity() throws Exception {
        Plugin plugin = plugin();
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + tempDir.resolve("artifacts.db"), null, null);
        try (Connection connection = database.openConnection()) {
            new SqlSchemaManager().ensureSchema(connection);
        }

        UUID playerId = UUID.randomUUID();
        JdbcPlayerStateStore store = new JdbcPlayerStateStore(database, plugin, JdbcPlayerStateStore.Dialect.SQLITE);
        Artifact artifact = new Artifact(playerId, "elytra");
        artifact.setArtifactSeed(321L);
        artifact.setSeedPrecisionAffinity(0.91D);
        artifact.setSeedMobilityAffinity(0.98D);
        artifact.setSeedBrutalityAffinity(0.12D);
        artifact.setSeedSurvivalAffinity(0.14D);
        artifact.setSeedChaosAffinity(0.22D);
        artifact.setSeedConsistencyAffinity(0.61D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        artifact.getNaming().setNamingSeed(987654321L);
        ArtifactNameResolver.refresh(artifact, artifact.getNaming());

        String originalName = artifact.getDisplayName();
        long originalNamingSeed = artifact.getNaming().getNamingSeed();

        store.saveArtifact(playerId, artifact);

        Artifact reloaded = store.loadArtifact(playerId);

        assertNotNull(reloaded);
        assertEquals(originalNamingSeed, reloaded.getNaming().getNamingSeed());
        assertEquals(originalName, reloaded.getDisplayName());
    }

    private Plugin plugin() {
        Logger logger = Logger.getLogger("JdbcArtifactNamingPersistenceTest");
        File dataFolder = tempDir.toFile();
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    case "getName" -> "ObtuseLootJdbcTest";
                    case "isEnabled" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ObtuseLootJdbcTestPlugin";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class || type == short.class || type == int.class || type == long.class) {
            return 0;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }
}
