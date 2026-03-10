package obtuseloot.analytics;

import java.util.*;

public class EcosystemHealthGaugeAnalyzer {
    public record GaugeResult(double endArtifacts,
                              Double endSpecies,
                              List<Double> endTrend,
                              List<Double> tntTrend,
                              List<Double> nserTrend,
                              double latestNser,
                              EcosystemStatus status,
                              String interpretation) {}

    public GaugeResult analyze(List<Map<String, Integer>> artifactOccupancyBySeason,
                               List<Map<String, Integer>> speciesOccupancyBySeason) {
        return analyze(artifactOccupancyBySeason, speciesOccupancyBySeason, List.of());
    }

    public GaugeResult analyze(List<Map<String, Integer>> artifactOccupancyBySeason,
                               List<Map<String, Integer>> speciesOccupancyBySeason,
                               List<Double> nserTrend) {
        List<Double> endTrend = new ArrayList<>();
        for (Map<String, Integer> occupancy : artifactOccupancyBySeason) {
            endTrend.add(round4(effectiveNicheDiversity(occupancy)));
        }

        List<Double> tntTrend = new ArrayList<>();
        for (int i = 1; i < artifactOccupancyBySeason.size(); i++) {
            tntTrend.add(round4(temporalNicheTurnover(artifactOccupancyBySeason.get(i - 1), artifactOccupancyBySeason.get(i))));
        }

        double endArtifacts = endTrend.isEmpty() ? 0.0D : endTrend.get(endTrend.size() - 1);
        Double endSpecies = null;
        if (speciesOccupancyBySeason != null && !speciesOccupancyBySeason.isEmpty()) {
            endSpecies = round4(effectiveNicheDiversity(speciesOccupancyBySeason.get(speciesOccupancyBySeason.size() - 1)));
        }
        double tnt = tntTrend.isEmpty() ? 0.0D : tntTrend.get(tntTrend.size() - 1);
        double latestNser = nserTrend == null || nserTrend.isEmpty() ? 0.0D : nserTrend.get(nserTrend.size() - 1);
        EcosystemStatus status = classify(endArtifacts, tnt);
        return new GaugeResult(endArtifacts, endSpecies, endTrend, tntTrend,
                nserTrend == null ? List.of() : List.copyOf(nserTrend), latestNser,
                status, interpretation(endArtifacts, tnt, latestNser, status));
    }

    public double effectiveNicheDiversity(Map<String, Integer> occupancy) {
        if (occupancy == null || occupancy.isEmpty()) {
            return 0.0D;
        }
        double total = occupancy.values().stream().mapToDouble(Integer::doubleValue).sum();
        if (total <= 0.0D) {
            return 0.0D;
        }
        double entropy = 0.0D;
        for (int count : occupancy.values()) {
            if (count <= 0) {
                continue;
            }
            double p = count / total;
            entropy -= p * Math.log(p);
        }
        return Math.exp(entropy);
    }

    public double temporalNicheTurnover(Map<String, Integer> previous, Map<String, Integer> next) {
        Map<String, Double> p = normalize(previous);
        Map<String, Double> n = normalize(next);
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(p.keySet());
        keys.addAll(n.keySet());
        double sum = 0.0D;
        for (String key : keys) {
            sum += Math.abs(n.getOrDefault(key, 0.0D) - p.getOrDefault(key, 0.0D));
        }
        return Math.max(0.0D, Math.min(1.0D, 0.5D * sum));
    }

    public EcosystemStatus classify(double end, double tnt) {
        if (end < 1.5D) {
            return tnt >= 0.40D ? EcosystemStatus.COLLAPSED : EcosystemStatus.STAGNANT;
        }
        if (end < 2.5D) {
            return tnt >= 0.60D ? EcosystemStatus.TURBULENT : EcosystemStatus.EARLY_DIVERGENCE;
        }
        if (end < 3.5D) {
            return tnt >= 0.60D ? EcosystemStatus.TURBULENT : EcosystemStatus.EARLY_DIVERGENCE;
        }
        if (end < 5.0D) {
            if (tnt < 0.40D) {
                return EcosystemStatus.HEALTHY_ECOSYSTEM;
            }
            return tnt >= 0.60D ? EcosystemStatus.FRAGMENTED : EcosystemStatus.TURBULENT;
        }
        if (tnt >= 0.60D) {
            return EcosystemStatus.FRAGMENTED;
        }
        return tnt >= 0.40D ? EcosystemStatus.TURBULENT : EcosystemStatus.HEALTHY_ECOSYSTEM;
    }

    private String interpretation(double end, double tnt, double nser, EcosystemStatus status) {
        String novelty = nser < 0.05D ? " Novelty signal indicates mostly strategy reshuffling."
                : nser < 0.15D ? " Novelty signal is weak but non-zero."
                : nser < 0.30D ? " Novelty signal is healthy and bounded."
                : nser < 0.50D ? " Novelty signal shows a strong innovation phase."
                : " Novelty signal is very high and may reflect over-fragmentation.";
        return switch (status) {
            case COLLAPSED -> "Low END with high TNT indicates unstable collapse or ecological thrashing." + novelty;
            case STAGNANT -> "Low END with low TNT indicates monoculture stagnation." + novelty;
            case EARLY_DIVERGENCE -> "END suggests early ecological differentiation with limited attractors." + novelty;
            case HEALTHY_ECOSYSTEM -> "END/TNT indicate a stable multi-niche ecosystem that is still active." + novelty;
            case TURBULENT -> "TNT indicates unstable or highly rotating niches despite some diversity." + novelty;
            case FRAGMENTED -> "Very high turnover with high END suggests chaotic or fragmented ecology." + novelty;
        };
    }

    private Map<String, Double> normalize(Map<String, Integer> occupancy) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (occupancy == null || occupancy.isEmpty()) {
            return out;
        }
        double total = occupancy.values().stream().mapToDouble(Integer::doubleValue).sum();
        if (total <= 0.0D) {
            return out;
        }
        for (Map.Entry<String, Integer> entry : occupancy.entrySet()) {
            if (entry.getValue() > 0) {
                out.put(entry.getKey(), entry.getValue() / total);
            }
        }
        return out;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0D) / 10000.0D;
    }
}
