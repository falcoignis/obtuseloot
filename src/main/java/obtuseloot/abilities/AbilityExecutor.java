package obtuseloot.abilities;

public final class AbilityExecutor {

    public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        if (definition.trigger() != context.trigger()) {
            return new AbilityExecutionResult(
                    definition.id(),
                    definition.mechanic(),
                    context.trigger(),
                    context.artifact().getArtifactStorageKey(),
                    context.artifact().getOwnerId(),
                    AbilityExecutionStatus.SUPPRESSED,
                    AbilityOutcomeType.FLAVOR_ONLY,
                    false,
                    "trigger-mismatch",
                    null
            );
        }

        if (context.value() < 0.0D) {
            return new AbilityExecutionResult(
                    definition.id(),
                    definition.mechanic(),
                    context.trigger(),
                    context.artifact().getArtifactStorageKey(),
                    context.artifact().getOwnerId(),
                    AbilityExecutionStatus.FAILED,
                    resolveOutcomeType(definition.mechanic()),
                    false,
                    "invalid-context-value",
                    definition.name() + " -> execution failed"
            );
        }

        AbilityOutcomeType outcomeType = resolveOutcomeType(definition.mechanic());
        boolean meaningful = outcomeType != AbilityOutcomeType.FLAVOR_ONLY;
        return new AbilityExecutionResult(
                definition.id(),
                definition.mechanic(),
                context.trigger(),
                context.artifact().getArtifactStorageKey(),
                context.artifact().getOwnerId(),
                AbilityExecutionStatus.SUCCESS,
                outcomeType,
                meaningful,
                null,
                definition.name() + " -> " + definition.stageDescription(stage)
        );
    }

    private AbilityOutcomeType resolveOutcomeType(AbilityMechanic mechanic) {
        return switch (mechanic) {
            case HARVEST_RELAY -> AbilityOutcomeType.CROP_REPLANT;
            case NAVIGATION_ANCHOR -> AbilityOutcomeType.NAVIGATION_HINT;
            case SENSE_PING -> AbilityOutcomeType.STRUCTURE_SENSE;
            case MEMORY_ECHO -> AbilityOutcomeType.MEMORY_MARK;
            case SOCIAL_ATTUNEMENT, RITUAL_CHANNEL -> AbilityOutcomeType.SOCIAL_SIGNAL;
            case INSIGHT_REVEAL -> AbilityOutcomeType.INFORMATION;
            default -> AbilityOutcomeType.FLAVOR_ONLY;
        };
    }
}
