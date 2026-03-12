package obtuseloot.lineage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class EvolutionaryBiasGenome {
    private final EnumMap<LineageBiasDimension, Double> tendencies = new EnumMap<>(LineageBiasDimension.class);

    public EvolutionaryBiasGenome() {
        for (LineageBiasDimension dimension : LineageBiasDimension.values()) {
            tendencies.put(dimension, 0.0D);
        }
    }

    public static EvolutionaryBiasGenome seeded(long seed) {
        EvolutionaryBiasGenome genome = new EvolutionaryBiasGenome();
        Random random = new Random(seed ^ 0x6F3A6D2CL);
        for (LineageBiasDimension dimension : LineageBiasDimension.values()) {
            genome.tendencies.put(dimension, clamp((random.nextDouble() - 0.5D) * 0.36D));
        }
        return genome;
    }

    public void mergeToward(EvolutionaryBiasGenome observed, double strength) {
        if (observed == null) {
            return;
        }
        for (LineageBiasDimension dimension : LineageBiasDimension.values()) {
            double current = tendencies.getOrDefault(dimension, 0.0D);
            double target = observed.tendency(dimension);
            tendencies.put(dimension, clamp(current + ((target - current) * strength)));
        }
    }

    public void applyDrift(Random random, double driftWindow) {
        for (LineageBiasDimension dimension : LineageBiasDimension.values()) {
            double drift = (random.nextDouble() - 0.5D) * driftWindow;
            tendencies.put(dimension, clamp(tendencies.getOrDefault(dimension, 0.0D) + drift));
        }
    }

    public void add(LineageBiasDimension dimension, double delta) {
        tendencies.put(dimension, clamp(tendencies.getOrDefault(dimension, 0.0D) + delta));
    }

    public double tendency(LineageBiasDimension dimension) {
        return tendencies.getOrDefault(dimension, 0.0D);
    }

    public Map<LineageBiasDimension, Double> tendencies() {
        return Map.copyOf(tendencies);
    }

    public EvolutionaryBiasGenome copy() {
        EvolutionaryBiasGenome copy = new EvolutionaryBiasGenome();
        for (Map.Entry<LineageBiasDimension, Double> entry : tendencies.entrySet()) {
            copy.tendencies.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private static double clamp(double value) {
        return Math.max(-0.35D, Math.min(0.35D, value));
    }
}
