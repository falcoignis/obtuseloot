package obtuseloot.evolution;

import java.util.Map;

public class RoleDifferentiationHeuristics {

    public NicheSpecializationProfile specializationFor(Map<MechanicNicheTag, Double> nicheScores,
                                                        Map<String, Double> subnicheScores,
                                                        MechanicNicheTag dominantNiche) {
        double total = nicheScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double dominantScore = nicheScores.getOrDefault(dominantNiche, 0.0D);
        double concentration = total <= 0.0D ? 0.0D : dominantScore / total;
        String dominantSubniche = subnicheScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unspecialized");
        double runnerUp = subnicheScores.values().stream().sorted(java.util.Comparator.reverseOrder()).skip(1).findFirst().orElse(0.0D);
        double specializationScore = concentration + Math.max(0.0D, subnicheScores.getOrDefault(dominantSubniche, 0.0D) - runnerUp);
        return new NicheSpecializationProfile(dominantNiche, dominantSubniche, clamp01(specializationScore), clamp01(concentration));
    }

    private double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
