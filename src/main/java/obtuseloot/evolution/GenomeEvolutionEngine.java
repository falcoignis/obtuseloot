package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeMutationEngine;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.*;
import obtuseloot.memory.ArtifactMemoryEngine;

/** Commits genetic feedback at explicit evolution boundaries, never during profile lookup. */
public final class GenomeEvolutionEngine {
    private final AbilityRegistry registry;
    private final ArtifactMemoryEngine memoryEngine;
    private final LineageRegistry lineages;
    private final LineageInfluenceResolver lineageResolver;
    private final ExperienceEvolutionEngine experience;
    private final TraitInterferenceResolver interference;
    private final GenomeMutationEngine mutation = new GenomeMutationEngine();
    private final LatentTraitActivationResolver activation = new LatentTraitActivationResolver();

    public GenomeEvolutionEngine() {
        this(new AbilityRegistry(), new ArtifactMemoryEngine(), null, null, null,
                ScoringMode.PROJECTION_WITH_CACHE);
    }

    public GenomeEvolutionEngine(AbilityRegistry registry, ArtifactMemoryEngine memoryEngine,
                                 LineageRegistry lineages, LineageInfluenceResolver lineageResolver,
                                 ExperienceEvolutionEngine experience,
                                 ScoringMode scoringMode) {
        this.registry = registry;
        this.memoryEngine = memoryEngine;
        this.lineages = lineages;
        this.lineageResolver = lineageResolver;
        this.experience = experience;
        this.interference = new TraitInterferenceResolver(registry.templates());
        this.interference.setScoringMode(scoringMode);
    }

    public boolean advance(Artifact artifact) {
        return advance(artifact, artifact.getArtifactSeed());
    }

    public boolean advanceReplacement(obtuseloot.artifacts.ArtifactIdentityTransition transition) {
        // Usage and local role telemetry still belong to the discarded identity until commit.
        return advance(transition.replacement(), transition.discarded().getArtifactSeed());
    }

    private boolean advance(Artifact artifact, long experienceSeed) {
        synchronized (artifact) {
            String state = artifact.evolutionStateKey();
            if (state.equals(artifact.getGenomeEvolutionState())) return false;
            long revision = Math.incrementExact(artifact.getGenomeRevision());
            int stage = ArtifactEvolutionStage.resolveStage(artifact);
            ArtifactLineage lineage = lineages == null ? null : lineages.assignLineage(artifact);
            String lineageId = lineage == null ? null : lineage.lineageId();
            double influence = lineageResolver == null ? 1.0D : lineageResolver.resolveMutationInfluence(lineage);
            AdaptiveSupportAllocation support = experience == null ? AdaptiveSupportAllocation.neutral()
                    : experience.adaptiveSupportFor(experienceSeed, lineageId, lineages);
            double pressure = experience == null ? 1.0D
                    : experience.mutationEcologyPressureFor(experienceSeed, lineageId, lineages);
            ArtifactGenome genome = mutation.mutate(artifact.getGenome(), stage, influence, pressure,
                    support.mutationOpportunity(), lineage == null ? null : lineage.evolutionaryBiasGenome().tendencies(), revision);
            // Parent inheritance happens at birth. Never replace an item's genes with lineage averages here.
            if (experience != null) {
                genome = experience.applyExperienceFeedback(genome, experienceSeed, 1,
                        influence, lineageId, lineages);
            }
            var memory = memoryEngine.profile(artifact);
            var snapshot = interference.summarizeInterference(registry.templates(), genome, interference.scoringMode());
            var context = LatentActivationContext.bounded(memory.survivalWeight() / 4.0D,
                    snapshot.latentActivationBias(), stage / 6.0D,
                    Math.min(1.0D, artifact.getDriftLevel() / 8.0D),
                    Math.min(1.0D, memory.pressure() / 12.0D));
            var result = activation.resolve(genome, context);
            artifact.setGenome(result.genome());
            artifact.restoreGenomeEvolution(revision, state);
            if (lineage != null) lineage.observeGenome(result.genome());
            artifact.setLastLatentActivationRate(result.activationRate());
            artifact.setLastActivatedLatentTraits(result.activatedTraits().toString());
            return true;
        }
    }
}
