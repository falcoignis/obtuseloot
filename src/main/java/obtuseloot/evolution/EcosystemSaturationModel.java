package obtuseloot.evolution;

import java.util.Map;

public class EcosystemSaturationModel {

    public RolePressureMetrics pressureFor(MechanicNicheTag niche,
                                           NicheUtilityRollup nicheRollup,
                                           Map<MechanicNicheTag, NicheUtilityRollup> allRollups) {
        double totalPopulation = allRollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        double share = totalPopulation <= 0.0D ? 0.0D : nicheRollup.activeArtifacts() / totalPopulation;

        double meanUtilityDensity = allRollups.values().stream().mapToDouble(NicheUtilityRollup::utilityDensity).average().orElse(0.0D);
        double utilityDelta = nicheRollup.utilityDensity() - meanUtilityDensity;

        double saturationPenalty = share > 0.10D && nicheRollup.utilityDensity() < meanUtilityDensity
                ? clamp((share - 0.10D) * 1.8D, 0.0D, 0.45D)
                : 0.0D;
        double scarcityBonus = share < 0.10D && utilityDelta > 0.0D
                ? clamp((0.10D - share) * 2.4D + utilityDelta * 0.30D, 0.0D, 0.40D)
                : 0.0D;
        double specializationPressure = share > 0.20D && utilityDelta >= 0.0D
                ? clamp((share - 0.20D) * 1.3D + nicheRollup.outcomeYield() * 0.25D, 0.0D, 0.35D)
                : 0.0D;

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
