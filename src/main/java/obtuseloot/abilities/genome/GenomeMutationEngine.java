package obtuseloot.abilities.genome;

import obtuseloot.lineage.LineageBiasDimension;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class GenomeMutationEngine {
    public ArtifactGenome mutate(ArtifactGenome baseGenome, int evolutionStage) {
        if (evolutionStage <= 1) {
            return baseGenome;
        }

        EnumMap<GenomeTrait, Double> mutated = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> latentMutated = new EnumMap<>(GenomeTrait.class);
        Random random = new Random(baseGenome.seed() ^ ((long) evolutionStage * 0x9E3779B97F4A7C15L));
        double sensitivity = baseGenome.trait(GenomeTrait.MUTATION_SENSITIVITY);
        double volatility = baseGenome.trait(GenomeTrait.VOLATILITY);
        double amplitude = (0.02D + (0.08D * sensitivity) + (0.06D * volatility)) * Math.min(1.0D, evolutionStage / 6.0D);

        for (Map.Entry<GenomeTrait, Double> entry : baseGenome.traits().entrySet()) {
            double delta = (random.nextDouble() - 0.5D) * 2.0D * amplitude;
            mutated.put(entry.getKey(), clamp01(entry.getValue() + delta));

            double latentDelta = (random.nextDouble() - 0.5D) * 2.0D * amplitude * 0.35D;
            latentMutated.put(entry.getKey(), clamp01(baseGenome.latentTrait(entry.getKey()) + latentDelta));
        }
        return new ArtifactGenome(baseGenome.seed(), mutated, latentMutated, baseGenome.activatedLatentTraits());
    }

    public ArtifactGenome mutate(ArtifactGenome baseGenome,
                                 int evolutionStage,
                                 double mutationInfluence,
                                 double ecologicalPressure,
                                 Map<LineageBiasDimension, Double> lineageBias) {
        if (evolutionStage <= 1) {
            return baseGenome;
        }

        EnumMap<GenomeTrait, Double> mutated = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> latentMutated = new EnumMap<>(GenomeTrait.class);
        Random random = new Random(baseGenome.seed() ^ ((long) evolutionStage * 0x9E3779B97F4A7C15L));
        double sensitivity = baseGenome.trait(GenomeTrait.MUTATION_SENSITIVITY);
        double volatility = baseGenome.trait(GenomeTrait.VOLATILITY);
        double lineageRisk = tendency(lineageBias, LineageBiasDimension.RISK_APPETITE);
        double lineageReliability = tendency(lineageBias, LineageBiasDimension.RELIABILITY);
        double lineageSpecialization = tendency(lineageBias, LineageBiasDimension.SPECIALIZATION);
        double ecologyResistance = clamp(1.0D - (Math.max(0.0D, ecologicalPressure - 1.0D) * 0.30D), 0.70D, 1.0D);
        double lineageVariance = 1.0D + (lineageRisk * 0.22D) - (lineageReliability * 0.18D);
        double effectiveInfluence = clamp(mutationInfluence * ecologyResistance, 0.74D, 1.26D);
        double amplitude = (0.02D + (0.08D * sensitivity) + (0.06D * volatility))
                * Math.min(1.0D, evolutionStage / 6.0D)
                * lineageVariance
                * effectiveInfluence;

        for (Map.Entry<GenomeTrait, Double> entry : baseGenome.traits().entrySet()) {
            double directionalBias = traitDirectionalBias(entry.getKey(), lineageSpecialization, lineageRisk, lineageReliability)
                    * effectiveInfluence;
            double delta = ((random.nextDouble() - 0.5D) * 2.0D * amplitude) + directionalBias;
            mutated.put(entry.getKey(), clamp01(entry.getValue() + delta));

            double latentDirectional = directionalBias * 0.32D;
            double latentDelta = ((random.nextDouble() - 0.5D) * 2.0D * amplitude * 0.35D) + latentDirectional;
            latentMutated.put(entry.getKey(), clamp01(baseGenome.latentTrait(entry.getKey()) + latentDelta));
        }
        return new ArtifactGenome(baseGenome.seed(), mutated, latentMutated, baseGenome.activatedLatentTraits());
    }

    private double traitDirectionalBias(GenomeTrait trait,
                                        double specializationBias,
                                        double riskBias,
                                        double reliabilityBias) {
        return switch (trait) {
            case STABILITY, RESONANCE, PRECISION_AFFINITY -> (reliabilityBias * 0.015D) + (specializationBias * 0.007D);
            case VOLATILITY, MUTATION_SENSITIVITY, CHAOS_AFFINITY -> (riskBias * 0.016D) - (reliabilityBias * 0.006D);
            case KINETIC_BIAS, MOBILITY_AFFINITY -> riskBias * 0.010D;
            case SURVIVAL_INSTINCT -> specializationBias * 0.010D;
        };
    }

    private double tendency(Map<LineageBiasDimension, Double> lineageBias, LineageBiasDimension dimension) {
        if (lineageBias == null) {
            return 0.0D;
        }
        return lineageBias.getOrDefault(dimension, 0.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
