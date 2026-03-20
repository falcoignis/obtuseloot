package obtuseloot.eligibility;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactEligibilityElytraTest {

    @Test
    void elytraParticipatesInGeneralEquipmentEligibility() {
        Artifact artifact = artifact("elytra");

        assertAll(
                () -> assertFalse(ArtifactEligibility.isGenericItem(artifact)),
                () -> assertTrue(ArtifactEligibility.isEvolutionEligible(artifact)),
                () -> assertTrue(ArtifactEligibility.isAbilityEligible(artifact)),
                () -> assertTrue(ArtifactEligibility.isMemoryEligible(artifact)),
                () -> assertTrue(ArtifactEligibility.isArmorEligible(artifact))
        );
    }

    @Test
    void elytraIsExcludedFromWeaponAndDefensiveArmorSystems() {
        Artifact artifact = artifact("elytra");

        assertAll(
                () -> assertFalse(ArtifactEligibility.isWeaponEligible(artifact)),
                () -> assertFalse(ArtifactEligibility.isDefensiveArmorEligible(artifact)),
                () -> assertFalse(ArtifactEligibility.isToolEligible(artifact)),
                () -> assertFalse(ArtifactEligibility.isToolWeaponHybridEligible(artifact))
        );
    }

    @Test
    void chestplatesRemainDefensiveArmorEligible() {
        Artifact chestplate = artifact("diamond_chestplate");

        assertAll(
                () -> assertTrue(ArtifactEligibility.isArmorEligible(chestplate)),
                () -> assertTrue(ArtifactEligibility.isDefensiveArmorEligible(chestplate)),
                () -> assertFalse(ArtifactEligibility.isWeaponEligible(chestplate))
        );
    }

    private Artifact artifact(String itemCategory) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setItemCategory(itemCategory);
        return artifact;
    }
}
