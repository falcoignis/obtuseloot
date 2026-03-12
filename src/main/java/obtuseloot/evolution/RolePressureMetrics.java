package obtuseloot.evolution;

public record RolePressureMetrics(
        double saturationPenalty,
        double scarcityBonus,
        double diversityIncentive,
        double ecologicalRepulsion,
        double specializationPressure,
        double retentionBias,
        double mutationBias
) {
    public double netPressure() {
        return scarcityBonus + diversityIncentive - saturationPenalty - ecologicalRepulsion;
    }

    public double templateWeightModifier() {
        return clamp(1.0D + netPressure() + (specializationPressure * 0.5D), 0.65D, 1.45D);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
