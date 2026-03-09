package obtuseloot.abilities;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.Map;

public final class AbilityTraitVector {
    private final String abilityId;
    private final double[] components;

    private AbilityTraitVector(String abilityId, double[] components) {
        this.abilityId = abilityId;
        this.components = components;
    }

    public static AbilityTraitVector fromWeights(String abilityId, Map<GenomeTrait, Double> weights) {
        double[] components = new double[GenomeTrait.values().length];
        for (Map.Entry<GenomeTrait, Double> entry : weights.entrySet()) {
            components[entry.getKey().ordinal()] = entry.getValue();
        }
        return new AbilityTraitVector(abilityId, components);
    }

    public static AbilityTraitVector fromWeights(Map<GenomeTrait, Double> weights) {
        return fromWeights("anonymous", weights);
    }

    public String abilityId() {
        return abilityId;
    }

    public int dimensions() {
        return components.length;
    }

    double component(int index) {
        return components[index];
    }

    double[] components() {
        return components;
    }
}
