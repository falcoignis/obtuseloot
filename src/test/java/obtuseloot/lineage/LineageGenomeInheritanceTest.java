package obtuseloot.lineage;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.artifacts.Artifact;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineageGenomeInheritanceTest {
    @Test
    void lineagesPersistInRegistryAndTrackAncestorSeeds() {
        LineageRegistry registry = new LineageRegistry();
        Artifact first = artifactFor(101L);
        first.setLatentLineage("lineage-test");
        Artifact second = artifactFor(202L);
        second.setLatentLineage("lineage-test");

        ArtifactLineage a = registry.assignLineage(first);
        ArtifactLineage b = registry.assignLineage(second);

        assertEquals(a, b);
        assertEquals(a, registry.lineageFor("lineage-test"));
        assertTrue(a.ancestorSeeds().contains(101L));
        assertTrue(a.ancestorSeeds().contains(202L));
        assertEquals(2, a.generationIndex());
    }

    @Test
    void genomesMutateAcrossGenerationsWithinBounds() {
        LineageGenomeInheritance inheritance = new LineageGenomeInheritance();
        ArtifactLineage lineage = new ArtifactLineage("lineage-mut");
        ArtifactGenome parent = new GenomeResolver().resolve(42L);

        ArtifactGenome child = inheritance.inherit(lineage, parent, 84L);

        boolean changed = false;
        for (GenomeTrait trait : GenomeTrait.values()) {
            double delta = child.trait(trait) - parent.trait(trait);
            if (Math.abs(delta) > 1.0E-12D) {
                changed = true;
            }
            assertTrue(Math.abs(delta) <= 0.1500001D, "Mutation should remain in +/-0.15 range");
        }
        assertTrue(changed, "Expected at least one mutated trait");
        assertEquals(1, lineage.generationIndex());
        assertTrue(lineage.ancestorSeeds().contains(84L));
        assertNotEquals(0.0D, lineage.genomeTraits().get(GenomeTrait.PRECISION_AFFINITY));
    }

    private Artifact artifactFor(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "Test");
        artifact.setArtifactSeed(seed);
        return artifact;
    }
}
