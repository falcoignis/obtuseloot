package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class EcosystemDiversityController {
    private static final double DOMINANT_SUPPRESSION_CAP = 0.05D;

    public Map<String, Double> computeAdjustments(Map<String, Integer> ecosystemFrequencies) {
        Map<String, Double> out = new LinkedHashMap<>();
        int total = ecosystemFrequencies.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0 || ecosystemFrequencies.isEmpty()) {
            return out;
        }

        ecosystemFrequencies.forEach((archetype, count) -> {
            int population = Math.max(1, count);
            double ecosystemFrequency = population / (double) total;
            double adjustment = 0.0D;

            if (ecosystemFrequency > 0.35D) {
                double suppression = clamp((ecosystemFrequency - 0.35D) * 0.1D, 0.0D, DOMINANT_SUPPRESSION_CAP);
                adjustment -= suppression;
            }
            out.put(archetype, adjustment);
        });

        return out;
    }

    public double effectiveFitness(double fitness, int nichePopulation) {
        return fitness / Math.max(1, nichePopulation);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
