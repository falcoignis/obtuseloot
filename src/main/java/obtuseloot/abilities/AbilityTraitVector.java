package obtuseloot.abilities;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.Map;

public final class AbilityTraitVector {
    private final double[] components;

    private AbilityTraitVector(double[] components) {
        this.components = components;
    }

    public static AbilityTraitVector fromWeights(Map<GenomeTrait, Double> weights) {
        double[] components = new double[GenomeTrait.values().length];
        for (Map.Entry<GenomeTrait, Double> entry : weights.entrySet()) {
            components[entry.getKey().ordinal()] = entry.getValue();
        }
        return new AbilityTraitVector(components);
    }

    double component(int index) {
        return components[index];
    }
}
