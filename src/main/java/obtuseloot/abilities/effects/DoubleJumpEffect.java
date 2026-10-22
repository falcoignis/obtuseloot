package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.movement.AirJumpRequest;

/** A catalog entry, with the same six-slot permission and identity checks as other event effects. */
public final class DoubleJumpEffect implements LiveAbilityEffect {
    @Override public boolean enabledForSelection() { return obtuseloot.config.RuntimeSettings.get().doubleJumpEnabled(); }
    @Override public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        boolean applied = context.liveExecution() && context.activation() != null
                && context.activation().input().kind() == AbilityInput.Kind.JUMP
                && context.liveEvent() instanceof AirJumpRequest request && request.apply(definition, context);
        // The write is not a measured success. The coordinator observes native velocity and subsequent movement.
        return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(),
                applied ? AbilityExecutionStatus.PENDING : AbilityExecutionStatus.SUPPRESSED, AbilityOutcomeType.WORLD_INTERACTION, false,
                applied ? "movement-observation-pending" : "air-jump-unavailable", null);
    }
}
