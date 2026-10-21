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
        Double observedAmount,
        String observationKey
) {
    public AbilityExecutionResult(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                  String artifactStorageKey, UUID holderId, AbilityExecutionStatus status,
                                  AbilityOutcomeType outcomeType, boolean meaningfulOutcome,
                                  String suppressionReason, String outputText, Double observedAmount) {
        this(abilityId, mechanic, trigger, artifactStorageKey, holderId, status, outcomeType,
                meaningfulOutcome, suppressionReason, outputText, observedAmount, null);
    }

    public AbilityExecutionResult withObservationKey(String key) {
        return new AbilityExecutionResult(abilityId, mechanic, trigger, artifactStorageKey, holderId, status,
                outcomeType, meaningfulOutcome, suppressionReason, outputText, observedAmount, key);
    }
    public static AbilityExecutionResult observed(AbilityDefinition definition, AbilityEventContext context,
                                                  AbilityOutcomeType type, double amount, String reason, String text) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid observation amount");
        return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(),
                amount > 0 ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP,
                type, amount > 0, reason, text, amount);
    }
    /** Legacy/offline classification has no observation. A measured no-op has amount zero. */
    public AbilityExecutionResult(String abilityId, AbilityMechanic mechanic, AbilityTrigger trigger,
                                  String artifactStorageKey, UUID holderId, AbilityExecutionStatus status,
                                  AbilityOutcomeType outcomeType, boolean meaningfulOutcome,
                                  String suppressionReason, String outputText) {
        this(abilityId, mechanic, trigger, artifactStorageKey, holderId, status, outcomeType,
                meaningfulOutcome, suppressionReason, outputText, null);
    }
}
