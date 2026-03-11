package obtuseloot.abilities;

public final class TriggerBudgetResolver {

    public TriggerBudgetProfile resolve(AbilityDefinition ability, AbilityEventContext context) {
        TriggerBudgetProfile metadataProfile = ability.metadata() == null ? null : ability.metadata().triggerBudgetProfile();
        TriggerBudgetProfile base = metadataProfile == null ? defaultsFor(ability, context.trigger()) : metadataProfile;
        if (isIntentional(context.trigger(), context.source())) {
            return new TriggerBudgetProfile(
                    base.triggerCost() * 0.85D,
                    base.evaluationCost(),
                    base.capacity(),
                    base.refillPerSecond(),
                    base.burstLimit(),
                    base.burstWindowMs(),
                    Math.max(base.priority(), 80),
                    base.policy() == TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY ? TriggerBudgetPolicy.ACTIVE_INTENTIONAL : base.policy(),
                    true,
                    base.cooldownFloorMs());
        }
        return base;
    }

    private TriggerBudgetProfile defaultsFor(AbilityDefinition ability, AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_MOVEMENT, ON_WORLD_SCAN -> new TriggerBudgetProfile(1.3D, 0.8D, 10.0D, 3.2D, 3, 1200L, 35, TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY, false, 250.0D);
            case ON_STRUCTURE_SENSE -> new TriggerBudgetProfile(2.2D, 1.1D, 9.0D, 2.4D, 2, 2000L, 30, TriggerBudgetPolicy.STRICT, false, 450.0D);
            case ON_MEMORY_EVENT, ON_WITNESS_EVENT -> new TriggerBudgetProfile(1.1D, 0.5D, 11.0D, 3.8D, 3, 1300L, 45, TriggerBudgetPolicy.SOFT, false, 200.0D);
            case ON_RITUAL_INTERACT, ON_ENTITY_INSPECT, ON_BLOCK_INSPECT, ON_BLOCK_HARVEST, ON_SOCIAL_INTERACT -> new TriggerBudgetProfile(0.8D, 0.2D, 14.0D, 6.5D, 6, 700L, 85, TriggerBudgetPolicy.ACTIVE_INTENTIONAL, true, 60.0D);
            default -> {
                TriggerBudgetPolicy policy = ability.trigger() == AbilityTrigger.ON_HIT || ability.trigger() == AbilityTrigger.ON_KILL
                        ? TriggerBudgetPolicy.CRITICAL_SYSTEM
                        : TriggerBudgetPolicy.SOFT;
                int priority = policy == TriggerBudgetPolicy.CRITICAL_SYSTEM ? 100 : 60;
                yield new TriggerBudgetProfile(1.0D, 0.4D, 12.0D, 5.0D, 4, 1000L, priority, policy, false, 120.0D);
            }
        };
    }

    private boolean isIntentional(AbilityTrigger trigger, String source) {
        if (trigger == AbilityTrigger.ON_RITUAL_INTERACT || trigger == AbilityTrigger.ON_ENTITY_INSPECT
                || trigger == AbilityTrigger.ON_BLOCK_INSPECT || trigger == AbilityTrigger.ON_BLOCK_HARVEST
                || trigger == AbilityTrigger.ON_SOCIAL_INTERACT) {
            return true;
        }
        return source != null && (source.contains("inspect") || source.contains("gesture") || source.contains("interact") || source.contains("harvest"));
    }
}
