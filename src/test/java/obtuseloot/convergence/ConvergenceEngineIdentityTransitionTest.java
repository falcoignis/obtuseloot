package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConvergenceEngineIdentityTransitionTest {

    @Test
    void convergenceProducesReplacementIdentityWithNewArchetypeSeedAndLineageContinuity() {
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = seeded(41L, EquipmentArchetype.BOW);
        artifact.setArchetypePath("deadeye");
        artifact.setEvolutionPath("advanced-deadeye");
        artifact.setAwakeningPath("Stormblade");
        artifact.setLatentLineage("lineage-omega");
        artifact.addLoreHistory("earned through battle");
        artifact.addNotableEvent("awakening.stormblade");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(28);
        rep.setMobility(18);
        rep.setBrutality(12);
        rep.setKills(14);
        rep.setBossKills(2);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        assertNotNull(transition);
        Artifact replacement = transition.replacement();
        assertNotSame(artifact, replacement);
        assertEquals("artifact-convergence:horizon-syndicate", transition.reason());
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey());
        assertEquals(artifact.getOwnerId(), replacement.getOwnerId());
        assertEquals("lineage-omega", replacement.getLatentLineage());
        assertEquals("horizon-syndicate", replacement.getConvergencePath());
        assertEquals("horizon", replacement.getArchetypePath());
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed());
        assertNotEquals(artifact.getNaming().getNamingSeed(), replacement.getNaming().getNamingSeed());
        assertEquals(EquipmentArchetype.CROSSBOW.id(), replacement.getItemCategory());
        assertTrue(replacement.getNotableEvents().stream().anyMatch(event -> event.startsWith("identity.replaced.")));
        assertEquals(artifact.getMemory().pressure(), replacement.getMemory().pressure());
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        return artifact;
    }
}
