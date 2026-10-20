package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProbeFixtureIsolationTest {
    @Test
    void probeGeneratorsHaveIndependentHistoryAndDoNotModifyTheGlobalPopulation() {
        var population = AbilityDiversityIndex.instance();
        var before = population.activePool(0L);
        List<List<String>> first = generate();
        assertEquals(before, population.activePool(0L), "A probe must not publish into the process-wide population");
        assertEquals(first, generate(), "A new probe fixture must start from the same empty history");
        assertEquals(before, population.activePool(0L));
    }

    private List<List<String>> generate() {
        var generator = ProbeFixtures.generator(new AbilityRegistry());
        var profiles = new ArrayList<List<String>>();
        var memory = new ArtifactMemoryProfile(4, 1, 1, 1, 1, 1, 0, 0);
        for (long seed = 0x151010L; seed < 0x151018L; seed++) {
            Artifact artifact = new Artifact(new UUID(0L, seed), "wooden_sword");
            artifact.setArtifactSeed(seed);
            profiles.add(generator.generate(artifact, 4, memory).abilities().stream()
                    .map(AbilityDefinition::id).toList());
        }
        return profiles;
    }
}
