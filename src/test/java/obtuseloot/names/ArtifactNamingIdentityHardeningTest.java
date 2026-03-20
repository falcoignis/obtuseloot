package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactNamingIdentityHardeningTest {

    @Test
    void everyEquipmentArchetypeResolvesToRootFormWithoutFallbacks() {
        for (EquipmentArchetype archetype : EquipmentArchetype.values()) {
            Artifact artifact = seeded(100L + archetype.ordinal(), archetype);
            ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);

            assertNotNull(naming.getRootForm());
            assertFalse(naming.getRootForm().isBlank());
            assertNotEquals("Artifact", naming.getRootForm());
            assertNotEquals("Nameless Artifact", naming.getDisplayName());
        }
    }

    @Test
    void rootFormFamiliesStayBoundToArchetypeIdentity() {
        assertEquals(Set.of("Blade"), Set.copyOf(EquipmentArchetype.DIAMOND_SWORD.rootForms()));
        assertEquals(Set.of("Axe"), Set.copyOf(EquipmentArchetype.NETHERITE_AXE.rootForms()));
        assertEquals(Set.of("Helm"), Set.copyOf(EquipmentArchetype.TURTLE_HELMET.rootForms()));
        assertEquals(Set.of("Cuirass"), Set.copyOf(EquipmentArchetype.DIAMOND_CHESTPLATE.rootForms()));
        assertEquals(Set.of("Greaves"), Set.copyOf(EquipmentArchetype.IRON_LEGGINGS.rootForms()));
        assertEquals(Set.of("Boots"), Set.copyOf(EquipmentArchetype.GOLDEN_BOOTS.rootForms()));
        assertEquals(Set.of("Bow"), Set.copyOf(EquipmentArchetype.BOW.rootForms()));
        assertEquals(Set.of("Spear"), Set.copyOf(EquipmentArchetype.TRIDENT.rootForms()));
        assertEquals(Set.of("Wings", "Mantle", "Glider"), Set.copyOf(EquipmentArchetype.ELYTRA.rootForms()));
    }

    @Test
    void elytraVariationProducesOnlyAerialRoots() {
        Set<String> observed = new java.util.LinkedHashSet<>();
        for (long seed = 0; seed < 24; seed++) {
            ArtifactNaming naming = ArtifactNameResolver.initialize(seeded(seed, EquipmentArchetype.ELYTRA));
            observed.add(naming.getRootForm());
            String display = naming.getDisplayName().toLowerCase();
            assertFalse(display.contains("cuirass"));
            assertFalse(display.contains("helm"));
            assertFalse(display.contains("blade"));
            assertFalse(display.contains("axe"));
        }
        assertEquals(Set.of("Wings", "Mantle", "Glider"), observed);
    }

    @Test
    void defensiveArmorNamesStayProtectiveAndNonWeaponized() {
        Artifact artifact = seeded(700L, EquipmentArchetype.DIAMOND_CHESTPLATE);
        artifact.setSeedSurvivalAffinity(0.95D);
        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);

        assertEquals("Cuirass", naming.getRootForm());
        assertTrue(naming.getIdentityTags().contains("defensive"));
        assertFalse(naming.getIdentityTags().contains("weapon"));
        assertTrue(naming.getDisplayName().contains("Cuirass"));
    }

    @Test
    void refreshUpdatesDerivedNameStateAcrossReplacementIdentity() {
        Artifact original = seeded(10L, EquipmentArchetype.DIAMOND_SWORD);
        Artifact replacement = seeded(11L, EquipmentArchetype.ELYTRA);

        String originalName = original.getGeneratedName();
        String replacementName = replacement.getGeneratedName();

        assertNotEquals(originalName, replacementName);
        assertTrue(Set.of("Wings", "Mantle", "Glider").contains(replacement.getNaming().getRootForm()));
        assertEquals(replacementName, replacement.getGeneratedName());
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.72D);
        artifact.setSeedSurvivalAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.DEFENSIVE_ARMOR) ? 0.9D : 0.3D);
        artifact.setSeedMobilityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.MOBILITY) ? 0.95D : 0.25D);
        artifact.setSeedBrutalityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.WEAPON) ? 0.8D : 0.1D);
        artifact.setSeedChaosAffinity(0.2D);
        artifact.setSeedConsistencyAffinity(0.6D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }
}
