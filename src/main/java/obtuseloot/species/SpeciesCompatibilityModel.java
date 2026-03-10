package obtuseloot.species;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpeciesCompatibilityModel {
    public record DivergenceProfile(double genome,
                                    double gates,
                                    double triggers,
                                    double mechanics,
                                    double branches,
                                    double environment,
                                    double nicheOccupancy,
                                    double weightedDistance) {}
    /**
     * Compatibility distance formula (normalized [0,1]):
     * D = 0.30*Δgenome + 0.22*Δgates + 0.18*Δtriggers + 0.12*Δmechanics + 0.10*Δbranches + 0.08*Δenvironment
     *
     * Each Δ* is a normalized profile distance:
     * - genome uses mean absolute difference over fixed trait keys
     * - all others use total variation distance (0.5 * sum |p_i - q_i|) over the union of keys
     *
     * The weighted sum is clamped into [0,1], making threshold tuning backend-agnostic and easy to reason about.
     */
    public double compatibilityDistance(ArtifactPopulationSignature left, ArtifactPopulationSignature right) {
        return divergenceProfile(left, right).weightedDistance();
    }

    public DivergenceProfile divergenceProfile(ArtifactPopulationSignature left, ArtifactPopulationSignature right) {
        double genome = averageAbsoluteDifference(left.genomeTraits(), right.genomeTraits(),
                Set.of("precision", "brutality", "survival", "mobility", "chaos", "consistency"));
        double gates = profileDistance(left.gateProfile(), right.gateProfile());
        double triggers = profileDistance(left.triggerProfile(), right.triggerProfile());
        double mechanics = profileDistance(left.mechanicProfile(), right.mechanicProfile());
        double branches = profileDistance(left.branchPreferences(), right.branchPreferences());
        double environment = profileDistance(left.environmentalProfile(), right.environmentalProfile());
        double nicheOccupancy = clamp01((branches * 0.55D) + (environment * 0.30D) + (mechanics * 0.15D));

        double weighted = (genome * 0.30D)
                + (gates * 0.22D)
                + (triggers * 0.18D)
                + (mechanics * 0.12D)
                + (branches * 0.10D)
                + (environment * 0.08D);
        return new DivergenceProfile(genome, gates, triggers, mechanics, branches, environment, nicheOccupancy, clamp01(weighted));
    }

    private double averageAbsoluteDifference(Map<String, Double> left, Map<String, Double> right, Set<String> keys) {
        double sum = 0.0D;
        for (String key : keys) {
            sum += Math.abs(left.getOrDefault(key, 0.0D) - right.getOrDefault(key, 0.0D));
        }
        return keys.isEmpty() ? 0.0D : clamp01(sum / keys.size());
    }

    private double profileDistance(Map<String, Double> left, Map<String, Double> right) {
        Set<String> keys = new HashSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        if (keys.isEmpty()) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (String key : keys) {
            sum += Math.abs(left.getOrDefault(key, 0.0D) - right.getOrDefault(key, 0.0D));
        }
        return clamp01(0.5D * sum);
    }

    private double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
