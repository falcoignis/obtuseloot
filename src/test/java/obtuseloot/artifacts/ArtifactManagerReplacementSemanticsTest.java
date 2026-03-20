package obtuseloot.artifacts;

import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactManagerReplacementSemanticsTest {

    @Test
    void reseedReplacesArtifactWhilePreservingOnlyOwnerAndStorageContinuity() {
        InMemoryPlayerStateStore store = new InMemoryPlayerStateStore();
        ArtifactManager manager = new ArtifactManager(store);
        UUID ownerId = UUID.randomUUID();

        Artifact original = manager.getOrCreate(ownerId);
        original.setDriftLevel(7);
        original.setTotalDrifts(9);
        original.setEvolutionPath("legendary");
        original.setAwakeningPath("awakened");
        original.setConvergencePath("converged");
        original.setLatentLineage("stormbound");
        original.setSpeciesId("species-a");
        original.setParentSpeciesId("species-parent");
        original.setLastUtilityHistory("history");
        original.addDriftHistory("old-drift");
        original.addLoreHistory("old-lore");
        original.addNotableEvent("old-event");
        original.getAwakeningTraits().add("trait");
        original.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);

        String originalStorageKey = original.getArtifactStorageKey();
        long originalSeed = original.getArtifactSeed();

        Artifact replacement = manager.reseed(ownerId, 42L);

        assertNotSame(original, replacement);
        assertSame(replacement, manager.getOrCreate(ownerId));
        assertEquals(ownerId, replacement.getOwnerId());
        assertEquals(originalStorageKey, replacement.getArtifactStorageKey());
        assertEquals(42L, replacement.getArtifactSeed());
        assertNotEquals(originalSeed, replacement.getArtifactSeed());

        assertEquals(0, replacement.getDriftLevel());
        assertEquals(0, replacement.getTotalDrifts());
        assertEquals("base", replacement.getEvolutionPath());
        assertEquals("dormant", replacement.getAwakeningPath());
        assertEquals("none", replacement.getConvergencePath());
        assertTrue(replacement.getDriftHistory().isEmpty());
        assertTrue(replacement.getLoreHistory().isEmpty());
        assertTrue(replacement.getNotableEvents().isEmpty());
        assertTrue(replacement.getAwakeningTraits().isEmpty());
        assertTrue(replacement.getMemory().snapshot().isEmpty());
        assertEquals("", replacement.getLastUtilityHistory());
        assertEquals("unspeciated", replacement.getSpeciesId());
        assertEquals("none", replacement.getParentSpeciesId());

        manager.save(ownerId);
        Artifact persisted = store.loadArtifact(ownerId);
        assertSame(replacement, persisted);
    }

    @Test
    void identityTransitionRejectsReusingSameInstanceAsReplacement() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        assertThrows(IllegalArgumentException.class, () -> new ArtifactIdentityTransition(artifact, artifact, "bad"));
    }

    private static final class InMemoryPlayerStateStore implements PlayerStateStore {
        private final Map<UUID, Artifact> artifacts = new HashMap<>();
        private final Map<UUID, ArtifactReputation> reputations = new HashMap<>();

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
            reputations.put(playerId, reputation);
        }

        @Override
        public ArtifactReputation loadReputation(UUID playerId) {
            return reputations.get(playerId);
        }
    }
}
