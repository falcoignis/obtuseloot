package obtuseloot.evolution;

public record NicheUtilityRollup(
        MechanicNicheTag niche,
        int activeArtifacts,
        long attempts,
        long meaningfulOutcomes,
        double validatedUtility,
        double budgetConsumed,
        double occupancy
) {
    /** Occupancy defaults to the member count. Population shares are occupancy over total occupancy. */
    public NicheUtilityRollup(MechanicNicheTag niche, int activeArtifacts, long attempts, long meaningfulOutcomes,
                              double validatedUtility, double budgetConsumed) {
        this(niche, activeArtifacts, attempts, meaningfulOutcomes, validatedUtility, budgetConsumed, activeArtifacts);
    }

    public double outcomeYield() {
        return attempts <= 0L ? 0.0D : meaningfulOutcomes / (double) attempts;
    }

    public double utilityDensity() {
        return validatedUtility / Math.max(1.0D, budgetConsumed);
    }

    public double budgetEfficiency() {
        return utilityDensity();
    }
}
