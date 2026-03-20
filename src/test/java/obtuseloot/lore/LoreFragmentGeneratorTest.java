package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoreFragmentGeneratorTest {
    private final LoreFragmentGenerator generator = new LoreFragmentGenerator();
    private final LoreEngine loreEngine = new LoreEngine();

    @Test
    void loreFragmentRetellsRealTransitionsEventsAndIdentitySignals() {
        Artifact artifact = seeded(77L, EquipmentArchetype.DIAMOND_SWORD);
        artifact.setAwakeningPath("storm vow");
        artifact.setAwakeningIdentityShape("tempest duelist");
        artifact.setAwakeningExpressionTrace("lightning cadence");
        artifact.setAwakeningContinuityTrace("oathbound continuity");
        artifact.setConvergencePath("horizon syndicate");
        artifact.setConvergenceIdentityShape("raider saint");
        artifact.setConvergenceExpressionTrace("split mercy");
        artifact.setConvergenceContinuityTrace("borrowed reign");
        artifact.addNotableEvent("awakening.stormblade");
        artifact.addNotableEvent("memory.boss kill");

        String lore = generator.loreFragment(artifact).toLowerCase();

        assertTrue(lore.contains("awakening and convergence") || lore.contains("awakening pulled") || lore.contains("convergence pulled"));
        assertTrue(lore.contains("storm vow") || lore.contains("horizon syndicate") || lore.contains("oathbound continuity") || lore.contains("borrowed reign"));
        assertTrue(lore.contains("stormblade") || lore.contains("boss kill"));
        assertTrue(lore.contains("tempest duelist") || lore.contains("raider saint") || lore.contains("lightning cadence") || lore.contains("split mercy"));
    }

    @Test
    void loreFragmentDoesNotLeakTraceOrDebugStyleTokens() {
        Artifact artifact = seeded(12L, EquipmentArchetype.ELYTRA);
        artifact.setAwakeningPath("stormflight");
        artifact.setAwakeningLoreTrace("tempest-vector:alpha:memory-signature");
        artifact.setConvergenceLoreTrace("trace=debug-leak");
        artifact.setConvergenceContinuityTrace("path->override");
        artifact.addNotableEvent("artifact-convergence:horizon-syndicate:aa44bb");

        String lore = generator.loreFragment(artifact).toLowerCase();

        assertFalse(lore.contains("trace="));
        assertFalse(lore.contains("artifact-convergence"));
        assertFalse(lore.contains("->"));
        assertFalse(lore.contains("aa44bb"));
    }

    @Test
    void epithetFragmentStaysBriefGroundedAndDeterministic() {
        Artifact artifact = seeded(101L, EquipmentArchetype.TRIDENT);
        artifact.setAwakeningPath("undertow choir");
        artifact.setConvergencePath("reef covenant");
        artifact.setConvergenceIdentityShape("tidal jurist");
        artifact.addNotableEvent("memory.shipwreck vigil");

        String first = generator.epithetFragment(artifact);
        String second = generator.epithetFragment(artifact);

        assertEquals(first, second);
        String[] sentences = first.split("(?<=[.!?])\\s+");
        assertTrue(sentences.length >= 1 && sentences.length <= 2);
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            assertFalse(trimmed.isBlank());
            assertTrue(trimmed.split("\\s+").length <= 10, trimmed);
        }
        assertFalse(first.toLowerCase().contains("tier"));
        assertFalse(first.toLowerCase().contains("rarity"));
    }

    @Test
    void loreEnginePlacesSignificanceFirstThenEpithetThenLore() {
        Artifact artifact = seeded(202L, EquipmentArchetype.DIAMOND_CHESTPLATE);
        artifact.setAwakeningPath("bastion hymn");
        artifact.addNotableEvent("memory.last stand");
        ArtifactReputation reputation = new ArtifactReputation();

        List<String> lines = loreEngine.buildLoreLines(artifact, reputation);

        assertFalse(lines.isEmpty());
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("—"));
        assertNotEquals(lines.get(0), lines.get(1));
        assertNotEquals(lines.get(1), lines.get(2));
    }

    @Test
    void loreEnginePlayerFacingStackOmitsInternalStatAndTraceResidue() {
        Artifact artifact = seeded(303L, EquipmentArchetype.TRIDENT);
        artifact.setAwakeningPath("undertow choir");
        artifact.setAwakeningLineageTrace("lineage:undertow-choir:pressure=22");
        artifact.setConvergenceExpressionTrace("trace=debug-leak");
        artifact.addNotableEvent("artifact-convergence:horizon-syndicate:aa44bb");

        ArtifactReputation reputation = new ArtifactReputation();
        reputation.setPrecision(8);
        reputation.setMobility(9);

        String combined = String.join(" ", loreEngine.buildLoreLines(artifact, reputation)).toLowerCase();

        assertFalse(combined.contains(" p8 "));
        assertFalse(combined.contains("trace="));
        assertFalse(combined.contains("pressure"));
        assertFalse(combined.contains("aa44bb"));
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.72D);
        artifact.setSeedSurvivalAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.DEFENSIVE_ARMOR) ? 0.92D : 0.34D);
        artifact.setSeedMobilityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.MOBILITY) ? 0.87D : 0.28D);
        artifact.setSeedBrutalityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.WEAPON) ? 0.81D : 0.22D);
        artifact.setSeedChaosAffinity(0.31D);
        artifact.setSeedConsistencyAffinity(0.63D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }
}
