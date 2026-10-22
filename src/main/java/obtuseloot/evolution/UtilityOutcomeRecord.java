package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;

public record UtilityOutcomeRecord(
        String abilityId,
        AbilityMechanic mechanic,
        AbilityTrigger trigger,
        AbilityExecutionStatus status,
        AbilityOutcomeType outcomeType,
        boolean meaningfulOutcome,
        boolean intentional,
        double contextualRelevance,
        double budgetCost,
        String source,
        long occurredAt,
        String observationKey,
        obtuseloot.abilities.AbilityPerformanceMeasurement performance,
        obtuseloot.abilities.genome.GenomeRevision genomeRevision
) {
    public UtilityOutcomeRecord(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                 AbilityExecutionStatus status, AbilityOutcomeType outcomeType,
                                 boolean meaningfulOutcome, boolean intentional, double contextualRelevance,
                                 double budgetCost, String source, long occurredAt, String observationKey,
                                 obtuseloot.abilities.AbilityPerformanceMeasurement performance) {
        this(abilityId, mechanic, trigger, status, outcomeType, meaningfulOutcome, intentional,
                contextualRelevance, budgetCost, source, occurredAt, observationKey, performance, null);
    }
    public UtilityOutcomeRecord(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                 AbilityExecutionStatus status, AbilityOutcomeType outcomeType,
                                 boolean meaningfulOutcome, boolean intentional, double contextualRelevance,
                                 double budgetCost, String source, long occurredAt, String observationKey) {
        this(abilityId, mechanic, trigger, status, outcomeType, meaningfulOutcome, intentional,
                contextualRelevance, budgetCost, source, occurredAt, observationKey, null);
    }
    public UtilityOutcomeRecord(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                 AbilityExecutionStatus status, AbilityOutcomeType outcomeType,
                                 boolean meaningfulOutcome, boolean intentional, double contextualRelevance,
                                 double budgetCost, String source, long occurredAt) {
        this(abilityId, mechanic, trigger, status, outcomeType, meaningfulOutcome, intentional,
                contextualRelevance, budgetCost, source, occurredAt, null);
    }
}

