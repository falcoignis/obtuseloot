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
        AbilityExecutionStatus status = meaningful ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP;
        return new AbilityExecutionResult(
                definition.id(),
                definition.mechanic(),
                context.trigger(),
                context.artifact().getArtifactStorageKey(),
                context.artifact().getOwnerId(),
                status,
                outcomeType,
                meaningful,
                null,
                definition.name() + " -> " + definition.stageDescription(stage)
        );
    }

    private AbilityOutcomeType resolveOutcomeType(AbilityMechanic mechanic) {
        return switch (mechanic) {
            case HARVEST_RELAY, FORAGING_MEMORY, CLUSTER_INTUITION -> AbilityOutcomeType.CROP_REPLANT;
            case NAVIGATION_ANCHOR, ECOLOGICAL_PATHING, CARTOGRAPHIC_ECHO, TERRAIN_ADAPTATION -> AbilityOutcomeType.NAVIGATION_HINT;
            case SENSE_PING, BIOME_RESONANCE, WEATHER_ATTUNEMENT, STRUCTURE_ATTUNEMENT -> AbilityOutcomeType.STRUCTURE_SENSE;
            case MEMORY_ECHO, WITNESS_IMPRINT, ALTAR_SIGNAL_BOOST -> AbilityOutcomeType.MEMORY_MARK;
            case SOCIAL_ATTUNEMENT, RITUAL_CHANNEL, RITUAL_STABILIZATION, COLLECTIVE_RELAY, TRADE_SCENT -> AbilityOutcomeType.SOCIAL_SIGNAL;
            case INSIGHT_REVEAL, RESOURCE_ECOLOGY_SCAN, TEMPORAL_SPECIALIZATION -> AbilityOutcomeType.INFORMATION;
            default -> AbilityOutcomeType.FLAVOR_ONLY;
        };
    }
}
