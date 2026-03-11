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
        double worldUtilityValue,
        TriggerBudgetProfile triggerBudgetProfile
) {
    public AbilityMetadata(Set<String> utilityDomains,
                           Set<String> triggerClasses,
                           Set<String> affinities,
                           double discoveryValue,
                           double explorationValue,
                           double informationValue,
                           double ritualValue,
                           double socialValue,
                           double worldUtilityValue) {
        this(utilityDomains, triggerClasses, affinities, discoveryValue, explorationValue, informationValue, ritualValue, socialValue, worldUtilityValue, null);
    }

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
                discoveryValue, explorationValue, informationValue, ritualValue, socialValue, worldUtilityValue, null);
    }

    public static AbilityMetadata of(Set<String> utilityDomains,
                                     Set<String> triggerClasses,
                                     Set<String> affinities,
                                     double discoveryValue,
                                     double explorationValue,
                                     double informationValue,
                                     double ritualValue,
                                     double socialValue,
                                     double worldUtilityValue,
                                     TriggerBudgetProfile triggerBudgetProfile) {
        return new AbilityMetadata(Set.copyOf(utilityDomains), Set.copyOf(triggerClasses), Set.copyOf(affinities),
                discoveryValue, explorationValue, informationValue, ritualValue, socialValue, worldUtilityValue, triggerBudgetProfile);
    }

    public boolean hasAffinity(String affinity) {
        return affinities.contains(affinity);
    }

    public double triggerEfficiency() {
        double utility = discoveryValue + explorationValue + informationValue + ritualValue + socialValue + worldUtilityValue;
        double cost = triggerBudgetProfile == null ? 1.0D : Math.max(0.1D, triggerBudgetProfile.triggerCost() + triggerBudgetProfile.evaluationCost());
        return utility / cost;
    }
}
