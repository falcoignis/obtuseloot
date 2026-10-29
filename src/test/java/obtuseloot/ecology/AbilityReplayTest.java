package obtuseloot.ecology;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.ArtifactStorageKeys;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AbilityReplayTest {
    @Test
    void suppliedIdentitiesPersistAndLeaveTheBirthRandomStreamAlone() {
        var store = new LocalEcosystemHealthTest.Store();
        var ids = new java.util.concurrent.atomic.AtomicLong();
        var ecosystem = new LocalEcosystem(store, new ArtifactManager(store), new LocalInheritancePool(),
                () -> "artifact:" + new UUID(77, ids.incrementAndGet()));
        var origin = new WorldPosition(new UUID(0, 77), 0, 0);
        var random = new Random(42);
        var expectedRandom = new Random(42);
        var founder = ecosystem.createEquipment(new UUID(1, 1), EquipmentArchetype.IRON_AXE, origin, random);
        assertEquals(expectedRandom.nextLong(), founder.getArtifactSeed());
        assertEquals("artifact:" + new UUID(77, 1), founder.getArtifactStorageKey());
        assertEquals("lineage-" + new UUID(77, 1), founder.getLatentLineage());
        assertEquals(founder.getLatentLineage(), store.loadArtifactProfile(founder.getArtifactStorageKey()).getLatentLineage());
        assertEquals(expectedRandom.nextLong(), random.nextLong(), "Identity generation must not consume the birth RNG");
        ecosystem.observe(LocalInheritancePoolTest.observation(founder, origin, true, 1, 0, "founder"));
        var child = ecosystem.createEquipment(new UUID(1, 2), EquipmentArchetype.IRON_AXE, origin, random);
        assertEquals("artifact:" + new UUID(77, 2), child.getArtifactStorageKey());
        assertEquals(founder.getArtifactStorageKey(), child.getParentStorageKey());
        assertEquals(founder.getLatentLineage(), child.getLatentLineage());
        assertEquals(child.getParentStorageKey(), store.loadArtifactProfile(child.getArtifactStorageKey()).getParentStorageKey());
    }

    @Test
    void lineageIdentityIsAnInputToDescendantDrift() {
        var left = new obtuseloot.lineage.ArtifactLineage("lineage-a");
        var right = new obtuseloot.lineage.ArtifactLineage("lineage-b");
        var observed = new obtuseloot.lineage.EvolutionaryBiasGenome();
        var heuristics = new obtuseloot.lineage.InheritanceBranchingHeuristics();
        left.registerDescendantBias(42, observed, 1, 1, 1, 0, heuristics);
        right.registerDescendantBias(42, observed, 1, 1, 1, 0, heuristics);
        assertNotEquals(left.evolutionaryBiasGenome().tendencies(), right.evolutionaryBiasGenome().tendencies(),
                "Holding artifact seeds fixed is insufficient when lineage identities change");
    }

    @Test
    void replayMatchesAcrossIndependentJvms() throws Exception {
        Path output = Path.of("target", "ability-replay");
        Files.createDirectories(output);
        String expected = null;
        for (int i = 0; i < 3; i++) {
            Path transcript = output.resolve("replay-" + i + ".tsv");
            Process process = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx512m",
                    "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    Witness.class.getName()).redirectErrorStream(true).redirectOutput(transcript.toFile()).start();
            try {
                assertTrue(process.waitFor(90, TimeUnit.SECONDS), "Ability replay timed out");
                assertEquals(0, process.exitValue(), () -> "See " + transcript);
                String actual = Files.readString(transcript);
                if (expected == null) expected = actual;
                else assertTrue(expected.equals(actual), "Seeded replay differs; see target/ability-replay");
            } finally {
                process.destroyForcibly();
            }
        }
    }

    public static class Witness {
        public static void main(String[] args) {
            var store = new LocalEcosystemHealthTest.Store();
            Random identities = new Random(0x554E495155L ^ 0x4944454E54495459L);
            var ecosystem = new LocalEcosystem(store, new ArtifactManager(store), new LocalInheritancePool(),
                    args.length == 0 ? () -> "artifact:" + new UUID(identities.nextLong(), identities.nextLong())
                            : ArtifactStorageKeys::create);
            var resolver = new SeededAbilityResolver(new AbilityRegistry().implementedOnly(), new ArtifactMemoryEngine(),
                    new ArtifactEcosystemSelfBalancingEngine(), new LineageRegistry(), new LineageInfluenceResolver(), null);
            Random random = new Random(0x554E495155L);
            var archetypes = EquipmentArchetype.values();
            for (int i = 0; i < 400; i++) {
                var origin = new WorldPosition(new UUID(0, 77), (i % 8) * 4_000, 0);
                var item = ecosystem.createEquipment(new UUID(1, i), archetypes[random.nextInt(archetypes.length)], origin, random);
                ecosystem.observe(LocalInheritancePoolTest.observation(item, origin, true,
                        1.0D + random.nextDouble() * 9.0D, random.nextDouble(), "measured-" + i));
                var abilities = resolver.resolve(item, item.getItemReputation()).abilities().stream()
                        .map(AbilityDefinition::id).sorted().collect(Collectors.joining("+"));
                System.out.println(i + "\t" + item.getArtifactSeed() + "\t" + item.getInheritanceGeneration()
                        + "\t" + abilities + "\t" + item.getLastGateCandidatePool()
                        + "\t" + item.getGenome().traits() + item.getGenome().latentTraits()
                        + "\t" + item.getSeedPrecisionAffinity() + "/" + item.getSeedBrutalityAffinity()
                        + "/" + item.getSeedSurvivalAffinity() + "/" + item.getSeedMobilityAffinity()
                        + "/" + item.getSeedChaosAffinity() + "/" + item.getSeedConsistencyAffinity()
                        + "\t" + item.getDisplayName());
            }
        }
    }
}
