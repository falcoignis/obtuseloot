package obtuseloot.abilities;

import obtuseloot.abilities.mutation.AbilityMutationEngine;
import obtuseloot.abilities.tree.AbilityBranchResolver;
import obtuseloot.abilities.tree.AbilityEvolutionTree;
import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.List;

public class SeededAbilityResolver implements AbilityResolver {
    private final ProceduralAbilityGenerator generator;
    private final AbilityMutationEngine mutationEngine = new AbilityMutationEngine();
    private final AbilityBranchResolver branchResolver = new AbilityBranchResolver();
    private final ArtifactMemoryEngine memoryEngine;

    public SeededAbilityResolver(AbilityRegistry registry, ArtifactMemoryEngine memoryEngine) {
        this.generator = new ProceduralAbilityGenerator(registry);
        this.memoryEngine = memoryEngine;
    }

    @Override
    public AbilityProfile resolve(Artifact artifact, ArtifactReputation reputation) {
        ArtifactMemoryProfile memoryProfile = memoryEngine.profile(artifact);
        int stage = ArtifactEvolutionStage.resolveStage(artifact);
        AbilityProfile generated = generator.generate(artifact, stage, memoryProfile);
        List<AbilityDefinition> enhanced = new ArrayList<>();
        for (AbilityDefinition d : generated.abilities()) {
            AbilityEvolutionTree tree = branchResolver.resolveTree(d.id(), artifact, memoryProfile, stage, artifact.getArchetypePath());
            String branch = tree.selectedBranch();
            enhanced.add(new AbilityDefinition(d.id(), d.name(), d.family(), d.trigger(), d.mechanic(),
                    d.effectPattern(), d.evolutionVariant(), d.driftVariant(), d.awakeningVariant(), d.fusionVariant(), d.memoryVariant(), d.supportModifiers(), d.effects(),
                    d.stage1(), d.stage2() + " [branch=" + branch + "]", d.stage3(), d.stage4(), d.stage5()));
        }
        artifact.setLastAbilityBranchPath(enhanced.stream().map(AbilityDefinition::id).toList().toString());
        artifact.setLastMutationHistory(mutationEngine.mutate(artifact, enhanced, memoryProfile, artifact.getTotalDrifts() > 0).toString());
        artifact.setLastMemoryInfluence("pressure=" + memoryProfile.pressure() + ", chaos=" + memoryProfile.chaosWeight() + ", discipline=" + memoryProfile.disciplineWeight());
        return new AbilityProfile(generated.profileId(), enhanced);
    }
}
