package obtuseloot.abilities;

import obtuseloot.ObtuseLoot;

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
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.evolution.NicheVariantProfile;
import obtuseloot.evolution.ArtifactNicheProfile;
import obtuseloot.evolution.EcosystemRoleClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SeededAbilityResolver implements AbilityResolver {
    private final ProceduralAbilityGenerator generator;
    private final AbilityRegistry registry;
    private final AbilityMutationEngine mutationEngine = new AbilityMutationEngine();
    private final AbilityBranchResolver branchResolver = new AbilityBranchResolver();
    private final ArtifactMemoryEngine memoryEngine;
    private final LineageRegistry lineageRegistry;
    private final LineageInfluenceResolver lineageResolver;
    private final EcosystemRoleClassifier roleClassifier = new EcosystemRoleClassifier();

    public SeededAbilityResolver(AbilityRegistry registry,
                                 ArtifactMemoryEngine memoryEngine,
                                 ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                 LineageRegistry lineageRegistry,
                                 LineageInfluenceResolver lineageResolver,
                                 ExperienceEvolutionEngine experienceEvolutionEngine) {
        this.registry = registry;
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
                    d.effectPattern(), d.evolutionVariant(), d.driftVariant(), d.awakeningVariant(), d.convergenceVariant(), d.memoryVariant(), d.supportModifiers(), d.effects(), d.metadata(),
                    d.stage1(), d.stage2() + " [branch=" + branch + "]", d.stage3(), d.stage4(), d.stage5()));
        }
        ArtifactNicheProfile nicheProfile = roleClassifier.classify(UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).signalByMechanicTrigger());
        NicheVariantProfile variantProfile = ObtuseLoot.get() == null ? null : ObtuseLoot.get().getArtifactUsageTracker().nichePopulationTracker().variantFor(artifact.getArtifactSeed());
        AbilityMutationResult mutationResult = mutationEngine.mutate(artifact, enhanced, memoryProfile, artifact.getTotalDrifts() > 0, UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()), lineageRegistry.assignLineage(artifact), variantProfile);
        var originals = enhanced.stream().collect(Collectors.toMap(AbilityDefinition::id, definition -> definition));
        List<String> unsupportedVariants = new ArrayList<>();
        List<AbilityDefinition> executableAbilities = mutationResult.abilities().stream().map(variant -> {
            AbilityDefinition supported = registry.executableVariant(variant, originals.get(variant.id()));
            if (supported != variant) unsupportedVariants.add(variant.id());
            return supported;
        }).toList();
        boolean mutated = !mutationResult.mutations().isEmpty();
        ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
        AbilityDiversityIndex.instance().recordDefinitions(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), nicheProfile.dominantNiche(), variantProfile, executableAbilities);

        artifact.setLastAbilityBranchPath(branchPath.toString());
        artifact.setLastTriggerProfile(executableAbilities.stream().map(a -> a.trigger().name()).collect(Collectors.joining(",")));
        artifact.setLastMechanicProfile(executableAbilities.stream().map(a -> a.mechanic().name()).collect(Collectors.joining(",")));
        artifact.setLastMutationHistory(mutationResult.mutations() + (unsupportedVariants.isEmpty() ? ""
                : " | unimplemented variants retained base effect: " + unsupportedVariants));
        artifact.setLastMemoryInfluence("pressure=" + memoryProfile.pressure() + ", chaos=" + memoryProfile.chaosWeight() + ", discipline=" + memoryProfile.disciplineWeight()
                + ", aggression=" + memoryProfile.aggressionWeight() + ", survival=" + memoryProfile.survivalWeight() + ", mobility=" + memoryProfile.mobilityWeight()
                + ", boss=" + memoryProfile.bossWeight() + ", trauma=" + memoryProfile.traumaWeight() + ", activeMutation=" + mutated
                + ", lineageTraits=" + lineageResolver.traitSnapshot(lineage)
                + ", templates=" + generated.abilities().stream().map(AbilityDefinition::id).toList());
        lineageRegistry.evaluateSpeciation(artifact);
        return new AbilityProfile(generated.profileId() + (mutated ? "-mutated" : ""), executableAbilities);
    }

    public TraitProjectionStats traitProjectionStats() {
        return generator.traitProjectionStats();
    }
}
