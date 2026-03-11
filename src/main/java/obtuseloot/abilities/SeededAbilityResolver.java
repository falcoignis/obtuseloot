package obtuseloot.abilities;

import obtuseloot.abilities.mutation.AbilityMutationEngine;
import obtuseloot.abilities.mutation.AbilityMutationResult;
import obtuseloot.abilities.tree.AbilityBranchResolver;
import obtuseloot.abilities.tree.AbilityEvolutionTree;
import obtuseloot.artifacts.Artifact;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.evolution.ExperienceEvolutionEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SeededAbilityResolver implements AbilityResolver {
    private final ProceduralAbilityGenerator generator;
    private final AbilityMutationEngine mutationEngine = new AbilityMutationEngine();
    private final AbilityBranchResolver branchResolver = new AbilityBranchResolver();
    private final ArtifactMemoryEngine memoryEngine;
    private final LineageRegistry lineageRegistry;
    private final LineageInfluenceResolver lineageResolver;

    public SeededAbilityResolver(AbilityRegistry registry,
                                 ArtifactMemoryEngine memoryEngine,
                                 ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                 LineageRegistry lineageRegistry,
                                 LineageInfluenceResolver lineageResolver,
                                 ExperienceEvolutionEngine experienceEvolutionEngine) {
        this.generator = new ProceduralAbilityGenerator(registry, ecosystemEngine, lineageRegistry, lineageResolver, experienceEvolutionEngine);
        this.memoryEngine = memoryEngine;
        this.lineageRegistry = lineageRegistry;
        this.lineageResolver = lineageResolver;
    }

    @Override
    public AbilityProfile resolve(Artifact artifact, ArtifactReputation reputation) {
        ArtifactMemoryProfile memoryProfile = memoryEngine.profile(artifact);
        int stage = ArtifactEvolutionStage.resolveStage(artifact);
        AbilityProfile generated = generator.generate(artifact, stage, memoryProfile);
        List<AbilityDefinition> enhanced = new ArrayList<>();
        List<String> branchPath = new ArrayList<>();
        for (AbilityDefinition d : generated.abilities()) {
            AbilityEvolutionTree tree = branchResolver.resolveTree(d.id(), artifact, memoryProfile, stage, artifact.getArchetypePath());
            String branch = tree.selectedBranch();
            branchPath.add(d.id() + "->" + branch);
            enhanced.add(new AbilityDefinition(d.id(), d.name(), d.family(), d.trigger(), d.mechanic(),
                    d.effectPattern(), d.evolutionVariant(), d.driftVariant(), d.awakeningVariant(), d.fusionVariant(), d.memoryVariant(), d.supportModifiers(), d.effects(), d.metadata(),
                    d.stage1(), d.stage2() + " [branch=" + branch + "]", d.stage3(), d.stage4(), d.stage5()));
        }
        AbilityMutationResult mutationResult = mutationEngine.mutate(artifact, enhanced, memoryProfile, artifact.getTotalDrifts() > 0);
        boolean mutated = !mutationResult.mutations().isEmpty();
        ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);

        artifact.setLastAbilityBranchPath(branchPath.toString());
        artifact.setLastTriggerProfile(mutationResult.abilities().stream().map(a -> a.trigger().name()).collect(Collectors.joining(",")));
        artifact.setLastMechanicProfile(mutationResult.abilities().stream().map(a -> a.mechanic().name()).collect(Collectors.joining(",")));
        artifact.setLastMutationHistory(mutationResult.mutations().toString());
        artifact.setLastMemoryInfluence("pressure=" + memoryProfile.pressure() + ", chaos=" + memoryProfile.chaosWeight() + ", discipline=" + memoryProfile.disciplineWeight()
                + ", aggression=" + memoryProfile.aggressionWeight() + ", survival=" + memoryProfile.survivalWeight() + ", mobility=" + memoryProfile.mobilityWeight()
                + ", boss=" + memoryProfile.bossWeight() + ", trauma=" + memoryProfile.traumaWeight() + ", activeMutation=" + mutated
                + ", lineageTraits=" + lineageResolver.traitSnapshot(lineage)
                + ", templates=" + generated.abilities().stream().map(AbilityDefinition::id).toList());
        lineageRegistry.evaluateSpeciation(artifact);
        return new AbilityProfile(generated.profileId() + (mutated ? "-mutated" : ""), mutationResult.abilities());
    }

    public TraitProjectionStats traitProjectionStats() {
        return generator.traitProjectionStats();
    }
}
