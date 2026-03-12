package obtuseloot.evolution;

import java.util.Map;

public class EcosystemCarryingCapacityModel {
    public AdaptiveSupportBudget calculate(Map<MechanicNicheTag, NicheUtilityRollup> rollups) {
        if (rollups == null || rollups.isEmpty()) {
            return new AdaptiveSupportBudget(1.0D, 0.70D, 1.0D, 0.70D, 0.0D, 0.0D, 0.20D);
        }
        double activeArtifacts = rollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        double totalAttempts = rollups.values().stream().mapToLong(NicheUtilityRollup::attempts).sum();
        double meaningful = rollups.values().stream().mapToLong(NicheUtilityRollup::meaningfulOutcomes).sum();
        double diversity = rollups.size();
        double avgUtilityDensity = rollups.values().stream().mapToDouble(NicheUtilityRollup::utilityDensity).average().orElse(0.0D);
        double meanYield = meaningful / Math.max(1.0D, totalAttempts);
        double churn = clamp(totalAttempts / Math.max(1.0D, activeArtifacts * 8.0D), 0.0D, 1.8D);

        double carryingCapacity = 1.0D
                + Math.sqrt(Math.max(1.0D, activeArtifacts)) * 0.08D
                + clamp(meanYield, 0.0D, 1.0D) * 0.55D
                + clamp(avgUtilityDensity, 0.0D, 1.5D) * 0.35D
                + Math.min(0.50D, diversity * 0.045D)
                + churn * 0.15D;

        double demand = 0.9D
                + (activeArtifacts * 0.11D)
                + (totalAttempts / Math.max(1.0D, activeArtifacts)) * 0.35D;
        double saturation = clamp((demand / Math.max(0.001D, carryingCapacity)) - 0.55D, 0.0D, 2.5D);
        double utilization = clamp(0.65D + (saturation * 0.25D), 0.35D, 1.0D);
        double totalBudget = carryingCapacity * (1.0D - smoothDiminishing(saturation, 0.55D));
        double utilizedBudget = totalBudget * utilization;
        double turnoverPressure = clamp(saturation * 0.34D + Math.max(0.0D, 0.30D - meanYield) * 0.40D, 0.0D, 0.95D);
        double explorationReserve = clamp(0.10D + turnoverPressure * 0.20D + Math.max(0.0D, 0.45D - meanYield) * 0.20D, 0.08D, 0.35D);

        return new AdaptiveSupportBudget(totalBudget, utilizedBudget, carryingCapacity, utilization, saturation, turnoverPressure, explorationReserve);
    }

    public double smoothDiminishing(double value, double slope) {
        double positive = Math.max(0.0D, value);
        return positive / (positive + Math.max(0.0001D, slope));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
