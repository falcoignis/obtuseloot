package obtuseloot.abilities.mutation;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilityMutationEngine {
    public List<AbilityMutation> mutate(Artifact artifact, List<AbilityDefinition> definitions, ArtifactMemoryProfile memoryProfile, boolean driftMutation) {
        List<AbilityMutation> out = new ArrayList<>();
        boolean instabilityExceeded = artifact.hasInstability() && artifact.getDriftLevel() > 2;
        boolean chaosGrowth = "volatile".equalsIgnoreCase(artifact.getDriftAlignment()) || "paradox".equalsIgnoreCase(artifact.getDriftAlignment());
        boolean awakeningDriftInteraction = !"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && chaosGrowth;
        boolean memoryPressure = memoryProfile.pressure() >= 4;
        if (!(driftMutation || instabilityExceeded || chaosGrowth || awakeningDriftInteraction || memoryPressure)) {
            return out;
        }

        Random r = new Random(artifact.getArtifactSeed() ^ artifact.getDriftLevel() ^ artifact.getAwakeningPath().hashCode() ^ memoryProfile.pressure());
        for (AbilityDefinition definition : definitions) {
            AbilityTrigger beforeTrigger = definition.trigger();
            AbilityTrigger afterTrigger = r.nextBoolean() ? beforeTrigger : AbilityTrigger.ON_CHAIN_COMBAT;
            if (beforeTrigger != afterTrigger) {
                out.add(new AbilityMutation("trigger mutation", beforeTrigger.name(), afterTrigger.name(), "drift mutation happens"));
            }
            AbilityMechanic beforeMechanic = definition.mechanic();
            AbilityMechanic afterMechanic = chaosGrowth ? AbilityMechanic.UNSTABLE_DETONATION : beforeMechanic;
            if (beforeMechanic != afterMechanic) {
                out.add(new AbilityMutation("mechanic mutation", beforeMechanic.name(), afterMechanic.name(), "chaos alignment grows"));
            }
            if (memoryPressure) {
                out.add(new AbilityMutation("memory-driven mutation", definition.memoryVariant(), definition.memoryVariant() + " + echo", "memory pressure threshold reached"));
            }
        }
        return out;
    }
}
