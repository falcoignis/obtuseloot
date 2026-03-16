package obtuseloot.evolution;

import java.util.Map;

public class EcosystemSaturationModel {

    static final double SATURATION_THRESHOLD = 0.20D;
    static final double SPECIALIZATION_THRESHOLD = 0.079D;

    public RolePressureMetrics pressureFor(MechanicNicheTag niche,
                                           NicheUtilityRollup nicheRollup,
                                           Map<MechanicNicheTag, NicheUtilityRollup> allRollups) {
        double totalPopulation = allRollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        double share = totalPopulation <= 0.0D ? 0.0D : nicheRollup.activeArtifacts() / totalPopulation;

        double meanUtilityDensity = allRollups.values().stream().mapToDouble(NicheUtilityRollup::utilityDensity).average().orElse(0.0D);
        double utilityDelta = nicheRollup.utilityDensity() - meanUtilityDensity;

        double saturationPenalty = clamp(share - SATURATION_THRESHOLD, 0.0D, 0.45D);
        double scarcityBonus = share < 0.10D && utilityDelta > 0.0D
                ? clamp((0.10D - share) * 2.4D + utilityDelta * 0.30D, 0.0D, 0.40D)
                : 0.0D;

        // Internal differentiation signal, intentionally independent from utility-density comparisons.
        double specializationScore = clamp(nicheRollup.outcomeYield(), 0.0D, 1.0D);
        double specializationPressure = clamp(specializationScore - SPECIALIZATION_THRESHOLD, 0.0D, 0.35D);

        double diversityIncentive = clamp((1.0D - share) * 0.08D, 0.0D, 0.08D);
        double ecologicalRepulsion = share > 0.18D && nicheRollup.outcomeYield() < 0.25D
                ? clamp((share - 0.18D) * 1.4D + (0.25D - nicheRollup.outcomeYield()) * 0.7D, 0.0D, 0.50D)
                : 0.0D;

        double retentionBias = clamp(1.0D + scarcityBonus + (utilityDelta * 0.25D) - saturationPenalty - (ecologicalRepulsion * 0.5D), 0.70D, 1.35D);
        double mutationBias = clamp(1.0D + scarcityBonus + specializationPressure - saturationPenalty - ecologicalRepulsion, 0.60D, 1.50D);

        return new RolePressureMetrics(saturationPenalty, scarcityBonus, diversityIncentive, ecologicalRepulsion, specializationPressure, retentionBias, mutationBias);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
