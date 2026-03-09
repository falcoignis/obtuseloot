package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenomeArchitectureTest {
    @Test
    void genomeGenerationIsDeterministic() {
        GenomeResolver resolver = new GenomeResolver();
        ArtifactGenome a = resolver.resolve(123456789L);
        ArtifactGenome b = resolver.resolve(123456789L);
        assertEquals(a.traits(), b.traits());
    }

    @Test
    void abilityScoringUsesTraitInterferenceWeights() {
        AbilityRegistry registry = new AbilityRegistry();
        TraitInterferenceResolver resolver = new TraitInterferenceResolver(registry.templates());
        AbilityTemplate precisionSigil = registry.templates().stream().filter(t -> t.id().equals("precision.sigil")).findFirst().orElseThrow();

        ArtifactGenome genome = new GenomeResolver().resolve(42L);
        double expected = genome.traits().get(obtuseloot.abilities.genome.GenomeTrait.PRECISION_AFFINITY) * 0.70D
                + genome.traits().get(obtuseloot.abilities.genome.GenomeTrait.RESONANCE) * 0.40D
                + genome.traits().get(obtuseloot.abilities.genome.GenomeTrait.VOLATILITY) * -0.20D;

        assertEquals(expected, resolver.score(precisionSigil, genome), 1.0E-9D);
    }

    @Test
    void generatorProducesDiverseAbilitySetsAcrossSeeds() {
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(new AbilityRegistry());
        ArtifactMemoryProfile memoryProfile = new ArtifactMemoryProfile(0, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D);

        Set<List<String>> distinctProfiles = new HashSet<>();
        for (long seed = 1L; seed <= 12L; seed++) {
            Artifact artifact = artifactFor(seed);
            AbilityProfile profile = generator.generate(artifact, 3, memoryProfile);
            distinctProfiles.add(profile.abilities().stream().map(AbilityDefinition::id).sorted().toList());
        }

        assertTrue(distinctProfiles.size() >= 3, "Expected diversity across seeds");
    }

    private Artifact artifactFor(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "Test");
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.3D);
        artifact.setSeedBrutalityAffinity(0.3D);
        artifact.setSeedSurvivalAffinity(0.3D);
        artifact.setSeedMobilityAffinity(0.3D);
        artifact.setSeedChaosAffinity(0.3D);
        artifact.setSeedConsistencyAffinity(0.3D);
        return artifact;
    }
}
