package obtuseloot.simulation.worldlab;

public record OpportunityWeightedMutationConfig(boolean enabled,
                                                double maxBias,
                                                double occupancyWeight,
                                                double persistenceWeight,
                                                double noveltyWeight,
                                                double capacityWeight,
                                                double interactionWeight) {
    public static OpportunityWeightedMutationConfig defaults() {
        return new OpportunityWeightedMutationConfig(true, 0.10D, 0.03D, 0.02D, 0.02D, 0.02D, 0.01D).bounded();
    }

    public OpportunityWeightedMutationConfig bounded() {
        return new OpportunityWeightedMutationConfig(
                enabled,
                clamp(maxBias, 0.0D, 0.15D),
                clamp(occupancyWeight, 0.0D, 0.08D),
                clamp(persistenceWeight, 0.0D, 0.08D),
                clamp(noveltyWeight, 0.0D, 0.08D),
                clamp(capacityWeight, 0.0D, 0.08D),
                clamp(interactionWeight, 0.0D, 0.08D));
    }

    public double totalWeight() {
        return occupancyWeight + persistenceWeight + noveltyWeight + capacityWeight + interactionWeight;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
