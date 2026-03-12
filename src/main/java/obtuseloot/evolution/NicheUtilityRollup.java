package obtuseloot.evolution;

public record NicheUtilityRollup(
        MechanicNicheTag niche,
        int activeArtifacts,
        long attempts,
        long meaningfulOutcomes,
        double validatedUtility,
        double budgetConsumed
) {
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
