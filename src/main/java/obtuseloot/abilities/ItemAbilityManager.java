package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAbilityManager {
    private final AbilityResolver resolver;
    private final Map<String, Integer> triggerCounts = new HashMap<>();

    public ItemAbilityManager(AbilityResolver resolver) {
        this.resolver = resolver;
    }

    public AbilityProfile profileFor(Artifact artifact, ArtifactReputation rep) {
        if (!ArtifactEligibility.isAbilityEligible(artifact)) {
            return new AbilityProfile("generic-baseline", List.of());
        }
        return resolver.resolve(artifact, rep);
    }

    public List<String> resolveEffects(AbilityEventContext context) {
        AbilityProfile profile = profileFor(context.artifact(), context.reputation());
        List<String> activated = new ArrayList<>();
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (AbilityDefinition def : profile.abilities()) {
            if (def.trigger() == context.trigger()) {
                activated.add(def.name() + " -> " + def.stageDescription(stage));
                triggerCounts.merge(def.id() + "@" + context.trigger(), 1, Integer::sum);
            }
        }
        return activated;
    }

    public Map<String, Integer> triggerCounts() {
        return Map.copyOf(triggerCounts);
    }

    public TraitProjectionStats traitProjectionStats() {
        if (resolver instanceof SeededAbilityResolver seeded) {
            return seeded.traitProjectionStats();
        }
        return new TraitProjectionStats(false, ScoringMode.BASELINE, 0, 0, 0, 0, 0, 0, 0, 0, 0.0D, 1.0D);
    }
}
