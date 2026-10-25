package obtuseloot.abilities;

/** Actual response in the effect's units and game ticks of recovery committed by that attempt. */
public record AbilityPerformanceMeasurement(double benefit, long recoveryTicks) {
    public AbilityPerformanceMeasurement {
        if (!Double.isFinite(benefit) || benefit < 0 || recoveryTicks < 0)
            throw new IllegalArgumentException("Invalid measured benefit/recovery");
    }
    public double adjustUtility(double utility) {
        // Preserve measured failure penalties. No predicted gene bonus or target distribution enters fitness.
        // Recovery is not divided out here: it already costs uses within the evidence window.
        return utility > 0 ? utility * benefit : utility;
    }
}
