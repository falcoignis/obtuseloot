package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraitInterferenceResolver {
    private final Map<String, EnumMap<GenomeTrait, Double>> abilityWeights = new HashMap<>();

    public TraitInterferenceResolver(List<AbilityTemplate> templates) {
        registerDefaults(templates);
    }

    public List<AbilityTemplate> selectTop(List<AbilityTemplate> templates, ArtifactGenome genome, int picks) {
        return templates.stream()
                .sorted(Comparator
                        .comparingDouble((AbilityTemplate t) -> -score(t, genome))
                        .thenComparing(AbilityTemplate::id))
                .limit(Math.max(0, picks))
                .toList();
    }

    public double score(AbilityTemplate template, ArtifactGenome genome) {
        Map<GenomeTrait, Double> weights = weightsFor(template);
        double score = 0.0D;
        for (Map.Entry<GenomeTrait, Double> entry : weights.entrySet()) {
            score += genome.trait(entry.getKey()) * entry.getValue();
        }
        return score;
    }

    public Map<GenomeTrait, Double> weightsFor(AbilityTemplate template) {
        return Map.copyOf(abilityWeights.getOrDefault(template.id(), inferByFamily(template.family())));
    }

    private void registerDefaults(List<AbilityTemplate> templates) {
        for (AbilityTemplate template : templates) {
            abilityWeights.put(template.id(), inferByFamily(template.family()));
        }

        abilityWeights.put("precision.sigil", weights(GenomeTrait.PRECISION_AFFINITY, 0.70D, GenomeTrait.RESONANCE, 0.40D, GenomeTrait.VOLATILITY, -0.20D));
        abilityWeights.put("precision.gambit", weights(GenomeTrait.PRECISION_AFFINITY, 0.64D, GenomeTrait.KINETIC_BIAS, 0.30D, GenomeTrait.STABILITY, 0.18D));
        abilityWeights.put("precision.refractor", weights(GenomeTrait.PRECISION_AFFINITY, 0.58D, GenomeTrait.RESONANCE, 0.52D, GenomeTrait.VOLATILITY, -0.16D));

        abilityWeights.put("mobility.wake", weights(GenomeTrait.MOBILITY_AFFINITY, 0.65D, GenomeTrait.KINETIC_BIAS, 0.45D, GenomeTrait.STABILITY, -0.10D));
        abilityWeights.put("survival.ward", weights(GenomeTrait.SURVIVAL_INSTINCT, 0.72D, GenomeTrait.STABILITY, 0.36D, GenomeTrait.VOLATILITY, -0.22D));
        abilityWeights.put("chaos.spore", weights(GenomeTrait.CHAOS_AFFINITY, 0.80D, GenomeTrait.VOLATILITY, 0.46D, GenomeTrait.STABILITY, -0.40D));
        abilityWeights.put("chaos.paradox", weights(GenomeTrait.CHAOS_AFFINITY, 0.88D, GenomeTrait.RESONANCE, 0.30D, GenomeTrait.STABILITY, -0.30D));

        // ensure we keep only registered templates
        Map<String, EnumMap<GenomeTrait, Double>> filtered = new HashMap<>();
        for (AbilityTemplate template : templates) {
            filtered.put(template.id(), abilityWeights.getOrDefault(template.id(), inferByFamily(template.family())));
        }
        abilityWeights.clear();
        abilityWeights.putAll(filtered);
    }

    private EnumMap<GenomeTrait, Double> inferByFamily(AbilityFamily family) {
        return switch (family) {
            case PRECISION -> weights(GenomeTrait.PRECISION_AFFINITY, 0.58D, GenomeTrait.RESONANCE, 0.26D, GenomeTrait.VOLATILITY, -0.14D);
            case BRUTALITY -> weights(GenomeTrait.KINETIC_BIAS, 0.50D, GenomeTrait.VOLATILITY, 0.30D, GenomeTrait.STABILITY, -0.16D);
            case SURVIVAL -> weights(GenomeTrait.SURVIVAL_INSTINCT, 0.62D, GenomeTrait.STABILITY, 0.34D, GenomeTrait.CHAOS_AFFINITY, -0.12D);
            case MOBILITY -> weights(GenomeTrait.MOBILITY_AFFINITY, 0.60D, GenomeTrait.KINETIC_BIAS, 0.32D, GenomeTrait.STABILITY, -0.08D);
            case CHAOS -> weights(GenomeTrait.CHAOS_AFFINITY, 0.70D, GenomeTrait.VOLATILITY, 0.35D, GenomeTrait.STABILITY, -0.24D);
            case CONSISTENCY -> weights(GenomeTrait.STABILITY, 0.66D, GenomeTrait.RESONANCE, 0.30D, GenomeTrait.VOLATILITY, -0.22D);
        };
    }

    private EnumMap<GenomeTrait, Double> weights(Object... values) {
        EnumMap<GenomeTrait, Double> out = new EnumMap<>(GenomeTrait.class);
        for (int i = 0; i < values.length; i += 2) {
            GenomeTrait trait = (GenomeTrait) values[i];
            Double weight = (Double) values[i + 1];
            out.put(trait, weight);
        }
        return out;
    }
}
