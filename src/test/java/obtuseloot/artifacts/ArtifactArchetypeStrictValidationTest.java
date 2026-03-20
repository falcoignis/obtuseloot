package obtuseloot.artifacts;

import obtuseloot.abilities.AbilityProfile;
import obtuseloot.abilities.AbilityResolver;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.convergence.ConvergenceEngine;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.persistence.PlayerDataMigrator;
import obtuseloot.persistence.YamlPlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactArchetypeStrictValidationTest {

    @Test
    void artifactRejectsInvalidCatchAllCategoryAssignments() {
        Artifact artifact = new Artifact(UUID.randomUUID());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> artifact.setItemCategory("generic"));

        assertTrue(ex.getMessage().contains("Invalid artifact archetype 'generic'"));
    }

    @Test
    void namingRefusesArtifactsWithoutValidArchetypes() {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(77L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ArtifactNameResolver.initialize(artifact));

        assertTrue(ex.getMessage().contains("artifact naming"));
    }

    @Test
    void invalidArtifactsFailBeforeAbilityEvolutionConvergenceAndMemoryFlows() {
        Artifact artifact = new Artifact(UUID.randomUUID());
        ArtifactReputation reputation = new ArtifactReputation();
        AbilityResolver abilityResolver = (a, rep) -> new AbilityProfile("should-not-resolve", java.util.List.of());
        ItemAbilityManager abilityManager = new ItemAbilityManager(abilityResolver);
        EvolutionEngine evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        ConvergenceEngine convergenceEngine = new ConvergenceEngine();
        ArtifactMemoryEngine memoryEngine = new ArtifactMemoryEngine();

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> abilityManager.profileFor(artifact, reputation)),
                () -> assertThrows(IllegalStateException.class, () -> evolutionEngine.evaluate(null, artifact, reputation)),
                () -> assertThrows(IllegalStateException.class, () -> convergenceEngine.evaluateSimulation(artifact, reputation)),
                () -> assertThrows(IllegalStateException.class, () -> memoryEngine.recordAndProfile(artifact, ArtifactMemoryEvent.FIRST_KILL))
        );
    }

    @Test
    void generatedArtifactsAlwaysResolveToEquipmentArchetypes() {
        for (int i = 0; i < 64; i++) {
            Artifact artifact = ArtifactGenerator.generateFor(UUID.randomUUID());
            assertDoesNotThrow(() -> EquipmentArchetype.fromId(artifact.getItemCategory()));
        }
    }

    @Test
    void yamlPersistenceRejectsGenericArtifactCategory(@TempDir Path tempDir) throws Exception {
        Logger logger = Logger.getLogger("YamlPlayerStateStoreTest");
        Plugin plugin = pluginStub(tempDir.toFile(), logger);

        YamlPlayerStateStore store = new YamlPlayerStateStore(plugin);
        UUID playerId = UUID.randomUUID();
        File playerFile = tempDir.resolve("playerdata").resolve(playerId + ".yml").toFile();
        playerFile.getParentFile().mkdirs();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("dataVersion", PlayerDataMigrator.CURRENT_DATA_VERSION);
        yaml.set("artifact.owner-id", playerId.toString());
        yaml.set("artifact.storage-key", Artifact.buildDefaultStorageKey(playerId));
        yaml.set("artifact.artifact-seed", 123L);
        yaml.set("artifact.item-category", "generic");
        yaml.save(playerFile);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> store.loadArtifact(playerId));
        assertTrue(ex.getMessage().contains("generic"));
    }

    @Test
    void migratorDoesNotRewriteGenericArtifactCategory() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("dataVersion", 2);
        yaml.set("artifact.item-category", "generic");

        PlayerDataMigrator.migrateToCurrent(yaml, Logger.getLogger("migrator"), "player.yml");

        assertEquals("generic", yaml.getString("artifact.item-category"));
    }

    private Plugin pluginStub(File dataFolder, Logger logger) {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
}
