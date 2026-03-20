package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

public class EvolutionEngine {
    private final ArchetypeResolver archetypeResolver;
    private final HybridEvolutionResolver hybridResolver;

    public EvolutionEngine(ArchetypeResolver archetypeResolver, HybridEvolutionResolver hybridResolver) {
        this.archetypeResolver = archetypeResolver;
        this.hybridResolver = hybridResolver;
    }

    public void evaluate(Player player, Artifact artifact, ArtifactReputation reputation) {
        RuntimeSettings.Snapshot s = RuntimeSettings.get();
        if (!ArtifactEligibility.isEvolutionEligible(artifact)) {
            artifact.setEvolutionPath("generic-baseline");
            artifact.setAwakeningPath("dormant");
            artifact.setFusionPath("none");
            return;
        }
        if (reputation.getTotalScore() < s.archetypeThreshold()) {
            return;
        }
        String archetype = archetypeResolver.resolve(artifact, reputation);
        artifact.setArchetypePath(archetype);

        int total = reputation.getTotalScore();
        if (total >= s.hybridThreshold()) {
            artifact.setEvolutionPath(hybridResolver.resolve(artifact, reputation));
        } else if (total >= s.advancedThreshold()) {
            artifact.setEvolutionPath(archetype + "-advanced");
        } else if (total >= s.temperedThreshold()) {
            artifact.setEvolutionPath(archetype + "-tempered");
        } else {
            artifact.setEvolutionPath(archetype + "-formed");
        }
    }
}
