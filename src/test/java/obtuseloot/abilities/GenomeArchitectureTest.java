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
        assertEquals(a.latentTraits(), b.latentTraits());
    }

    @Test
    void latentTraitsActivateDeterministicallyFromContext() {
        GenomeResolver resolver = new GenomeResolver();
        LatentTraitActivationResolver activationResolver = new LatentTraitActivationResolver();
        ArtifactGenome genome = resolver.resolve(2025L);
        LatentActivationContext context = LatentActivationContext.bounded(0.95D, 0.95D, 0.95D, 0.75D, 1.0D);

        LatentActivationResult first = activationResolver.resolve(genome, context);
        LatentActivationResult second = activationResolver.resolve(genome, context);

        assertEquals(first.activatedTraits(), second.activatedTraits());
        assertEquals(first.activationRate(), second.activationRate(), 1.0E-9D);
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

    @Test
    void traitScoringUsesDotProductProjection() {
        AbilityRegistry registry = new AbilityRegistry();
        TraitInterferenceResolver resolver = new TraitInterferenceResolver(registry.templates());
        AbilityTemplate precisionSigil = registry.templates().stream().filter(t -> t.id().equals("precision.sigil")).findFirst().orElseThrow();

        ArtifactGenome genome = new GenomeResolver().resolve(42L);
        GenomeProjection genomeProjection = GenomeProjection.fromGenome(genome);
        AbilityTraitVector abilityVector = AbilityTraitVector.fromWeights(resolver.weightsFor(precisionSigil));

        assertEquals(genomeProjection.dot(abilityVector), resolver.score(precisionSigil, genome), 1.0E-9D);
    }

    @Test
    void projectionCacheReusesScoringAcrossSimilarGenomes() {
        AbilityRegistry registry = new AbilityRegistry();
        TraitInterferenceResolver resolver = new TraitInterferenceResolver(registry.templates(), 50_000);
        for (int i = 0; i < 10_000; i++) {
            resolver.selectTop(registry.templates(), new GenomeResolver().resolve(1234L + (i % 5)), 3);
        }

        assertTrue(resolver.cacheHits() > resolver.cacheMisses(), "Expected projection cache to produce repeated hits");
    }


    @Test
    void projectionCacheScalesToMillionGenerationsAndTenThousandLineages() {
        AbilityRegistry registry = new AbilityRegistry();
        TraitInterferenceResolver resolver = new TraitInterferenceResolver(registry.templates(), 100_000);
        GenomeResolver genomeResolver = new GenomeResolver();

        int lineages = 10_000;
        int generations = 1_000_000;
        for (int i = 0; i < generations; i++) {
            long lineageSeed = i % lineages;
            resolver.selectTop(registry.templates(), genomeResolver.resolve(10_000L + lineageSeed), 3);
        }

        assertTrue(resolver.cacheHits() >= (generations - lineages), "Expected cache reuse across repeated lineage genomes");
    }



    @Test
    void regulatoryGateResolutionIsDeterministic() {
        RegulatoryGateResolver resolver = new RegulatoryGateResolver();
        Artifact artifact = artifactFor(99L);
        ArtifactGenome genome = new GenomeResolver().resolve(99L);
        ArtifactMemoryProfile memory = new ArtifactMemoryProfile(6, 1.2D, 1.3D, 0.8D, 1.4D, 1.1D, 0.6D, 0.4D);

        AbilityRegulatoryProfile first = resolver.resolve(artifact, genome, memory, null, null);
        AbilityRegulatoryProfile second = resolver.resolve(artifact, genome, memory, null, null);

        assertEquals(first.profileKey(), second.profileKey());
        assertEquals(first.openGatesCsv(), second.openGatesCsv());
    }

    @Test
    void regulatoryEligibilityFilterShapesCandidatePool() {
        AbilityRegistry registry = new AbilityRegistry();
        RegulatoryEligibilityFilter filter = new RegulatoryEligibilityFilter();

        AbilityRegulatoryProfile profile = new AbilityRegulatoryProfile(java.util.Map.of(
                RegulatoryGate.SURVIVAL, RegulatoryGateState.open(0.9D),
                RegulatoryGate.MEMORY, RegulatoryGateState.open(0.9D),
                RegulatoryGate.DISCIPLINE, RegulatoryGateState.closed(0.1D),
                RegulatoryGate.VOLATILITY, RegulatoryGateState.closed(0.1D),
                RegulatoryGate.MOBILITY, RegulatoryGateState.closed(0.1D),
                RegulatoryGate.RESONANCE, RegulatoryGateState.closed(0.1D),
                RegulatoryGate.ENVIRONMENT, RegulatoryGateState.closed(0.1D),
                RegulatoryGate.LINEAGE_MILESTONE, RegulatoryGateState.closed(0.1D)
        ));

        List<AbilityTemplate> filtered = filter.filter(registry.templates(), profile);
        assertTrue(filtered.size() < registry.templates().size(), "Expected gating to reduce candidate pool");
        assertTrue(filtered.stream().anyMatch(t -> t.family() == AbilityFamily.SURVIVAL));
    }

    private Artifact artifactFor(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID());
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
