package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** A failed read or write must be reported, never mistaken for "no record" or silently dropped. */
class PlayerStateStoreFailureTest {

    @TempDir
    Path tempDir;

    @Test
    void jdbcLoadFailureThrowsInsteadOfReportingNoRecord() {
        // No schema: every query fails with "no such table".
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + tempDir.resolve("broken.db"), null, null);
        JdbcPlayerStateStore store = new JdbcPlayerStateStore(database, plugin(), JdbcPlayerStateStore.Dialect.SQLITE);
        UUID playerId = UUID.randomUUID();

        PlayerStateLoadException artifactFailure = assertThrows(PlayerStateLoadException.class, () -> store.loadArtifact(playerId));
        assertEquals(playerId, artifactFailure.playerId());
        assertThrows(PlayerStateLoadException.class, () -> store.loadReputation(playerId));
    }

    @Test
    void jdbcSaveFailureThrows() {
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + tempDir.resolve("broken.db"), null, null);
        JdbcPlayerStateStore store = new JdbcPlayerStateStore(database, plugin(), JdbcPlayerStateStore.Dialect.SQLITE);
        UUID playerId = UUID.randomUUID();
        Artifact artifact = new Artifact(playerId, "wooden_sword");
        artifact.setArtifactSeed(1L);

        assertThrows(PlayerStateSaveException.class, () -> store.saveArtifact(playerId, artifact));
        assertThrows(PlayerStateSaveException.class, () -> store.saveReputation(playerId, new ArtifactReputation()));
    }

    @Test
    void yamlUnreadableFileThrowsAndIsNotOverwritten() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path playerData = tempDir.resolve("playerdata");
        Files.createDirectories(playerData);
        Path file = playerData.resolve(playerId + ".yml");
        String corrupt = "artifact:\n  item-category: [unterminated\n";
        Files.writeString(file, corrupt, StandardCharsets.UTF_8);
        YamlPlayerStateStore store = new YamlPlayerStateStore(plugin());

        assertThrows(PlayerStateLoadException.class, () -> store.loadArtifact(playerId));
        assertThrows(PlayerStateLoadException.class, () -> store.loadReputation(playerId));
        Artifact replacement = new Artifact(playerId, "wooden_sword");
        replacement.setArtifactSeed(5L);
        assertThrows(PlayerStateLoadException.class, () -> store.saveArtifact(playerId, replacement));
        store.flushPendingWrites();

        assertEquals(corrupt, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void yamlMissingFileIsNoRecord() {
        YamlPlayerStateStore store = new YamlPlayerStateStore(plugin());

        assertNull(store.loadArtifact(UUID.randomUUID()));
        assertNull(store.loadReputation(UUID.randomUUID()));
    }

    private Plugin plugin() {
        Logger logger = Logger.getLogger("PlayerStateStoreFailureTest");
        File dataFolder = tempDir.toFile();
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    case "getName" -> "ObtuseLootFailureTest";
                    case "isEnabled" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ObtuseLootFailureTestPlugin";
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }
}
