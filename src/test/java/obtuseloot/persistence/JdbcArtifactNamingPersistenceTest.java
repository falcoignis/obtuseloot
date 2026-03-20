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
import java.util.Set;
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


    @Test
    void jdbcReloadPreservesAwakeningIdentityMetadataForReplacementIdentity() throws Exception {
        Plugin plugin = plugin();
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + tempDir.resolve("awakening-artifacts.db"), null, null);
        try (Connection connection = database.openConnection()) {
            new SqlSchemaManager().ensureSchema(connection);
        }

        UUID playerId = UUID.randomUUID();
        JdbcPlayerStateStore store = new JdbcPlayerStateStore(database, plugin, JdbcPlayerStateStore.Dialect.SQLITE);
        Artifact artifact = new Artifact(playerId, "elytra");
        artifact.setArtifactSeed(654321L);
        artifact.setSeedPrecisionAffinity(0.41D);
        artifact.setSeedBrutalityAffinity(0.17D);
        artifact.setSeedSurvivalAffinity(0.26D);
        artifact.setSeedMobilityAffinity(0.95D);
        artifact.setSeedChaosAffinity(0.74D);
        artifact.setSeedConsistencyAffinity(0.19D);
        artifact.setAwakeningPath("Tempest Stride");
        artifact.setAwakeningVariantId("tempest-stride::wind-channel::beadfeed");
        artifact.setAwakeningIdentityShape("wind-channel");
        artifact.setAwakeningLineageTrace("lineage:motion-breakpoint:lineage-gale:pressure=22");
        artifact.setAwakeningLoreTrace("lore:velocity-cascade:chain=6");
        artifact.setAwakeningContinuityTrace("owner-storage|memory-imprint|lineage-thread|bounded-history");
        artifact.setAwakeningExpressionTrace("mobility|chaos|skirmish");
        artifact.setAwakeningMemorySignature("pressure=8|agg=10|surv=11|disc=9|chaos=7|events=5");
        artifact.getAwakeningTraits().add("windrunner");
        artifact.getAwakeningBiasAdjustments().put("mobility", 1.5D);
        artifact.getAwakeningGainMultipliers().put("mobility", 2.0D);
        artifact.refreshNamingProjection();
        String originalName = artifact.getDisplayName();
        long originalNamingSeed = artifact.getNaming().getNamingSeed();

        store.saveArtifact(playerId, artifact);

        Artifact reloaded = store.loadArtifact(playerId);

        assertNotNull(reloaded);
        assertEquals("Tempest Stride", reloaded.getAwakeningPath());
        assertEquals("tempest-stride::wind-channel::beadfeed", reloaded.getAwakeningVariantId());
        assertEquals("wind-channel", reloaded.getAwakeningIdentityShape());
        assertEquals("lineage:motion-breakpoint:lineage-gale:pressure=22", reloaded.getAwakeningLineageTrace());
        assertEquals("lore:velocity-cascade:chain=6", reloaded.getAwakeningLoreTrace());
        assertEquals("owner-storage|memory-imprint|lineage-thread|bounded-history", reloaded.getAwakeningContinuityTrace());
        assertEquals("mobility|chaos|skirmish", reloaded.getAwakeningExpressionTrace());
        assertEquals("pressure=8|agg=10|surv=11|disc=9|chaos=7|events=5", reloaded.getAwakeningMemorySignature());
        assertEquals(Set.of("windrunner"), reloaded.getAwakeningTraits());
        assertEquals(1.5D, reloaded.getAwakeningBiasAdjustments().get("mobility"));
        assertEquals(2.0D, reloaded.getAwakeningGainMultipliers().get("mobility"));
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
