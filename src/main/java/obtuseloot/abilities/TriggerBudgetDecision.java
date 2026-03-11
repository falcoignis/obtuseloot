package obtuseloot.abilities;

public record TriggerBudgetDecision(
        boolean allowed,
        double consumed,
        double pressure,
        TriggerSuppressionReason suppressionReason
) {
    public static TriggerBudgetDecision allowed(double consumed, double pressure) {
        return new TriggerBudgetDecision(true, consumed, pressure, null);
    }

    public static TriggerBudgetDecision denied(TriggerSuppressionReason reason, double pressure) {
        return new TriggerBudgetDecision(false, 0.0D, pressure, reason == null ? TriggerSuppressionReason.UNKNOWN : reason);
    }
}
