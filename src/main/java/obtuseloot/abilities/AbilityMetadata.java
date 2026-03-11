package obtuseloot.abilities;

import java.util.Set;

public record AbilityMetadata(
        Set<String> utilityDomains,
        Set<String> triggerClasses,
        Set<String> affinities,
        double discoveryValue,
        double explorationValue,
        double informationValue,
        double ritualValue,
        double socialValue,
        double worldUtilityValue
) {
    public static AbilityMetadata of(Set<String> utilityDomains,
                                     Set<String> triggerClasses,
                                     Set<String> affinities,
                                     double discoveryValue,
                                     double explorationValue,
                                     double informationValue,
                                     double ritualValue,
                                     double socialValue,
                                     double worldUtilityValue) {
        return new AbilityMetadata(Set.copyOf(utilityDomains), Set.copyOf(triggerClasses), Set.copyOf(affinities),
                discoveryValue, explorationValue, informationValue, ritualValue, socialValue, worldUtilityValue);
    }

    public boolean hasAffinity(String affinity) {
        return affinities.contains(affinity);
    }
}
