package obtuseloot.abilities;

public record TriggerBudgetProfile(
        double triggerCost,
        double evaluationCost,
        double capacity,
        double refillPerSecond,
        int burstLimit,
        long burstWindowMs,
        int priority,
        TriggerBudgetPolicy policy,
        boolean intentionalPreferred,
        double cooldownFloorMs
) {
    public static TriggerBudgetProfile defaults() {
        return new TriggerBudgetProfile(0.9D, 0.3D, 12.0D, 5.0D, 5, 900L, 50, TriggerBudgetPolicy.SOFT, false, 0.0D);
    }

    public TriggerBudgetProfile withPolicy(TriggerBudgetPolicy value) {
        return new TriggerBudgetProfile(triggerCost, evaluationCost, capacity, refillPerSecond, burstLimit, burstWindowMs, priority, value, intentionalPreferred, cooldownFloorMs);
    }
}
