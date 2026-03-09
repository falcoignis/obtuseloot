package obtuseloot.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RankAbundanceAnalyzer {
    public RankAbundanceResult analyze(Map<String, Integer> distribution) {
        List<Map.Entry<String, Integer>> sorted = distribution.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .toList();

        LinkedHashMap<String, Integer> rankedAbundance = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            rankedAbundance.put(entry.getKey(), entry.getValue());
        }

        int total = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        int richness = sorted.size();
        double entropy = shannonEntropy(sorted, total);
        double evenness = richness > 1 ? entropy / Math.log(richness) : 1.0;
        double dominanceRatio = total == 0 || sorted.isEmpty() ? 0.0 : sorted.getFirst().getValue() / (double) total;
        double variance = variance(sorted);
        double concentration = concentration(sorted, total);

        return new RankAbundanceResult(rankedAbundance, total, richness, entropy, evenness, dominanceRatio, variance, concentration);
    }

    private double shannonEntropy(List<Map.Entry<String, Integer>> sorted, int total) {
        if (total == 0) {
            return 0.0;
        }
        double entropy = 0.0;
        for (Map.Entry<String, Integer> entry : sorted) {
            double p = entry.getValue() / (double) total;
            entropy -= p * Math.log(p);
        }
        return entropy;
    }

    private double variance(List<Map.Entry<String, Integer>> sorted) {
        if (sorted.isEmpty()) {
            return 0.0;
        }
        List<Integer> values = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            values.add(entry.getValue());
        }
        double mean = values.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0);
        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
    }

    private double concentration(List<Map.Entry<String, Integer>> sorted, int total) {
        if (total == 0) {
            return 0.0;
        }
        double hhi = 0.0;
        for (Map.Entry<String, Integer> entry : sorted) {
            double p = entry.getValue() / (double) total;
            hhi += p * p;
        }
        return hhi;
    }

    public record RankAbundanceResult(
            LinkedHashMap<String, Integer> rankedAbundance,
            int totalCount,
            int richness,
            double entropy,
            double evenness,
            double dominanceRatio,
            double variance,
            double concentration
    ) {
    }
}
