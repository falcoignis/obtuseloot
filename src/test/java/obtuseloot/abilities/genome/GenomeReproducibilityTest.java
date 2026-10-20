package obtuseloot.abilities.genome;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GenomeReproducibilityTest {
    @Test
    void snapshotsKeepDeclarationOrderAndCannotBeModified() {
        ArtifactGenome genome = new GenomeResolver().resolve(42L);
        assertEquals(Arrays.asList(GenomeTrait.values()), new ArrayList<>(genome.traits().keySet()));
        assertEquals(Arrays.asList(GenomeTrait.values()), new ArrayList<>(genome.latentTraits().keySet()));
        ArtifactGenome activated = new ArtifactGenome(42L, genome.traits(), genome.latentTraits(),
                EnumSet.allOf(GenomeTrait.class));
        assertEquals(Arrays.asList(GenomeTrait.values()), new ArrayList<>(activated.activatedLatentTraits()));
        assertThrows(UnsupportedOperationException.class, () -> genome.traits().clear());
        assertThrows(UnsupportedOperationException.class, () -> genome.latentTraits().clear());
        assertThrows(UnsupportedOperationException.class, () -> activated.activatedLatentTraits().clear());
    }

    @Test
    void seededMutationIsIdenticalAcrossSeparateJvms() throws Exception {
        String expected = null;
        for (int i = 0; i < 6; i++) {
            Process process = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    Witness.class.getName()).redirectErrorStream(true).start();
            try {
                assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Genome witness timed out");
                String actual = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(0, process.exitValue(), actual);
                if (expected == null) expected = actual;
                else assertEquals(expected, actual, "A seeded genome must not depend on JVM collection salt");
            } finally {
                process.destroyForcibly();
            }
        }
    }

    public static class Witness {
        public static void main(String[] args) throws Exception {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            var resolver = new GenomeResolver();
            var mutation = new GenomeMutationEngine();
            for (long seed = -30; seed < 30; seed++) {
                ArtifactGenome genome = mutation.mutate(resolver.resolve(seed), 4);
                digest.update((genome.traits().toString() + genome.latentTraits())
                        .getBytes(StandardCharsets.UTF_8));
            }
            System.out.print(java.util.HexFormat.of().formatHex(digest.digest()));
        }
    }
}
