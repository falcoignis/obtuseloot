package obtuseloot.simulation.worldlab;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class OpportunityWeightedMutationEngine {
    public record OpportunitySignal(String role,
                                    double occupancyScarcity,
                                    double persistenceScarcity,
                                    double noveltyScarcity,
                                    double capacityScarcity,
                                    double interactionScarcity,
                                    double score,
                                    double normalized,
                                    double mutationBias,
                                    double latentBias) {}

    private final OpportunityWeightedMutationConfig config;
    private Map<String, OpportunitySignal> latestSignals = Map.of();

    public OpportunityWeightedMutationEngine(OpportunityWeightedMutationConfig config) {
        this.config = config == null ? OpportunityWeightedMutationConfig.defaults() : config.bounded();
    }

    public OpportunityWeightedMutationConfig config() {
        return config;
    }

    public Map<String, OpportunitySignal> updateSignals(Map<String, Object> roleAxisDistribution,
                                                        Map<String, Object> crowdingDistribution,
                                                        Map<String, Object> coEvolutionRelationships,
                                                        Map<String, Object> adaptiveCapacityDistribution,
                                                        double latestNser) {
        Map<String, Double> occupancy = toDoubleMap(crowdingDistribution.get("occupancyByNiche"));
        Map<String, Double> persistence = toDoubleMap(adaptiveCapacityDistribution.get("nichePersistence"));
        Map<String, Double> novelty = toDoubleMap(adaptiveCapacityDistribution.get("nicheNovelty"));
        Map<String, Double> capacityUse = toDoubleMap(adaptiveCapacityDistribution.get("nicheCapacityUtilization"));
        Map<String, Double> interaction = toDoubleMap(roleAxisDistribution.get("interaction_heavy_vs_solo"));

        Map<String, OpportunitySignal> out = new LinkedHashMap<>();
        double maxRaw = Math.max(0.0001D, config.totalWeight());
        for (String role : occupancy.keySet()) {
            double occupancyScarcity = clamp01(1.0D - occupancy.getOrDefault(role, 0.0D));
            double persistenceScarcity = clamp01(1.0D - persistence.getOrDefault(role, 0.0D));
            double noveltyScarcity = clamp01(1.0D - clamp01((novelty.getOrDefault(role, 0.0D) * 0.8D) + (latestNser * 0.2D)));
            double capacityScarcity = clamp01(1.0D - capacityUse.getOrDefault(role, 0.0D));
            double interactionScarcity = clamp01(1.0D - interaction.getOrDefault(role, 0.0D));
            double score = (occupancyScarcity * config.occupancyWeight())
                    + (persistenceScarcity * config.persistenceWeight())
                    + (noveltyScarcity * config.noveltyWeight())
                    + (capacityScarcity * config.capacityWeight())
                    + (interactionScarcity * config.interactionWeight());
            double normalized = clamp01(score / maxRaw);
            double boundedTilt = config.enabled() ? clamp(config.maxBias() * normalized, 0.0D, config.maxBias()) : 0.0D;
            double mutationBias = 1.0D + boundedTilt;
            double latentBias = 1.0D + (boundedTilt * 0.6D);
            out.put(role, new OpportunitySignal(
                    role,
                    occupancyScarcity,
                    persistenceScarcity,
                    noveltyScarcity,
                    capacityScarcity,
                    interactionScarcity,
                    clamp(score, 0.0D, maxRaw),
                    normalized,
                    mutationBias,
                    latentBias));
        }
        latestSignals = out;
        return latestSignals;
    }

    public OpportunitySignal signalForRole(String role) {
        if (role == null || role.isBlank()) {
            return baseline("unassigned");
        }
        return latestSignals.getOrDefault(role.toLowerCase(Locale.ROOT), baseline(role.toLowerCase(Locale.ROOT)));
    }

    public Map<String, Object> diagnostics() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", config.enabled());
        out.put("maxBias", config.maxBias());
        out.put("weights", Map.of(
                "occupancyWeight", config.occupancyWeight(),
                "persistenceWeight", config.persistenceWeight(),
                "noveltyWeight", config.noveltyWeight(),
                "capacityWeight", config.capacityWeight(),
                "interactionWeight", config.interactionWeight()));
        out.put("topOpportunityRoles", latestSignals.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(5)
                .map(signal -> Map.of(
                        "role", signal.role(),
                        "score", signal.score(),
                        "normalized", signal.normalized(),
                        "mutationBias", signal.mutationBias(),
                        "latentBias", signal.latentBias()))
                .toList());
        return out;
    }

    private OpportunitySignal baseline(String role) {
        return new OpportunitySignal(role, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D);
    }

    private Map<String, Double> toDoubleMap(Object obj) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (!(obj instanceof Map<?, ?> map)) {
            return out;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            Object v = entry.getValue();
            if (v instanceof Number number) {
                out.put(key, clamp01(number.doubleValue()));
            }
        }
        return out;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }
}
