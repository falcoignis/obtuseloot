package obtuseloot.artifacts;

import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactGenerationEquipmentCoverageTest {

    @Test
    void generatorOnlyProducesRealEquipmentArchetypes() {
        Set<String> generated = new HashSet<>();
        for (long seed = 0; seed < 10_000; seed++) {
            String category = ArtifactGenerator.resolveCategory(seed);
            generated.add(category);
            assertTrue(EquipmentArchetype.isEquipment(category), () -> "non-equipment category: " + category);
            assertNotEquals("generic", category);
        }

        assertEquals(new HashSet<>(EquipmentArchetype.allIds()), generated);
    }

    @Test
    void coverageIncludesArmorWeaponAndToolWeaponHybridRequirements() {
        assertTrue(EquipmentArchetype.isEquipment("chainmail_helmet"));
        assertTrue(EquipmentArchetype.isEquipment("turtle_helmet"));
        assertTrue(EquipmentArchetype.isEquipment("trident"));

        Artifact axeArtifact = artifact("diamond_axe");
        assertTrue(ArtifactEligibility.isWeaponEligible(axeArtifact));
        assertTrue(ArtifactEligibility.isToolEligible(axeArtifact));
        assertTrue(ArtifactEligibility.isToolWeaponHybridEligible(axeArtifact));

        Artifact tridentArtifact = artifact("trident");
        assertTrue(ArtifactEligibility.isWeaponEligible(tridentArtifact));
        assertFalse(ArtifactEligibility.isArmorEligible(tridentArtifact));
    }

    @Test
    void invalidUtilityItemsAndLegacyGenericCategoryAreRejected() {
        for (String invalid : Set.of(
                "generic",
                "shears",
                "fishing_rod",
                "flint_and_steel",
                "brush",
                "carrot_on_a_stick",
                "warped_fungus_on_a_stick")) {
            assertFalse(EquipmentArchetype.isEquipment(invalid), () -> "unexpectedly allowed: " + invalid);
            assertThrows(IllegalStateException.class, () -> artifact(invalid));
        }
    }

    private Artifact artifact(String category) {
        return new Artifact(UUID.randomUUID(), category);
    }
}
