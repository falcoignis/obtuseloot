package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactNamingRefactorTest {

    @Test
    void namingIsDeterministicForEquivalentIdentity() {
        Artifact left = seeded(99L, "iron_sword");
        Artifact right = seeded(99L, "iron_sword");
        ArtifactNaming first = ArtifactNameResolver.initialize(left);
        ArtifactNaming second = ArtifactNameResolver.initialize(right);

        assertEquals(first.getDisplayName(), second.getDisplayName());
    }

    @Test
    void trueNamePersistsWhileDisplayEvolvesAcrossDiscovery() {
        Artifact artifact = seeded(7L, "diamond_sword");
        artifact.setEvolutionPath("advanced");
        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);
        naming.setTrueName("Vesper");
        artifact.setNaming(naming);

        String early = artifact.getDisplayName();
        for (int i = 0; i < 20; i++) {
            artifact.addLoreHistory("memory." + i);
            artifact.addNotableEvent("notable." + i);
        }
        ArtifactNameResolver.refresh(artifact, artifact.getNaming());

        assertEquals("Vesper", artifact.getTrueName());
        assertEquals(ArtifactDiscoveryState.OBSCURED, artifact.getNaming().getDiscoveryState());
        assertFalse(artifact.getDisplayName().isBlank());
        assertEquals(early, artifact.getDisplayName());
    }

    @Test
    void namingRemainsStableAcrossMutableStateChangesAndTimeProgression() {
        Artifact artifact = seeded(77L, "diamond_sword");
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        String initialName = artifact.getDisplayName();
        long namingSeed = artifact.getNaming().getNamingSeed();

        artifact.setEvolutionPath("advanced");
        artifact.setAwakeningPath("storm");
        artifact.setConvergencePath("vanguard");
        artifact.setDriftAlignment("volatile");
        artifact.setDriftLevel(4);
        artifact.setTotalDrifts(9);
        artifact.setLastDriftTimestamp(1_000_000L);
        artifact.addLoreHistory("history.1");
        artifact.addNotableEvent("event.1");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.setLastDriftTimestamp(9_999_999L);
        artifact.refreshNamingProjection();

        assertEquals(initialName, artifact.getDisplayName());
        assertEquals(namingSeed, artifact.getNaming().getNamingSeed());
    }

    @Test
    void differentIdentityAllowsNameChange() {
        Artifact original = seeded(88L, "diamond_sword");
        Artifact replacement = seeded(89L, "elytra");

        assertNotEquals(original.getNaming().getNamingSeed(), replacement.getNaming().getNamingSeed());
        assertNotEquals(original.getDisplayName(), replacement.getDisplayName());
    }

    @Test
    void compressionRemovesBloat() {
        String compressed = ArtifactNameCompressor.compress("Blazing Ashen Oathbound Cinder Sword of Ember Memory", 4);
        assertEquals("Blazing Ashen Oathbound Cinder", compressed);
    }


    @Test
    void elytraUsesDedicatedRootForm() {
        Artifact artifact = seeded(401L, "elytra");
        artifact.setSeedMobilityAffinity(0.95D);

        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);

        assertTrue(Set.of("Wings", "Mantle", "Glider").contains(naming.getRootForm()));
        assertTrue(naming.getDisplayName().contains(naming.getRootForm()));
    }

    @Test
    void armorIdentityCanProduceEquipmentFlavor() {
        Artifact artifact = seeded(241L, "chainmail_helmet");
        artifact.setSeedSurvivalAffinity(0.9D);
        artifact.setSeedMobilityAffinity(0.91D);
        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);

        assertTrue(naming.getDisplayName().contains("Helm") || naming.getDisplayName().contains("Vigil") || naming.getDisplayName().contains("Ward"));
    }

    private Artifact seeded(long seed, String category) {
        Artifact artifact = new Artifact(UUID.randomUUID(), category);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.7D);
        artifact.setSeedSurvivalAffinity(0.8D);
        artifact.setSeedChaosAffinity(0.2D);
        artifact.setSeedMobilityAffinity(0.5D);
        artifact.setSeedBrutalityAffinity(0.3D);
        artifact.setSeedConsistencyAffinity(0.6D);
        return artifact;
    }
}
