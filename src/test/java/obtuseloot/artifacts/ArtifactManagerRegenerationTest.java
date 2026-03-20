package obtuseloot.artifacts;

import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.species.SpeciesRegistrySnapshot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactManagerRegenerationTest {

    @Test
    void reseedCreatesReplacementArtifactInsteadOfMutatingExistingIdentity() {
        InMemoryPlayerStateStore store = new InMemoryPlayerStateStore();
        ArtifactManager manager = new ArtifactManager(store);
        UUID playerId = UUID.randomUUID();

        Artifact original = manager.getOrCreate(playerId);
        String originalCategory = original.getItemCategory();
        original.setDriftLevel(7);
        original.addLoreHistory("found in ash");
        original.setLastUtilityHistory("utility-before-reseed");

        long newSeed = original.getArtifactSeed();
        while (ArtifactGenerator.resolveCategory(newSeed).equals(original.getItemCategory())) {
            newSeed++;
        }

        long replacementSeed = newSeed;
        Artifact replacement = manager.reseed(playerId, replacementSeed);

        assertAll(
                () -> assertNotSame(original, replacement, "reseed should return a replacement Artifact instance"),
                () -> assertEquals(playerId, replacement.getOwnerId()),
                () -> assertEquals(original.getArtifactStorageKey(), replacement.getArtifactStorageKey()),
                () -> assertEquals(ArtifactGenerator.resolveCategory(replacementSeed), replacement.getItemCategory()),
                () -> assertEquals(replacementSeed, replacement.getArtifactSeed()),
                () -> assertEquals(originalCategory, original.getItemCategory(), "original artifact instance remains unchanged"),
                () -> assertEquals(7, original.getDriftLevel(), "previous state remains on the discarded instance"),
                () -> assertEquals("utility-before-reseed", original.getLastUtilityHistory()),
                () -> assertEquals(0, replacement.getDriftLevel(), "replacement starts from a clean mutable state"),
                () -> assertTrue(replacement.getLoreHistory().isEmpty(), "history must be intentionally reset during regeneration"),
                () -> assertEquals("", replacement.getLastUtilityHistory(), "utility history must not be accidentally carried forward"),
                () -> assertSame(replacement, manager.getLoadedArtifacts().get(playerId), "manager should publish the replacement instance")
        );
    }

    @Test
    void saveAfterReseedPersistsOnlyReplacementArtifactState() {
        InMemoryPlayerStateStore store = new InMemoryPlayerStateStore();
        ArtifactManager manager = new ArtifactManager(store);
        UUID playerId = UUID.randomUUID();

        Artifact original = manager.getOrCreate(playerId);
        original.addLoreHistory("legacy-entry");

        long newSeed = original.getArtifactSeed();
        while (ArtifactGenerator.resolveCategory(newSeed).equals(original.getItemCategory())) {
            newSeed++;
        }

        long replacementSeed = newSeed;
        Artifact replacement = manager.reseed(playerId, replacementSeed);
        replacement.addNotableEvent("replacement-born");
        manager.save(playerId);

        Artifact persisted = store.loadArtifact(playerId);
        assertAll(
                () -> assertNotNull(persisted),
                () -> assertEquals(replacement.getItemCategory(), persisted.getItemCategory()),
                () -> assertEquals(replacementSeed, persisted.getArtifactSeed()),
                () -> assertTrue(persisted.getLoreHistory().isEmpty(), "discarded history should not leak into persisted replacement"),
                () -> assertEquals(1, persisted.getNotableEvents().size()),
                () -> assertEquals("replacement-born", persisted.getNotableEvents().getFirst())
        );
    }

    @Test
    void elytraArchetypesRemainValidAtConstruction() {
        Artifact artifact = new Artifact(UUID.randomUUID(), EquipmentArchetype.ELYTRA);

        assertAll(
                () -> assertEquals("elytra", artifact.getItemCategory()),
                () -> assertEquals(EquipmentArchetype.ELYTRA, ArtifactArchetypeValidator.requireValid(artifact, "elytra test"))
        );
    }

    private static final class InMemoryPlayerStateStore implements PlayerStateStore {
        private final Map<UUID, Artifact> artifacts = new HashMap<>();
        private final Map<UUID, ArtifactReputation> reputations = new HashMap<>();
        private SpeciesRegistrySnapshot speciesSnapshot;

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

        @Override
        public void saveSpeciesSnapshot(SpeciesRegistrySnapshot snapshot) {
            this.speciesSnapshot = snapshot;
        }

        @Override
        public SpeciesRegistrySnapshot loadSpeciesSnapshot() {
            return speciesSnapshot;
        }
    }
}
