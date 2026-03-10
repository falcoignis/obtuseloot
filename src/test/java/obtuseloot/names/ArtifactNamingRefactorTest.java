package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactNamingRefactorTest {
    @Test
    void rankDerivationPriorityFusedBeatsAwakened() {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setEvolutionPath("mythic");
        artifact.setAwakeningPath("emberwake");
        artifact.setFusionPath("twin-reliquary");

        assertEquals(ArtifactRank.FUSED, ArtifactRankResolver.resolve(artifact));
    }

    @Test
    void namingIsDeterministicForEquivalentIdentity() {
        Artifact left = seeded(99L, "artifact");
        Artifact right = seeded(99L, "artifact");
        ArtifactNaming first = ArtifactNameResolver.initialize(left);
        ArtifactNaming second = ArtifactNameResolver.initialize(right);

        assertEquals(first.getDisplayName(), second.getDisplayName());
    }

    @Test
    void trueNamePersistsWhileDisplayEvolvesAcrossDiscovery() {
        Artifact artifact = seeded(7L, "blade");
        artifact.setEvolutionPath("mythic");
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
        assertNotEquals(early, artifact.getDisplayName());
    }

    @Test
    void compressionRemovesBloat() {
        String compressed = ArtifactNameCompressor.compress("Blazing Ashen Oathbound Cinder Sword of Ember Memory", 4);
        assertEquals("Blazing Ashen Oathbound Cinder", compressed);
    }

    @Test
    void nonCombatIdentityCanProduceUtilityFlavor() {
        Artifact artifact = seeded(241L, "bell");
        artifact.setSeedSurvivalAffinity(0.9D);
        artifact.setSeedMobilityAffinity(0.91D);
        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);

        assertTrue(naming.getDisplayName().contains("Bell") || naming.getDisplayName().contains("Ward") || naming.getDisplayName().contains("Vigil"));
    }

    private Artifact seeded(long seed, String category) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setItemCategory(category);
        artifact.setSeedPrecisionAffinity(0.7D);
        artifact.setSeedSurvivalAffinity(0.8D);
        artifact.setSeedChaosAffinity(0.2D);
        artifact.setSeedMobilityAffinity(0.5D);
        artifact.setSeedBrutalityAffinity(0.3D);
        artifact.setSeedConsistencyAffinity(0.6D);
        return artifact;
    }
}
