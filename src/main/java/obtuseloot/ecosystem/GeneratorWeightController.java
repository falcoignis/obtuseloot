package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class GeneratorWeightController {
    private static final double MIN = 0.80D;
    private static final double MAX = 1.20D;

    private final Map<String, Double> baseGeneratorWeights = new LinkedHashMap<>();
    private final Map<String, Double> ecosystemBiasWeights = new LinkedHashMap<>();
    private final Map<String, Double> balanceAdjustmentWeights = new LinkedHashMap<>();
    private final Map<String, Double> diversityAdjustmentWeights = new LinkedHashMap<>();

    public GeneratorWeightController() {
        for (String family : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency"}) {
            baseGeneratorWeights.put(family, 1.0D);
            ecosystemBiasWeights.put(family, 1.0D);
            balanceAdjustmentWeights.put(family, 1.0D);
            diversityAdjustmentWeights.put(family, 1.0D);
        }
    }

    public void applyEcosystemBias(EcosystemBiasState state) {
        state.biasByFamily().forEach((k, v) -> ecosystemBiasWeights.put(k, clamp(1.0D + v)));
    }

    public void applyBalanceAdjustments(Map<String, Double> increments) {
        increments.forEach((family, delta) -> {
            double current = balanceAdjustmentWeights.getOrDefault(family, 1.0D);
            double next = current + delta;
            balanceAdjustmentWeights.put(family, clamp(next));
        });
    }

    public void applyDiversityAdjustments(Map<String, Double> adjustments) {
        adjustments.forEach((family, delta) -> {
            double current = diversityAdjustmentWeights.getOrDefault(family, 1.0D);
            double next = current + delta;
            diversityAdjustmentWeights.put(family, clamp(next));
        });
    }

    public double finalWeight(String family) {
        String key = family.toLowerCase();
        return baseGeneratorWeights.getOrDefault(key, 1.0D)
                * ecosystemBiasWeights.getOrDefault(key, 1.0D)
                * balanceAdjustmentWeights.getOrDefault(key, 1.0D)
                * diversityAdjustmentWeights.getOrDefault(key, 1.0D);
    }

    public Map<String, Double> ecosystemBiasWeights() { return ecosystemBiasWeights; }
    public Map<String, Double> balanceAdjustmentWeights() { return balanceAdjustmentWeights; }
    public Map<String, Double> diversityAdjustmentWeights() { return diversityAdjustmentWeights; }

    private double clamp(double value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
