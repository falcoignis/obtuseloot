package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.persistence.DatabaseManager;
import obtuseloot.persistence.JdbcPlayerStateStore;
import obtuseloot.persistence.SqlSchemaManager;
import obtuseloot.persistence.YamlPlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ConvergenceProceduralValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void proceduralExpansionCreatesBoundedVariationWithinRecipeFamily() {
        ConvergenceEngine engine = new ConvergenceEngine();
        Set<String> variantIds = new HashSet<>();
        Set<String> identityShapes = new HashSet<>();
        Set<String> expressionTraces = new HashSet<>();

        for (int i = 0; i < 8; i++) {
            int sample = i;
            Artifact artifact = preparedRangedArtifact(100L + sample, sample);
            ArtifactReputation rep = rangedReputation(sample);

            var transition = engine.evaluateSimulation(artifact, rep);
            assertNotNull(transition, () -> "Expected convergence for sample " + sample);
            Artifact replacement = transition.replacement();

            assertEquals("horizon-syndicate", replacement.getConvergencePath());
            assertTrue(Set.of("elytra", "trident").contains(replacement.getItemCategory()));
            assertTrue(EquipmentArchetype.fromId(replacement.getItemCategory()).hasRole(obtuseloot.artifacts.EquipmentRole.WEAPON)
                            || EquipmentArchetype.fromId(replacement.getItemCategory()).hasRole(obtuseloot.artifacts.EquipmentRole.MOBILITY),
                    "Replacement must stay semantically anchored to the recipe family");
            assertTrue(replacement.getConvergenceVariantId().contains("-") && !"none".equals(replacement.getConvergenceVariantId()));
            assertTrue(replacement.getConvergenceIdentityShape().startsWith("horizon-"));
            assertTrue(replacement.getConvergenceExpressionTrace().startsWith(replacement.getItemCategory() + ":"));
            variantIds.add(replacement.getConvergenceVariantId());
            identityShapes.add(replacement.getConvergenceIdentityShape());
            expressionTraces.add(replacement.getConvergenceExpressionTrace());
        }

        assertTrue(variantIds.size() >= 3, "Expected materially distinct procedural variant ids");
        assertTrue(identityShapes.size() >= 2, "Expected more than cosmetic identity shape variation");
        assertTrue(expressionTraces.size() >= 2, "Expected downstream-useful expression trace diversity");
    }

    @Test
    void convergenceMetadataPersistsAcrossYamlAndJdbc() throws Exception {
        UUID playerId = UUID.randomUUID();
        Artifact artifact = preparedRangedArtifact(900L, 2);
        ArtifactReputation rep = rangedReputation(2);
        Artifact replacement = new ConvergenceEngine().evaluateSimulation(artifact, rep).replacement();

        YamlPlayerStateStore yamlStore = new YamlPlayerStateStore(plugin());
        yamlStore.saveArtifact(playerId, replacement);
        yamlStore.flushPendingWrites();
        Artifact yamlReloaded = new YamlPlayerStateStore(plugin()).loadArtifact(playerId);
        assertConvergenceMetadataPreserved(replacement, yamlReloaded);

        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + tempDir.resolve("artifacts.db"), null, null);
        try (Connection connection = database.openConnection()) {
            new SqlSchemaManager().ensureSchema(connection);
        }
        JdbcPlayerStateStore jdbcStore = new JdbcPlayerStateStore(database, plugin(), JdbcPlayerStateStore.Dialect.SQLITE);
        jdbcStore.saveArtifact(playerId, replacement);
        Artifact jdbcReloaded = jdbcStore.loadArtifact(playerId);
        assertConvergenceMetadataPreserved(replacement, jdbcReloaded);
    }

    private void assertConvergenceMetadataPreserved(Artifact expected, Artifact actual) {
        assertNotNull(actual);
        assertEquals(expected.getConvergencePath(), actual.getConvergencePath());
        assertEquals(expected.getConvergenceVariantId(), actual.getConvergenceVariantId());
        assertEquals(expected.getConvergenceIdentityShape(), actual.getConvergenceIdentityShape());
        assertEquals(expected.getConvergenceLineageTrace(), actual.getConvergenceLineageTrace());
        assertEquals(expected.getConvergenceLoreTrace(), actual.getConvergenceLoreTrace());
        assertEquals(expected.getConvergenceContinuityTrace(), actual.getConvergenceContinuityTrace());
        assertEquals(expected.getConvergenceExpressionTrace(), actual.getConvergenceExpressionTrace());
        assertEquals(expected.getConvergenceMemorySignature(), actual.getConvergenceMemorySignature());
    }

    private Artifact preparedRangedArtifact(long seed, int sample) {
        Artifact artifact = new Artifact(UUID.randomUUID(), EquipmentArchetype.BOW);
        artifact.setArtifactSeed(seed);
        artifact.setAwakeningPath(sample % 2 == 0 ? "Stormblade" : "Skywake");
        artifact.setArchetypePath("deadeye");
        artifact.setEvolutionPath("advanced-deadeye");
        artifact.setLatentLineage("lineage-" + sample);
        artifact.addLoreHistory("earned-through-battle-" + sample);
        artifact.addLoreHistory(sample % 2 == 0 ? "vault-dive" : "storm-pursuit");
        artifact.addNotableEvent("awakening.stormblade." + sample);
        artifact.addNotableEvent(sample % 3 == 0 ? "boss.sky-serpent" : "boss.deep-warden");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        if (sample % 2 == 0) {
            artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
            artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        }
        if (sample % 3 == 0) {
            artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        }
        if (sample % 4 == 0) {
            artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        }
        return artifact;
    }

    private ArtifactReputation rangedReputation(int sample) {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(24 + sample);
        rep.setMobility(16 + (sample % 4) * 3);
        rep.setConsistency(12 + (sample % 3) * 2);
        rep.setChaos(5 + (sample % 2) * 4);
        rep.setKills(20 + sample);
        rep.setRecentKillChain(2 + (sample % 5));
        rep.setBossKills(2 + (sample % 2));
        return rep;
    }

    private Plugin plugin() {
        Logger logger = Logger.getLogger("ConvergenceProceduralValidationTest");
        File dataFolder = tempDir.toFile();
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    case "getName" -> "ObtuseLootConvergenceTest";
                    case "isEnabled" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ObtuseLootConvergenceTestPlugin";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class || type == short.class || type == int.class || type == long.class) return 0;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
