package obtuseloot.abilities;

import java.util.UUID;

public record AbilityExecutionResult(
        String abilityId,
        AbilityMechanic mechanic,
        AbilityTrigger trigger,
        String artifactStorageKey,
        UUID holderId,
        AbilityExecutionStatus status,
        AbilityOutcomeType outcomeType,
        boolean meaningfulOutcome,
        String suppressionReason,
        String outputText
) {
}
