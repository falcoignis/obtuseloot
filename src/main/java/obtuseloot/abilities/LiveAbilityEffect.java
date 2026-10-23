package obtuseloot.abilities;

/** A catalog-owned effect. Timing and feedback depend on its observation, never its ID. */
@FunctionalInterface
public interface LiveAbilityEffect {
    default boolean enabledForSelection() { return true; }
    default AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage,
                                            AbilityPerformance performance) {
        return execute(definition, context, stage);
    }
    AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage);
}
