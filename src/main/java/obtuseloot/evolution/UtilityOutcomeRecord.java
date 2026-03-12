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
        long occurredAt
) {
}

