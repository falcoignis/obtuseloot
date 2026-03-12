package obtuseloot.evolution;

import obtuseloot.lineage.ArtifactLineage;

import java.util.LinkedHashMap;
import java.util.Map;

public class LineageCompetitionModel {
    public LineageMomentumPool evaluate(Map<String, ArtifactLineage> lineages, AdaptiveSupportBudget budget) {
        if (lineages == null || lineages.isEmpty()) {
            return new LineageMomentumPool(Map.of(), 0.0D, 0.0D, 0.0D);
        }
        Map<String, LineageMomentumProfile> momentum = new LinkedHashMap<>();
        double total = 0.0D;
        double peak = 0.0D;

        for (Map.Entry<String, ArtifactLineage> entry : lineages.entrySet()) {
            ArtifactLineage lineage = entry.getValue();
            double utilityDensity = lineage.utilityDensityHistory().stream().mapToDouble(Double::doubleValue).average().orElse(0.35D);
            double branchSurvival = lineage.branchBirths() <= 0
                    ? 0.45D
                    : lineage.branchSurvivors() / (double) Math.max(1, lineage.branchBirths());
            double ecologicalCompatibility = 1.0D - clamp(lineage.ecologicalPressureHistory().stream().mapToDouble(Double::doubleValue).average().orElse(1.0D) - 1.0D, 0.0D, 1.0D);
            double outcomeEfficiency = clamp(utilityDensity * 0.70D + branchSurvival * 0.30D, 0.0D, 1.2D);
            double specializationTrajectory = lineage.specializationTrajectoryDelta();
            double nicheContribution = clamp(lineage.specializationTrajectory().stream().mapToDouble(Math::abs).average().orElse(0.15D) * 1.1D, 0.0D, 1.0D);

            double rawMomentum = Math.max(0.05D,
                    (utilityDensity * 0.40D)
                            + (branchSurvival * 0.22D)
                            + (ecologicalCompatibility * 0.15D)
                            + (outcomeEfficiency * 0.15D)
                            + (nicheContribution * 0.08D));
            total += rawMomentum;
            peak = Math.max(peak, rawMomentum);
            momentum.put(entry.getKey(), new LineageMomentumProfile(entry.getKey(), rawMomentum, utilityDensity, branchSurvival,
                    ecologicalCompatibility, outcomeEfficiency, nicheContribution, specializationTrajectory, 0.0D,
                    1.0D, 1.0D, 1.0D, 1.0D));
        }

        double dominanceShare = total <= 0.0D ? 0.0D : peak / total;
        double displacementPressure = clamp((dominanceShare - 0.42D) * 1.4D + budget.saturationIndex() * 0.25D, 0.0D, 0.95D);

        Map<String, LineageMomentumProfile> normalized = new LinkedHashMap<>();
        for (LineageMomentumProfile profile : momentum.values()) {
            double share = profile.momentum() / Math.max(0.0001D, total);
            double diminishing = clamp(1.0D - smoothDiminishing(Math.max(0.0D, share - 0.26D) + budget.saturationIndex() * 0.10D, 0.55D), 0.45D, 1.0D);
            double mutationSupport = clamp((0.78D + share * 1.20D) * diminishing + budget.explorationReserve() * 0.25D, 0.55D, 1.35D);
            double retentionSupport = clamp((0.82D + share * 0.95D) * diminishing, 0.60D, 1.25D);
            double branchSupport = clamp(0.75D + (profile.branchSurvivalRate() * 0.40D) + budget.turnoverPressure() * 0.20D, 0.65D, 1.35D);
            double templateWeight = clamp(0.80D + (profile.outcomeEfficiency() * 0.35D) - (1.0D - diminishing) * 0.22D, 0.62D, 1.30D);
            normalized.put(profile.lineageId(), new LineageMomentumProfile(
                    profile.lineageId(), share, profile.utilityDensity(), profile.branchSurvivalRate(), profile.ecologicalCompatibility(),
                    profile.outcomeEfficiency(), profile.nicheContribution(), profile.specializationTrajectory(), diminishing,
                    mutationSupport, retentionSupport, branchSupport, templateWeight));
        }

        return new LineageMomentumPool(Map.copyOf(normalized), total, dominanceShare, displacementPressure);
    }

    private double smoothDiminishing(double value, double slope) {
        return value / (value + Math.max(0.0001D, slope));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
