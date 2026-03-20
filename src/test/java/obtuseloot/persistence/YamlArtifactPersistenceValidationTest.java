package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class YamlArtifactPersistenceValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void loadArtifactRejectsInvalidPersistedCategoryBeforeReturningArtifact() throws IOException {
        UUID playerId = UUID.randomUUID();
        writePlayerFile(playerId, "generic");
        YamlPlayerStateStore store = new YamlPlayerStateStore(plugin());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.loadArtifact(playerId));

        assertTrue(exception.getMessage().contains("artifact.item-category"));
    }

    @Test
    void loadArtifactRestoresValidPersistedElytra() throws IOException {
        UUID playerId = UUID.randomUUID();
        writePlayerFile(playerId, "elytra");
        YamlPlayerStateStore store = new YamlPlayerStateStore(plugin());

        Artifact artifact = store.loadArtifact(playerId);

        assertNotNull(artifact);
        assertEquals("elytra", artifact.getItemCategory());
    }

    private void writePlayerFile(UUID playerId, String itemCategory) throws IOException {
        Path playerDataDir = tempDir.resolve("playerdata");
        Files.createDirectories(playerDataDir);
        String yaml = """
                dataVersion: 3
                artifact:
                  owner-id: %s
                  item-category: %s
                  artifact-seed: 99
                """.formatted(playerId, itemCategory);
        Files.writeString(playerDataDir.resolve(playerId + ".yml"), yaml);
    }

    private Plugin plugin() {
        Logger logger = Logger.getLogger("YamlArtifactPersistenceValidationTest");
        File dataFolder = tempDir.toFile();
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    case "getName" -> "ObtuseLootTest";
                    case "isEnabled" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ObtuseLootTestPlugin";
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
