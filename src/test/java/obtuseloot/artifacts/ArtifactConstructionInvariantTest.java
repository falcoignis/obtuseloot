package obtuseloot.artifacts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactConstructionInvariantTest {

    @Test
    void constructorRejectsMissingArchetype() {
        UUID ownerId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> new Artifact(ownerId, (String) null)),
                () -> assertThrows(IllegalStateException.class, () -> new Artifact(ownerId, "   "))
        );
    }

    @Test
    void constructorRejectsUnknownOrLegacyArchetypeIds() {
        UUID ownerId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> new Artifact(ownerId, "artifact")),
                () -> assertThrows(IllegalStateException.class, () -> new Artifact(ownerId, "generic")),
                () -> assertThrows(IllegalStateException.class, () -> new Artifact(ownerId, "unknown_category"))
        );
    }

    @Test
    void constructorCanonicalizesAndAcceptsValidArchetypesIncludingElytra() {
        Artifact elytra = new Artifact(UUID.randomUUID(), "ELYTRA");
        Artifact sword = new Artifact(UUID.randomUUID(), EquipmentArchetype.WOODEN_SWORD);

        assertAll(
                () -> assertEquals("elytra", elytra.getItemCategory()),
                () -> assertEquals("wooden_sword", sword.getItemCategory())
        );
    }
}
