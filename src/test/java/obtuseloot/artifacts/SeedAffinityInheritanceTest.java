package obtuseloot.artifacts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SeedAffinityInheritanceTest {
    private final ArtifactSeedFactory factory = new ArtifactSeedFactory();

    @Test void birthVariationUsesAllSeedBitsAndLeavesParentUnchanged() {
        Artifact parent = seeded(17L);
        double[] before = affinities(parent);
        Artifact first = seeded(-19L);
        Artifact replay = seeded(-19L);
        Artifact sibling = seeded(-19L + (1L << 48));
        factory.inheritAffinities(first, parent);
        factory.inheritAffinities(replay, parent);
        factory.inheritAffinities(sibling, parent);
        assertArrayEquals(affinities(first), affinities(replay));
        assertFalse(java.util.Arrays.equals(affinities(first), affinities(sibling)));
        assertArrayEquals(before, affinities(parent));
        for (int i = 0; i < before.length; i++) {
            assertNotEquals(before[i], affinities(first)[i]);
            assertTrue(Math.abs(before[i] - affinities(first)[i]) <= 0.16D);
        }
    }

    @Test void evolvedAffinitiesAreInheritedLocallyInsteadOfClippedToFounderRange() {
        Artifact parent = seeded(7L);
        parent.setSeedPrecisionAffinity(4.0D);
        parent.setSeedBrutalityAffinity(-4.0D);
        Artifact child = seeded(Long.MIN_VALUE);
        factory.inheritAffinities(child, parent);
        assertEquals(4.0D, child.getSeedPrecisionAffinity(), 0.16D);
        assertEquals(-4.0D, child.getSeedBrutalityAffinity(), 0.16D);
    }

    private Artifact seeded(long seed) {
        Artifact artifact = new Artifact(new UUID(0L, 1L), EquipmentArchetype.IRON_AXE);
        factory.applySeedProfile(artifact, seed);
        return artifact;
    }

    private double[] affinities(Artifact artifact) {
        return new double[]{artifact.getSeedPrecisionAffinity(), artifact.getSeedBrutalityAffinity(),
                artifact.getSeedSurvivalAffinity(), artifact.getSeedMobilityAffinity(),
                artifact.getSeedChaosAffinity(), artifact.getSeedConsistencyAffinity()};
    }
}
