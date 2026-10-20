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
        String outputText,
        Double observedAmount
) {
    /** Legacy/offline classification has no observation. A measured no-op has amount zero. */
    public AbilityExecutionResult(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                  String artifactStorageKey, UUID holderId, AbilityExecutionStatus status,
                                  AbilityOutcomeType outcomeType, boolean meaningfulOutcome,
                                  String suppressionReason, String outputText) {
        this(abilityId, mechanic, trigger, artifactStorageKey, holderId, status, outcomeType,
                meaningfulOutcome, suppressionReason, outputText, null);
    }
}
