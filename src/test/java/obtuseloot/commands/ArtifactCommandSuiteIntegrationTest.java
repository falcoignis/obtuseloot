package obtuseloot.commands;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactCommandSuiteIntegrationTest {

    @Test
    void givePipelineCreatesValidatedArtifactIdentity() {
        ArtifactManager manager = new ArtifactManager(new InMemoryStateStore());
        UUID owner = UUID.randomUUID();

        Artifact artifact = manager.recreate(owner);

        assertNotNull(artifact.getNaming(), "give pipeline must initialize naming");
        assertTrue(artifact.getNaming().getNamingSeed() != 0L, "give pipeline must assign naming seed");
        assertNotNull(artifact.getArtifactStorageKey(), "give pipeline must stamp storage key");
        assertTrue(artifact.getIdentityBirthTimestamp() > 0L, "give pipeline must set identity birth timestamp");
        assertTrue(artifact.getPersistenceOriginTimestamp() > 0L, "give pipeline must set persistence origin timestamp");
    }

    @Test
    void convertPipelineCreatesReplacementWithRequestedEquipmentArchetype() {
        ArtifactManager manager = new ArtifactManager(new InMemoryStateStore());
        UUID owner = UUID.randomUUID();
        Artifact previous = manager.recreate(owner);

        Artifact replacement = manager.recreateWithArchetype(owner, EquipmentArchetype.NETHERITE_SWORD, "test-convert");

        assertNotEquals(previous.getArtifactSeed(), replacement.getArtifactSeed(), "convert must replace identity seed");
        assertEquals("netherite_sword", replacement.getItemCategory(), "convert must use held equipment archetype");
        assertEquals(previous.getArtifactStorageKey(), replacement.getArtifactStorageKey(), "convert must preserve storage continuity");
        assertEquals(previous.getPersistenceOriginTimestamp(), replacement.getPersistenceOriginTimestamp(), "convert keeps persistence origin continuity");
        assertTrue(replacement.getIdentityBirthTimestamp() >= previous.getIdentityBirthTimestamp(), "convert must assign fresh identity birth timestamp");
    }

    @Test
    void rerollPipelineKeepsArchetypeButReplacesIdentity() {
        ArtifactManager manager = new ArtifactManager(new InMemoryStateStore());
        UUID owner = UUID.randomUUID();
        Artifact converted = manager.recreateWithArchetype(owner, EquipmentArchetype.DIAMOND_AXE, "test-convert");

        Artifact rerolled = manager.recreateWithArchetype(owner, EquipmentArchetype.fromId(converted.getItemCategory()), "test-reroll");

        assertEquals(converted.getItemCategory(), rerolled.getItemCategory(), "reroll should stay in same family when requested");
        assertNotEquals(converted.getArtifactSeed(), rerolled.getArtifactSeed(), "reroll must replace identity");
        assertEquals(converted.getArtifactStorageKey(), rerolled.getArtifactStorageKey(), "reroll must preserve storage continuity");
    }

    @Test
    void pluginYmlDeclaresExpandedCommandPermissions() throws Exception {
        String pluginYml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/plugin.yml"));
        assertTrue(pluginYml.contains("obtuseloot.command.give"));
        assertTrue(pluginYml.contains("obtuseloot.command.convert"));
        assertTrue(pluginYml.contains("obtuseloot.command.reroll"));
        assertTrue(pluginYml.contains("obtuseloot.command.inspect"));
        assertTrue(pluginYml.contains("obtuseloot.command.forceawaken"));
    }

    @Test
    void forceAwakenPipelineUsesIdentityTransitionSemantics() {
        obtuseloot.awakening.AwakeningEngine awakeningEngine = new obtuseloot.awakening.AwakeningEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArchetypePath("ravager");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.MULTIKILL_CHAIN);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(20);
        rep.setKills(10);

        var transition = awakeningEngine.forceAwakening(null, artifact, rep);
        assertNotNull(transition, "force-awaken should produce a replacement when path is valid");
        assertNotEquals(artifact.getArtifactSeed(), transition.replacement().getArtifactSeed(), "force-awaken must replace identity");
    }

    private static final class InMemoryStateStore implements PlayerStateStore {
        private final Map<UUID, Artifact> artifacts = new HashMap<>();

        @Override
        public void saveArtifact(UUID playerId, Artifact artifact) {
            artifacts.put(playerId, artifact);
        }

        @Override
        public Artifact loadArtifact(UUID playerId) {
            return artifacts.get(playerId);
        }

        @Override
        public void saveReputation(UUID playerId, ArtifactReputation reputation) {
        }

        @Override
        public ArtifactReputation loadReputation(UUID playerId) {
            return new ArtifactReputation();
        }
    }
}
