package obtuseloot.simulation.worldlab;

public record AdaptiveNicheCapacityConfig(boolean enabled,
                                          double minCapacity,
                                          double maxCapacity,
                                          double noveltyWeight,
                                          double diversityWeight,
                                          double persistenceWeight,
                                          double overcrowdingWeight,
                                          double stagnationWeight,
                                          double maxSeasonDelta) {
    public static AdaptiveNicheCapacityConfig defaults() {
        return new AdaptiveNicheCapacityConfig(true, 0.80D, 1.25D, 0.03D, 0.02D, 0.02D, 0.03D, 0.03D, 0.05D);
    }

    public AdaptiveNicheCapacityConfig bounded() {
        double boundedMin = clamp(minCapacity, 0.50D, 1.10D);
        double boundedMax = clamp(maxCapacity, Math.max(1.0D, boundedMin + 0.01D), 1.50D);
        return new AdaptiveNicheCapacityConfig(
                enabled,
                boundedMin,
                boundedMax,
                clamp(noveltyWeight, 0.0D, 0.10D),
                clamp(diversityWeight, 0.0D, 0.10D),
                clamp(persistenceWeight, 0.0D, 0.10D),
                clamp(overcrowdingWeight, 0.0D, 0.10D),
                clamp(stagnationWeight, 0.0D, 0.10D),
                clamp(maxSeasonDelta, 0.005D, 0.10D));
    }

    public double baselineCapacity() {
        return clamp(1.0D, minCapacity, maxCapacity);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
