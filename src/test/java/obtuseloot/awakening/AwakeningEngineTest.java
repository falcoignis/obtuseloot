package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AwakeningEngineTest {
    private final AwakeningEngine engine = new AwakeningEngine();

    @Test
    void awakeningReplacesIdentityAndCarriesBoundedContinuity() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(42L);
        artifact.setArchetypePath("ravager");
        artifact.setLatentLineage("lineage-alpha");
        artifact.addLoreHistory("Won a brutal campaign.");
        artifact.addNotableEvent("boss.cut-down");
        artifact.addNotableEvent("survived.hunt");
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(8);
        rep.setBossKills(1);

        ArtifactIdentityTransition transition = engine.evaluate(null, artifact, rep);
        assertNotNull(transition);
        Artifact replacement = transition.replacement();
        assertNotSame(artifact, replacement);
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed());
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey());
        assertEquals("Executioner's Oath", replacement.getAwakeningPath());
        assertNotEquals("none", replacement.getAwakeningVariantId());
        assertNotEquals("none", replacement.getAwakeningIdentityShape());
        assertTrue(replacement.getNotableEvents().stream().anyMatch(e -> e.startsWith("awakening.variant.")));
        assertTrue(replacement.getLoreHistory().stream().anyMatch(e -> e.contains("Awakening replaced")));
    }

    @Test
    void awakeningCannotRepeat() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setAwakeningPath("Stormblade");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(20);
        rep.setKills(20);

        assertNull(engine.evaluate(null, artifact, rep));
        assertNull(engine.forceAwakening(null, artifact, rep));
    }
}
