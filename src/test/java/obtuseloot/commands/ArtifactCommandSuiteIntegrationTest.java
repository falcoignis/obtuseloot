package obtuseloot.commands;

import obtuseloot.abilities.AbilityFamily;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.convergence.ConvergenceEngine;
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
        assertTrue(pluginYml.contains("obtuseloot.command.forceconverge"));
        assertTrue(pluginYml.contains("obtuseloot.command.repairstate"));
        assertTrue(pluginYml.contains("obtuseloot.command.debugprofile"));
        assertTrue(pluginYml.contains("obtuseloot.command.givespecific"));
        assertTrue(pluginYml.contains("obtuseloot.command.dumpheld"));
    }

    @Test
    void giveSpecificFamilyConstraintsResolveToRealEquipmentArchetypes() {
        for (AbilityFamily family : AbilityFamily.values()) {
            var constrained = ObtuseLootCommand.archetypesForFamily(family);
            assertFalse(constrained.isEmpty(), "family " + family + " must have explicit constrained archetypes");
            for (EquipmentArchetype archetype : constrained) {
                assertTrue(EquipmentArchetype.isEquipment(archetype.id()),
                        "constrained archetype must be real equipment: " + archetype.id());
            }
        }
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

    @Test
    void forceConvergePipelineProducesReplacementWithConvergenceMetadata() {
        ConvergenceEngine convergenceEngine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "trident");
        artifact.setArchetypePath("deadeye");
        artifact.setAwakeningPath("awakened-path");
        artifact.setConvergencePath("none");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.LONG_BATTLE);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(60);
        rep.setMobility(24);
        rep.setBossKills(2);
        rep.setKills(24);
        rep.setConsistency(22);

        var transition = convergenceEngine.evaluate(null, artifact, rep);
        assertNotNull(transition, "force-converge should produce a replacement for a valid awakened profile");
        Artifact replacement = transition.replacement();
        assertNotSame(artifact, replacement, "force-converge must replace identity instance");
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed(), "force-converge must replace identity seed");
        assertNotEquals("none", replacement.getConvergencePath(), "replacement must carry convergence path");
        assertNotEquals("none", replacement.getConvergenceVariantId(), "replacement must carry convergence variant");
        assertNotEquals("none", replacement.getConvergenceIdentityShape(), "replacement must carry convergence identity shape");
        assertNotEquals("none", replacement.getConvergenceLineageTrace(), "replacement must carry convergence lineage trace");
        assertNotEquals("none", replacement.getConvergenceExpressionTrace(), "replacement must carry convergence expression trace");
        assertNotEquals("none", replacement.getConvergenceMemorySignature(), "replacement must carry convergence memory signature");
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
