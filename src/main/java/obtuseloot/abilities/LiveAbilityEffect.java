package obtuseloot.abilities;

/** A catalog-owned effect. Timing and feedback depend on its observation, never its ID. */
@FunctionalInterface
public interface LiveAbilityEffect {
    AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage);
}
