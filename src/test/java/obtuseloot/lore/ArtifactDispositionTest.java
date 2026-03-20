package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactDispositionTest {
    @Test
    void resolvesDeterministicDispositionFromRealSignals() {
        Artifact artifact = seeded(404L, EquipmentArchetype.DIAMOND_CHESTPLATE);
        artifact.setAwakeningPath("bastion hymn");
        artifact.setAwakeningIdentityShape("unyielding ward");
        artifact.setAwakeningExpressionTrace("measured shelter");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.LONG_BATTLE);

        ArtifactReputation reputation = new ArtifactReputation();
        reputation.setSurvival(14);
        reputation.setConsistency(10);
        reputation.setPrecision(4);

        ArtifactDisposition first = ArtifactDisposition.resolve(artifact, reputation);
        ArtifactDisposition second = ArtifactDisposition.resolve(artifact, reputation);

        assertEquals(first, second);
        assertEquals("survival", first.drive());
        assertTrue(first.pressure() > 0.0D);
        assertTrue(first.temperament() == ArtifactDisposition.Temperament.WATCHFUL
                || first.temperament() == ArtifactDisposition.Temperament.RESTRAINED);
    }

    @Test
    void loreAndEpithetShiftWordingWithoutExposingDispositionLabels() {
        Artifact artifact = seeded(505L, EquipmentArchetype.TRIDENT);
        artifact.setConvergencePath("reef covenant");
        artifact.setConvergenceIdentityShape("tidal jurist");
        artifact.setConvergenceExpressionTrace("split mercy");
        artifact.addNotableEvent("memory.shipwreck vigil");

        ArtifactReputation reputation = new ArtifactReputation();
        reputation.setPrecision(13);
        reputation.setMobility(11);

        LoreFragmentGenerator generator = new LoreFragmentGenerator();
        String epithet = generator.epithetFragment(artifact, reputation).toLowerCase();
        String lore = generator.loreFragment(artifact, reputation).toLowerCase();

        assertFalse(epithet.contains("temperament"));
        assertFalse(epithet.contains("drive"));
        assertFalse(lore.contains("direction"));
        assertTrue(epithet.contains("quick hands") || epithet.contains("two inheritances") || epithet.contains("edge bends"));
        assertTrue(lore.contains("split mercy") || lore.contains("reef covenant") || lore.contains("shipwreck vigil"));
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.68D);
        artifact.setSeedSurvivalAffinity(0.88D);
        artifact.setSeedMobilityAffinity(0.52D);
        artifact.setSeedBrutalityAffinity(0.24D);
        artifact.setSeedChaosAffinity(0.22D);
        artifact.setSeedConsistencyAffinity(0.74D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }
}
