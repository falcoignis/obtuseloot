package obtuseloot.abilities;

/** Actual response in the effect's units and game ticks of recovery committed by that attempt. */
public record AbilityPerformanceMeasurement(double benefit, long recoveryTicks) {
    public AbilityPerformanceMeasurement {
        if (!Double.isFinite(benefit) || benefit < 0 || recoveryTicks < 0)
            throw new IllegalArgumentException("Invalid measured benefit/recovery");
    }
    public double adjustUtility(double utility) {
        // Preserve measured failure penalties. No predicted gene bonus or target distribution enters fitness.
        return utility > 0 ? utility * benefit / (1 + recoveryTicks / 20.0D) : utility;
    }
}
